package main;


import javax.persistence.EntityManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.ebi.fg.core_model.resources.Resources;

public class App {
	private static final Logger logger = LogManager.getLogger(App.class.getName());
	
    public static void main( String[] args ) {
    	logger.info("Entering application.");

		EntityManagerFactory factory = Resources.getInstance().getEntityManagerFactory();

    }

}
