package uk.ac.ebi.biosamples.solrindexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.solrindexer.service.CSVMappingService;
import uk.ac.ebi.biosamples.solrindexer.service.MyEquivalenceManager;
import uk.ac.ebi.biosamples.solrindexer.service.SolrManager;
import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;

@Component
// this makes sure that we have a different instance wherever it is used
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SampleRepoCallable implements Callable<Integer> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private ConcurrentUpdateSolrClient client;
	private ConcurrentUpdateSolrClient mergedClient;
	private Iterable<String> accessions;

	@Autowired
	private SolrManager solrManager;

	@Autowired
	private MyEquivalenceManager myEquivalenceManager;

	private CSVMappingService csvService;
	
	@Value("${solrindexer.solr.commitwithin:60000}")
	private int commitWithin;

	public SampleRepoCallable(ConcurrentUpdateSolrClient client, ConcurrentUpdateSolrClient mergedClient, Iterable<String> accessions, CSVMappingService csvService) {
		super();
		this.client = client;
		this.accessions = accessions;
		this.mergedClient = mergedClient;
		this.csvService = csvService;
	}

	@Override
	public Integer call() throws Exception {
		log.info("Starting call()");

		// setup the entity manager for interacting with relational database
		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = null;
		int toReturn;
		try {
			em = emf.createEntityManager();

			// start a transaction within the entity amanger
			EntityTransaction transaction = em.getTransaction();
			try {
				transaction.begin();
				transaction.setRollbackOnly();
				
				// we can get a connection to myEquivalents
				EntityMappingManager entityMappingManager = null;
				try {
					entityMappingManager = myEquivalenceManager.getManagerFactory().newEntityMappingManager();

					// and a connection to the annotation system within the
					// relational database
					AnnotatorAccessor annotator = null;
					try {
						annotator = new AnnotatorAccessor(em);

						toReturn = processSamples(accessions, em, entityMappingManager, annotator);

					} finally {
						if (annotator != null) {
							annotator.close();
						}
					}
				} finally {
					if (entityMappingManager != null) {
						entityMappingManager.close();
					}
				}
				
			} finally {
				transaction.rollback();
			}
		} finally {
			if (em != null && em.isOpen()) {
				em.close();
			}
		}
		log.info("Finished call()");

		return toReturn;
	}

	private int processSamples(Iterable<String> accessions, EntityManager em, EntityMappingManager entityMappingManager,
			AnnotatorAccessor annotator) throws SolrServerException, IOException {

		AccessibleDAO<BioSample> dao = new AccessibleDAO<>(BioSample.class, em);
		int count = 0;
		for (String accession : accessions) {
			
			log.trace("processing "+accession);
			
			BioSample sample = dao.find(accession);
			
			Optional<SolrInputDocument> doc = solrManager.generateBioSampleSolrDocument(sample,
					entityMappingManager, annotator);
			if (doc.isPresent()) {
				client.add(doc.get(), commitWithin);
				mergedClient.add(doc.get(), commitWithin);
				count += 1;
				
				//only output the csv if we managed to generate a solr document successfully
				if (csvService != null) {
					csvService.handle(sample, entityMappingManager);
				}
			}
			
		}
		return count;
	}
}
