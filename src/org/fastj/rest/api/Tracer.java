package org.fastj.rest.api;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.fastj.log.LogUtil;
import org.fastj.rest.annotation.PathVar;

public interface Tracer {

	String KEY_OPERATE_NAME = "operationName";
	String KEY_METHOD = "method";
	String KEY_REMOTE_PEER = "remotePeer";
	String KEY_URI = "uri";
	String KEY_COMPONENT_NAME = "componentName";

	String CROSS_THREAD = "cross_thread";
	String HTTP_SERVER = "http.server";
	String HTTP = "http";
	String REDIS = "redis";
	String SQL = "sql";
	String MQ = "mq";

	void init();

	void start(Map<String, Object> args);

	void stop(int code, String description);

	void error(Throwable t);

	static Map<String, Class<? extends Tracer>> TRACER_MAP = new HashMap<>(19);
	static NOPTracer nop = new NOPTracer();

	static void regist(Class<? extends Tracer> c) {
		PathVar nameVar = c.getAnnotation(PathVar.class);
		if (nameVar == null) {
			LogUtil.error("Tracer [{}] load fail.", c.getName());
			return;
		}

		TRACER_MAP.put(nameVar.value(), c);
	}

	static Tracer get(String name) {
		Class<? extends Tracer> c = TRACER_MAP.get(name);
		if (c == null) {
			LogUtil.debug("No tracer {}", name);
			return nop;
		}

		try {
			Tracer t = c.newInstance();
			LogUtil.debug("Tracer init ok , {}", name);
			return t;
		} catch (Throwable e) {
			LogUtil.error("Tracer init fail , {}", name);
			return null;
		}
	}

	static <V> Callable<V> wrapper(Callable<V> call) {
		return new CallableWrapper<>(call);
	}

	static Runnable wrapper(Runnable r) {
		return new RunnableWrapper(r);
	}

	static void load(ClassLoader cl, String main, String tracerHome) {
		if (cl == null || main == null) {
			LogUtil.warn("Tracer load fail, no config.");
			return;
		}

		try {
			Class<?> mc = Class.forName(main, true, cl);
			Method mm = mc.getDeclaredMethod("main", String[].class);
			mm.invoke(null, new Object[] { new String[] { tracerHome } });
		} catch (Throwable e) {
			LogUtil.error("Tracer load fail. home=[], main=[]", e, tracerHome, main);
		}
	}

	static abstract class Adaptor implements Tracer {
		static final int BEFORE = 0;
		static final int INIT = 1;
		static final int START = 2;
		static final int STOP = 3;
		protected AtomicInteger status = new AtomicInteger(0);

		@Override
		public void init() {
			if (status.compareAndSet(BEFORE, INIT)) {
				try {
					init0();
				} catch (Throwable e) {
					LogUtil.error("Tracer[{}] init fail", e, this.getClass().getName());
				}
			}
		}

		public abstract void init0();

		@Override
		public void start(Map<String, Object> args) {
			if (status.compareAndSet(INIT, START)) {
				try {
					start0(args);
				} catch (Throwable e) {
					LogUtil.error("Tracer[{}] start fail", e, this.getClass().getName());
				}
			}
		}

		public abstract void start0(Map<String, Object> args);

		@Override
		public void stop(int code, String description) {
			if (status.compareAndSet(START, STOP)) {
				try {
					stop0(code, description);
				} catch (Throwable e) {
					LogUtil.error("Tracer[{}] stop fail", e, this.getClass().getName());
				}
			}
		}

		public abstract void stop0(int code, String description);

		@Override
		public void error(Throwable t) {
			try {
				error0(t);
			} catch (Throwable e) {
				LogUtil.error("Tracer[{}] error log", e, this.getClass().getName());
			}
		}

		public abstract void error0(Throwable t);

	}

	static class CallableWrapper<V> implements Callable<V> {
		Callable<V> call = null;
		Tracer tracer = Tracer.get(CROSS_THREAD);

		CallableWrapper(Callable<V> call) {
			this.call = call;
			tracer.init();
		}

		@Override
		public V call() throws Exception {
			try {
				tracer.start(null);
				if (call == null) {
					return null;
				}
				return call.call();
			} finally {
				tracer.stop(0, "");
			}
		}
	}

	static class RunnableWrapper implements Runnable {
		Runnable r = null;
		Tracer tracer = Tracer.get(CROSS_THREAD);

		RunnableWrapper(Runnable r) {
			this.r = r;
			tracer.init();
		}

		@Override
		public void run() {
			try {
				tracer.start(null);
				if (r == null) {
					return;
				}
				r.run();
			} finally {
				tracer.stop(0, "");
			}
		}
	}

	@PathVar("default")
	static class NOPTracer implements Tracer {
		public void init() {
		}

		public void start(Map<String, Object> args) {
		}

		public void stop(int code, String description) {
		}

		public void error(Throwable t) {
		}
	}
}
