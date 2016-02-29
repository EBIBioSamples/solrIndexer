package uk.ac.ebi.solrIndexer.main;

import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.CONTENT_TYPE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_NAME;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_URL;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.GROUP_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.GROUP_UPDATE_DATE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.ID;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SAMPLE_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SAMPLE_RELEASE_DATE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SAMPLE_UPDATE_DATE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_DESCRIPTION;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_TITLE;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.SUBMISSION_UPDATE_DATE;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.solrIndexer.common.Formater;

@Component
public class SolrManager {
	
	private Logger log = LoggerFactory.getLogger (this.getClass());

	@Value("${onto.mapping.annotator:false}")
	private boolean useAnnotator;

	//Generate Group Solr Document
	public SolrInputDocument generateBioSampleGroupSolrDocument(BioSampleGroup bsg) {
		
		log.trace("Creating solr document for group "+bsg.getAcc());
		
		SolrInputDocument document;

		document = new SolrInputDocument();

		document.addField(ID, bsg.getId());
		document.addField(GROUP_ACC, bsg.getAcc());
		document.addField(GROUP_UPDATE_DATE, Formater.formatDateToSolr(bsg.getUpdateDate()));
		document.addField(CONTENT_TYPE, "group");

		Set<MSI> msi = bsg.getMSIs();
		if (msi.iterator().hasNext()) {
			MSI submission = msi.iterator().next();
			document.addField(SUBMISSION_ACC,submission.getAcc());
			document.addField(SUBMISSION_DESCRIPTION,submission.getDescription());
			document.addField(SUBMISSION_TITLE, submission.getTitle());
			document.addField(SUBMISSION_UPDATE_DATE,Formater.formatDateToSolr(submission.getUpdateDate()));

			Set<DatabaseRecordRef> db = submission.getDatabaseRecordRefs();
			if (db.iterator().hasNext()) {
				DatabaseRecordRef dbrr = db.iterator().next();
				document.addField(DB_ACC, dbrr.getAcc());
				document.addField(DB_NAME, dbrr.getDbName());
				document.addField(DB_URL, dbrr.getUrl());
			}
		}

		for (ExperimentalPropertyValue<?> epv : bsg.getPropertyValues()) {
			document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), epv.getTermText());

			// Ontologies from Annotator
			if (useAnnotator) {
				List<String> urls = new ArrayList<>();

				//DataBaseManager dbm = new DataBaseManager();
				//urls = dbm.getOntologyFromAnnotator(epv);

				for (String url : urls) {
					if (url != null) {
						document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url);
					}
				}
			// Ontologies from Submission
			} else {
				Optional<String> url = getProvidedOntologyTerms(epv);
				if (url.isPresent()) {
					document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url.get());
				}
			}
		}

		return document;
	}

	//Generate Sample Solr Document
	public SolrInputDocument generateBioSampleSolrDocument(BioSample bs) {
		SolrInputDocument document = null;

		Set<MSI> msi = bs.getMSIs();
		if (msi.size() > 1) {
			StringBuffer msiAccs = new StringBuffer();
			msi.forEach(m -> msiAccs.append(m.getAcc() + "|"));
			log.warn("Sample with accession [" + bs.getAcc() + "] has multiple MSI [" + msiAccs.substring(0, msiAccs.length() - 1) + "] - sample skipped.");
			return null;
		}

		if (msi.iterator().hasNext()) {
			MSI submission = msi.iterator().next();
			if (submission.getReleaseDate().after(Calendar.getInstance().getTime())) {
				log.trace("Private sample skipped [" + bs.getAcc() + "]");
				return null;
			} 
			document = new SolrInputDocument();

			document.addField(SUBMISSION_ACC,submission.getAcc());
			document.addField(SUBMISSION_DESCRIPTION,submission.getDescription());
			document.addField(SUBMISSION_TITLE, submission.getTitle());
			document.addField(SUBMISSION_UPDATE_DATE,Formater.formatDateToSolr(submission.getUpdateDate()));

			Set<DatabaseRecordRef> db = submission.getDatabaseRecordRefs();
			if (db.iterator().hasNext()) {
				DatabaseRecordRef dbrr = db.iterator().next();
				document.addField(DB_ACC, dbrr.getAcc());
				document.addField(DB_NAME, dbrr.getDbName());
				document.addField(DB_URL, dbrr.getUrl());
			}
		}

		document.addField(ID, bs.getId());
		document.addField(SAMPLE_ACC, bs.getAcc());
		document.addField(SAMPLE_UPDATE_DATE, Formater.formatDateToSolr(bs.getUpdateDate()));
		document.addField(SAMPLE_RELEASE_DATE, Formater.formatDateToSolr(bs.getReleaseDate()));
		document.addField(CONTENT_TYPE, "sample");

		for (ExperimentalPropertyValue<?> epv : bs.getPropertyValues()) {
			document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), epv.getTermText());

			// Ontologies from Annotator
			if (useAnnotator) {
				List<String> urls = new ArrayList<>();

				//DataBaseManager dbm = new DataBaseManager();
				//urls = dbm.getOntologyFromAnnotator(epv);

				for (String url : urls) {
					if (url != null) {
						document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url);
					}
				}


			// Ontologies from Submission
			} else {
				Optional<String> url = getProvidedOntologyTerms(epv);
				if (url.isPresent()) {
					document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url.get());
				}
			}
		}
		return document;
	}

	private Optional<String> getProvidedOntologyTerms(ExperimentalPropertyValue<?> epv) {
		//NB this will currently cause a hibernate mapping error...
		OntologyEntry onto = epv.getSingleOntologyTerm();
		Optional<String> url = Optional.empty();

		if (onto != null) {
			url = Formater.formatOntologyTermURL(onto);
		}
		return url;
	}
}
