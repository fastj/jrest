package org.fastj.pchk.annotation;

public @interface IntEnum {
	int [] value();
	String message() default "Value({_value}) not in({value})";
}
