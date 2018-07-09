package org.fastj.rest.api;

public class Responses {

	public Response get404() {
		return new Response(404, "404 Not found.");
	}

	public Response get404(Object message) {
		return new Response().setHttpcode(404).setContent(message);
	}

	public Response get400(Object message) {
		return new Response().setHttpcode(400).setContent(message);
	}

	public Response get500(Throwable t) {
		return new Response(500, t.getMessage());
	}

	public Response get500(Throwable t, String message) {
		return new Response(500, t.getMessage());
	}

	public Response get500(Object message) {
		return new Response().setHttpcode(500).setContent(message);
	}
}
