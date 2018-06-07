package org.fastj.pchk;

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fastj.pchk.annotation.Length;

public class LengthChecker implements PChecker{

	@Override
	public String check(Object value, Annotation a) {
		if (value == null) {
			return null;
		}
		
		Length l = (Length) a;
		int min = l.min();
		int max = l.max();
		
		if (value instanceof String) {
			String v = (String) value;
			return v.length() >= min && v.length() <=max ? null : formatMsg(l.message(), String.valueOf(l.min()), String.valueOf(l.max()), v.length());
		}
		
		return "Only String parameter can use Length Annotation.";
	}

	private String formatMsg(String msg, String min, String max, int value) {
		Matcher m = Pattern.compile("\\{[A-Za-z0-9_]{1,}\\}").matcher(msg);
		StringBuilder buff = new StringBuilder();
		int astart = 0;
		while(m.find()) {
			int mstart = m.start();
			for (int i = astart; i < mstart; buff.append(msg.charAt(i)), i++);
			astart = m.end();
			String mstr = m.group();
			buff.append("{min}".equals(mstr) ? min : "{max}".equals(mstr) ? max : "{_value}".equals(mstr) ? value : mstr);
		}
		for (int i = astart; i < msg.length(); buff.append(msg.charAt(i)), i++);
		return buff.toString();
	}
	
}
