package org.fastj.db;

import org.fastj.app.IConfig;
import static org.fastj.app.Args.*;

public class DBConfig implements IConfig {

	@Override
	public void config() {
		String configDir = get(ARG_CFG_DIR, DEF_CFG_DIR);
		HSessionUtils.scanConfig(configDir);
	}

}
