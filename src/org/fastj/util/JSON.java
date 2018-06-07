package org.fastj.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fastj.log.LogUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSON {
	
	public static Map<String, Object> parse(String json) {
		ObjectMapper mapper = new ObjectMapper();
		
		if (json == null || (json = json.trim()).isEmpty())
		{
			return new HashMap<String, Object>();
		}
		
		Map<String, Object> jo = null;
		if (json.matches("^(\\{|\\[)[\\S\\s]*(\\}|\\])$"))
		{
			try {
				if (json.startsWith("{"))
				{
					jo = mapper.readValue(json, new JsonType<Map<String, Object>>());
				}
				else
				{
					Object l = mapper.readValue(json, new JsonType<List<Object>>());
					jo = new HashMap<>();
					jo.put("list", l);
				}
			} catch (Throwable e) {
			}
		}
		
		return jo;
	}
	
	public static List<?> parseList(String json) {
		if (json.startsWith("[") && json.endsWith("]")) {
			Map<String, Object> ml = parse(json);
			return (List<?>) ml.get("list");
		}
		
		return new ArrayList<>();
	}
	
	public static <T> T toBean(Class<T> c, String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, c);
		} catch (Throwable e) {
			LogUtil.error("parse bean fail", e);
			return null;
		}
	}
	
	public static <T> List<T> toList(Class<T> c, String json) {
		List<?> l = parseList(json);
		List<T> rlt = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		l.forEach(o -> {
			T t = mapper.convertValue(o, c);
			rlt.add(t);
		});
		return rlt;
	}
	
	public static String toJson(Object o) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(o);
		} catch (Throwable e) {
			LogUtil.error("to json fail", e);
			return e.getMessage();
		}
	}
	
}
