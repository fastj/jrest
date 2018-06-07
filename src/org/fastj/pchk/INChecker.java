package org.fastj.pchk;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.fastj.pchk.annotation.StringEnum;

public class INChecker implements PChecker{

	@Override
	public String check(Object value, Annotation a) {
		
		if (value == null) {
			return null;
		}
		
		System.out.println(value);
		System.out.println(a);
		
		StringEnum in = (StringEnum) a;
		
		String[] enumValue = in.value();
		
		String v = value == null ? "null" : value.toString();
		
		for (String ev : enumValue) {
			if (v.equals(ev)) {
				return null;
			}
		}
		
		return in.message();
	}
	
	public static void main(String[] args) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		
		
		INChecker inc = new INChecker();
		Field f = INChecker.class.getDeclaredField("anoo");
		Annotation [] as = f.getAnnotations();
		
		inc.check(f.get(inc), as[0]);
		
	}

}
