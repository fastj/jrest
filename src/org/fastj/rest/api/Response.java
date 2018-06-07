package org.fastj.rest.api;

public class Response {

	private int httpcode = 204;
	private AttachTable headers = new AttachTable();
	private AttachTable attachments = new AttachTable();
	private Object content;

	public Response(){}
	
	public Response(int code, String ct){
		this.httpcode = code;
		this.content = ct;
	}
	
	public int getHttpcode() {
		return httpcode;
	}

	public void setHttpcode(int httpcode) {
		this.httpcode = httpcode;
	}

	public AttachTable getHeaders() {
		return headers;
	}

	public void setHeaders(AttachTable headers) {
		this.headers = headers;
	}
	
	public void addHeader(String key, String v)
	{
		headers.put(key, v);
	}

	public Object getContent() {
		return content;
	}
	
	public void setContent(Object ct) {
		content = ct;
	}

	public AttachTable getAttachments() {
		return attachments;
	}

}
