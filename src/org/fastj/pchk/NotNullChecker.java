package org.fastj.pchk;

import java.lang.annotation.Annotation;

public class NotNullChecker implements PChecker{

	@Override
	public String check(Object value, Annotation a) {
		return value != null ? null : "Null value is not allowed.";
	}

}
