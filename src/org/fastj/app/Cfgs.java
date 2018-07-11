package org.fastj.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.fastj.log.LogUtil;

public class Cfgs {

	private static Properties cfgs = new Properties();

	static {
		String cfgFile = Args.get(Args.ARG_CFG_DIR, Args.DEF_CFG_DIR) + "/config.properties";
		File cfg = new File(cfgFile);
		try (InputStream in = cfg.exists() ? new FileInputStream(cfg) : null;
				InputStream inRes = in != null ? null : Args.class.getResourceAsStream("/" + cfgFile);) {
			if (in != null) {
				cfgs.load(in);
			} else if (inRes != null) {
				cfgs.load(inRes);
			} else {
				LogUtil.trace("[Cfgs]No cfg-file[{}] found. default: config/config.properties", cfgFile);
			}
		} catch (Throwable e) {
			LogUtil.error("[Cfgs]Load default config fail, {}", e, cfgFile);
		}
	}

	public static void append(File conf) {
		if (conf == null) {
			return;
		}

		try (InputStream in = new FileInputStream(conf);) {
			cfgs.load(in);
		} catch (Throwable e) {
			LogUtil.error("[Cfgs]Load config fail: {}", e, conf);
		}
	}

	public static String get(String key) {
		return cfgs.getProperty(key);
	}

	public static String get(String key, String def) {
		return cfgs.getProperty(key, def);
	}

	public static String get(String key, String key2, String def) {
		String v = cfgs.getProperty(key);
		return v != null ? v : cfgs.getProperty(key2, def);
	}

	public static int getInt(String key, int def) {
		String v = cfgs.getProperty(key);
		if (v == null) {
			return def;
		}

		return Integer.valueOf(v);
	}

	public static long getLong(String key, long def) {
		String v = cfgs.getProperty(key);
		if (v == null) {
			return def;
		}

		return Long.valueOf(v);
	}

	public static double getDouble(String key, double def) {
		String v = cfgs.getProperty(key);
		if (v == null) {
			return def;
		}

		return Double.valueOf(v);
	}

	public static boolean getBoolean(String key, boolean def) {
		String v = cfgs.getProperty(key);
		return v == null ? def : Boolean.valueOf(v);
	}

}
