package uk.ac.ebi.solrIndexer.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;

public class BioSDEntities {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());
/*
	public static List<BioSample> fetchSamples(DataBaseConnection dbc) {
		log.debug("fetchSamples()");
		List<BioSample> samples = new ArrayList<BioSample>();

		if (dbc != null) {
			CriteriaBuilder criteriaBuilder = dbc.getEntityManager().getCriteriaBuilder();
			CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
			Root<BioSample> bioSample = criteriaQuery.from(BioSample.class);

			Calendar calendar = Calendar.getInstance();
			Date end = new Date(calendar.getTimeInMillis());
			calendar.set(0, 0, 0);
			Date start = new Date(calendar.getTimeInMillis());

			criteriaQuery.select(bioSample);
			//TypedQuery<BioSample> typedQuery = dbc.getEntityManager().createQuery(criteriaQuery);
			
			criteriaQuery.where(criteriaBuilder.between(bioSample.<Date>get("releaseDate"), start, end));
			TypedQuery<BioSample> typedQuery = dbc.getEntityManager().createQuery(criteriaQuery);
			samples = typedQuery.getResultList();
		}

		return samples;
	}
*/
	public static List<BioSampleGroup> fetchGroups(DataBaseConnection dbc) {
		log.debug("fetchGroups()");
		
		List<BioSampleGroup> groups = new ArrayList<BioSampleGroup>();

		if (dbc != null) {
			CriteriaBuilder criteriaBuilder = dbc.getEntityManager().getCriteriaBuilder();
			CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
			Root<BioSampleGroup> bioSampleGroup = criteriaQuery.from(BioSampleGroup.class);
			criteriaQuery.select(bioSampleGroup);
			TypedQuery<BioSampleGroup> query = dbc.getEntityManager().createQuery(criteriaQuery);
			groups = query.getResultList();
		}

		return groups;
	}

	@SuppressWarnings({ "rawtypes" })
	public static Collection<ExperimentalPropertyValue> fetchExperimentalPropertyValues(BioSample bs){
		log.debug("fetchExperimentalPropertyValues()");
		return bs.getPropertyValues();
	}

}
