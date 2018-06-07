package org.fastj.db;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.persistence.Table;

import org.fastj.log.LogUtil;
import org.fastj.scheduler.Scheduler;
import org.fastj.util.ScanLoader;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * 
 * @author zhou
 *
 */
public class HSessionUtils {

	private static final Map<String, SessionFactory> SFS = new HashMap<>();

	public static void scanConfig(String dir) {
		File cfgDir = new File(dir);
		
		File cfgs[] = cfgDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("hibernate.cfg") && (name.endsWith(".xml"));
			}
		});

		if (cfgs == null || cfgs.length == 0) {
			LogUtil.warn("No hibernate cfg find.");
			return;
		}

		CountDownLatch cdl = new CountDownLatch(cfgs.length);
		for (File cfg : cfgs) {
			Scheduler.execute(new Runnable() {
				public void run() {
					try {
						buildSF(cfg);
					} catch (Throwable e) {
						LogUtil.error("Build SessionFactory failed: cfg={}", e, cfg.getName());
					} finally {
						cdl.countDown();
					}
				}
			});
		}

		try {
			cdl.await();
		} catch (InterruptedException e) {
		}
	}

	public static Session getSession(String key) {
		SessionFactory sf = SFS.get(key);
		return sf == null ? null : sf.getCurrentSession();
	}

	private static void buildSF(File cfg) {
		Configuration conf = new Configuration();
		conf.configure(cfg);

		String sps = conf.getProperties().getProperty("scan_package");
		if (sps != null) {
			for (String sp : sps.split(",")) {
				Set<Class<?>> sl = ScanLoader.ins(sp).filter(null, Table.class).scan();
				sl.forEach(c -> conf.addAnnotatedClass(c));
			}
		}
		
		String key = conf.getProperties().getProperty("hikey", "all");
		LogUtil.trace("Load hibernate cfg[{}] by key : {}", cfg.getName(), key);
		
		SessionFactory sf = conf.buildSessionFactory();
		if (sf != null) {
			SFS.put(key, sf);
			return;
		}

		LogUtil.error("Build SessionFactory failed. cfg={}", cfg);
	}

}
