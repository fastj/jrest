package org.fastj.jetty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.PathContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.fastj.log.LogUtil;
import org.fastj.rest.api.Response;
import org.fastj.rest.api.Tracer;
import org.fastj.util.JSON;

public class HttpUtil {

	public static final int HTTP_ERROR_REQUEST = -1;
	public static final int HTTP_ERROR_RESPONE = -2;
	public static final int HTTP_ERROR_OTHER = -3;

	public static final int DEFAULT_API_TIMEOUT = 15;

	private static HttpClient client = new HttpClient(new SslContextFactory(true));

	private static Map<String, HttpClient> clients = new ConcurrentHashMap<>(31);

	static {
		try {
			client.setFollowRedirects(false);
			client.setConnectBlocking(false);
			client.setTCPNoDelay(true);
			client.start();

			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					try {
						client.stop();
					} catch (Throwable e) {
					}
				}
			}));

			clients.put("default", client);
		} catch (Exception e) {
			LogUtil.error("Init Jetty httpclient fail: {}#{}", e.getClass(), e.getMessage());
		}
	}

	public static void registClient(String httpEndpoint, HttpClient client) {
		clients.put(httpEndpoint, client);
	}

	public static void get(String url, HashMap<String, String> headers, ResponseHandler handler) {
		exec("GET", url, null, headers, DEFAULT_API_TIMEOUT, handler);
	}

	public static void get(String url, HashMap<String, String> headers, int tm, ResponseHandler handler) {
		exec("GET", url, null, headers, tm, handler);
	}

	public static void post(String url, Object ct, HashMap<String, String> headers, ResponseHandler handler) {
		exec("POST", url, ct, headers, DEFAULT_API_TIMEOUT, handler);
	}

	public static void post(String url, Object ct, HashMap<String, String> headers, int tm, ResponseHandler handler) {
		exec("POST", url, ct, headers, tm, handler);
	}

	public static void put(String url, Object ct, HashMap<String, String> headers, ResponseHandler handler) {
		exec("PUT", url, ct, headers, DEFAULT_API_TIMEOUT, handler);
	}

	public static void put(String url, Object ct, HashMap<String, String> headers, int tm, ResponseHandler handler) {
		exec("PUT", url, ct, headers, tm, handler);
	}

	public static void delete(String url, Object ct, HashMap<String, String> headers, ResponseHandler handler) {
		exec("DELETE", url, ct, headers, DEFAULT_API_TIMEOUT, handler);
	}

	public static void delete(String url, Object ct, HashMap<String, String> headers, int tm, ResponseHandler handler) {
		exec("DELETE", url, ct, headers, tm, handler);
	}

	public static void patch(String url, Object ct, HashMap<String, String> headers, ResponseHandler handler) {
		exec("PATCH", url, ct, headers, DEFAULT_API_TIMEOUT, handler);
	}

	public static void patch(String url, Object ct, HashMap<String, String> headers, int tm, ResponseHandler handler) {
		exec("PATCH", url, ct, headers, tm, handler);
	}

	public static Response get(String url, HashMap<String, String> headers, int tm) {
		return execR("GET", url, null, headers, tm);
	}

	public static Response get(String url, HashMap<String, String> headers) {
		return execR("GET", url, null, headers, DEFAULT_API_TIMEOUT);
	}

	public static Response post(String url, Object content, HashMap<String, String> headers, int tm) {
		return execR("POST", url, content, headers, tm);
	}

	public static Response post(String url, Object content, HashMap<String, String> headers) {
		return execR("POST", url, content, headers, DEFAULT_API_TIMEOUT);
	}

	public static Response put(String url, Object content, HashMap<String, String> headers, int tm) {
		return execR("PUT", url, content, headers, tm);
	}

	public static Response put(String url, Object content, HashMap<String, String> headers) {
		return execR("PUT", url, content, headers, DEFAULT_API_TIMEOUT);
	}

	public static Response delete(String url, Object content, HashMap<String, String> headers, int tm) {
		return execR("DELETE", url, content, headers, tm);
	}

	public static Response delete(String url, Object content, HashMap<String, String> headers) {
		return execR("DELETE", url, content, headers, DEFAULT_API_TIMEOUT);
	}

	public static Response patch(String url, Object content, HashMap<String, String> headers, int tm) {
		return execR("PATCH", url, content, headers, tm);
	}

	public static Response patch(String url, Object content, HashMap<String, String> headers) {
		return execR("PATCH", url, content, headers, DEFAULT_API_TIMEOUT);
	}

	public static Response exec(String method, String url, Object content, HashMap<String, String> headers) {
		return execR(method, url, content, headers, DEFAULT_API_TIMEOUT);
	}

	public static void exec(String method, String url, Object content, HashMap<String, String> headers, int timeout, ResponseHandler handler) {
		Request req = newRequest(method, url, content, headers);

		Tracer tracer = Tracer.get(Tracer.HTTP);
		if (tracer != null) {
			Map<String, Object> args = new HashMap<>();
			args.put(Tracer.KEY_METHOD, req.getMethod());
			args.put(Tracer.KEY_OPERATE_NAME, req.getURI().getPath());
			args.put(Tracer.KEY_REMOTE_PEER, req.getHost() + ":" + req.getPort());
			args.put(Tracer.KEY_URI, req.getURI().toString());
			tracer.start(args);
		}
		RespAdapter adapter = new RespAdapter(new ResponseHandler() {
			public void handle(Response resp) {
				try {
					handler.handle(resp);
				} finally {
					if (tracer != null) {
						tracer.stop(resp.getHttpcode(), "");
					}
				}
			}
		});
		req.timeout(timeout, TimeUnit.SECONDS).send(adapter);
	}

	public static org.fastj.rest.api.Response execR(String method, String url, Object content, HashMap<String, String> headers, int timeout) {
		Request req = newRequest(method, url, content, headers);
		Tracer tracer = Tracer.get(Tracer.HTTP);
		if (tracer != null) {
			Map<String, Object> args = new HashMap<>();
			args.put(Tracer.KEY_METHOD, req.getMethod());
			args.put(Tracer.KEY_OPERATE_NAME, req.getURI().getPath());
			args.put(Tracer.KEY_REMOTE_PEER, req.getHost() + ":" + req.getPort());
			args.put(Tracer.KEY_URI, req.getURI().toString());
			tracer.start(args);
		}
		RespAdapter adapter = new RespAdapter(null);
		req.timeout(timeout, TimeUnit.SECONDS).send(adapter);

		int code = -1;
		try {
			Response r = adapter.get();
			code = r.getHttpcode();
			return r;
		} finally {
			if (tracer != null) {
				tracer.stop(code, "");
			}
		}
	}

	private static Request newRequest(String method, String url, Object content, HashMap<String, String> headers) {
		HttpClient client = HttpUtil.client;

		for (String key : new ArrayList<>(clients.keySet())) {
			if (url.startsWith(key + "/") || url.equals(key)) {
				client = clients.get(key);
				break;
			}
		}

		Request req = client.newRequest(url);
		req.method(HttpMethod.fromString(method));
		req.followRedirects(false);
		setContent(req, content);
		if (headers != null && !headers.isEmpty()) {
			for (String key : headers.keySet()) {
				req.header(key, headers.get(key));
			}
		}
		return req;
	}

	// Response.Listener.Adapter
	public static class RespAdapter extends org.eclipse.jetty.client.api.Response.Listener.Adapter {

		ResponseHandler handler;
		ByteArrayOutputStream2 bao = new ByteArrayOutputStream2(2048);
		CountDownLatch cdl = new CountDownLatch(1);
		org.fastj.rest.api.Response response = new org.fastj.rest.api.Response();
		File dowload;
		FileChannel channel;

		public RespAdapter(ResponseHandler handle) {
			this.handler = handle;
		}

		@SuppressWarnings("resource")
		public RespAdapter(ResponseHandler handle, File dlf) {
			this.handler = handle;
			this.dowload = dlf;
			try {
				channel = new FileOutputStream(dowload).getChannel();
			} catch (Throwable e) {
				LogUtil.error("Init download stream fail: {}#{}", e.getClass().getName(), e.getMessage());
			}
		}

		@Override
		public void onComplete(Result r) {
			try {
				if (r.isFailed()) {
					int code = r.getRequestFailure() != null ? -1 : r.getResponseFailure() != null ? -2 : -3;
					String error = "No Failure E";
					if (r.getFailure() != null) {
						ByteArrayOutputStream2 err = new ByteArrayOutputStream2(1024);
						r.getFailure().printStackTrace(new PrintStream(err));
						error = err.toString();
					}

					response.setHttpcode(code);
					response.setContent(error);
				} else {
					response.setHttpcode(r.getResponse().getStatus());
					response.setContent(bao.toString());
					r.getResponse().getHeaders().forEach(h -> {
						response.addHeader(h.getName(), h.getValue());
					});
				}
			} catch (Throwable e) {
				LogUtil.error("HttpResponse build error", e);
			} finally {
				cdl.countDown();
				if (channel != null) {
					try {
						channel.close();
					} catch (IOException e) {
						LogUtil.error("Close dowload stream fail: {}#{}", e.getClass().getName(), e.getMessage());
					}
				}

				if (handler != null) {
					handler.handle(response);
				}
			}
		}

		@Override
		public void onContent(org.eclipse.jetty.client.api.Response response, ByteBuffer bb) {
			if (channel == null) {
				byte[] buff = new byte[bb.remaining()];
				bb.get(buff);
				bao.write(buff, 0, buff.length);
			} else {
				try {
					channel.write(bb);
				} catch (Throwable e) {
					LogUtil.error("Write dowload stream fail: {}#{}", e.getClass().getName(), e.getMessage());
				}
			}
		}

		public org.fastj.rest.api.Response get() {
			try {
				cdl.await();
			} catch (InterruptedException e) {
			}
			return response;
		}

	}

	private static void setContent(Request req, Object content) {
		if (content != null) {
			if (content instanceof CharSequence || content.getClass().isPrimitive()) {
				req.content(new StringContentProvider(String.valueOf(content)));
			} else if (content instanceof Path) {
				try {
					req.content(new PathContentProvider((Path) content));
				} catch (IOException e) {
				}
			} else if (content instanceof File) {
				try {
					req.content(new PathContentProvider(((File) content).toPath()));
				} catch (IOException e) {
				}
			} else if (content instanceof byte[]) {
				req.content(new BytesContentProvider((byte[]) content));
			} else {
				String jc = JSON.toJson(content);
				req.content(new StringContentProvider(jc));
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {

		ResponseHandler handler = new ResponseHandler() {

			public void handle(Response resp) {
				System.out.println(System.currentTimeMillis() + "  invoked @" + Thread.currentThread().getName());
				System.out.println(resp.getHttpcode() + " " + resp.getContent());
			}

		};
		for (int i = 0; i < 10; i++)
			get("http://127.0.0.1:8080/fastj/test/sleep?time=2000", null, handler);
		System.out.println(Thread.currentThread().getName() + " >> " + System.currentTimeMillis());
		// System.out.println(r.getHttpcode());
		// System.out.println(r.getContent());
		// final org.fastj.rest.api.Response fr = r;
		// r.getHeaders().getKeys().forEach(k -> {
		// System.out.println((String) fr.getHeaders().get(k));
		// });
		// r = exec("POST", "http://127.0.0.1:8080/fastj/test/ping", null, null);
		// System.out.println(r.getHttpcode());
		// System.out.println(r.getContent());
		Thread.sleep(15000);
		System.exit(0);
	}

}
