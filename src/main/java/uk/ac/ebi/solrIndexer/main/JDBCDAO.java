package uk.ac.ebi.solrIndexer.main;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.resources.Resources;

@Component
public class JDBCDAO {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public List<String> getPublicSamples() {
		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = emf.createEntityManager();

		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
		Root<BioSample> root = criteriaQuery.from(BioSample.class);
		criteriaQuery.select(root.get("acc"));
		TypedQuery<String> query = em.createQuery(criteriaQuery);
		List<String> results = query.getResultList();

		em.close();

		return results;
	}

	public List<String> getPublicGroups() {
		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = emf.createEntityManager();

		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
		Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);
		criteriaQuery.select(root.get("acc"));
		TypedQuery<String> query = em.createQuery(criteriaQuery);
		List<String> results = query.getResultList();

		em.close();

		return results;
	}
}
