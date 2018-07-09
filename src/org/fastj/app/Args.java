package org.fastj.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.fastj.log.LogUtil;

public final class Args {

	public static final String ARG_LOG_LEVEL = "log_level";
	public static final String ARG_LOG_FILE = "log_file";
	public static final String ARG_CFG_DIR = "config_dir";
	public static final String ARG_SRV_PKG = "service_package";
	public static final String ARG_PATH_PREFIX = "path_prefix";
	public static final String ARG_WEB_PREFIX = "web_prefix";
	public static final String ARG_IP = "ip";
	public static final String ARG_PORT = "port";
	public static final String ARG_SSL = "ssl";

	public static final String ARG_TRACE_HOME = "trace_home";
	public static final String ARG_TRACE_MAIN = "trace_main";

	public static final int DEF_PORT = 8080;
	public static final String DEF_LOG_LEVEL = "info";
	public static final String DEF_LOG_DIR = "logs";
	public static final String DEF_CFG_DIR = "config";

	private static Properties cfgs = new Properties();

	static {
		String cfgFile = Args.get(ARG_CFG_DIR, DEF_CFG_DIR) + "/app.properties";
		File cfg = new File(cfgFile);
		try (InputStream in = cfg.exists() ? new FileInputStream(cfg) : null;
				InputStream inRes = in != null ? null : Args.class.getResourceAsStream("/" + cfgFile);) {
			if (in != null) {
				cfgs.load(in);
			} else if (inRes != null) {
				cfgs.load(inRes);
			} else {
				LogUtil.trace("No cfg-file[{}] found. default: config/app.properties", cfgFile);
			}
		} catch (Throwable e) {
			LogUtil.error("Load default config fail, {}", e, cfgFile);
		}

	}

	public static void parse(String... args) {
		if (args != null) {
			for (String arg : args) {
				if (arg.startsWith("--")) {
					String[] parts = arg.substring(2).split("=", 2);
					if (parts.length == 2) {
						cfgs.put(parts[0], parts[1]);
					} else {
						LogUtil.error("invalid args : {}", arg);
					}
				} else if (arg.startsWith("-")) {
					cfgs.put(arg.substring(1), "true");
				}
			}
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
