package uk.ac.ebi.solrIndexer.main;

import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.solr.common.SolrInputDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.myequivalents.model.Entity;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.service.xml.BioSampleGroupXMLService;
import uk.ac.ebi.solrIndexer.service.xml.BioSampleXMLService;

@Component
public class SolrManager {

	private Logger log = LoggerFactory.getLogger (this.getClass());

	private AnnotatorAccessor annotator = null;

    @Autowired
    private BioSampleGroupXMLService groupXmlService;

    @Autowired
    private BioSampleXMLService sampleXmlService;

	@Autowired
	private MyEquivalenceManager myEquivalenceManager;

	private boolean includeXML = false;

	final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

	public AnnotatorAccessor getAnnotator() {
		return annotator;
	}

	public void setAnnotator(AnnotatorAccessor annotator) {
		this.annotator = annotator;
	}

	public boolean isIncludeXML() {
		return includeXML;
	}

	public void setIncludeXML(boolean includeXML) {
		this.includeXML = includeXML;
	}

	//Generate Group Solr Document
	public Optional<SolrInputDocument> generateBioSampleGroupSolrDocument(BioSampleGroup bsg) {
		//check if it should be public
		boolean pub;
		try {
			pub = bsg.isPublic();

		} catch (IllegalStateException e) {
			//has multiple msis
			log.error("Group " + bsg.getAcc() + " has unusual MSIs", e);
			return Optional.empty();
		}

		if (!pub) {
			log.trace("Group "+bsg.getAcc()+" is private, skipping");
			return Optional.empty();
		}

		log.trace("Creating solr document for group "+bsg.getAcc());

		SolrInputDocument document = new SolrInputDocument();

		document.addField(ID, bsg.getId());
		document.addField(GROUP_ACC, bsg.getAcc());
		document.addField(GROUP_UPDATE_DATE, Formater.formatDateToSolr(bsg.getUpdateDate()));
		document.addField(GROUP_RELEASE_DATE, Formater.formatDateToSolr(bsg.getReleaseDate()));
		document.addField(CONTENT_TYPE, "group");

		Set<MSI> msi = bsg.getMSIs();
		if (msi.size() == 1) {
			if (msi.iterator().hasNext()) {
				MSI submission = msi.iterator().next();
				handleMSI(submission, document, bsg);
			}
		} else {
			log.warn("Group "+bsg.getAcc()+" has "+msi.size()+" MSIs");
			return Optional.empty();
		}

        List<String> characteristic_types = new ArrayList<>();
        for (ExperimentalPropertyValue<?> epv : bsg.getPropertyValues()) {
            handlePropertyValue(epv, characteristic_types, document);
        }
		document.addField(CRT_TYPE, characteristic_types);

		Set<BioSample> samples = bsg.getSamples();
		int samples_nr = samples.size();
		if (samples_nr > 0) {
			samples.forEach(sample -> document.addField(GRP_SAMPLE_ACC, sample.getAcc()));
		}
		document.addField(NUMBER_OF_SAMPLES, samples_nr);

		if (includeXML) {
			String xml = groupXmlService.getXMLString(bsg);
			document.addField(XML, xml);
		}

		return Optional.of(document);
	}

	//Generate Sample Solr Document
	public Optional<SolrInputDocument> generateBioSampleSolrDocument(BioSample bs) {
		//check if it should be public
		boolean pub;
		try {
			pub = bs.isPublic();

		} catch (IllegalStateException e) {
			//has multiple msis
			log.error("Sample " + bs.getAcc() + " has unusual MSIs", e);
			return Optional.empty();
		}

		if (!pub) {
			log.trace("Sample "+bs.getAcc()+" is private, skipping");
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
				handleMSI(submission, document, bs);
			}
		} else {
			log.warn("Sample "+bs.getAcc()+" has "+msi.size()+" MSIs");
			return Optional.empty();
		}

        List<String> characteristic_types = new ArrayList<>();
		for (ExperimentalPropertyValue<?> epv : bs.getPropertyValues()) {
            handlePropertyValue(epv, characteristic_types, document);
		}
		document.addField(CRT_TYPE, characteristic_types);

		Set<BioSampleGroup> groups = bs.getGroups();
		if (groups.size() > 0) {
			groups.forEach(group -> document.addField(SAMPLE_GRP_ACC, group.getAcc()));
		}

		if (includeXML) {
			String xml = sampleXmlService.getXMLString(bs);
			document.addField(XML, xml);
		}

		return Optional.of(document);
	}

	private void handleMSI(MSI submission, SolrInputDocument document, Object obj) {
		Set<Entity> externalEquivalences;
		if (obj instanceof BioSampleGroup) {
			BioSampleGroup bsg = (BioSampleGroup) obj;
			externalEquivalences = myEquivalenceManager.getGroupExternalEquivalences(bsg.getAcc());
			handleMSI(submission, document, externalEquivalences);

		} else if (obj instanceof BioSample) {
			BioSample bs = (BioSample) obj;
			externalEquivalences = myEquivalenceManager.getSampleExternalEquivalences(bs.getAcc());
			handleMSI(submission, document, externalEquivalences);
		}
	}

	private void handleMSI(MSI submission, SolrInputDocument document, Set<Entity> externalEquivalences) {
		document.addField(SUBMISSION_ACC,submission.getAcc());
		document.addField(SUBMISSION_DESCRIPTION,submission.getDescription());
		document.addField(SUBMISSION_TITLE, submission.getTitle());
		document.addField(SUBMISSION_UPDATE_DATE,Formater.formatDateToSolr(submission.getUpdateDate()));

		ArrayNode array = new ArrayNode(nodeFactory);

		// External references data from submission
		Set<DatabaseRecordRef> databaseRecordRefs = submission.getDatabaseRecordRefs();

		databaseRecordRefs.stream()
				.filter(databaseRecordRef -> UrlValidator.getInstance().isValid(databaseRecordRef.getUrl()))
				.forEach(databaseRecordRef -> {

					// For search purposes
					document.addField(DB_NAME, StringUtils.isNotEmpty(databaseRecordRef.getDbName()) ? databaseRecordRef.getDbName() : "-");
					document.addField(DB_URL, databaseRecordRef.getUrl());
					document.addField(DB_ACC, StringUtils.isNotEmpty(databaseRecordRef.getAcc()) ? databaseRecordRef.getAcc() : "-");

					ObjectNode ref = nodeFactory.objectNode();
					ref.put("Name", StringUtils.isNotEmpty(databaseRecordRef.getDbName()) ? databaseRecordRef.getDbName() : "");
					ref.put("URL", databaseRecordRef.getUrl());
					ref.put("Acc", StringUtils.isNotEmpty(databaseRecordRef.getAcc()) ? databaseRecordRef.getAcc() : "");

					array.add(ref);
				});

		// External references data from MyEquivalences
		externalEquivalences.stream()
				.filter(entity -> UrlValidator.getInstance().isValid(entity.getURI()))
				.forEach(entity -> {

					document.addField(DB_NAME,  StringUtils.isNotEmpty(entity.getService().getTitle()) ? entity.getService().getName() : "-");
					document.addField(DB_URL, entity.getURI());
					document.addField(DB_ACC, StringUtils.isNotEmpty(entity.getAccession()) ? entity.getAccession() : "-");

					ObjectNode ref = nodeFactory.objectNode();
					ref.put("Name", StringUtils.isNotEmpty(entity.getService().getTitle()) ? entity.getService().getName() : "");
					ref.put("URL", entity.getURI());
					ref.put("Acc", StringUtils.isNotEmpty(entity.getAccession()) ? entity.getAccession() : "");

					array.add(ref);
				});


		if (array.size() > 0) {
			document.addField(REFERENCES, array.toString());
		}

	}

	private void handlePropertyValue(ExperimentalPropertyValue<?> epv, List<String> characteristic_types, SolrInputDocument document) {
        String fieldName = Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText());
        String jsonFieldName = fieldName + "_json";
        characteristic_types.add(fieldName);

        document.addField(fieldName, epv.getTermText());

        if (annotator != null) {
			// Ontologies from Annotator
        	
            List<OntologyEntry> ontologyEntries = annotator.getAllOntologyEntries(epv);
            List<URI> uris = new ArrayList<>();
            for (OntologyEntry oe : ontologyEntries) {
				Optional<URI> uri = Formater.getOntologyTermURI(oe);
				if (uri.isPresent() && !uris.contains(uri.get())) {
					uris.add(uri.get());
				}	
            }

            // format json
            StringBuilder sb = new StringBuilder();
            sb.append("{\"text\":\"").append(epv.getTermText()).append("\"");
            if (uris.size() > 0) {
                sb.append(",");
                sb.append("\"ontology_terms\":[");
            }
            Iterator<URI> urlIt = uris.iterator();
            while (urlIt.hasNext()) {
                sb.append("\"").append(urlIt.next()).append("\"");
                if (urlIt.hasNext()) {
                    sb.append(",");
                }
            }
            if (uris.size() > 0) {
                sb.append("]");
            }
            sb.append("}");
            document.addField(jsonFieldName, sb.toString());

            //populate biosolr fields
			for (URI uri : uris) {
				log.trace("adding "+BIO_SOLR_FIELD+" "+uri);
				if (uri != null) {
					document.addField(BIO_SOLR_FIELD, uri);
				}
			}
        } else {
			// Ontologies from Submission
            if (epv.getSingleOntologyTerm() != null) {
				Optional<URI> uri = Formater.getOntologyTermURI(epv.getSingleOntologyTerm());
				if (uri.isPresent()) {
					document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), uri.get().toString());
				}
			}
		}
	}

}
