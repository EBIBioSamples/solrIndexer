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

	public static List<String> getOntologyFromAnnotator(ExperimentalPropertyValue<?> epv) {
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
