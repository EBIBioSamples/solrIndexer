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

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.Product;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.toplevel.Annotation;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;

public class BioSampleXMLService implements XMLService<BioSample> {


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

		return root;
	}


	private Document generateBaseDocument() {

		Document doc = new Document();
		doc.addContent(new Comment("BioSamples XML API - version 1.0"));

		return doc;
	}

	private Element getDocumentRoot(BioSample sample) {

		Namespace xmlns = Namespace.getNamespace("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0");
		Element   root  = new Element("Biosample", xmlns);

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


		for (ExperimentalPropertyValue val : allProperties) {

			Element property = getProperty(val);
			properties.add(property);
		}

		return properties;

	}

	//TODO Handle the situation where each property has multiple qualified values
	private Element getProperty(ExperimentalPropertyValue pv) {

		Element propertyElement = new Element("Property");

		ExperimentalPropertyType propType = pv.getType();

		Attribute classAttr          = new Attribute("class", propType.getTermText());
		Attribute typeAttr           = new Attribute("type", "STRING");
		Attribute characteristicAttr = new Attribute("characteristic", "true");
		Attribute commentAttr        = new Attribute("comment", "false");

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

	//TODO a property can have multiple qualified values
	private Element getPropertyQualifiedValue(ExperimentalPropertyValue pv) {

		Element qualityValueElement = new Element("QualifiedValue");


		Element value         = new Element("Value").setText(pv.getTermText());
		Element termSourceRef = getQualityValue_TermSourceRef(pv);
		Element unit          = new Element("Unit").setText(pv.getUnit().getTermText());


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

	private List<Element> getBiosampleAnnotations(BioSample sample) {

		List<Element> annotations = new ArrayList<>();

		Set<Annotation> annotationSet = sample.getAnnotations();
		annotationSet.forEach(annotation -> {

			Element annotationElement = new Element("Annotation").setText(annotation.getInternalNotes());
			annotationElement.setAttribute("type",annotation.getType().getName());
			annotations.add(annotationElement);

		});

		return annotations;
	}

	private List<Element> getBiosampleDatabase(BioSample sample) {
		List<Element> databases = new ArrayList<>();

		Set<DatabaseRecordRef> dbSet = sample.getDatabaseRecordRefs();
		dbSet.forEach(db -> {

			Element dbElement = new Element("Database");

			Element nameElement = new Element("Name").setText(db.getDbName());
			Element idElement = new Element("ID").setText(db.getAcc());
			Element uriElement = new Element("URI").setText(db.getUrl());

			dbElement.addContent(nameElement).addContent(idElement).addContent(uriElement);

			databases.add(dbElement);

		});

		return databases;
	}

	private Element getBiosampleGroupIds(BioSample sample) {

		Element groupElement = new Element("GroupIds");

		Set<BioSampleGroup> sampleGroups = sample.getGroups();
		sampleGroups.forEach(gr -> {

			Element grId = new Element("Id").setText(gr.getAcc());
			groupElement.addContent(grId);

		});



		return groupElement;
	}

	private List<Element> getBiosampleDerivedFrom(BioSample sample) {
		List<Element> derivations = new ArrayList<>();

		Set<Product> derivedFromSet = sample.getDerivedFrom();
		derivedFromSet.forEach(product -> {
			Element derivation = new Element("derivedFrom").setText(product.getAcc());
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
