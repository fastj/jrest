package org.fastj.pchk;

import java.lang.annotation.Annotation;

import org.fastj.pchk.annotation.DoubleRange;
import org.fastj.pchk.annotation.Range;

public class RangeChecker implements PChecker{

	@Override
	public String check(Object value, Annotation a) {
		
		if (value == null) {
			return null;
		}
		
		double dv = getDoubleValue(value);
		if (dv == Double.NaN) {
			return "Invalid Range check setting: value NaN";
		}
		
		if (a instanceof DoubleRange) {
			DoubleRange dr = (DoubleRange) a;
			boolean ok = dv >= dr.min() && dv <= dr.max();
			return ok ? null : dr.message();
		}
		else if (a instanceof Range) {
			Range r = (Range) a;
			boolean ok = dv >= r.min() && dv <= r.max();
			return ok ? null : r.message();
		}
		
		return "Invalid Range check setting: Unkown";
	}
	
	private double getDoubleValue(Object value) {
		
		if (value instanceof Number) {
			Number n = (Number) value;
			return n.doubleValue();
		}
		else if (value instanceof String) {
			try {
				return Double.valueOf((String)value);
			} catch (NumberFormatException e) {
				return Double.NaN;
			}
		}
		
		return Double.NaN;
	}
	
	
}
