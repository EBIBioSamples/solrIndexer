package uk.ac.ebi.service;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filter;

import java.util.Iterator;

public interface XMLService<E> {

	String getXMLString(E type);

	Document getXMLDocument(E type);

	Element getXMLElement(E type);

	public default void filterDescendantOf(Element element, Filter filter) {

		Iterator iterator = element.getDescendants().iterator();

		while(iterator.hasNext()) {
			Content child = (Content)iterator.next();
			if ( !filter.matches(child) ) {
				iterator.remove();
			}

		}
	}

}
