package uk.ac.ebi.solrIndexer.main;

import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.CONTENT_TYPE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_NAME;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_URL;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.GROUP_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.GROUP_UPDATE_DATE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.ID;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.NUMBER_OF_SAMPLES;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SAMPLE_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SAMPLE_RELEASE_DATE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SAMPLE_UPDATE_DATE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_DESCRIPTION;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_TITLE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_UPDATE_DATE;

import java.util.Set;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.solrIndexer.common.Formater;

public class SolrIndexer {
	private static Logger log = LoggerFactory.getLogger (SolrIndexer.class.getName());

	private static SolrIndexer instance = null;
	private ConcurrentUpdateSolrClient client;

	private SolrIndexer() {
		log.debug("Create SolrIndexer Client Connection");
		client = new ConcurrentUpdateSolrClient("", 10, 8); //FIXME
		client.setSoTimeout(1000);
		client.setConnectionTimeout(1000);
		client.setParser(new XMLResponseParser());
	}

	public static synchronized SolrIndexer getInstance() {
		if (instance == null) {
			instance = new SolrIndexer();
		}
		return instance;
	}

	public ConcurrentUpdateSolrClient getConcurrentUpdateSolrClient() {
		return this.client;
	}

	public static SolrInputDocument generateBioSampleGroupSolrDocument(BioSampleGroup bsg) {
		SolrInputDocument document = new SolrInputDocument();

		document.addField(ID, bsg.getId());
		document.addField(GROUP_ACC, bsg.getAcc());
		document.addField(GROUP_UPDATE_DATE, Formater.formatDateToSolr(bsg.getUpdateDate()));
		document.addField(CONTENT_TYPE, "group");
		document.addField(NUMBER_OF_SAMPLES, bsg.getSamples().size());
		//document.addField(GROUP_SAMPLES, samples); TODO - is required?

		return document;
	}

	@SuppressWarnings({ "rawtypes" })
	public static SolrInputDocument generateBioSampleSolrDocument(BioSample bs) {
		SolrInputDocument document = new SolrInputDocument();

		Set<MSI> msi = bs.getMSIs();
		if (msi.size() > 1) {
			StringBuffer msiAccs = new StringBuffer();
			msi.forEach(m -> msiAccs.append(m.getAcc() + "|"));
			log.error("Sample with accession [" + bs.getAcc() + "] has multiple MSI [" + msiAccs.substring(0, msiAccs.length() - 1) + "] - sample skipped.");
			return null;
		} 

		document.addField(ID, bs.getId());
		document.addField(SAMPLE_ACC, bs.getAcc());
		document.addField(SAMPLE_UPDATE_DATE, Formater.formatDateToSolr(bs.getUpdateDate()));
		document.addField(SAMPLE_RELEASE_DATE, Formater.formatDateToSolr(bs.getReleaseDate()));
		document.addField(CONTENT_TYPE, "sample");

		if (msi.iterator().hasNext()) {
			MSI submission = msi.iterator().next();
			document.addField(SUBMISSION_ACC,submission.getAcc());
			document.addField(SUBMISSION_DESCRIPTION,submission.getDescription());
			document.addField(SUBMISSION_TITLE, submission.getTitle());
			document.addField(SUBMISSION_UPDATE_DATE,Formater.formatDateToSolr(submission.getUpdateDate()));
		}

		Set<DatabaseRecordRef> db = bs.getDatabaseRecordRefs();
		if (db.iterator().hasNext()) {
			DatabaseRecordRef dbrr = db.iterator().next();
			document.addField(DB_ACC, dbrr.getAcc());
			document.addField(DB_NAME, dbrr.getDbName());
			document.addField(DB_URL, dbrr.getUrl());
		}

		for (ExperimentalPropertyValue epv : bs.getPropertyValues()) {
			document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), epv.getTermText());
		}

		return document;
	}

}
