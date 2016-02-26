package uk.ac.ebi.solrIndexer.threads;

import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.hibernate.Hibernate;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class ThreadGroupByOffset implements Callable<Integer> {

	

	private final ConcurrentUpdateSolrClient client;
	private final int offset;
	private final int max;
	
	public ThreadGroupByOffset(ConcurrentUpdateSolrClient client, int offset, int max) {
		this.client = client;
		this.offset =offset;
		this.max = max;
	}
	
	@Override
	public Integer call() throws Exception {


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
		
		ThreadGroup threadGroup = new ThreadGroup(result, client);
		
		return threadGroup.call();
	}

}
