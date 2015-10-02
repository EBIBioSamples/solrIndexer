package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBaseConnection {
	static final Logger logger = LogManager.getLogger(DataBaseConnection.class.getName());

	public boolean doIt() {
		logger.entry();
		logger.trace("trace");
		logger.error("did it again");
		return logger.exit(false);
	}

	
}
