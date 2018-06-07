package org.fastj.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RAuth {
	
	int NONE = 0;
	int READ = 1;
	int CREATE = 2;
	int UPDATE = 4;
	int DELETE = 8;
	
	//int WRITE = CREATE | UPDATE | DELETE;
	//int ALL = READ | WRITE;
	
	/**
	 * @return ResAuth id
	 */
	int value() default NONE;
	
	/**
	 * MultiRAuth combine 
	 * @return OR logic
	 */
	boolean or() default true;
}
