package uk.ac.ebi.solrIndexer.main;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;

public class DataBaseManager {
	private static Logger log = LoggerFactory.getLogger (DataBaseManager.class.getName());

	public static List<BioSampleGroup> getAllIterableGroups (int offset, int max) {
		log.debug("Fetching Groups . . .");

		DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
		Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);

		criteriaQuery.select(root);
		List<BioSampleGroup> result = connection.getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();

		// Force eager initialization
		result.forEach(bsg -> bsg.getMSIs().forEach(m -> Hibernate.initialize( m.getDatabaseRecordRefs() )));
		result.forEach(bsg -> bsg.getPropertyValues().forEach(epv -> Hibernate.initialize( epv.getTermText() )));
		result.forEach(bsg -> bsg.getPropertyValues().forEach(epv -> Hibernate.initialize( epv.getType() )));
		result.forEach(bsg -> bsg.getPropertyValues().forEach(epv -> epv.getOntologyTerms().forEach(oe -> Hibernate.initialize( oe.getAcc() ))));

		connection.closeDataBaseConnection();
		return result;
	}

	public static List<BioSample> getAllIterableSamples (int offset, int max) {
		log.debug("Fetching Samples . . .");

		DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
		Root<BioSample> root = criteriaQuery.from(BioSample.class);

		criteriaQuery.select(root);
		List<BioSample> result = connection.getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();

		// Force eager initialization
		result.forEach(bs -> bs.getMSIs().forEach(m -> Hibernate.initialize(m.getDatabaseRecordRefs())));
		result.forEach(bs -> bs.getPropertyValues().forEach(epv -> Hibernate.initialize(epv.getTermText())));
		result.forEach(bs -> bs.getPropertyValues().forEach(epv -> Hibernate.initialize( epv.getType().getTermText() )));
		result.forEach(bs -> bs.getPropertyValues().forEach(epv -> epv.getOntologyTerms().forEach(oe -> Hibernate.initialize( oe.getAcc() ))));

		connection.closeDataBaseConnection();
		return result;
	}

	public static int getGroupCount() {
	    EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
	    EntityManager em = emf.createEntityManager();
	    AccessibleDAO<BioSampleGroup> dao = new AccessibleDAO<>(BioSampleGroup.class, em);
	    //cast this to int
	    //unlikely to really need a long for this! ~2 billion
	    int count = (int) dao.count();
	    return count;
	}

	public static int getSampleCount() {
	    EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
	    EntityManager em = emf.createEntityManager();
	    AccessibleDAO<BioSample> dao = new AccessibleDAO<>(BioSample.class, em);
	    //cast this to int
	    //unlikely to really need a long for this! ~2 billion
	    int count = (int) dao.count();
	    return count;
	}

	@SuppressWarnings("rawtypes")
	public static List<String> getOntologyFromAnnotator(ExperimentalPropertyValue epv) {
		DataBaseConnection connection = new DataBaseConnection();
		EntityManager manager = connection.getEntityManager();
		AnnotatorAccessor annotator = new AnnotatorAccessor(manager);

		List<String> urls = new ArrayList<String>();
		List<OntologyEntry> ontologies = annotator.getAllOntologyEntries(epv);
		ontologies.forEach(oe -> urls.add(oe.getAcc()));

		connection.closeDataBaseConnection();
		return urls;
	}
}
