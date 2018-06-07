package org.fastj.rest.api;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.fastj.rest.annotation.ReqVar;

public class RestService {
	
	private Request<?> request;
	private AttachTable pathParameters = new AttachTable();
	private AttachTable queryParameters = new AttachTable();
	private Object[] args;
	
	private Response response = new Response();
	
	public void setRequest(Request<?> request, String pathDef) {
		this.request = request;
		parsePath(pathDef);
	}

	public Request<?> getRequest() {
		return request;
	}

	public String getPathParameter(String name)
	{
		return pathParameters.get(name);
	}
	
	public AttachTable pathParams() {
		return pathParameters;
	}
	
	public AttachTable queryParams() {
		return queryParameters;
	}
	
	public String getQueryParameter(String qname, String def) {
		return queryParameters.hasKey(qname) ? queryParameters.get(qname) : ReqVar.DEFAULT.equals(def) ? null : def;
	}
	
	public <T> T getRequest(Class<T> tclazz)
	{
		return null;
	}
	
	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public void saveFile(File dir)
	{
		
	}
	
	public void setHttpCode(int httpcode)
	{
		response.setHttpcode(httpcode);
	}
	
	public void addHeader(String hname, String hvalue)
	{
		response.addHeader(hname, hvalue);
	}
	
	public void setResponse(String content) throws UnsupportedEncodingException
	{
		response.setContent(content);
	}
	
	public Response getResponse() {
		return response;
	}

	private void parsePath(String pathDef)
	{
		String pathStr = this.request.getUri();
		String uri = pathStr.contains("?") ? pathStr.substring(0, pathStr.indexOf('?')) : pathStr;
		String queryStr = pathStr.contains("?") ? pathStr.substring(pathStr.indexOf('?') + 1, pathStr.length()) : null;
		
		String pstr = pathDef;
		if (pstr.contains("{"))
		{
			String parts[] = pstr.split("/");
			String rparts[] = uri.split("/");
			
			if (parts.length != rparts.length) throw new RuntimeException("Path Not Match.");
			
			for (int i = 0;i < parts.length; i++)
			{
				if (parts[i].startsWith("{"))
				{
					String reqName = parts[i].substring(1, parts[i].length() - 1);
					pathParameters.put(reqName, rparts[i]);
				}
			}
		}
		
		if (queryStr != null && !queryStr.isEmpty())
		{
			String[] parts = queryStr.split("&");
			for (String qp : parts)
			{
				if (qp.contains("="))
				{
					String[] par = qp.split("=");
					queryParameters.put(par[0], par.length > 1 ? par[1] : "");
				}
				else
				{
					queryParameters.put(qp, "");
				}
			}
		}
		
	}
}
