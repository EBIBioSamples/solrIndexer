package uk.ac.ebi.solrIndexer.threads;


import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.solrIndexer.main.repo.BioSampleRepository;


@Component
//this makes sure that we have a different instance wherever it is used
@Scope("prototype")
public class SampleRepoCallable extends SampleCallable {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private BioSampleRepository bioSampleRepository;
	
	private int pageStart;
	private int pageSize;
		
	public SampleRepoCallable() {
		super();
	}
	
	public SampleRepoCallable(int pageStart, int pageSize, ConcurrentUpdateSolrClient client) {
		this.pageStart = pageStart;
		this.pageSize = pageSize;
		this.client = client;
	}
	
	public ConcurrentUpdateSolrClient getClient() {
		return client;
	}

	public void setClient(ConcurrentUpdateSolrClient client) {
		this.client = client;
	}

	public int getPageStart() {
		return pageStart;
	}

	public void setPageStart(int pageStart) {
		this.pageStart = pageStart;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	@Transactional
	public Integer call() throws Exception {
		log.info("Processing samples "+pageStart+" to "+(pageStart+pageSize));
		samples = bioSampleRepository.findAll(new PageRequest(pageStart/pageSize, pageSize));
		int toReturn = super.call();
		log.info("Processed samples "+pageStart+" to "+(pageStart+pageSize));
		return toReturn;
	}
}
