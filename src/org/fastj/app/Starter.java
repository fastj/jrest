package org.fastj.app;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.fastj.jetty.RestHandler;
import org.fastj.log.FileLoggor;
import org.fastj.log.LogUtil;
import org.fastj.rest.api.Responses;
import org.fastj.rest.api.SecurityChecker;
import org.fastj.rest.api.ServiceManager;
import org.fastj.rest.api.Tracer;
import org.fastj.util.ScanLoader;
import org.fastj.util.SysUtil;

import static org.fastj.app.Args.*;

public class Starter {

	public static void main(String[] args) throws IOException {

		parse(args);
		LogUtil.setLevel(get(ARG_LOG_LEVEL, DEF_LOG_LEVEL));

		new File(DEF_LOG_DIR).mkdirs();
		LogUtil.setLoggor(new FileLoggor(DEF_LOG_DIR + "/" + get(ARG_LOG_FILE, "jrest.log"), 0));

		String traceHome = get(ARG_TRACE_HOME);
		String traceMain = get(ARG_TRACE_MAIN);
		if (traceHome != null && traceMain != null) {
			Tracer.load(SysUtil.initClassLoader(new File(traceHome)), traceMain, traceHome);
		}

		// load global configuration
		loadConfig();

		Collection<Application> apps = scanApp();

		apps.forEach(app -> {
			try {
				app.loadResource();
			} catch (Throwable e) {
				LogUtil.error("App load resource fail: {}", e, app);
			}
		});

		String configDir = get(ARG_CFG_DIR, DEF_CFG_DIR);

		ServiceManager smanager = new ServiceManager(loadResponses());
		smanager.scan(get(ARG_SRV_PKG, ""));
		smanager.setSecutityChecker(scanSecurityHandler());

		apps.forEach(app -> {
			try {
				app.start();
			} catch (Throwable e) {
				LogUtil.error("App start fail: {}", e, app);
			}
		});

		InetSocketAddress sock = InetSocketAddress.createUnresolved(get(ARG_IP, "0.0.0.0"), getInt(ARG_PORT, 8080));
		Server server = new Server(sock);

		HandlerList hl = new HandlerList();
		String apiPrefix = get(ARG_PATH_PREFIX, "/");
		hl.addHandler(new RestHandler(apiPrefix, smanager));

		if (new File(configDir, "webdefault.xml").exists()) {
			new File("webapps").mkdirs();
			WebAppContext context = new WebAppContext();
			String webPrefix = get(ARG_WEB_PREFIX, "/");
			context.setContextPath(webPrefix);
			context.setResourceBase("./webapps");
			context.setDescriptor(configDir + "/webdefault.xml");
			hl.addHandler(context);
		}

		HandlerCollection hc = new HandlerCollection();
		hc.setHandlers(new Handler[] { hl, new GzipHandler() });
		server.setHandler(hc);

		try {
			server.start();
			server.join();
		} catch (Throwable e) {
			LogUtil.error("Server error", e);
		}
	}

	public static SecurityChecker scanSecurityHandler() {
		Set<Class<?>> cs = ScanLoader.ins(Args.get(ARG_SRV_PKG, "")).filter(SecurityChecker.class, null).scan();
		List<SecurityChecker> rlt = new ArrayList<>();
		for (Class<?> c : cs) {
			try {
				SecurityChecker sc = (SecurityChecker) c.newInstance();
				rlt.add(sc);
				LogUtil.trace("Find and load SecurityChecker : {}", c.getName());
			} catch (Throwable e) {
				LogUtil.error("Init SecurityChecker fail: {}", e, c.getName());
			}
		}

		SecurityChecker sc = rlt.isEmpty() ? null : rlt.get(0);
		LogUtil.trace("Set SecurityChecker : {}", sc == null ? "None" : sc.getClass().getName());
		return sc;
	}

	public static Collection<Application> scanApp() {
		Set<Class<?>> cs = ScanLoader.ins(Args.get(ARG_SRV_PKG, "")).filter(Application.class, null).scan();
		List<Application> rlt = new ArrayList<>();

		for (Class<?> c : cs) {
			try {
				Application app = (Application) c.newInstance();
				rlt.add(app);
			} catch (Throwable e) {
				LogUtil.error("Init app fail: {}", e, c.getName());
			}
		}

		return rlt;
	}

	public static Responses loadResponses() {
		Set<Class<?>> cs = ScanLoader.ins(Args.get(ARG_SRV_PKG, "")).filter(Responses.class, null).scan();
		cs.remove(Responses.class);
		for (Class<?> c : cs) {
			try {
				Responses r = (Responses) c.newInstance();
				return r;
			} catch (Throwable e) {
				LogUtil.error("Init config fail: {}", e, c.getName());
			}
		}

		return new Responses();
	}

	public static void loadConfig() {
		Set<Class<?>> cs = ScanLoader.ins(Args.get(ARG_SRV_PKG, "")).filter(IConfig.class, null).scan();

		for (Class<?> c : cs) {
			try {
				IConfig cfg = (IConfig) c.newInstance();
				cfg.config();
			} catch (Throwable e) {
				LogUtil.error("Init config fail: {}", e, c.getName());
			}
		}

	}
}
