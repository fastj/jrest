package org.fastj.pchk;

import java.lang.annotation.Annotation;

public interface PChecker {
	
	String check(Object value, Annotation a);
	
}
