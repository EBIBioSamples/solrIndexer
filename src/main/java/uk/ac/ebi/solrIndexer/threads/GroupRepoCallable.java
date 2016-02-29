package uk.ac.ebi.solrIndexer.threads;

import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.repo.BioSampleGroupRepository;


@Component
@Scope("prototype")
public class GroupRepoCallable implements Callable<Integer> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private BioSampleGroupRepository bioSampleGroupRepository;
	
	private ConcurrentUpdateSolrClient client;
	private int pageStart;
	private int pageSize;
		
	public GroupRepoCallable() {
		
	}
	
	public GroupRepoCallable(int pageStart, int pageSize, ConcurrentUpdateSolrClient client) {
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
		log.info("Processing groups "+pageStart+" to "+(pageStart+pageSize));
		Page<BioSampleGroup> page = bioSampleGroupRepository.findAll(new PageRequest(pageStart/pageSize, pageSize));
		int toReturn = new GroupCallable(page, client).call();
		log.info("Processed groups "+pageStart+" to "+(pageStart+pageSize));
		return toReturn;
	}
}
