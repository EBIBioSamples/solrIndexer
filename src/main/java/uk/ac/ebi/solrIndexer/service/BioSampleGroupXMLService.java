package org.ebi.ac.uk.service;

import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.ContactRole;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class BioSampleGroupXMLService implements XMLService<BioSampleGroup>{


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

		List<Attribute> completeRootAttributes = root.getAttributes();
		List<Attribute> specificRootAttribute  = getRootAttributes(group);
		completeRootAttributes.addAll(specificRootAttribute);
		root.setAttributes(completeRootAttributes);

		List<Element> groupAnnotations = getAnnotationsElements(group);
		List<Element> groupTermSource  = getTermSourceElements(group);
		List<Element> groupProperties  = getPropertiesElements(group);
		List<Element> groupOrganizations = getOrganizationElements(group);
		List<Element> groupPersons 	= getPersonElements(group);
		List<Element> groupDatabases = getDatabaseElements(group);
		List<Element> groupPublication = getPublicationElements(group);
		Content groupSampleIds = getSampleIdsElement(group);
		List<Element> groupBiosamples = getBiosampleElements(group);

		root.addContent(groupAnnotations)
				.addContent(groupTermSource)
				.addContent(groupProperties)
				.addContent(groupOrganizations)
				.addContent(groupPersons)
				.addContent(groupDatabases)
				.addContent(groupPublication)
				.addContent(groupSampleIds)
				.addContent(groupBiosamples);

		return root;

	}


	public Document generateBaseDocument() {

		Document doc = new Document();
		doc.addContent(new Comment("BioSamples XML API - version 1.0"));

		return doc;

	}

	private Element getDocumentRoot(BioSampleGroup group){

		Namespace xmlns = Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");
		Element   root  = new Element("BiosampleGroup", xmlns);

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

		Element property = new Element("Property");

		ExperimentalPropertyType propType = propertyValue.getType();

		Attribute classAttr          = new Attribute("class", propType.getTermText());
		Attribute typeAttr           = new Attribute("type", "STRING");
		Attribute characteristicAttr = new Attribute("characteristic", "true");
		Attribute commentAttr        = new Attribute("comment", "false");

		List<Attribute> propertyAttributes = new ArrayList<>();
		propertyAttributes.add(classAttr);
		propertyAttributes.add(typeAttr);
		propertyAttributes.add(characteristicAttr);
		propertyAttributes.add(commentAttr);

		property.setAttributes(propertyAttributes);

		List<Element> qualityValues = getQualifiedValues(propertyValue);
		property.setContent(qualityValues);

		return property;

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

		Element qualifiedValue = new Element("QualifiedValue");

		Element value         = new Element("Value").setText(propertyValue.getTermText());
		Element termSourceRef = getQualityValue_TermSourceRef(propertyValue);
		Element unit          = new Element("Unit").setText(propertyValue.getUnit().getTermText());


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


		Element termSourceRef = new Element("TermSourceRef");

		OntologyEntry   ontology          = pv.getSingleOntologyTerm();
		ReferenceSource ontologyRefSource = ontology.getSource();
		Element         tsrName           = new Element("Name").setText(ontologyRefSource.getName());
		Element         tsrDescription    = new Element("Description").setText(ontologyRefSource.getDescription());
		Element         tsrURI            = new Element("URI").setText(ontologyRefSource.getUrl());
		Element         tsrVersion        = new Element("Version").setText(ontologyRefSource.getVersion());
		Element         tsrTermSourceID   = new Element("TermSourceID").setText(ontology.getAcc());

		List<Element> allContents = new ArrayList<>();
		allContents.add(tsrName);
		allContents.add(tsrDescription);
		allContents.add(tsrURI);
		allContents.add(tsrVersion);
		allContents.add(tsrTermSourceID);

		termSourceRef.addContent(allContents);

		return termSourceRef;


	}

	private List<Element> getOrganizationElements(BioSampleGroup group) {
		List<Element> organizations = new ArrayList<>();

		if ( existsAndUniqueMSI(group) ) {

			Set<Organization> organizationSet =
					group.getMSIs().stream().findFirst().get().getOrganizations();

			// Add all the organizations to the parent element
			organizationSet.forEach(organization -> {
				organizations.add(getOrganizationFields(organization));
			});

		}

		return organizations;
	}

	private Element getOrganizationFields(Organization organization) {

		Element organizationElement = new Element("Organization");

		organizationElement.addContent(new Element("Name").setText(organization.getName()));
		organizationElement.addContent(new Element("Address").setText(organization.getAddress()));
		organizationElement.addContent(new Element("URI").setText(organization.getUrl()));
		organizationElement.addContent(new Element("Email").setText(organization.getEmail()));
		organizationElement.addContent(new Element("Role").setText(organization.getOrganizationRoles().stream().findFirst().get().getName()));

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

		Element personElement = new Element("Person");

		personElement.addContent(new Element("FirstName").setText(user.getFirstName()));
		personElement.addContent(new Element("LastName").setText(user.getLastName()));
		personElement.addContent(new Element("MidInitials").setText(user.getMidInitials()));
		personElement.addContent(new Element("Email").setText(user.getEmail()));


		if ( ! user.getContactRoles().isEmpty() ) {
			ContactRole firstRole = user.getContactRoles().stream().findFirst().get();
			personElement.addContent(new Element("Role").setText(firstRole.getName()));
		}



		return personElement;

	}

	private List<Element> getDatabaseElements(BioSampleGroup group) {

		List<Element> databaseElements = new ArrayList<>();



		group.getDatabaseRecordRefs().forEach(databaseRecordRef -> {
			Element dbRecord = new Element("Database");
			dbRecord.addContent(new Element("Name").setText(databaseRecordRef.getDbName()));
			dbRecord.addContent(new Element("ID").setText(databaseRecordRef.getAcc()));
			dbRecord.addContent(new Element("URI").setText(databaseRecordRef.getUrl()));

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

		Element publicationElement = new Element("Publication");

		publicationElement.addContent(new Element("DOI").setText(publication.getDOI()));
		publicationElement.addContent(new Element("PubMedID").setText(publication.getPubmedId()));

		return publicationElement;

	}

	private Element getSampleIdsElement(BioSampleGroup group) {

		Element sampleIdsElement = new Element("SampleIds");
		if ( existsAndUniqueMSI(group) ) {
			Set<BioSample> biosamples = group.getMSIs().stream().findFirst().get().getSamples();
			biosamples.forEach(biosample -> {
				sampleIdsElement.addContent(new Element("Id").setText(biosample.getAcc()));
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
				biosampleElements.add(xmlService.getBiosampleElement(biosample));
			});
		}


		return biosampleElements;
	}

	private boolean existsAndUniqueMSI(BioSampleGroup group) {
		return !group.getMSIs().isEmpty() && group.getMSIs().size() == 1;
	}

	private String renderDocument(Document doc) {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		return xmlOutput.outputString(doc);
	}







}
