package org.fastj.rest.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class Request<T> {
	
	private String method;
	private String uri;
	
	private AttachTable headers = new AttachTable();
	
	private String content;
	private T entity;
	
	private String remoteIp;

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public AttachTable getHeaders() {
		return headers;
	}

	public void setHeaders(AttachTable headers) {
		if (headers == null) return;
		this.headers = headers;
	}

	public void addHeader(String h, String v){
		headers.put(h, v);
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}
	
	public String getContent() throws IOException
	{
		if (content != null) return content;
		if (this.entity == null) return null;
		return content = readEntity(entity);
	}
	
	protected abstract String readEntity(T entity) throws IOException;
	
	public String readAll(InputStream in) throws IOException
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream(in.available());
		byte[] buff = new byte[1024];
		int len = -1;
		for (len = in.read(buff); len > 0; bout.write(buff, 0, len), len = in.read(buff)) ;
		return bout.toString();
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}
}
