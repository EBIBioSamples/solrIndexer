package uk.ac.ebi.solrIndexer.main;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseInteraction {
	private static Logger log = LoggerFactory.getLogger (App.class.getName());

	private static EntityManagerFactory entityManagerFactory;
	private EntityManager manager = null;
	private static EntityTransaction transaction = null;

	public DataBaseInteraction() {
		log.debug("Creating DataBaseInteraction");
		try {
			if (!transaction.isActive()) {
	    		entityManagerFactory = Resources.getInstance().getEntityManagerFactory();
	    		manager = entityManagerFactory.createEntityManager();
	    		transaction = manager.getTransaction();
	    		transaction.begin();
			}

    	} catch (Exception e) {
    		if(transaction != null && transaction.isActive()) {
    			transaction.rollback();
    			e.printStackTrace();
    		}
    		e.printStackTrace();
    	}
	}

	public EntityManager getEntityManager() {
		return this.manager;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataBaseInteraction []");
		return builder.toString();
	}

}
