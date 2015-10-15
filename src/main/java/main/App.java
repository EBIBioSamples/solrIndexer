package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
	private static Logger log = LogManager.getLogger (App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");

    	DataBaseConnection dbc = new DataBaseConnection();
    }

}
