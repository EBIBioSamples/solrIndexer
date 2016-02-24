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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.common.PropertiesManager;

public class SolrManager {
	private static Logger log = LoggerFactory.getLogger (SolrManager.class.getName());

	private static final String ERROR = "ERROR";

	//Generate Group Solr Document
	@SuppressWarnings({ "rawtypes" })
	public static SolrInputDocument generateBioSampleGroupSolrDocument(BioSampleGroup bsg) {
		SolrInputDocument document;

		try{
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

			for (ExperimentalPropertyValue epv : bsg.getPropertyValues()) {
				document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), epv.getTermText());

				// Ontologies from Annotator
				if (PropertiesManager.isAnnotatorActive()) {
					List<String> urls = null;

					try {
						urls = DataBaseManager.getOntologyFromAnnotator(epv);

						for (String url : urls) {
							if (url != null) {
								document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url);
							}
						}

					} catch (IllegalArgumentException e) {
						log.error("Group: [" + bsg.getAcc() + "] for ExperimentalPropertyValue [" + epv.getTermText() + "]", e);
					}

				// Ontologies from Submission
				} else {
					String url = getOntologyFromSubmission(epv);
					if (url != null && !url.equals(ERROR)) {
						document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url);
					} else if (url != null && url.equals(ERROR)) {
						log.error("Error fetching ontology mapping for group [" + bsg.getAcc() + "] with property type: [" + epv.getType() + "]");
					}
				}
			}

		} catch (Exception e) {
			log.error("Error creating group [" + bsg.getAcc() + "] solr document: ", e);
			return null;
		}

		return document;
	}

	//Generate Sample Solr Document
	@SuppressWarnings({ "rawtypes" })
	public static SolrInputDocument generateBioSampleSolrDocument(BioSample bs) {
		SolrInputDocument document = null;

		try {
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

			for (ExperimentalPropertyValue epv : bs.getPropertyValues()) {
				String fieldName = Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText());
				String jsonFieldName = fieldName + "_json";

				document.addField(fieldName, epv.getTermText());

				// Ontologies from Annotator
				if (PropertiesManager.isAnnotatorActive()) {
					try {
						List<String> urls = DataBaseManager.getOntologyFromAnnotator(epv);

						// format json
						StringBuilder sb = new StringBuilder();
						sb.append("{\"text\":\"").append(epv.getTermText()).append("\"");
						if (urls.size() > 0) {
							sb.append(",");
							sb.append("\"ontology_terms\":[");
						}
						Iterator<String> urlIt = urls.iterator();
						while (urlIt.hasNext()) {
							sb.append("\"").append(urlIt.next()).append("\"");
							if (urlIt.hasNext()) {
								sb.append(",");
							}
						}
						if (urls.size() > 0) {
							sb.append("]");
						}
						sb.append("}");
						document.addField(jsonFieldName, sb.toString());
					} catch (IllegalArgumentException e) {
						log.error("Sample: [" + bs.getAcc() + "] for ExperimentalPropertyValue [" + epv.getTermText() + "]", e);
					}

				// Ontologies from Submission
				} else {
					String url = getOntologyFromSubmission(epv);
					if (url != null && !url.equals(ERROR)) {
						document.addField(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()), url);
					} else if (StringUtils.equals(url, ERROR)) {
						log.error("Error fetching ontology mapping for sample [" + bs.getAcc() + "] with property type: [" + epv.getType() + "]");
					}
				}
			}

		} catch (Exception e) {
			log.error("Error creating sample [" + bs.getAcc() + "] solr document: ", e);
			return null;
		}

		return document;
	}

	@SuppressWarnings("rawtypes")
	private static String getOntologyFromSubmission(ExperimentalPropertyValue epv) {
		OntologyEntry onto = epv.getSingleOntologyTerm();
		String url = null;

		if (onto != null) {
			url = Formater.formatOntologyTermURL(onto);
		}
		return url;
	}
}
