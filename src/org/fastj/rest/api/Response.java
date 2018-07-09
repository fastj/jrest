package org.fastj.rest.api;

public class Response {

	private int httpcode = 204;
	private AttachTable headers = new AttachTable();
	private AttachTable attachments = new AttachTable();
	private Object content;

	public Response() {
	}

	public Response(int code, String ct) {
		this.httpcode = code;
		this.content = ct;
	}

	public int getHttpcode() {
		return httpcode;
	}

	public Response setHttpcode(int httpcode) {
		this.httpcode = httpcode;
		return this;
	}

	public AttachTable getHeaders() {
		return headers;
	}

	public Response setHeaders(AttachTable headers) {
		this.headers = headers;
		return this;
	}

	public Response addHeader(String key, String v) {
		headers.put(key, v);
		return this;
	}

	public Object getContent() {
		return content;
	}

	public Response setContent(Object ct) {
		content = ct;
		return this;
	}

	public AttachTable getAttachments() {
		return attachments;
	}

}
