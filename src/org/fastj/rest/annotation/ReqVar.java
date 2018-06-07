package org.fastj.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ReqVar {
	
	String DEFAULT = "_NULL_";
	
	String name();
	
	String def() default DEFAULT;
}
