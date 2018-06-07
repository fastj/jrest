package org.fastj.pchk;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import org.fastj.pchk.annotation.Size;

public class SizeChecker implements PChecker{

	@Override
	public String check(Object value, Annotation a) {
		
		if (value == null) {
			return null;
		}
		
		Size sa = (Size) a;
		
		if (value instanceof Collection<?>) {
			Collection<?> cv = (Collection<?>) value;
			boolean ok = cv.size() >= sa.min() && cv.size() <= sa.max();
			return ok ? null : sa.message();
		}
		if (value instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?,?>) value;
			boolean ok = map.size() >= sa.min() && map.size() <= sa.max();
			return ok ? null : sa.message();
		}
		
		return "Invalid size check setting";
	}

}
