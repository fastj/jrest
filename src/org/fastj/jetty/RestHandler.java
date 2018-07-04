package org.fastj.jetty;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.fastj.rest.api.Response;
import org.fastj.rest.api.ServiceManager;
import org.fastj.rest.api.Tracer;

public class RestHandler extends DefaultHandler {

	private String prefix = "/";
	private ServiceManager manager = null;

	public RestHandler(String path, ServiceManager service) {
		this.prefix = path;
		this.manager = service;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		String uri = baseRequest.getRequestURI();
		String method = baseRequest.getMethod().toUpperCase();
		String queryStr = baseRequest.getQueryString();

		if (!uri.startsWith(prefix)) {
			return;
		}

		JettyRequest req = new JettyRequest();
		String encoding = baseRequest.getCharacterEncoding() == null ? "UTF-8" : baseRequest.getCharacterEncoding();
		req.setUri(URLDecoder.decode(queryStr == null ? uri : uri + "?" + queryStr, encoding));
		req.setMethod(method);
		req.setEntity(baseRequest);
		req.setEncoding(encoding);

		Enumeration<String> hl = baseRequest.getHeaderNames();
		while (hl.hasMoreElements()) {
			String h = hl.nextElement();
			req.addHeader(h, baseRequest.getHeader(h));
		}

		Tracer tracer = Tracer.get(Tracer.HTTP_SERVER);
		if (tracer != null) {
			Map<String, Object> args = new HashMap<>();
			args.put(Tracer.KEY_METHOD, req.getMethod());
			args.put(Tracer.KEY_OPERATE_NAME, uri);
			args.put(Tracer.KEY_REMOTE_PEER, baseRequest.getRemoteHost() + ":" + baseRequest.getRemotePort());
			args.put(Tracer.KEY_URI, uri);
			tracer.start(args);
		}

		Response msg = null;
		try {
			msg = manager.process(req);
			ServerUtils.doResponse(baseRequest, response, msg);
		} finally {
			if (tracer != null && msg != null) {
				tracer.stop(msg.getHttpcode(), "");
			}
		}

	}

}
