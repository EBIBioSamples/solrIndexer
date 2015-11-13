package uk.ac.ebi.solrIndexer.main;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseConnection {
	private static Logger log = LoggerFactory.getLogger (App.class.getName());

	private static DataBaseConnection instance = new DataBaseConnection();

	private static EntityManagerFactory entityManagerFactory;
	private EntityManager manager = null;
	private static EntityTransaction transaction = null;

	private DataBaseConnection() {
		log.debug("Creating DataBaseConnection");
		try {
    		entityManagerFactory = Resources.getInstance().getEntityManagerFactory();
    		manager = entityManagerFactory.createEntityManager();
    		transaction = manager.getTransaction();
    		transaction.begin();

    	} catch (Exception e) {
    		if(transaction != null && transaction.isActive()) {
    			log.error("Rolling back.");
    			transaction.rollback();
    		}
    		log.error("Error while creating DataBaseConnection: ", e);
    		instance = null;
    	}
	}

	public static DataBaseConnection getInstance() {
		return instance;
	}

	public EntityManager getEntityManager() {
		return this.manager;
	}

}
