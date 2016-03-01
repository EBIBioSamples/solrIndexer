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

import java.net.URI;
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

import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
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

	private AnnotatorAccessor annotator = null;

	public AnnotatorAccessor getAnnotator() {
		return annotator;
	}

	public void setAnnotator(AnnotatorAccessor annotator) {
		this.annotator = annotator;
	}

	//Generate Group Solr Document
	public Optional<SolrInputDocument> generateBioSampleGroupSolrDocument(BioSampleGroup bsg) {
		//check if it should be public
		if (!bsg.isPublic()) {
			log.trace("Group "+bsg.getAcc()+" is private, skipping");
			return Optional.empty();
		}
		
		log.trace("Creating solr document for group "+bsg.getAcc());

		SolrInputDocument document = new SolrInputDocument();
		
		document.addField(ID, bsg.getId());
		document.addField(GROUP_ACC, bsg.getAcc());
		document.addField(GROUP_UPDATE_DATE, Formater.formatDateToSolr(bsg.getUpdateDate()));
		//TODO add group release date here too
		document.addField(CONTENT_TYPE, "group");

		Set<MSI> msi = bsg.getMSIs();
		if (msi.size() == 1) {
			if (msi.iterator().hasNext()) {
				MSI submission = msi.iterator().next();
				handleMSI(submission, document);
			}
		} else {
			log.warn("Group "+bsg.getAcc()+" has "+msi.size()+" MSIs");
			return Optional.empty();
		}

		for (ExperimentalPropertyValue<?> epv : bsg.getPropertyValues()) {
			handlePropertyValue(epv, document);
		}

		return Optional.of(document);
	}

	//Generate Sample Solr Document
	public Optional<SolrInputDocument> generateBioSampleSolrDocument(BioSample bs) {
		//check if it should be public
		if (!bs.isPublic()) {
			log.trace("Group "+bs.getAcc()+" is private, skipping");
			return Optional.empty();
		}
		
		SolrInputDocument document = new SolrInputDocument();
		
		document.addField(ID, bs.getId());
		document.addField(SAMPLE_ACC, bs.getAcc());
		document.addField(SAMPLE_UPDATE_DATE, Formater.formatDateToSolr(bs.getUpdateDate()));
		document.addField(SAMPLE_RELEASE_DATE, Formater.formatDateToSolr(bs.getReleaseDate()));
		document.addField(CONTENT_TYPE, "sample");

		Set<MSI> msi = bs.getMSIs();
		if (msi.size() == 1) {
			if (msi.iterator().hasNext()) {
				MSI submission = msi.iterator().next();
				handleMSI(submission, document);
			}
		} else {
			log.warn("Sample "+bs.getAcc()+" has "+msi.size()+" MSIs");
			return Optional.empty();
		}

		for (ExperimentalPropertyValue<?> epv : bs.getPropertyValues()) {
			handlePropertyValue(epv, document);
		}

		return Optional.of(document);
	}
	
	private void handleMSI(MSI submission, SolrInputDocument document) {
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
	
	private void handlePropertyValue(ExperimentalPropertyValue<?> epv, SolrInputDocument document) {

		document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), epv.getTermText());

		// Ontologies from Annotator
		if (annotator != null) {

			List<String> urls = new ArrayList<String>();
			List<OntologyEntry> ontologies = annotator.getAllOntologyEntries(epv);
			ontologies.forEach(oe -> urls.add(oe.getAcc()));
			
			for (String url : urls) {
				log.trace(url);
				if (url != null) {
					document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url);
				}
			}


		// Ontologies from Submission
		} else {
			if (epv.getSingleOntologyTerm() != null) {
				Optional<URI> uri = Formater.getOntologyTermURI(epv.getSingleOntologyTerm());
				if (uri.isPresent()) {
					document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), uri.get().toString());
				}
			}
		}
	}
}
