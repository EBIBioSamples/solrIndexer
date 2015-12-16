package uk.ac.ebi.solrIndexer.main;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseManager {
	private static Logger log = LoggerFactory.getLogger (App.class.getName());

	//private static EntityManagerFactory entityManagerFactory;
	private static EntityManager manager = null;
	//private static EntityTransaction transaction = null;

	private DataBaseManager() {
		log.debug("Creating DataBaseManager");
		
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
    		log.error("Error while creating DataBaseManager: ", e);

    	}
	}

	private static EntityManager getEntityManager() {
		return manager;
	}

	public static void closeDataBaseConnection() {
		if (manager != null && manager.isOpen()) {
			manager.close();
		}	
	}

	/* --------------------- */
	/* -- Querying the DB -- */
	/* --------------------- */

	public static List<BioSampleGroup> getAllIterableGroups (int offset, int max) {
		log.debug("Fetching Groups . . .");

		new DataBaseManager();
		CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
		Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);

		criteriaQuery.select(root);
		List<BioSampleGroup> result = getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();

		//DataBaseManager.closeDataBaseConnection();
		return result;
	}

	public static List<BioSample> getAllIterableSamples (int offset, int max) {
		log.debug("Fetching Samples . . .");

		CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
		Root<BioSample> root = criteriaQuery.from(BioSample.class);

		criteriaQuery.select(root);
		return getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();
	}
}
