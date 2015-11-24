package uk.ac.ebi.solrIndexer.main;

import java.sql.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.resources.Resources;

public class DataBaseManager {
	private static Logger log = LoggerFactory.getLogger (App.class.getName());

	private static DataBaseManager connection = null;

	private static EntityManagerFactory entityManagerFactory;
	private static EntityManager manager = null;
	private static EntityTransaction transaction = null;

	private DataBaseManager() {
		log.debug("Creating DataBaseManager");
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
    		log.error("Error while creating DataBaseManager: ", e);
    		connection = null;
    	}
	}

	public synchronized static DataBaseManager getConnection() {
		if (connection == null) {
			connection = new DataBaseManager();
		}
		return connection;
	}

	public synchronized static void closeConnection() {
    	if (manager != null && manager.isOpen()) {
    		manager.close();
    		connection = null;
		}
	}

	public EntityManager getEntityManager() {
		return manager;
	}

	/* --------------------- */
	/* -- Querying the DB -- */
	/* --------------------- */

	/**
	 * Queries the Biosamples DB retrieving all the groups.
	 * @return List<BioSampleGroup>
	 */
	public static List<BioSampleGroup> fetchGroups() {
		log.debug("Fetching Groups . . .");

		CriteriaBuilder criteriaBuilder = getConnection().getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
		Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);

		criteriaQuery.select(root);
		TypedQuery<BioSampleGroup> query = getConnection().getEntityManager().createQuery(criteriaQuery);
		List<BioSampleGroup> groups = query.getResultList();

		return groups;
	}

	/**
	 * Queries the Biosamples DB retrieving all the public submissions accessions.
	 * @return List<String>
	 */
	public static List<String> fetchSubmissionsAccessions() {
		log.debug("Fetching Samples Accessions . . .");

		EntityManager entityManager = getConnection().getEntityManager();
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
		Root<MSI> root = criteriaQuery.from(MSI.class);

		Calendar calendar = Calendar.getInstance();
		Date end = new Date(calendar.getTimeInMillis());
		calendar.set(0, 0, 0);
		Date start = new Date(calendar.getTimeInMillis());

		criteriaQuery.select(root.<String>get("acc"));
		criteriaQuery.where(criteriaBuilder.between(root.get("releaseDate"), start, end));
		TypedQuery<String> typedQuery = getConnection().getEntityManager().createQuery(criteriaQuery);
		List<String> accessions = (List<String>) typedQuery.getResultList();

		return accessions;
	}

	/**
	 * Queries the Biosamples DB retrieving the submission with the accession acc.
	 * @param acc
	 * @return
	 */
	public static MSI fetchSubmission(String acc) {
		log.debug("Fetching Submission with accession: " + acc);
	
		EntityManager entityManager = getConnection().getEntityManager();
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<MSI> criteriaQuery = criteriaBuilder.createQuery(MSI.class);
		Root<MSI> root = criteriaQuery.from(MSI.class);

		criteriaQuery.select(root);
		criteriaQuery.where(criteriaBuilder.equal(root.get("acc"), acc));
		TypedQuery<MSI> query = getConnection().getEntityManager().createQuery(criteriaQuery);
		MSI msi = (MSI) query.getResultList().get(0);
		return msi;
	}

	/**
	 * Queries the Biosamples DB retrieving the sample with the accession acc.
	 * @param acc Accession
	 * @return BioSample
	 */
	public static BioSample fetchSample (String acc) {
		log.debug("Fetching Sample with accession: " + acc);

		CriteriaBuilder criteriaBuilder = getConnection().getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
		Root<BioSample> root = criteriaQuery.from(BioSample.class);

		criteriaQuery.select(root);
		criteriaQuery.where(criteriaBuilder.equal(root.get("acc"), acc));
		TypedQuery<BioSample> query = getConnection().getEntityManager().createQuery(criteriaQuery);
		BioSample sample = (BioSample) query.getResultList();

		return sample;
	}

	/**
	 * Receives a BioSample and returns all its properties pairs.
	 * @param bs BioSample
	 * @return Collection<ExperimentalPropertyValue>
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Collection<ExperimentalPropertyValue> fetchExperimentalPropertyValues(BioSample bs){
		log.debug("fetchExperimentalPropertyValues()");
		return bs.getPropertyValues();
	}
}
