package uk.ac.ebi.biosamples.solrindexer.service.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.solrindexer.service.MyEquivalenceManager;
import uk.ac.ebi.biosamples.solrindexer.service.MyEquivalenceNameConverter;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.toplevel.Annotation;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.model.Entity;

@Component
public class BioSampleXMLService implements XMLService<BioSample> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final Namespace XMLNS = Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");

	private final DateTimeZone dtz = DateTimeZone.forID("Etc/GMT");
	private final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
	// private final DateTimeFormatter dateFormatter = new
	// DateTimeFormatterBuilder()
	// .appendYear(4,4)
	// .appendLiteral('-')
	// .appendMonthOfYear(2)
	// .appendLiteral('-')
	// .appendDayOfMonth(2)
	// .appendLiteral('T')
	// .appendHourOfDay(2)
	// .appendLiteral(':')
	// .appendMinuteOfHour(2)
	// .appendLiteral(':')
	// .appendSecondOfMinute(2)
	// .appendTimeZoneOffset("+00:00",true,2,2)
	// .toFormatter();

	@Autowired
	private MyEquivalenceManager myEquivalentsManager;

	public BioSampleXMLService() {

	}

	public MyEquivalenceManager getMyEquivalentsManager() {
		return myEquivalentsManager;
	}

	public void setMyEquivalentsManager(MyEquivalenceManager myEquivalentsManager) {
		this.myEquivalentsManager = myEquivalentsManager;
	}

	@Override
	public String getXMLString(BioSample sample, EntityMappingManager entityMappingManager) {

		return renderDocument(getXMLDocument(sample, entityMappingManager));
	}

	@Override
	public Document getXMLDocument(BioSample sample, EntityMappingManager entityMappingManager) {

		Document doc = generateBaseDocument();
		Element biosampleElement = getXMLElement(sample, entityMappingManager);

		doc.setRootElement(biosampleElement);

		return doc;

	}

	@Override
	public Element getXMLElement(BioSample sample, EntityMappingManager entityMappingManager) {
		Element root = getDocumentRoot(sample);

		// Add all properties
		List<Element> annotations = getBiosampleAnnotations(sample);
		List<Element> properties = getBiosampleProperties(sample);
		List<Element> derivedFrom = getBiosampleDerivedFrom(sample);
		List<Element> databases = getBiosampleDatabase(sample, entityMappingManager);
		Content groupIds = getBiosampleGroupIds(sample);

		root.addContent(annotations).addContent(properties).addContent(derivedFrom).addContent(databases);
		// .addContent(groupIds);

		// filterDescendantOf(root, new EmptyElementFilter().negate());

		return root;
	}

	private Document generateBaseDocument() {

		Document doc = new Document();
		doc.addContent(new Comment("BioSamples XML API - version 1.0"));

		return doc;
	}

	private Element getDocumentRoot(BioSample sample) {

		Element root = new Element("BioSample", XMLNS);

		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.addNamespaceDeclaration(xsi);

		List<Attribute> rootAttributes = getRootAttributes(sample);
		rootAttributes.add(rootAttributes.size() - 1,
				new Attribute("schemaLocation",
						"http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/BioSDSchema.xsd",
						xsi));

		root.setAttributes(rootAttributes);

		return root;
	}

	private List<Attribute> getRootAttributes(BioSample sample) {

		List<Attribute> rootAttributes = new ArrayList<>();

		Attribute id = new Attribute("id", sample.getAcc());
		Attribute releaseDate = new Attribute("submissionReleaseDate", "");
		Attribute updateDate = new Attribute("submissionUpdateDate", "");

		Set<MSI> allMSIs = sample.getMSIs();
		if (allMSIs.size() == 1) {

			MSI singleMSI = allMSIs.iterator().next();
			DateTime gmtReleaseDate = getGMTDateTime(singleMSI.getReleaseDate());
			DateTime gmtUpdateDate = getGMTDateTime(singleMSI.getUpdateDate());
			releaseDate.setValue(gmtReleaseDate.toString(dtf));
			updateDate.setValue(gmtUpdateDate.toString(dtf));
		}

		rootAttributes.add(releaseDate);
		rootAttributes.add(updateDate);
		rootAttributes.add(id);

		return rootAttributes;

	}

	private List<Element> getBiosampleProperties(BioSample sample) {

		List<Element> properties = new ArrayList<>();

		Collection<ExperimentalPropertyValue> allProperties = sample.getPropertyValues();

		allProperties = allProperties.stream().filter(expProperty -> !isDerivedFromPropertyType(expProperty))
				.collect(Collectors.toList());

		allProperties.forEach(propertyValue -> {
			Element property = getProperty(propertyValue);

			Optional<Element> possibleMultiValueProperty = searchPropertyWithSameAttributes(properties, property);
			if (!possibleMultiValueProperty.isPresent()) {
				properties.add(property);
			} else {
				Element multiValueProperty = possibleMultiValueProperty.get();
				properties.remove(multiValueProperty);
				Element detachedInnerValue = property.getChild("QualifiedValue", property.getNamespace()).detach();
				multiValueProperty.addContent(detachedInnerValue);
				properties.add(multiValueProperty);
			}

		});
		return properties;

	}

	// TODO Handle the situation where each property has multiple qualified
	// values
	private Element getProperty(ExperimentalPropertyValue<?> pv) {

		Element propertyElement = new Element("Property", XMLNS);

		ExperimentalPropertyType propType = pv.getType();

		Attribute classAttr = new Attribute("class", clean(propType.getTermText()));
		Attribute typeAttr = new Attribute("type", "STRING");
		Attribute characteristicAttr = new Attribute("characteristic", Boolean.toString(isCharacterstic(pv)));
		Attribute commentAttr = new Attribute("comment", Boolean.toString(isComment(pv)));

		List<Attribute> propertyAttributes = new ArrayList<>();
		propertyAttributes.add(classAttr);
		propertyAttributes.add(characteristicAttr);
		propertyAttributes.add(commentAttr);
		propertyAttributes.add(typeAttr);

		propertyElement.setAttributes(propertyAttributes);

		Element qualityValue = getPropertyQualifiedValue(pv);
		propertyElement.setContent(qualityValue);

		return propertyElement;

	}

	private Boolean isComment(ExperimentalPropertyValue<?> propertyValue) {
		return propertyValue instanceof SampleCommentValue;
	}

	private Boolean isCharacterstic(ExperimentalPropertyValue<?> propertyValue) {
		return propertyValue instanceof BioCharacteristicValue;
	}

	// TODO a property can have multiple qualified values
	private Element getPropertyQualifiedValue(ExperimentalPropertyValue<?> pv) {

		Element qualityValueElement = new Element("QualifiedValue", XMLNS);

		Element value = new Element("Value", XMLNS).setText(clean(pv.getTermText()));

		Element termSourceRef = getQualityValue_TermSourceRef(pv);
		Element unit = new Element("Unit", XMLNS);
		if (pv.getUnit() != null) {
			unit.setText(clean(pv.getUnit().getTermText()));
		}

		qualityValueElement.addContent(value);

		if (!termSourceRef.getValue().isEmpty()) {
			qualityValueElement.addContent(termSourceRef);
		}

		if (!unit.getValue().isEmpty()) {
			qualityValueElement.addContent(unit);
		}

		return qualityValueElement;

	}

	private Element getQualityValue_TermSourceRef(ExperimentalPropertyValue<?> pv) {

		Element termSourceRef = new Element("TermSourceREF", XMLNS);

		OntologyEntry ontology = pv.getSingleOntologyTerm();
		if (ontology != null) {
			ReferenceSource ontologyRefSource = ontology.getSource();

			if (ontologyRefSource != null) {

				Element tsrName = new Element("Name", XMLNS).setText(clean(ontologyRefSource.getName()));
				Element tsrDescription = new Element("Description", XMLNS).setText(clean(ontologyRefSource.getDescription()));
				Element tsrURI = new Element("URI", XMLNS).setText(clean(ontologyRefSource.getUrl()));
				Element tsrVersion = new Element("Version", XMLNS).setText(clean(ontologyRefSource.getVersion()));
				Element tsrTermSourceID = new Element("TermSourceID", XMLNS).setText(clean(ontology.getAcc()));

				List<Element> allContents = new ArrayList<>();
				allContents.add(tsrName);
				allContents.add(tsrDescription);
				allContents.add(tsrURI);
				allContents.add(tsrVersion);
				allContents.add(tsrTermSourceID);

				termSourceRef.addContent(allContents);

			}
		}

		return termSourceRef;

	}

	private List<Element> getBiosampleAnnotations(BioSample sample) {

		List<Element> annotations = new ArrayList<>();

		Set<Annotation> annotationSet = sample.getAnnotations();
		annotationSet.forEach(annotation -> {
			//TODO do we want to export this? probably not
			//Element annotationElement = new Element("Annotation", XMLNS).setText(clean(annotation.getInternalNotes()));
			//annotationElement.setAttribute("type", annotation.getType().getName());
			//annotations.add(annotationElement);
		});

		return annotations;
	}

	private List<Element> getBiosampleDatabase(BioSample sample, EntityMappingManager entityMappingManager) {

		List<Element> databaseElements = new ArrayList<>();

		// if (existsAndUniqueMSI(sample)) {
		//
		// MSI msi = sample.getMSIs().iterator().next();
		//
		// Set<DatabaseRecordRef> databases = msi.getDatabaseRecordRefs();
		// databases.forEach(databaseRecordRef -> {
		//
		// if
		// (!ExternalDBReferenceChecker.isReferenceValid(databaseRecordRef.getUrl()))
		// {
		// return;
		// }
		//
		// Element dbRecord = new Element("Database", XMLNS);
		// dbRecord.addContent(new Element("Name",
		// XMLNS).setText(databaseRecordRef.getDbName()));
		// dbRecord.addContent(new Element("ID",
		// XMLNS).setText(databaseRecordRef.getAcc()));
		// dbRecord.addContent(new Element("URI",
		// XMLNS).setText(databaseRecordRef.getUrl()));
		//
		// databaseElements.add(dbRecord);
		// });
		//
		// }
		Set<DatabaseRecordRef> databases = sample.getDatabaseRecordRefs();
		databases.forEach(databaseRecordRef -> {

			// if
			// (!ExternalDBReferenceChecker.isReferenceValid(databaseRecordRef.getUrl()))
			// {
			// return;
			// }

			Element dbRecord = new Element("Database", XMLNS);
			
			String name = databaseRecordRef.getDbName();
			name = MyEquivalenceNameConverter.convert(name);
			
			dbRecord.addContent(new Element("Name", XMLNS).setText(clean(name)));
			dbRecord.addContent(new Element("ID", XMLNS).setText(clean(databaseRecordRef.getAcc())));
			dbRecord.addContent(new Element("URI", XMLNS).setText(clean(databaseRecordRef.getUrl())));

			databaseElements.add(dbRecord);
		});

		// Add MyEquivalence references
		Set<Entity> externalEquivalences = myEquivalentsManager.getSampleExternalEquivalences(sample.getAcc(), entityMappingManager);
		externalEquivalences.forEach(entity -> {

			Element dbRecord = new Element("Database", XMLNS);
			
			String name = entity.getService().getTitle();
			name = MyEquivalenceNameConverter.convert(name);
			
			dbRecord.addContent(new Element("Name", XMLNS).setText(clean(name)));
			dbRecord.addContent(new Element("ID", XMLNS).setText(clean(entity.getAccession())));
			dbRecord.addContent(new Element("URI", XMLNS).setText(clean(entity.getURI())));
			databaseElements.add(dbRecord);

		});

		return databaseElements;

	}

	private Element getBiosampleGroupIds(BioSample sample) {

		Element groupElement = new Element("GroupIds", XMLNS);

		Set<BioSampleGroup> sampleGroups = sample.getGroups();
		sampleGroups.forEach(gr -> {

			Element grId = new Element("Id", XMLNS).setText(clean(gr.getAcc()));
			groupElement.addContent(grId);

		});

		return groupElement;
	}

	private List<Element> getBiosampleDerivedFrom(BioSample sample) {
		List<Element> derivations = new ArrayList<>();

		Optional<ExperimentalPropertyValue> propertyDerivedFrom = sample.getPropertyValues().stream()
				.filter(exp -> isDerivedFromPropertyType(exp)).findFirst();

		if (propertyDerivedFrom.isPresent()) {
			Element derivedFrom = new Element("derivedFrom", XMLNS).setText(clean(propertyDerivedFrom.get().getTermText()));
			derivations.add(derivedFrom);
		}

		return derivations;
	}

	private Optional<Element> searchPropertyWithSameAttributes(List<Element> elements, Element testElement) {
		return elements.stream().filter(element -> {

			/*
			 * List<Attribute> testAttributeList = testElement.getAttributes();
			 * 
			 * boolean allAttributesAreEqual = true;
			 * 
			 * for(Attribute testAttribute: testAttributeList) {
			 * 
			 * Attribute comparableAttribute =
			 * element.getAttribute(testAttribute.getName(),testAttribute.
			 * getNamespace()); if ( comparableAttribute != null ) {
			 * allAttributesAreEqual =
			 * comparableAttribute.getValue().equals(testAttribute.getValue());
			 * if(!allAttributesAreEqual) { return false; } } } return
			 * allAttributesAreEqual;
			 */

			Attribute testClassAttribute = testElement.getAttribute("class");
			Attribute controlClassAttribute = element.getAttribute("class");

			return testClassAttribute.getValue().equals(controlClassAttribute.getValue());

		}).findFirst();
	}

	private boolean isDerivedFromPropertyType(ExperimentalPropertyValue<?> pv) {
		return pv.getType().getTermText().equals("Derived From");
	}

	private boolean existsAndUniqueMSI(BioSample sample) {
		return !sample.getMSIs().isEmpty() && sample.getMSIs().size() == 1;
	}

	private DateTime getGMTDateTime(Date date) {
		return new DateTime(date).toDateTime(DateTimeZone.forID("Etc/GMT"));
	}

	private DateTime getGMTDateTime(DateTime dateTime) {
		return dateTime.toDateTime(DateTimeZone.forID("Etc/GMT"));
	}

	private String renderDocument(Document doc) {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		return xmlOutput.outputString(doc);
	}

}
