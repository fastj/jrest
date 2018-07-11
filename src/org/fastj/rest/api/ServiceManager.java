package org.fastj.rest.api;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fastj.app.Args;
import org.fastj.log.LogUtil;
import org.fastj.pchk.CheckUtil;
import org.fastj.pchk.CheckUtil.ChkNode;
import org.fastj.rest.annotation.Path;
import org.fastj.rest.annotation.PathVar;
import org.fastj.rest.annotation.ReqVar;
import org.fastj.rest.api.SecurityChecker.AuthNode;
import org.fastj.scheduler.Scheduler;
import org.fastj.util.ScanLoader;
import org.fastj.rest.annotation.RAuth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServiceManager {
	private static ObjectMapper mapper = new ObjectMapper();
	private final Responses responseHolder;

	private Map<String, SHolder> srvMap = new HashMap<>();
	private List<String> serviceKeys = new ArrayList<>();

	private SecurityChecker secutityChecker;
	private List<String> serviceTags = new ArrayList<>();

	static {
		boolean flag = Args.getBoolean("fail_on_unknown_properties", true);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, flag);
	}

	public ServiceManager(Responses holder) {
		this.responseHolder = holder == null ? new Responses() : holder;
	}

	public Response process(Request<?> req) {
		Response rlt = responseHolder.get404();

		String key = req.getMethod() + req.getUri();
		key = key.contains("?") ? key.substring(0, key.indexOf('?')) : key;
		SHolder sh = srvMap.get(key);

		if (sh == null) {
			for (String sKey : serviceKeys) {
				if (match(sKey, key)) {
					sh = srvMap.get(sKey);
					break;
				}
			}
		}

		if (sh != null) {
			rlt = sh.call(req);
		}

		return rlt;
	}

	public void scan(String... packages) {
		if (packages != null) {
			for (String p : packages) {
				Set<Class<?>> cs = ScanLoader.ins(p).filter(RestService.class, null).scan();
				for (Class<?> c : cs) {
					LogUtil.trace("D-Load service : {}", c.getName());
					bindService(c.asSubclass(RestService.class));
				}
			}
		}
	}

	public void bindService(Class<? extends RestService> c) {
		Path clzPath = c.getAnnotation(Path.class);

		String pathPrefix = clzPath != null ? clzPath.uri() : "";

		for (Method m : c.getDeclaredMethods()) {
			Path p = m.getAnnotation(Path.class);
			if (p == null) {
				continue;
			}

			String resUri = pathPrefix + p.uri();
			resUri = resUri.contains("?") ? resUri.substring(0, resUri.indexOf('?')) : resUri;
			String[] tags = p.tags();

			// Invalid setting
			if (resUri.trim().isEmpty()) {
				continue;
			}

			// Check service tags
			if (allow(tags)) {
				SHolder holder = new SHolder(c, m, resUri);
				// key = GET/service/resource
				srvMap.put(p.method().toUpperCase() + resUri, holder);
				LogUtil.trace("Bind API: {} {}", p.method().toUpperCase(), resUri);
			}
		}

		refreshServices();

		Scheduler.execute(new Runnable() {
			public void run() {
				try {
					c.newInstance();
				} catch (Throwable e) {
					LogUtil.error("Test RestService[{}] init fail", e, c.getClass().getName());
				}
			}
		});
	}

	public SecurityChecker getSecutityChecker() {
		return secutityChecker;
	}

	public void setSecutityChecker(SecurityChecker secutityChecker) {
		this.secutityChecker = secutityChecker;
	}

	public List<String> getServiceTags() {
		return serviceTags;
	}

	private void refreshServices() {
		serviceKeys.clear();
		serviceKeys.addAll(srvMap.keySet());
		Collections.sort(serviceKeys, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return countL(o2).compareTo(countL(o1));
			}
		});
	}

	private final Integer countL(String path) {
		int cnt = 0;
		int idx = -1;
		while (true) {
			idx = path.indexOf('{', idx + 1);
			if (idx == -1) {
				break;
			}
			cnt++;
		}
		return cnt;
	}

	private final boolean match(String key, String rpath) {
		String kpar[] = key.split("/");
		String rpar[] = rpath.split("/");

		if (kpar.length != rpar.length)
			return false;

		for (int i = 0; i < kpar.length; i++) {
			String o1 = kpar[i];
			String o2 = rpar[i];

			if (o1.equals(o2) || o1.startsWith("{"))
				continue;
			return false;
		}

		return true;
	}

	public void setServiceTags(String... serviceTags) {
		for (String tag : serviceTags) {
			this.serviceTags.add(tag);
		}
	}

	private boolean allow(String[] tags) {
		for (String tag : tags) {
			if (!serviceTags.contains(tag)) {
				return false;
			}
		}
		return true;
	}

	private class SHolder {

		private Class<? extends RestService> clazz;
		private Method method;
		private String pathDef;

		private Path path;
		private String pkey;
		private List<Parameter> resParas = new ArrayList<>();
		private HashMap<String, ChkNode> chks = new HashMap<>();

		public SHolder(Class<? extends RestService> clazz, Method m, String pathDef) {
			this.clazz = clazz;
			this.method = m;
			this.pathDef = pathDef;
			path = m.getAnnotation(Path.class);
			pkey = path.method() + pathDef;
			Parameter[] params = m.getParameters();
			for (int i = 0; i < params.length; i++) {
				Parameter p = params[i];
				if (p.getAnnotation(RAuth.class) != null && (p.getAnnotation(PathVar.class) != null || p.getAnnotation(ReqVar.class) != null)) {
					resParas.add(p);
				}
				ChkNode chknode = CheckUtil.chkNode(p);

				if (chknode != null) {
					chknode.key = pkey + "." + p.getName();
					chks.put(chknode.key, chknode);
				} else if (i == 0 && !p.getType().isPrimitive() && p.getType() != String.class) {
					chknode = new ChkNode();
					chknode.key = pkey + "." + p.getName();
					chks.put(chknode.key, chknode);
				}

			}
		}

		public Response call(final Request<?> req) {
			Response resp = null;
			try {
				resp = call0(req);
				return resp;
			} catch (Throwable e) {
				LogUtil.error("RestCall Fail", e);
				return responseHolder.get500(e);
			}
		}

		private Response call0(Request<?> req) throws Throwable {
			final RestService instance = clazz.newInstance();
			instance.setRequest(req, pathDef);

			Response failResp = secCheck(instance);
			// Check fail
			if (failResp != null) {
				return failResp;
			}

			failResp = getArgs(instance);
			// Parameter Invalid
			if (failResp != null) {
				return failResp;
			}

			method.invoke(instance, instance.getArgs());
			return instance.getResponse();
		}

		private Response secCheck(RestService instance) {
			if (secutityChecker != null) {
				AuthNode[] resAuth = getResAuth(instance);
				return secutityChecker.checkAccess(instance, resAuth);
			}

			return null;
		}

		private AuthNode[] getResAuth(RestService instance) {
			AuthNode[] resAuth = new AuthNode[resParas.size()];
			int idx = 0;
			for (Parameter p : resParas) {
				RAuth ra = p.getAnnotation(RAuth.class);
				PathVar pv = p.getAnnotation(PathVar.class);
				ReqVar rv = p.getAnnotation(ReqVar.class);
				resAuth[idx] = new AuthNode();
				resAuth[idx].auth = ra.value();
				resAuth[idx].resourceId = pv != null ? instance.getPathParameter(pv.value())
						: rv != null ? instance.getQueryParameter(rv.name(), rv.def()) : null;
				resAuth[idx].or = ra.or();

				idx++;
			}
			return resAuth;
		}

		private Response getArgs(RestService instance) throws IOException {
			Parameter[] paras = method.getParameters();
			Object[] args = new Object[paras.length];

			List<ChkNode> rtchk = new ArrayList<>();

			for (int i = 0; i < paras.length; i++) {
				Parameter p = paras[i];
				Type type = p.getParameterizedType();
				PathVar pv = p.getAnnotation(PathVar.class);
				ReqVar rv = p.getAnnotation(ReqVar.class);

				String v = pv != null ? instance.getPathParameter(pv.value())
						: rv != null ? instance.getQueryParameter(rv.name(), rv.def()) : i == 0 ? instance.getRequest().getContent() : null;
				if (v == null) {
					args[i] = null;
					continue;
				}

				try {
					args[i] = wrap(v, type);
				} catch (Throwable e) {
					LogUtil.error("Wrap value fail", e);
					return responseHolder.get400("Parse Request fail :" + e.getMessage());
				}

				String key = pkey + "." + p.getName();
				if (chks.containsKey(key)) {
					rtchk.add(chks.get(key).copy(args[i]));
				}
			}

			// check parameter
			List<String> error = CheckUtil.check(rtchk.toArray(new ChkNode[rtchk.size()]));
			if (!error.isEmpty()) {
				return responseHolder.get400(error);
			}

			instance.setArgs(args);
			return null;
		}

		private <T> Object wrap(String value, Type clazz) throws Throwable {
			if (clazz == String.class) {
				return value;
			} else if (clazz == int.class || clazz == Integer.class) {
				return Integer.valueOf(value);
			} else if (clazz == long.class || clazz == Long.class) {
				return Long.valueOf(value);
			} else if (clazz == double.class || clazz == Double.class) {
				return Double.valueOf(value);
			} else if (clazz == float.class || clazz == Float.class) {
				return Float.valueOf(value);
			} else if (clazz == char.class || clazz == Character.class) {
				return value.charAt(0);
			} else if (clazz == short.class || clazz == Short.class) {
				return Short.valueOf(value);
			} else if (clazz == byte.class || clazz == Byte.class) {
				return Byte.valueOf(value);
			}

			if (value == null || value.trim().isEmpty()) {
				return null;
			}

			TypeReference<T> type = new TypeReference<T>() {
				public Type getType() {
					return clazz;
				}
			};
			return mapper.readValue(value, type);
		}

	}
}
