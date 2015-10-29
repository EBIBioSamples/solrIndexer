package uk.ac.ebi.solrIndexer.main;

import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;

public class BioSDEntities {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());
	
	private static DataBaseConnection dbc = DataBaseConnection.getInstance();

	public static List<BioSample> fetchSamples() {
		log.debug("fetchSamples()");

		CriteriaBuilder criteria = dbc.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSample> querySample = criteria.createQuery(BioSample.class);
		List<BioSample> samples = dbc.getEntityManager().createQuery(querySample.select(querySample.from(BioSample.class))).getResultList();
		return samples;
	}

	public static List<BioSampleGroup> fetchGroups() {
		log.debug("fetchGroups()");

		CriteriaBuilder criteria = dbc.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSampleGroup> queryGroup = criteria.createQuery(BioSampleGroup.class);
		List<BioSampleGroup> groups = dbc.getEntityManager().createQuery(queryGroup.select(queryGroup.from(BioSampleGroup.class))).getResultList();
		return groups;
	}

	@SuppressWarnings({ "rawtypes" })
	public static Collection<ExperimentalPropertyValue> fetchExperimentalPropertyValues(BioSample bs){
		log.debug("fetchExperimentalPropertyValues()");
		return bs.getPropertyValues();
	}

}
