package main;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseInteraction {
	private static Logger log = LoggerFactory.getLogger (App.class.getName());

	private static EntityManagerFactory factory;
	//@PersistenceUnit(unitName="BioSampleGroup")
	private EntityManager manager = null;
	private static EntityTransaction transaction = null;

	public DataBaseInteraction() {
		log.debug("Creating DataBaseInteraction");
		try {
    		factory = Resources.getInstance().getEntityManagerFactory();
    		manager = factory.createEntityManager();
    		transaction = manager.getTransaction();
    		transaction.begin();

    	} catch (Exception e) {
    		if(transaction != null && transaction.isActive()) {
    			transaction.rollback();
    			e.printStackTrace();
    		}
    		e.printStackTrace();
    	}
	}

	public List<BioSampleGroup> fetchGroups() {
		log.debug("fetchGroups()");

		CriteriaBuilder criteria = manager.getCriteriaBuilder();
		CriteriaQuery<BioSampleGroup> queryGroup = criteria.createQuery(BioSampleGroup.class);
		List<BioSampleGroup> groups = manager.createQuery(queryGroup.select(queryGroup.from(BioSampleGroup.class))).getResultList();
		return groups;
	}

	public List<BioSample> fetchSamples() {
		log.debug("fetchSamples()");

		CriteriaBuilder criteria = manager.getCriteriaBuilder();
		CriteriaQuery<BioSample> queryGroup = criteria.createQuery(BioSample.class);
		List<BioSample> samples = manager.createQuery(queryGroup.select(queryGroup.from(BioSample.class))).getResultList();
		return samples;
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
