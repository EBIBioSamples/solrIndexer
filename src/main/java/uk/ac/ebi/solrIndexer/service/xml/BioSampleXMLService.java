package uk.ac.ebi.solrIndexer.service.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.Product;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.toplevel.Annotation;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;
import uk.ac.ebi.solrIndexer.service.xml.filters.EmptyElementFilter;

public class BioSampleXMLService implements XMLService<BioSample> {

	private static Logger log = LoggerFactory.getLogger(BioSampleGroupXMLService.class.getName());
	private final Namespace XMLNS =
			Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");

	public BioSampleXMLService() {}

	@Override
	public String getXMLString(BioSample sample) {

		return renderDocument(getXMLDocument(sample));
	}

	@Override
	public Document getXMLDocument(BioSample sample) {

		Document doc = generateBaseDocument();
		Element biosampleElement = getXMLElement(sample);

		doc.setRootElement(biosampleElement);

		return doc;

	}

	@Override
	public Element getXMLElement(BioSample sample) {
		Element root = getDocumentRoot(sample);

		// Add all properties
		List<Element> annotations = getBiosampleAnnotations(sample);
		List<Element> properties  = getBiosampleProperties(sample);
		List<Element> derivedFrom = getBiosampleDerivedFrom(sample);
		List<Element> databases   = getBiosampleDatabase(sample);
		Content groupIds 		  = getBiosampleGroupIds(sample);

		root.addContent(annotations)
			.addContent(properties)
			.addContent(derivedFrom)
			.addContent(databases)
			.addContent(groupIds);

		filterDescendantOf(root, new EmptyElementFilter().negate());

		return root;
	}


	private Document generateBaseDocument() {

		Document doc = new Document();
		doc.addContent(new Comment("BioSamples XML API - version 1.0"));

		return doc;
	}

	private Element getDocumentRoot(BioSample sample) {

		Element   root  = new Element("BioSample", XMLNS);

		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.addNamespaceDeclaration(xsi);

		List<Attribute> rootAttributes = getRootAttributes(sample);
		rootAttributes.add(new Attribute("schemaLocation", "http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/BioSDSchema.xsd",
				xsi));

		root.setAttributes(rootAttributes);

		return root;
	}

	private List<Attribute> getRootAttributes(BioSample sample) {

		List<Attribute> rootAttributes = new ArrayList<>();

		Attribute id = new Attribute("id", sample.getAcc());


		rootAttributes.add(id);


		return rootAttributes;

	}

	private List<Element> getBiosampleProperties(BioSample sample) {

		List<Element> properties = new ArrayList<>();


		Collection<ExperimentalPropertyValue> allProperties = sample.getPropertyValues();


		allProperties.forEach(
				propertyValue -> {
					Element property = getProperty(propertyValue);
					properties.add(property);
				}
		);


		return properties;

	}

	//TODO Handle the situation where each property has multiple qualified values
	private Element getProperty(ExperimentalPropertyValue pv) {

		Element propertyElement = new Element("Property",XMLNS);

		ExperimentalPropertyType propType = pv.getType();

		Attribute classAttr          = new Attribute("class", propType.getTermText());
		Attribute typeAttr           = new Attribute("type", "STRING");
		Attribute characteristicAttr = new Attribute("characteristic", Boolean.toString(!isComment(pv)));
		Attribute commentAttr        = new Attribute("comment", Boolean.toString(isComment(pv)));

		List<Attribute> propertyAttributes = new ArrayList<>();
		propertyAttributes.add(classAttr);
		propertyAttributes.add(typeAttr);
		propertyAttributes.add(characteristicAttr);
		propertyAttributes.add(commentAttr);

		propertyElement.setAttributes(propertyAttributes);


		Element qualityValue = getPropertyQualifiedValue(pv);
		propertyElement.setContent(qualityValue);

		return propertyElement;

	}

	private Boolean isComment(ExperimentalPropertyValue propertyValue) {
		return propertyValue instanceof SampleCommentValue;
	}

	//TODO a property can have multiple qualified values
	private Element getPropertyQualifiedValue(ExperimentalPropertyValue pv) {

		Element qualityValueElement = new Element("QualifiedValue",XMLNS);


		Element value         = new Element("Value",XMLNS).setText(pv.getTermText());

		Element termSourceRef = getQualityValue_TermSourceRef(pv);
		Element unit = new Element("Unit",XMLNS);
		if (pv.getUnit() != null) {
			unit.setText(pv.getUnit().getTermText());
		}


		qualityValueElement.addContent(value);

		if ( !termSourceRef.getValue().isEmpty() ) {
			qualityValueElement.addContent(termSourceRef);
		}

		if ( !unit.getValue().isEmpty() ) {
			qualityValueElement.addContent(unit);
		}


		return qualityValueElement;

	}

	private Element getQualityValue_TermSourceRef(ExperimentalPropertyValue pv) {


		Element termSourceRef = new Element("TermSourceREF",XMLNS);

		OntologyEntry   ontology          = pv.getSingleOntologyTerm();
		if (ontology != null) {
			ReferenceSource ontologyRefSource = ontology.getSource();
			Element tsrName = new Element("Name", XMLNS).setText(ontologyRefSource.getName());
			Element tsrDescription = new Element("Description", XMLNS).setText(ontologyRefSource.getDescription());
			Element tsrURI = new Element("URI", XMLNS).setText(ontologyRefSource.getUrl());
			Element tsrVersion = new Element("Version", XMLNS).setText(ontologyRefSource.getVersion());
			Element tsrTermSourceID = new Element("TermSourceID", XMLNS).setText(ontology.getAcc());

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

	private List<Element> getBiosampleAnnotations(BioSample sample) {

		List<Element> annotations = new ArrayList<>();

		Set<Annotation> annotationSet = sample.getAnnotations();
		annotationSet.forEach(annotation -> {

			Element annotationElement = new Element("Annotation",XMLNS).setText(annotation.getInternalNotes());
			annotationElement.setAttribute("type",annotation.getType().getName());
			annotations.add(annotationElement);

		});

		return annotations;
	}

	private List<Element> getBiosampleDatabase(BioSample sample) {
		List<Element> databases = new ArrayList<>();

		Set<DatabaseRecordRef> dbSet = sample.getDatabaseRecordRefs();
		dbSet.forEach(db -> {

			Element dbElement = new Element("Database",XMLNS);

			Element nameElement = new Element("Name",XMLNS).setText(db.getDbName());
			Element idElement = new Element("ID",XMLNS).setText(db.getAcc());
			Element uriElement = new Element("URI",XMLNS).setText(db.getUrl());

			dbElement.addContent(nameElement).addContent(idElement).addContent(uriElement);

			databases.add(dbElement);

		});

		return databases;
	}

	private Element getBiosampleGroupIds(BioSample sample) {

		Element groupElement = new Element("GroupIds",XMLNS);

		Set<BioSampleGroup> sampleGroups = sample.getGroups();
		sampleGroups.forEach(gr -> {

			Element grId = new Element("Id",XMLNS).setText(gr.getAcc());
			groupElement.addContent(grId);

		});



		return groupElement;
	}

	private List<Element> getBiosampleDerivedFrom(BioSample sample) {
		List<Element> derivations = new ArrayList<>();

		Set<Product> derivedFromSet = sample.getDerivedFrom();
		derivedFromSet.forEach(product -> {
			Element derivation = new Element("derivedFrom",XMLNS).setText(product.getAcc());
			derivations.add(derivation);
		});

		return derivations;
	}

	private String renderDocument(Document doc) {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		return xmlOutput.outputString(doc);
	}


}
