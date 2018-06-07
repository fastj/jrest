package org.fastj.pchk;

import java.lang.annotation.Annotation;

import org.fastj.pchk.annotation.DoubleEnum;
import org.fastj.pchk.annotation.IntEnum;
import org.fastj.pchk.annotation.StringEnum;

public class EnumChecker implements PChecker{

	@Override
	public String check(Object value, Annotation a) {
		
		if (value == null) {
			return null;
		}
		
		if (a instanceof StringEnum && value instanceof String) {
			StringEnum ra = (StringEnum) a;
			for (String v : ra.value()) {
				if (v.equals(value)) {
					return null;
				}
			}
			return ra.message();
		}
		else if (a instanceof IntEnum && value instanceof Integer) {
			IntEnum ra = (IntEnum) a;
			for (int v : ra.value()) {
				if (v == ((Integer)value).intValue()) {
					return null;
				}
			}
			return ra.message();
		}
		else if (a instanceof DoubleEnum && value instanceof Double) {
			DoubleEnum ra = (DoubleEnum) a;
			for (double v : ra.value()) {
				if (v == ((Double)value).doubleValue()) {
					return null;
				}
			}
			return ra.message();
		}
		
		return "Invalid Enum check Setting";
	}

}
