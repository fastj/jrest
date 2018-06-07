/*
 * Copyright 2015  FastJ
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fastj.log;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jetty.util.ByteArrayOutputStream2;

public class LogUtil {

	public static final int DEBUG = 0;
	public static final int INFO = 1;
	public static final int WARN = 2;
	public static final int ERROR = 3;
	public static final int TRACE = 4;
	public static final int FATAL = 5;
	public static final int CLOSE = 6;

	public static final String[] LEVELS = { "DEBUG", "INFO", "WARN", "ERROR", "TRACE", "FATAL", "CLOSE" };

	private static final String DFORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final ThreadLocal<SimpleDateFormat> sdfl = new ThreadLocal<>();
	private static final ThreadLocal<ByteArrayOutputStream2> tmpoutl = new ThreadLocal<>();
	private static Loggor deflog = new Loggor();

	public static int level = INFO;

	public static void setLevel(String level) {
		LogUtil.level = getIntLevel(level);
	}

	public static String date() {
		SimpleDateFormat sdf = sdfl.get();
		if (sdf == null) {
			sdfl.set(sdf = new SimpleDateFormat(DFORMAT));
		}
		return sdf.format(new Date());
	}

	public static void setLoggor(Loggor out) {
		if (out != null) {
			deflog = out;
		}
	}

	public static boolean debug() {
		return level <= DEBUG;
	}

	public static boolean info() {
		return level <= INFO;
	}

	public static boolean warn() {
		return level <= WARN;
	}

	public static boolean error() {
		return level <= ERROR;
	}

	public static boolean trace() {
		return level <= TRACE;
	}

	public static void closeLog() {
		level = CLOSE;
	}

	public static void debug(String msg, Object... args) {
		deflog.debug(msg, args);
	}

	public static void info(String msg, Object... args) {
		deflog.info(msg, args);
	}

	public static void warn(String msg, Object... args) {
		deflog.warn(msg, args);
	}

	public static void trace(String msg, Object... args) {
		deflog.trace(msg, args);
	}

	public static void error(String msg, Object... args) {
		deflog.error(msg, args);
	}

	public static void error(String msg, Throwable t, Object... args) {
		deflog.error(msg, t, args);
	}

	public static void fatal(String msg, Object... args) {
		deflog.fatal(msg, args);
	}

	public static void fatal(String msg, Throwable t, Object... args) {
		deflog.fatal(msg, t, args);
	}

	public static void rawLog(byte[] msg, int offset, int len) {
		deflog.rawLog(msg, offset, len);
	}

	public static void rawLog(String msg) {
		deflog.rawLog(msg);
	}

	static void log(PrintStream ps, int level, String expr, Throwable t, Object... args) {
		if (ps == null)
			return;

		try {
			ByteArrayOutputStream2 bout = tmpoutl.get();
			if (bout == null) {
				tmpoutl.set(bout = new ByteArrayOutputStream2(1024));
			}

			bout.reset();
			PrintStream tmp = new PrintStream(bout);
			log0(tmp, LEVELS[level], expr, t, args);
			ps.write(bout.getBuf(), 0, bout.getCount());
		} catch (Throwable e) {
			System.err.println("Log fail: " + e.getMessage());
		}
	}

	private static void log0(PrintStream ps, String level, String expr, Throwable t, Object... args) {
		ps.print(date());
		ps.print("  ");
		ps.print(level);
		ps.print("  ");

		if (args == null || args.length == 0) {
			ps.print(String.valueOf(expr));
			ps.println();
			if (t != null)
				t.printStackTrace(ps);
			ps.flush();
			return;
		}

		int idx = -1, pidx = 0;
		int tag = 0;

		while ((idx = expr.indexOf("{}", pidx)) >= 0 && tag < args.length) {
			ps.append(expr, pidx, idx);
			pidx = idx + 2;
			ps.print(String.valueOf(args[tag++]));
		}

		if (pidx < expr.length() - 1) {
			ps.append(expr, pidx, expr.length());
		}

		ps.println();
		if (t != null)
			t.printStackTrace(ps);
		ps.flush();
	}

	public static int getIntLevel(String lvStr) {
		switch (lvStr.toUpperCase()) {
		case "DEBUG":
			return DEBUG;
		case "INFO":
			return INFO;
		case "WARN":
			return WARN;
		case "ERROR":
			return ERROR;
		case "TRACE":
			return TRACE;
		case "CLOSE":
			return CLOSE;
		default:
			try {
				return Integer.valueOf(lvStr.trim());
			} catch (Exception e) {
				return INFO;
			}
		}
	}

}
