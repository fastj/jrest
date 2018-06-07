package org.fastj.pchk.annotation;

public @interface DoubleEnum {
	double [] value();
	String message() default "Value({_value}) not in({value})";
}
