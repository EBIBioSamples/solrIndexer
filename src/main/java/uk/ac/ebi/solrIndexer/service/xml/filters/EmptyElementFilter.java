package uk.ac.ebi.solrIndexer.service.xml.filters;

import org.jdom2.Element;
import org.jdom2.filter.AbstractFilter;

public class EmptyElementFilter extends AbstractFilter<Element> {

	public EmptyElementFilter() {
		super();
	}

	@Override
	public Element filter(Object obj) {
		if (obj instanceof Element) {
			Element elem = (Element) obj;
			if (elem.getValue().isEmpty() || elem.getChildren().isEmpty()) {
				return elem;
			}
		}
		return null;
	}
}
