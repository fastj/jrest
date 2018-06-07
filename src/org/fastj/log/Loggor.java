package org.fastj.log;

import java.io.PrintStream;

public class Loggor {

	protected PrintStream ps = System.out;

	public Loggor() {

	}

	public void debug(String msg, Object... args) {
		if (!LogUtil.debug() || ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.DEBUG, msg, (Throwable) null, args);
			postProcess();
		}
	}

	public void info(String msg, Object... args) {
		if (!LogUtil.info() || ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.INFO, msg, (Throwable) null, args);
			postProcess();
		}
	}

	public void warn(String msg, Object... args) {
		if (!LogUtil.warn() || ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.WARN, msg, (Throwable) null, args);
			postProcess();
		}
	}

	public void trace(String msg, Object... args) {
		if (!LogUtil.trace() || ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.TRACE, msg, (Throwable) null, args);
			postProcess();
		}
	}

	public void error(String msg, Object... args) {
		if (!LogUtil.error() || ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.ERROR, msg, (Throwable) null, args);
			postProcess();
		}
	}

	public void error(String msg, Throwable t, Object... args) {
		if (!LogUtil.error() || ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.ERROR, msg, t, args);
			postProcess();
		}
	}

	public void fatal(String msg, Object... args) {
		if (ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.ERROR, msg, (Throwable) null, args);
			postProcess();
		}
	}

	public void fatal(String msg, Throwable t, Object... args) {
		if (ps == null)
			return;
		synchronized (ps) {
			LogUtil.log(ps, LogUtil.FATAL, msg, t, args);
			postProcess();
		}
	}

	public void rawLog(byte[] msg, int offset, int len) {
		if (!LogUtil.trace() || ps == null)
			return;
		synchronized (ps) {
			ps.write(msg, offset, len);
			postProcess();
		}
	}

	public void rawLog(String msg) {
		if (!LogUtil.trace() || ps == null)
			return;
		synchronized (ps) {
			ps.println(msg);
			postProcess();
		}
	}

	public void close() {

	}

	protected void postProcess() {

	}

}
