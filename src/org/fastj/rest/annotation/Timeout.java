package org.fastj.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Timeout {
	
	int DEFAULT_TIMEOUT = 30000;
	
	int value() default DEFAULT_TIMEOUT; //default 30 seconds
}
