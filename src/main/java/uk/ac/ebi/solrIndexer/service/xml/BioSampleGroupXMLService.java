package uk.ac.ebi.service;

import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.database.MyEquivalenceManager;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.ContactRole;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;
import uk.ac.ebi.fg.myequivalents.dao.EntityMappingDAO;
import uk.ac.ebi.fg.myequivalents.managers.impl.db.DbMyEquivalentsManager;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.*;
import uk.ac.ebi.fg.myequivalents.model.Entity;
import uk.ac.ebi.fg.myequivalents.resources.Resources;


import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

import static jdk.nashorn.internal.objects.NativeArray.forEach;


public class BioSampleGroupXMLService implements XMLService<BioSampleGroup>{


	private static Logger log = LoggerFactory.getLogger(BioSampleGroupXMLService.class.getName());
	private final Namespace XMLNS =
			Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");

    private final DateTimeZone dtz = DateTimeZone.forID("Etc/GMT");
    private final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

	public BioSampleGroupXMLService() { }

	@Override
	public String getXMLString(BioSampleGroup group) {

		return renderDocument(getXMLDocument(group));

	}

	@Override
	public Document getXMLDocument(BioSampleGroup group) {
		Document doc = generateBaseDocument();
		Element biosampleElement = getXMLElement(group);

		doc.setRootElement(biosampleElement);

		return doc;
	}

	@Override
	public Element getXMLElement(BioSampleGroup group) {
		Element root = getDocumentRoot(group);

		List<Element> submissionInfos 		= getSubmissionInformationElements(group);
		List<Element> groupAnnotations 		= getAnnotationsElements(group);
		List<Element> groupTermSource  		= getTermSourceElements(group);
		List<Element> groupProperties  		= getPropertiesElements(group);
		List<Element> groupOrganizations 	= getOrganizationElements(group);
		List<Element> groupPersons 			= getPersonElements(group);
		List<Element> groupDatabases 		= getDatabaseElements(group);
		List<Element> groupPublication 		= getPublicationElements(group);
		Element groupSampleIds 				= getSampleIdsElement(group);
		List<Element> groupBiosamples 		= getBiosampleElements(group);

		root.addContent(submissionInfos)
			.addContent(groupAnnotations)
			.addContent(groupTermSource)
			.addContent(groupProperties)
			.addContent(groupOrganizations)
			.addContent(groupPersons)
			.addContent(groupDatabases)
			.addContent(groupPublication);
//			.addContent(groupSampleIds)
//			.addContent(groupBiosamples);

		//Filter notEmptyFilter = new EmptyElementFilter().negate();
		//filterDescendantOf(root,notEmptyFilter);

		return root;

	}


	public Document generateBaseDocument() {

		Document doc = new Document();
		doc.addContent(new Comment("BioSamples XML API - version 1.0"));

		return doc;

	}

	private Element getDocumentRoot(BioSampleGroup group){

//		Namespace xmlns = Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");
		Element   root  = new Element("BioSampleGroup", XMLNS);

		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.addNamespaceDeclaration(xsi);

		List<Attribute> rootAttributes = getRootAttributes(group);
		rootAttributes.add(new Attribute("schemaLocation","http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/BioSDSchema.xsd",
				xsi));

		root.setAttributes(rootAttributes);

		return root;
	}

	private List<Attribute> getRootAttributes(BioSampleGroup group) {

		List<Attribute> rootAttributes = new ArrayList<>();

		Attribute id = new Attribute("id", group.getAcc());


		rootAttributes.add(id);
		return rootAttributes;

	}

	private List<Element> getSubmissionInformationElements(BioSampleGroup group) {

		List<Element> submissionInformations = new ArrayList<>();

		Set<MSI> groupMSIs = group.getMSIs();
		if (groupMSIs.size() == 1) {
			MSI msi = groupMSIs.iterator().next();
			submissionInformations.add(getSubmReleaseDateElement(msi));
			submissionInformations.add(getSubmUpdateDateElement(msi));
			submissionInformations.add(getSubmIdentifierElement(msi));
			submissionInformations.add(getSubmTitleElement(msi));
			submissionInformations.add(getSubmDescriptionElement(msi));

		}

        submissionInformations.add(getSubmReferenceLayerElement(group));
		return submissionInformations;

	}

	private Element getSubmReleaseDateElement(MSI msi) {

		Element property = new Element("Property",XMLNS);

		Attribute classAttr          = new Attribute("class", "Submission Release Date");
		Attribute characteristicAttr = new Attribute("characteristic", "false");
        Attribute typeAttr           = new Attribute("type", "DATETIME");
        Attribute commentAttr        = new Attribute("comment", "false");

		List<Attribute> attributes = new ArrayList<>();
		attributes.add(classAttr);
		attributes.add(characteristicAttr);
        attributes.add(typeAttr);
        attributes.add(commentAttr);

		Element qualifiedValue = new Element("QualifiedValue",XMLNS);

        DateTime gmtReleaseDate = getGMTDateTime(msi.getReleaseDate());

		Element value = new Element("Value",XMLNS).setText(gmtReleaseDate.toString(dtf));
		qualifiedValue.setContent(value);

		property.setAttributes(attributes);
		property.setContent(qualifiedValue);

		return property;

	}

	private Element getSubmUpdateDateElement(MSI msi) {
		Element property = new Element("Property",XMLNS);

		Attribute classAttr          = new Attribute("class", "Submission Update Date");
		Attribute characteristicAttr = new Attribute("characteristic", "false");
        Attribute typeAttr           = new Attribute("type", "DATETIME");
        Attribute commentAttr        = new Attribute("comment", "false");

		List<Attribute> attributes = new ArrayList<>();
		attributes.add(classAttr);
		attributes.add(characteristicAttr);
        attributes.add(typeAttr);
        attributes.add(commentAttr);

		Element qualifiedValue = new Element("QualifiedValue",XMLNS);
        DateTime gmtUpdateDate = getGMTDateTime(msi.getUpdateDate());

		Element value = new Element("Value",XMLNS).setText(gmtUpdateDate.toString(dtf));
		qualifiedValue.setContent(value);

		property.setAttributes(attributes);
		property.setContent(qualifiedValue);

		return property;
	}

	private Element getSubmIdentifierElement(MSI msi) {
		Element property = new Element("Property",XMLNS);

		Attribute classAttr          = new Attribute("class", "Submission Identifier");
		Attribute characteristicAttr = new Attribute("characteristic", "false");
        Attribute typeAttr           = new Attribute("type", "STRING");
        Attribute commentAttr        = new Attribute("comment", "false");

		List<Attribute> attributes = new ArrayList<>();
		attributes.add(classAttr);
		attributes.add(characteristicAttr);
        attributes.add(typeAttr);
        attributes.add(commentAttr);

		Element qualifiedValue = new Element("QualifiedValue",XMLNS);
		Element value = new Element("Value",XMLNS).setText(msi.getAcc());
		qualifiedValue.setContent(value);

		property.setAttributes(attributes);
		property.setContent(qualifiedValue);

		return property;
	}

	private Element getSubmTitleElement(MSI msi) {
		Element property = new Element("Property",XMLNS);

		Attribute classAttr          = new Attribute("class", "Submission Title");
		Attribute characteristicAttr = new Attribute("characteristic", "false");
        Attribute typeAttr           = new Attribute("type", "STRING");
        Attribute commentAttr        = new Attribute("comment", "false");

		List<Attribute> attributes = new ArrayList<>();
		attributes.add(classAttr);
		attributes.add(characteristicAttr);
        attributes.add(typeAttr);
        attributes.add(commentAttr);

		Element qualifiedValue = new Element("QualifiedValue",XMLNS);
		Element value = new Element("Value",XMLNS).setText(msi.getTitle());
		qualifiedValue.setContent(value);

		property.setAttributes(attributes);
		property.setContent(qualifiedValue);

		return property;
	}

	private Element getSubmDescriptionElement(MSI msi) {
		Element property = new Element("Property",XMLNS);

		Attribute classAttr          = new Attribute("class", "Submission Description");
		Attribute characteristicAttr = new Attribute("characteristic", "false");
        Attribute typeAttr           = new Attribute("type", "STRING");
        Attribute commentAttr        = new Attribute("comment", "false");

		List<Attribute> attributes = new ArrayList<>();
		attributes.add(classAttr);
		attributes.add(characteristicAttr);
        attributes.add(typeAttr);
        attributes.add(commentAttr);

		Element qualifiedValue = new Element("QualifiedValue",XMLNS);
		Element value = new Element("Value",XMLNS).setText(msi.getDescription());
		qualifiedValue.setContent(value);

		property.setAttributes(attributes);
		property.setContent(qualifiedValue);

		return property;
	}

	private Element getSubmReferenceLayerElement(BioSampleGroup group) {
		Element property = new Element("Property",XMLNS);

		Attribute classAttr          = new Attribute("class", "Submission Reference Layer");
		Attribute characteristicAttr = new Attribute("characteristic", "false");
        Attribute typeAttr           = new Attribute("type", "STRING");
        Attribute commentAttr        = new Attribute("comment", "false");

		List<Attribute> attributes = new ArrayList<>();
		attributes.add(classAttr);
		attributes.add(characteristicAttr);
        attributes.add(typeAttr);
        attributes.add(commentAttr);

		Element qualifiedValue = new Element("QualifiedValue",XMLNS);
		Element value = new Element("Value",XMLNS).setText(Boolean.toString(group.isInReferenceLayer()));
		qualifiedValue.setContent(value);

		property.setAttributes(attributes);
		property.setContent(qualifiedValue);

		return property;
	}


	private List<Element> getAnnotationsElements(BioSampleGroup group) {
		List<Element> annotations = new ArrayList<>();

		return annotations;
	}

	private List<Element> getTermSourceElements(BioSampleGroup group) {
		List<Element> termSources = new ArrayList<>();

		return termSources;
	}

	private List<Element> getPropertiesElements(BioSampleGroup group) {
		List<Element> properties = new ArrayList<>();

		group.getPropertyValues().forEach(propertyValue -> {

			Element singleProperty = getPropertyElement(propertyValue);
			properties.add(singleProperty);

		});

		return properties;
	}

	public Element getPropertyElement(ExperimentalPropertyValue propertyValue) {

		Element property = new Element("Property",XMLNS);


		ExperimentalPropertyType propertyType = propertyValue.getType();

		Attribute classAttr          = new Attribute("class", propertyType.getTermText());
		Attribute characteristicAttr = new Attribute("characteristic", Boolean.toString(isCharacterstic(propertyValue)));
        Attribute typeAttr           = new Attribute("type", "STRING");
        Attribute commentAttr        = new Attribute("comment", Boolean.toString(isComment(propertyValue)));

		List<Attribute> propertyAttributes = new ArrayList<>();
		propertyAttributes.add(classAttr);
		propertyAttributes.add(characteristicAttr);
        propertyAttributes.add(typeAttr);
        propertyAttributes.add(commentAttr);

		property.setAttributes(propertyAttributes);

		List<Element> qualityValues = getQualifiedValues(propertyValue);
		property.setContent(qualityValues);

		return property;

	}

	private Boolean isCharacterstic(ExperimentalPropertyValue propertyValue) {
		return propertyValue instanceof BioCharacteristicValue;
	}

	private Boolean isComment(ExperimentalPropertyValue propertyValue) {
		return propertyValue instanceof SampleCommentValue;
	}

	public List<Element> getQualifiedValues(ExperimentalPropertyValue propertyValue) {
		List<Element> qualifiedValues = new ArrayList<>();


		//propertyValues.getQualifiedValues().forEach(qv -> {
			Element qualifiedValue = getQualifiedValue(propertyValue);
			qualifiedValues.add(qualifiedValue);
		//})
		return qualifiedValues;
	}

	public Element getQualifiedValue(ExperimentalPropertyValue propertyValue) {

		Element qualifiedValue = new Element("QualifiedValue",XMLNS);

		Element value         = new Element("Value",XMLNS).setText(propertyValue.getTermText());
		Element termSourceRef = getQualityValue_TermSourceRef(propertyValue);
		Element unit = new Element("Unit",XMLNS);
		if (propertyValue.getUnit() != null) {
			unit.setText(propertyValue.getUnit().getTermText());
		}

		qualifiedValue.addContent(value);

		if ( !termSourceRef.getValue().isEmpty() ) {
			qualifiedValue.addContent(termSourceRef);
		}

		if ( !unit.getValue().isEmpty() ) {
			qualifiedValue.addContent(unit);
		}


		return qualifiedValue;

	}

	private Element getQualityValue_TermSourceRef(ExperimentalPropertyValue pv) {


		Element termSourceRef = new Element("TermSourceREF",XMLNS);

		if (pv.getSingleOntologyTerm() != null) {
			OntologyEntry ontology = pv.getSingleOntologyTerm();
			ReferenceSource ontologyRefSource = ontology.getSource();
			Element tsrName 		= new Element("Name",XMLNS).setText(ontologyRefSource.getName());
			Element tsrDescription 	= new Element("Description",XMLNS).setText(ontologyRefSource.getDescription());
			Element tsrURI 			= new Element("URI",XMLNS).setText(ontologyRefSource.getUrl());
			Element tsrVersion 		= new Element("Version",XMLNS).setText(ontologyRefSource.getVersion());
			Element tsrTermSourceID = new Element("TermSourceID",XMLNS).setText(ontology.getAcc());

			List<Element> allContents = new ArrayList<>();
			allContents.add(tsrName);
			allContents.add(tsrDescription);
			allContents.add(tsrURI);
			allContents.add(tsrVersion);
			allContents.add(tsrTermSourceID);

			termSourceRef.addContent(allContents);
		}

		return termSourceRef;


	}

	private List<Element> getOrganizationElements(BioSampleGroup group) {
		List<Element> organizations = new ArrayList<>();

		if ( existsAndUniqueMSI(group) ) {

			MSI msi = group.getMSIs().iterator().next();
			Set<Organization> organizationSet = msi.getOrganizations();

			// Add all the organizations to the parent element
			organizationSet.forEach(organization -> {
				organizations.add(getOrganizationFields(organization));
			});

		}

		return organizations;
	}

	private Element getOrganizationFields(Organization organization) {

		Element organizationElement = new Element("Organization",XMLNS);

        organizationElement.addContent(new Element("Name",XMLNS).setText(organization.getName()));
        organizationElement.addContent(new Element("Address",XMLNS).setText(organization.getAddress()));
        organizationElement.addContent(new Element("URI",XMLNS).setText(organization.getUrl()));
        organizationElement.addContent(new Element("Email",XMLNS).setText(organization.getEmail()));

		if (organization.getOrganizationRoles().size() > 0) {
			organization.getOrganizationRoles().forEach(role -> {
				organizationElement.addContent(new Element("Role", XMLNS).setText(role.getName()));
			});
		} else {
			organizationElement.addContent(new Element("Role",XMLNS));
		}



		return organizationElement;

	}

	private List<Element> getPersonElements(BioSampleGroup group) {
		List<Element> personElements = new ArrayList<>();

		if ( existsAndUniqueMSI(group) ) {

			MSI msi = group.getMSIs().stream().findFirst().get();
			msi.getContacts().forEach(user -> {
				personElements.add(getPersonWithFields(user));
			});

		}

		return personElements;

	}

	private Element getPersonWithFields(Contact user) {

		Element personElement = new Element("Person",XMLNS);

		personElement.addContent(new Element("FirstName",XMLNS).setText(user.getFirstName()));
		personElement.addContent(new Element("LastName",XMLNS).setText(user.getLastName()));
		personElement.addContent(new Element("MidInitials",XMLNS).setText(user.getMidInitials()));
		personElement.addContent(new Element("Email",XMLNS).setText(user.getEmail()));


		if ( ! user.getContactRoles().isEmpty() ) {
			ContactRole firstRole = user.getContactRoles().stream().findFirst().get();
			personElement.addContent(new Element("Role",XMLNS).setText(firstRole.getName()));
		} else {
			personElement.addContent(new Element("Role",XMLNS));
		}

		return personElement;

	}

	private List<Element> getDatabaseElements(BioSampleGroup group) {


		List<Element> databaseElements = new ArrayList<>();

        if (existsAndUniqueMSI(group)) {

            MSI msi = group.getMSIs().iterator().next();

            Set<DatabaseRecordRef> databases = msi.getDatabaseRecordRefs();
            databases.forEach(databaseRecordRef -> {
                Element dbRecord = new Element("Database",XMLNS);
                dbRecord.addContent(new Element("Name",XMLNS).setText(databaseRecordRef.getDbName()));
                dbRecord.addContent(new Element("ID",XMLNS).setText(databaseRecordRef.getAcc()));
                dbRecord.addContent(new Element("URI",XMLNS).setText(databaseRecordRef.getUrl()));

                databaseElements.add(dbRecord);
            });

        }



        // Add MyEquivalence references
        Set<Entity> externalEquivalences = MyEquivalenceManager.getGroupExternalEquivalences(group.getAcc());
        externalEquivalences.forEach(entity -> {

            Element dbRecord = new Element("Database",XMLNS);
            dbRecord.addContent(new Element("Name",XMLNS).setText(entity.getService().getTitle()));
            dbRecord.addContent(new Element("ID",XMLNS).setText(entity.getAccession()));
            dbRecord.addContent(new Element("URI",XMLNS).setText(entity.getURI()));
            databaseElements.add(dbRecord);

        });


		return databaseElements;
	}

	private List<Element> getPublicationElements(BioSampleGroup group) {
		List<Element> publicationElements = new ArrayList<>();

		if ( existsAndUniqueMSI(group) ) {
			Set<Publication> publications = group.getMSIs().stream().findFirst().get().getPublications();

			publications.forEach(publication -> {
				publicationElements.add(getPublicationWithFields(publication));
			});

		}

		return publicationElements;
	}

	private Element getPublicationWithFields(Publication publication) {

		Element publicationElement = new Element("Publication",XMLNS);

		publicationElement.addContent(new Element("DOI",XMLNS).setText(publication.getDOI()));
		publicationElement.addContent(new Element("PubMedID",XMLNS).setText(publication.getPubmedId()));

		return publicationElement;

	}

	private Element getSampleIdsElement(BioSampleGroup group) {

		Element sampleIdsElement = new Element("SampleIds",XMLNS);
		if ( existsAndUniqueMSI(group) ) {
			Set<BioSample> biosamples = group.getMSIs().stream().findFirst().get().getSamples();
			biosamples.forEach(biosample -> {
				sampleIdsElement.addContent(new Element("Id",XMLNS).setText(biosample.getAcc()));
			});
		}

		return sampleIdsElement;
	}


	private List<Element> getBiosampleElements(BioSampleGroup group) {
		List<Element> biosampleElements = new ArrayList<>();

		if ( existsAndUniqueMSI(group) ) {

			BioSampleXMLService xmlService = new BioSampleXMLService();

			Set<BioSample> biosamples = group.getMSIs().stream().findFirst().get().getSamples();
			biosamples.forEach(biosample -> {
				biosampleElements.add(xmlService.getXMLElement(biosample));
			});
		}


		return biosampleElements;
	}

	private boolean existsAndUniqueMSI(BioSampleGroup group) {
		return !group.getMSIs().isEmpty() && group.getMSIs().size() == 1;
	}

    private DateTime getGMTDateTime(Date date){
        return new DateTime(date).toDateTime(DateTimeZone.forID("Etc/GMT"));
    }

	private String renderDocument(Document doc) {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		return xmlOutput.outputString(doc);
	}

	private String renderElement(Element el) {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		return xmlOutput.outputString(el);

	}




}
