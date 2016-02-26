package uk.ac.ebi.solrIndexer.main;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseConnection {
	private static Logger log = LoggerFactory.getLogger (DataBaseConnection.class.getName());

	private EntityManager manager = null;
	private Session session = null;

	public DataBaseConnection() {
		log.trace("Creating DataBaseConnection");

		//EntityTransaction transaction = null;
		try {
			EntityManagerFactory entityManagerFactory = Resources.getInstance().getEntityManagerFactory();
    		manager = entityManagerFactory.createEntityManager();
    		//transaction = manager.getTransaction();
    		//transaction.begin();

    		session = manager.unwrap(Session.class);
    		session.beginTransaction();
    		
		} catch (Exception e) {
			log.error("Rolling back.");
			session.getTransaction().rollback();
    		
    		log.error("Error while creating DataBaseConnection: ", e);

    	}
	}

	public EntityManager getEntityManager() {
		if (!manager.isOpen()) {
			new DataBaseConnection();
		}
		return manager;
	}

	public void closeDataBaseConnection() {
		if (session.isOpen()) {
			//session.getTransaction().commit();
			session.close();
		}
		//if (manager != null && manager.isOpen()) manager.close();
	}
}
