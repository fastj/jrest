package org.fastj.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.fastj.rest.api.AttachTable;
import org.fastj.rest.api.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerUtils {

	public static final String CONTENT_TYPE = "application/json;charset=utf-8";

	public static void doResponse(Request req, HttpServletResponse response, Response msg) throws IOException {

		req.setHandled(true);
		String encoding = req.getCharacterEncoding() == null ? "utf-8" : req.getCharacterEncoding();

		Object entity = msg.getContent();
		boolean noct = entity == null || "".equals(entity);
		response.setStatus(noct ? 204 : msg.getHttpcode());

		AttachTable respHeaders = msg.getHeaders();
		if (respHeaders != null) {
			if (respHeaders.getHeader("Content-Type") == null) {
				respHeaders.put("Content-Type", CONTENT_TYPE);
			}

			for (String h : respHeaders.getKeys()) {
				String hv = respHeaders.get(h);
				response.addHeader(h, hv);
			}
		}

		if (!noct) {
			if (entity instanceof String) {
				response.getOutputStream().write(((String) entity).getBytes(Charset.forName(encoding)));
			} else if (entity instanceof InputStream) {
				bridgeOut((InputStream) entity, response.getOutputStream());
			} else {
				ObjectMapper om = new ObjectMapper();

				String ct = om.writeValueAsString(entity);
				response.getOutputStream().write(ct.getBytes(Charset.forName(encoding)));
			}
			response.getOutputStream().flush();
		} else {
			response.getOutputStream().flush();
		}
	}

	private static void bridgeOut(InputStream in, OutputStream out) throws IOException {
		byte[] buff = new byte[1024];
		int len = -1;
		while ((len = in.read(buff)) > 0) {
			out.write(buff, 0, len);
		}
	}

}
