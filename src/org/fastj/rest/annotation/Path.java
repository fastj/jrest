package org.fastj.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Path {
	
	String GET = "GET";
	String POST = "POST";
	String PUT = "PUT";
	String DELETE = "DELETE";
	String PATCH = "PATCH";
	
	/**
	 * Request Method
	 * @return String
	 */
	String method() default GET;
	
	/**
	 * example: /book
	 * @return URI
	 */
	String uri() default "";
	
	/**
	 * Service export filter tags
	 * @return Service Tags
	 */
	String[] tags() default {};
}
