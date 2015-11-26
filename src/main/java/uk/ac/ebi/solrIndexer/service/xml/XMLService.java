package uk.ac.ebi.solrIndexer.service.xml;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filter;
import uk.ac.ebi.solrIndexer.service.xml.filters.EmptyElementFilter;

import java.util.Iterator;

public interface XMLService<E> {

	public String getXMLString(E type);

	public Document getXMLDocument(E type);

	public Element getXMLElement(E type);

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
