package uk.ac.ebi.solrIndexer.main;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseConnection {
	private static Logger log = LoggerFactory.getLogger (DataBaseConnection.class.getName());

	private static EntityManager manager = null;

	public DataBaseConnection() {
		log.debug("Creating DataBaseConnection");

		EntityTransaction transaction = null;
		try {
			EntityManagerFactory entityManagerFactory = Resources.getInstance().getEntityManagerFactory();
    		manager = entityManagerFactory.createEntityManager();
    		transaction = manager.getTransaction();
    		transaction.begin();

		} catch (Exception e) {
    		if(transaction != null && transaction.isActive()) {
    			log.error("Rolling back.");
    			transaction.rollback();
    		}
    		log.error("Error while creating DataBaseConnection: ", e);

    	}
	}

	public EntityManager getEntityManager() {
		return manager;
	}

	public void closeDataBaseConnection() {
		if (manager != null && manager.isOpen()) {
			manager.close();
		}	
	}
}
