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
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;

@Component
public class BioSDDAO {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public int getMSICount() {
		return getCount(MSI.class);
	}
	
	public List<String> getMSIAccessions() {
		return getSampleAccessions(-1, -1);
	}

	public List<String> getMSIAccessions(int startPosition, int maxResultCount) {
		return getAccessions(MSI.class, startPosition, maxResultCount);
	}	

	public int getSampleCount() {
		return getCount(BioSample.class);
	}
	
	public List<String> getSampleAccessions() {
		return getSampleAccessions(-1, -1);
	}

	public List<String> getSampleAccessions(int startPosition, int maxResultCount) {
		return getAccessions(BioSample.class, startPosition, maxResultCount);
	}

	public int getGroupCount() {
		return getCount(BioSampleGroup.class);
	}

	public List<String> getGroupAccessions() {
		return getGroupAccessions(-1, -1);
	}

	public List<String> getGroupAccessions(int startPosition, int maxResultCount) {
		return getAccessions(BioSampleGroup.class, startPosition, maxResultCount);
	}
	
	private int getCount(Class<?> clazz) {
		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = null;
		int result = 0;
		try {
			em = emf.createEntityManager();
			AccessibleDAO<?> dao = new AccessibleDAO(clazz, em);
			result = (int) dao.count();
		} finally {
			if (em != null) {
				em.close();
			}
		}
		return result;
	}
	
	private List<String> getAccessions(Class<?> clazz, int startPosition, int maxResultCount) {
		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = null;
		List<String> results = null;
		try {
			em = emf.createEntityManager();
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
			Root<?> root = criteriaQuery.from(clazz);
			criteriaQuery.select(root.get("acc"));
			TypedQuery<String> query = em.createQuery(criteriaQuery);
			if (startPosition > 0) {
				query.setFirstResult(startPosition);
			}
			if (maxResultCount > 0) {
				query.setMaxResults(maxResultCount);
			}
			results = query.getResultList();
		} finally {
			if (em != null) {
				em.close();
			}
		}
		return results;
		
	}
}
