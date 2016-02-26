package uk.ac.ebi.solrIndexer.threads;

import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.hibernate.Hibernate;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class ThreadSampleByOffset implements Callable<Integer> {

	

	private final ConcurrentUpdateSolrClient client;
	private final int offset;
	private final int max;
	
	public ThreadSampleByOffset(ConcurrentUpdateSolrClient client, int offset, int max) {
		this.client = client;
		this.offset =offset;
		this.max = max;
	}
	
	@Override
	public Integer call() throws Exception {
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
		
		ThreadSample threadSample = new ThreadSample(result, client, null);
		
		return threadSample.call();
	}

}
