package uk.ac.ebi.solrIndexer.threads;

import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.repo.BioSampleGroupRepository;


@Component
public class GroupRepoCallable implements Callable<Integer> {

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
		Page<BioSampleGroup> page = bioSampleGroupRepository.findAll(new PageRequest(pageStart/pageSize, pageSize));
		return new GroupCallable(page, client).call();
	}

}
