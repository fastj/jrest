package org.fastj.rest.api;

public interface SecurityChecker {
	
	Response checkAccess(RestService rest, AuthNode[] resAuthPairs);
	
	class AuthNode {
		public String resourceId;
		public int auth = 0;
		public boolean or = true;
	}
}
