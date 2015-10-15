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
	private static Logger log = LogManager.getLogger (App.class.getName());

	private static EntityManagerFactory factory;
	@PersistenceUnit(unitName="BioSampleGroup")
	private static EntityManager manager = null;
	private static EntityTransaction transaction = null;

	public DataBaseConnection() {
		try {
    		factory = Resources.getInstance().getEntityManagerFactory();
    		manager = factory.createEntityManager();
    		transaction = manager.getTransaction();
    		transaction.begin();

    		CriteriaBuilder criteria = manager.getCriteriaBuilder();
    		CriteriaQuery<BioSampleGroup> queryGroup = criteria.createQuery(BioSampleGroup.class);
    		List<BioSampleGroup> groups = manager.createQuery(queryGroup.select(queryGroup.from(BioSampleGroup.class))).getResultList();
    		int i = 0;
    		for (BioSampleGroup bsg : groups) {
    			System.out.println(bsg.getAcc());
    		  if ( ++i == 10 ) break;
    		}
    		
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
