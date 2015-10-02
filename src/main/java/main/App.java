package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
	private static final Logger logger = LogManager.getLogger(App.class);
	
    public static void main( String[] args ) {
    	logger.trace("Entering application.");
    	
    	DataBaseConnection dbc = new DataBaseConnection();
    	if (!dbc.doIt()) {
    		logger.error("Didn't do it.");
    	}
    	logger.trace("Exiting application.");

    }
}
