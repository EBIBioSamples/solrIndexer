package uk.ac.ebi.solrIndexer.main;

import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.CONTENT_TYPE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_NAME;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_URL;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.FORMATVERSION;
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

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.SolrInputDocument;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.solrIndexer.common.Formater;

public class SolrIndexer {

	private ConcurrentUpdateSolrClient client;
	//private Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

	private SolrIndexer() {
		client = new ConcurrentUpdateSolrClient("path to solr core", 10, 8);
		client.setSoTimeout(1000);
		client.setConnectionTimeout(1000);
		client.setParser(new XMLResponseParser());
	}

	public void buildIndexer() {
		//TODO
	}

	@SuppressWarnings({ "rawtypes" })
	public static SolrInputDocument generateBioSampleSolrDocument(BioSample bs) {
		SolrInputDocument document = new SolrInputDocument();
		document.addField(ID, bs.getId());
		document.addField(SAMPLE_ACC, bs.getAcc());
		document.addField(SAMPLE_UPDATE_DATE, Formater.formatDateToSolr(bs.getUpdateDate()));
		document.addField(SAMPLE_RELEASE_DATE, Formater.formatDateToSolr(bs.getReleaseDate()));
		document.addField(SUBMISSION_ACC, ""); //TODO
		document.addField(SUBMISSION_DESCRIPTION, ""); //TODO
		document.addField(SUBMISSION_TITLE, ""); //TODO
		document.addField(SUBMISSION_UPDATE_DATE, ""); //TODO
		document.addField(FORMATVERSION, ""); //TODO
		document.addField(DB_ACC, ""); //TODO
		document.addField(DB_NAME, ""); //TODO
		document.addField(DB_URL, ""); //TODO
		document.addField(CONTENT_TYPE, "sample");

		for (ExperimentalPropertyValue epv : bs.getPropertyValues()) {
			document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), epv.getTermText());
		}

		return document;
	}

	public static void generateBioSampleGroupSolrDocument(BioSampleGroup bsg) {
		SolrInputDocument document = new SolrInputDocument();
		document.addField(ID, bsg.getId());
		document.addField(GROUP_ACC, bsg.getAcc());
		document.addField(GROUP_UPDATE_DATE, Formater.formatDateToSolr(bsg.getUpdateDate()));
		/*
		String samples = "";
		for (BioSample bs : bsg.getSamples()) {
			samples += bs.getAcc() + ";";
		}
		document.addField(GROUP_SAMPLES, samples);
		 */
		document.addField(NUMBER_OF_SAMPLES, bsg.getSamples().size());
		document.addField(CONTENT_TYPE, "group");
	}
}
