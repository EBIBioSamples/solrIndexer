package uk.ac.ebi.solrIndexer.threads;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;

@Component
// this makes sure that we have a different instance wherever it is used
@Scope("prototype")
public class GroupRepoCallable extends GroupCallable {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Iterable<String> accessions;
	
	public GroupRepoCallable(ConcurrentUpdateSolrClient client, ConcurrentUpdateSolrClient mergedClient, Iterable<String> accessions) {
		super();
		this.client = client;
		this.accessions = accessions;
		this.mergedClient = mergedClient;
	}

	@Override
	@Transactional
	public Integer call() throws Exception {
		log.info("Starting call()");

		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = null;
		int toReturn;
		try {
			em = emf.createEntityManager();
			AccessibleDAO<BioSampleGroup> dao = new AccessibleDAO<>(BioSampleGroup.class, em);

			List<BioSampleGroup> groups = new ArrayList<>();
			for (String accession : accessions) {
				log.trace(accession);
				groups.add(dao.find(accession));
			}

			this.groups = groups;
			toReturn = super.call();
		} finally {
			if (em != null && em.isOpen()) {
				em.close();
			}
		}
		log.info("Finished call()");
		return toReturn;
	}
}
