package org.ebi.ac.uk.service;

import org.jdom2.Document;
import org.jdom2.Element;

public interface XMLService<E> {

	public String getXMLString(E type);

	public Document getXMLDocument(E type);

	public Element getXMLElement(E type);

}
