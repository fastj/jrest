package org.fastj.jetty;

import java.io.IOException;

import org.fastj.rest.api.Request;

public class JettyRequest extends Request<org.eclipse.jetty.server.Request>{

	@Override
	protected String readEntity(org.eclipse.jetty.server.Request entity) throws IOException {
		return readAll(entity.getInputStream());
	}
	
}
