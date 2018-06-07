package org.fastj.rest.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class AttachTable {
	
	private Map<String, Object> data = new HashMap<String, Object>();
	
	public void put(String key, Object v)
	{
		data.put(key, v);
	}
	
	public Collection<String> getKeys()
	{
		return data.keySet();
	}
	
	public String getHeader(String key)
	{
		if (data.containsKey(key))
		{
			return String.valueOf(data.get(key));
		}
		
		for (Iterator<Entry<String, Object>> it = data.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, Object> eo = it.next();
			if (eo.getKey().equalsIgnoreCase(key))
			{
				return String.valueOf(eo.getValue());
			}
		}
		
		return null;
	}
	
	public <T> T get(String key)
	{
		return (T) data.get(key);
	}
	
	public boolean hasKey(String key) {
		return data.containsKey(key);
	}
	
}
