package uk.ac.ebi.biosamples.solrindexer.service;

import static uk.ac.ebi.biosamples.solrindexer.SolrSchemaFields.*;

import java.net.URI;
import java.util.*;

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

import uk.ac.ebi.biosamples.solrindexer.Formater;
import uk.ac.ebi.biosamples.solrindexer.service.xml.BioSampleGroupXMLService;
import uk.ac.ebi.biosamples.solrindexer.service.xml.BioSampleXMLService;
import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.ContactRole;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.model.Entity;

@Component
public class SolrManager {

	private Logger log = LoggerFactory.getLogger (this.getClass());

    @Autowired
    private BioSampleGroupXMLService groupXmlService;

    @Autowired
    private BioSampleXMLService sampleXmlService;

	@Autowired
	private MyEquivalenceManager myEquivalenceManager;

	private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

	//Generate Group Solr Document
	public Optional<SolrInputDocument> generateBioSampleGroupSolrDocument(BioSampleGroup bsg, EntityMappingManager entityMappingManager, AnnotatorAccessor annotator ) {
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

		document.addField(ACC, bsg.getAcc());
		try {
			document.addField(UPDATE_DATE, Formater.formatDateToSolr(handleUpdateDate(bsg)));
			document.addField(RELEASE_DATE, Formater.formatDateToSolr(handleReleaseDate(bsg)));
		} catch (IllegalArgumentException e) {
			log.error(String.format("Invalid date for group %s",bsg.getAcc()), e);
			return Optional.empty();
		}
		document.addField(CONTENT_TYPE, "group");

		Set<MSI> msi = bsg.getMSIs();
        MSI submission = null; 
        
		if (msi.size() == 1) {
			if (msi.iterator().hasNext()) {
				submission = msi.iterator().next();
                try {
                    handleMSI(submission, document, bsg, entityMappingManager);
                } catch (IllegalArgumentException e) {
                    log.error(String.format("Error while creating document %s", bsg.getAcc()),e);
                    return Optional.empty();
                }
            }

		} else {
			log.warn("Group "+bsg.getAcc()+" has "+msi.size()+" MSIs");
			return Optional.empty();
		}

        List<String> characteristic_types = new ArrayList<>();
        ExperimentalPropertyValue<?> groupDescriptionProperty = null;
        ExperimentalPropertyValue<?> groupNameProperty = null;

        for (ExperimentalPropertyValue<?> epv : bsg.getPropertyValues()) {
			String fieldName = epv.getType().getTermText();
			if (fieldName.equals("Group Description")) {
                groupDescriptionProperty = epv;
			} else if (fieldName.equals("Group Name")) {
				groupNameProperty = epv;
				//handlePropertyValue(epv, characteristic_types, document, annotator);
			} else{
				handlePropertyValue(epv, characteristic_types, document, annotator);
            }
        }
        document.addField(CRT_TYPE, characteristic_types);
        
        // Handle description field 
        handleGroupDescription(groupDescriptionProperty,submission,document);


        if (groupNameProperty != null) {
    		document.addField(NAME, groupNameProperty.getTermText());
        }
        
		Set<BioSample> samples = bsg.getSamples();
		int samples_nr = samples.size();
		if (samples_nr > 0) {
			samples.forEach(sample -> document.addField(GRP_SAMPLE_ACC, sample.getAcc()));
		}

		document.addField(XML, groupXmlService.getXMLString(bsg, entityMappingManager));

		return Optional.of(document);
	}


	//Generate Sample Solr Document
	public Optional<SolrInputDocument> generateBioSampleSolrDocument(BioSample bs, EntityMappingManager entityMappingManager, AnnotatorAccessor annotator ) {
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

		document.addField(ACC, bs.getAcc());
		try {
			document.addField(UPDATE_DATE, Formater.formatDateToSolr(handleUpdateDate(bs)));
			document.addField(RELEASE_DATE, Formater.formatDateToSolr(handleReleaseDate(bs)));
		} catch (IllegalArgumentException e) {
			log.error(String.format("Invalid date for sample %s",bs.getAcc()), e);
			return Optional.empty();
		}

		document.addField(CONTENT_TYPE, "sample");

		Set<MSI> msi = bs.getMSIs();
        MSI submission = null;
		if (msi.size() == 1) {
			if (msi.iterator().hasNext()) {
				submission = msi.iterator().next();
                try {
                    handleMSI(submission, document, bs, entityMappingManager);
                } catch (IllegalArgumentException e) {
                    log.error(String.format("Error while creating document %s",bs.getAcc()),e);
                    return Optional.empty();
                }
			}
		} else {
			log.warn("Sample "+bs.getAcc()+" has "+msi.size()+" MSIs");
			return Optional.empty();
		}

        List<String> characteristic_types = new ArrayList<>();
        ExperimentalPropertyValue<?> sampleDescriptionProperty = null;
        ExperimentalPropertyValue<?> sampleNameProperty = null;

		for (ExperimentalPropertyValue<?> epv : bs.getPropertyValues()) {
            String fieldName = epv.getType().getTermText();
            if (fieldName.equals("Sample Description")) {
            	sampleDescriptionProperty = epv;
            } else if (fieldName.equals("Sample Name")) {
            	sampleNameProperty = epv;
            	handlePropertyValue(epv, characteristic_types, document, annotator);
            } else {
            	handlePropertyValue(epv, characteristic_types, document, annotator);
            }
		}
		document.addField(CRT_TYPE, characteristic_types);

        // Handle sample description
        handleSampleDescription(bs,sampleDescriptionProperty, submission, document);

        if (sampleNameProperty != null) {
    		document.addField(NAME, sampleNameProperty.getTermText());
        }
        
		Set<BioSampleGroup> groups = bs.getGroups();
		if (groups.size() > 0) {
			groups.forEach(group -> document.addField(SAMPLE_GRP_ACC, group.getAcc()));
		}

		document.addField(XML, sampleXmlService.getXMLString(bs, entityMappingManager));

		return Optional.of(document);
	}

	private void handleMSI(MSI submission, SolrInputDocument document, Object obj, EntityMappingManager entityMappingManager) throws IllegalArgumentException {
		Set<Entity> externalEquivalences;
		if (obj instanceof BioSampleGroup) {
			BioSampleGroup bsg = (BioSampleGroup) obj;
			externalEquivalences = myEquivalenceManager.getGroupExternalEquivalences(bsg.getAcc(), entityMappingManager);
			handleMSI(submission, document, externalEquivalences);
		} else if (obj instanceof BioSample) {
			BioSample bs = (BioSample) obj;
			externalEquivalences = myEquivalenceManager.getSampleExternalEquivalences(bs.getAcc(), entityMappingManager);
			handleMSI(submission, document, externalEquivalences);
		} else {
			throw new IllegalArgumentException("Unrecognised object "+obj);
		}

		handleOrganizations(submission, document);
		handleContacts(submission, document);
		handlePublications(submission, document);
	}


	private void handleMSI(MSI submission, SolrInputDocument document, Set<Entity> externalEquivalences) throws IllegalArgumentException{
		document.addField(SUBMISSION_ACC,submission.getAcc());
//		document.addField(SUBMISSION_DESCRIPTION,submission.getDescription());
		document.addField(SUBMISSION_TITLE, submission.getTitle());

		// Update date is not anymore saved for submission and sample but instead as a unique field
//		document.addField(SUBMISSION_UPDATE_DATE,Formater.formatDateToSolr(submission.getUpdateDate()));

		ArrayNode array = new ArrayNode(nodeFactory);

		// External references data from submission
		Set<DatabaseRecordRef> databaseRecordRefs = submission.getDatabaseRecordRefs();

		databaseRecordRefs.stream()
				.filter(databaseRecordRef -> !StringUtils.equals(databaseRecordRef.getAcc(), ("ebi.biosamples.samples"))
						&& !StringUtils.equals(databaseRecordRef.getAcc(), ("ebi.biosamples.groups"))
						&& UrlValidator.getInstance().isValid(databaseRecordRef.getUrl()))
				.forEach(databaseRecordRef -> {

					// For search purposes
					document.addField(REFERENCES_NAME, StringUtils.isNotEmpty(databaseRecordRef.getDbName()) ? databaseRecordRef.getDbName() : "-");
					document.addField(REFERENCES_URL, databaseRecordRef.getUrl());
					document.addField(REFERENCES_ACC, StringUtils.isNotEmpty(databaseRecordRef.getAcc()) ? databaseRecordRef.getAcc() : "-");

					ObjectNode ref = nodeFactory.objectNode();
					ref.put("Name", StringUtils.isNotEmpty(databaseRecordRef.getDbName()) ? databaseRecordRef.getDbName() : "");
					ref.put("URL", databaseRecordRef.getUrl());
					ref.put("Acc", StringUtils.isNotEmpty(databaseRecordRef.getAcc()) ? databaseRecordRef.getAcc() : "");

					array.add(ref);
				});

		// External references data from MyEquivalences
		externalEquivalences.stream()
				.filter(entity -> !StringUtils.equals(entity.getAccession(), "ebi.biosamples.samples")
						&& !StringUtils.equals(entity.getAccession(), ("ebi.biosamples.groups"))
						&& UrlValidator.getInstance().isValid(entity.getURI()))
				.forEach(entity -> {

					document.addField(REFERENCES_NAME,  StringUtils.isNotEmpty(entity.getService().getTitle()) ? entity.getService().getName() : "-");
					document.addField(REFERENCES_URL, entity.getURI());
					document.addField(REFERENCES_ACC, StringUtils.isNotEmpty(entity.getAccession()) ? entity.getAccession() : "-");

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

	private void handlePropertyValue(ExperimentalPropertyValue<?> epv, List<String> characteristic_types, SolrInputDocument document, AnnotatorAccessor annotator) {

        if (epv.getType().getTermText().equals("SampleTab FTP location")) {
        	return;
        }
		
		String fieldName = Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText());
        String jsonFieldName = fieldName + "_json";
        characteristic_types.add(fieldName);
        
        

        document.addField(fieldName, epv.getTermText());

		// add ontology term(s) from Annotator
        //convert them to URIs
        List<OntologyEntry> ontologyEntries = annotator.getAllOntologyEntries(epv);
        List<URI> uris = new ArrayList<>();
        for (OntologyEntry oe : ontologyEntries) {
			Optional<URI> uri = Formater.getOntologyTermURI(oe);
			if (uri.isPresent() && !uris.contains(uri.get())) {
				uris.add(uri.get());
			}	
        }

        // format json
		ObjectNode json = new ObjectNode(nodeFactory);
		json.put("text", epv.getTermText());
		//Removed as it can be confusing because it duplicates and mangles the type string  in the JSON map of attributes
		//json.put("type", epv.getType().getTermText());
		if (epv.getUnit() != null) {
			json.put("unit",epv.getUnit().getTermText());
		}
		if (uris.size() > 0) {
			ArrayNode ontologyTerms = json.putArray("ontology_terms");
			for (URI uri : uris) {
				ontologyTerms.add(uri.toString());
			}
		}
		//now add the json to the solr document
		document.addField(jsonFieldName, json.toString());

        //populate biosolr fields on the json document
		for (URI uri : uris) {
			log.trace("adding "+BIO_SOLR_FIELD+" "+uri);
			if (uri != null) {
				document.addField(BIO_SOLR_FIELD, uri);
			}
		}
	}

	private Date handleUpdateDate(BioSample bs) {
		Date sampleUpdateDate = bs.getUpdateDate();

		if (sampleUpdateDate == null) {
			Set<MSI> msis = bs.getMSIs();
			if ( msis.size() == 1 ) {
                return msis.iterator().next().getUpdateDate();
			}
		}

		return sampleUpdateDate;
	}

	private Date handleUpdateDate(BioSampleGroup bsg) {
		Date groupUpdateDate = bsg.getUpdateDate();

		if (groupUpdateDate == null) {
			Set<MSI> msis = bsg.getMSIs();
			if ( msis.size() == 1 ) {
				return msis.iterator().next().getUpdateDate();
			}
		}

		return groupUpdateDate;
	}

	private Date handleReleaseDate(BioSample bs) {
		Date sampleReleaseDate = bs.getReleaseDate();

		if (sampleReleaseDate == null) {
			Set<MSI> msis = bs.getMSIs();
			if ( msis.size() == 1 ) {
				return msis.iterator().next().getReleaseDate();
			}
		}

		return sampleReleaseDate;
	}

    private Date handleReleaseDate(BioSampleGroup bsg) {
		Date groupReleaseDate = bsg.getReleaseDate();

		if (groupReleaseDate == null) {
			Set<MSI> msis = bsg.getMSIs();
			if ( msis.size() == 1 ) {
				return msis.iterator().next().getReleaseDate();
			}
		}

		return groupReleaseDate;
	}

	private void handleGroupDescription(ExperimentalPropertyValue<?> description, MSI msi, SolrInputDocument document) {
        if (description != null && description.getType().getTermText().equalsIgnoreCase("Group Description")) {
            document.addField(DESCRIPTION,description.getTermText());
        } else if (msi != null) {
            document.addField(DESCRIPTION,msi.getDescription());
        }
	}

	private void handleSampleDescription(BioSample sample, ExperimentalPropertyValue<?> description, MSI msi, SolrInputDocument document) {
        if (description != null && description.getType().getTermText().equalsIgnoreCase("Sample Description")) {
            document.addField(DESCRIPTION,description.getTermText());
        } else {
            if (sample.getGroups().size() == 0 && msi != null) {
                document.addField(DESCRIPTION,msi.getDescription());
            }
        }
	}

	private void handleOrganizations(MSI submission, SolrInputDocument document) {
		ArrayNode array = new ArrayNode(nodeFactory);
		Set<Organization> organizations = submission.getOrganizations();

		organizations.stream().forEach(o -> {
			ObjectNode org = nodeFactory.objectNode();
			if (!StringUtils.isEmpty(o.getName())) {
				document.addField(ORG_NAME, o.getName());
				org.put("Name", o.getName());
			}
			/* if(!StringUtils.isEmpty(organization.getAddress())) {document.addField(ORG_ADDRESS, organization.getAddress()); }*/
			Set<ContactRole> roles = o.getOrganizationRoles();
			if(roles != null && !roles.isEmpty()) {
				String role_str = "";
				Iterator it = roles.iterator();
				while (it.hasNext()) {
					ContactRole cr = (ContactRole) it.next();
					role_str += cr.getName() + " ";
				}
				document.addField(ORG_ROLE, role_str);
				org.put("Role", role_str);
			}
			if(!StringUtils.isEmpty(o.getEmail())) {
				document.addField(ORG_EMAIL, o.getEmail());
				org.put("E-mail", o.getEmail());
			}
			if(!StringUtils.isEmpty(o.getUrl())) {
				document.addField(ORG_URL, o.getUrl());
				org.put("URL", o.getUrl());
			}
			array.add(org);
		});

		if (array.size() > 0) {
			document.addField(ORG_JSON, array.toString());
		}
	}

	private void handleContacts(MSI submission, SolrInputDocument document) {
		ArrayNode array = new ArrayNode(nodeFactory);
		Set<Contact> contacts = submission.getContacts();

		contacts.stream().forEach(c -> {
			ObjectNode contact = nodeFactory.objectNode();
			if(!StringUtils.isEmpty(c.getFirstName()) && !StringUtils.isEmpty(c.getLastName())) {
				document.addField(CONTACT_NAME, c.getFirstName() + " " + c.getLastName());
				contact.put("Name", c.getFirstName() + " " + c.getLastName());
			}
			if(!StringUtils.isEmpty(c.getAffiliation())) {
				document.addField(CONTACT_AFFILIATION, c.getAffiliation());
				contact.put("Affiliation", c.getAffiliation());
			}
			if(!StringUtils.isEmpty(c.getUrl())) {
				document.addField(CONTACT_URL, c.getUrl());
				contact.put("URL", c.getUrl());
			}
			array.add(contact);
		});

		if (array.size() > 0) {
			document.addField(CONTACT_JSON, array.toString());
		}
	}

	private void handlePublications(MSI submission, SolrInputDocument document) {
		ArrayNode array = new ArrayNode(nodeFactory);
		Set<Publication> publications = submission.getPublications();

		publications.stream().forEach(p -> {
			ObjectNode pub = nodeFactory.objectNode();
			if(!StringUtils.isEmpty(p.getDOI())) {
				document.addField(PUB_DOI, p.getDOI());
				pub.put("doi", p.getDOI());
			}
			if(!StringUtils.isEmpty(p.getPubmedId())) {
				document.addField(PUB_PUBMED, p.getPubmedId());
				pub.put("pubmed_id", p.getPubmedId());
			}
			array.add(pub);
		});

		if (array.size() > 0) {
			document.addField(PUB_JSON, array.toString());
		}
	}

}
