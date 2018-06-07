package org.fastj.scheduler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface Invoker {
	
	String TMPATTERN = "([0-1][0-9]|2[0-3]):([0-5][0-9])";

	boolean take(int tp);
	
	void off();
	
	boolean isOff();
	
	public static Invoker simple(String start, String end, int interval) {
		return new SimpleInvoker(str2tp(start), str2tp(end), interval);
	}
	
	public static Invoker byMinute() {
		return new SimpleInvoker(true);
	}
	
	public static Invoker simple(int interval) {
		return new SimpleInvoker(0, 1439, interval);
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private Set<Integer> tpsets = new HashSet<>();
		
		public Invoker build() {
			return new TPSetInvoker(tpsets);
		}
		
		public Builder append(String start, String end, int interval) {
			int stp = str2tp(start);
			int etp = str2tp(end);
			if (stp < 0 || etp > 1439 || stp > etp) {
				throw new IllegalArgumentException("Invalid Params");
			}
			
			for (int i = stp; i <= etp; i += interval) {
				tpsets.add(i);
			}
			
			return this;
		}
		
		public Builder append(int ... tps) {
			Arrays.stream(tps).forEach(i -> tpsets.add(i));
			return this;
		}
		
	}

	class SimpleInvoker implements Invoker {

		int start = 0;
		int end = 1439;
		int interval = -1;
		boolean allwaysInvoke = false;
		boolean off = false;
		
		SimpleInvoker(int start, int end, int interval){
			this.start = start;
			this.end = end;
			this.interval = interval;
		}
		
		SimpleInvoker(boolean everyminute){
			this.allwaysInvoke = true;
		}

		public void off() {
			off = true; 
		}
		
		public boolean isOff() {
			return off || (!allwaysInvoke && interval <= 0);
		}
		
		public boolean take(int tp) {
			if (allwaysInvoke)
				return true;

			if (interval <= 0)
				return false;
			
			int delta = tp - start;
			if (delta >= 0 && tp - end <= 0 && delta % interval == 0) {
				return true;
			}
			
			return false;
		}
	}

	class TPSetInvoker implements Invoker {

		private Set<Integer> tpsets = new HashSet<>();
		
		private boolean off = false;

		public TPSetInvoker(Set<Integer> tps) {
			tpsets.addAll(tps);
		}
		
		public void off() {
			off = true; 
		}
		
		public boolean isOff() {
			return off || tpsets.isEmpty();
		}
		
		public boolean take(int tp) {
			return tpsets.contains(tp);
		}
	}
	
	static int str2tp(String tstr) {
		if (match(tstr)) {
			String pars[] = tstr.split(":");
			int h = Integer.valueOf(pars[0]);
			int m = Integer.valueOf(pars[1]);
			return h * 60 + m;
		}
		
		return -1;
	}
	
	static boolean match(String tstr) {
		return tstr != null && tstr.matches(TMPATTERN);
	}
}
