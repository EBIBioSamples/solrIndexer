package main;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnit;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseConnection {
	static final Logger log = LogManager.getLogger(DataBaseConnection.class.getName());

	private static EntityManagerFactory factory;
	@PersistenceUnit(unitName="BioSampleGroup")
	private static EntityManager manager = null;
	private static EntityTransaction transaction = null;

	public DataBaseConnection() {
		try {
    		factory = Resources.getInstance().getEntityManagerFactory();
    		log.info("factory: " + factory);
    		manager = factory.createEntityManager();
    		log.info("manager: " + manager);
    		transaction = manager.getTransaction();
    		transaction.begin();

    		CriteriaBuilder criteria = manager.getCriteriaBuilder();
    		CriteriaQuery<BioSampleGroup> queryGroup = criteria.createQuery(BioSampleGroup.class);
    		List<BioSampleGroup> groups = manager.createQuery(queryGroup.select(queryGroup.from(BioSampleGroup.class))).getResultList();
    		for (int i = 0; i < 10; i++)
    			log.info(i + " - " + groups.get(i));
    		
    	} catch (Exception e) {
    		if(transaction != null && transaction.isActive()) {
    			transaction.rollback();
    			e.printStackTrace();
    		}
    		e.printStackTrace();
    	} finally {
    		if (manager != null && manager.isOpen()) {
    			manager.close();
    		}
    	}
	}

}
