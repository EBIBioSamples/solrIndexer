package uk.ac.ebi.service.filters;

import org.jdom2.Element;
import org.jdom2.filter.AbstractFilter;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.service.BioSampleGroupXMLService;

public class EmptyElementFilter extends AbstractFilter<Element> {

	private static Logger log = LoggerFactory.getLogger(BioSampleGroupXMLService.class.getName());

	public EmptyElementFilter() {
		super();
	}

	@Override
	public Element filter(Object obj) {
		if (obj instanceof Element) {
			Element elem = (Element) obj;
//			log.info("\n" + outputXML(elem));
			if ( !elem.getDescendants(Filters.element()).hasNext() && elem.getValue().isEmpty()) {
				return elem;
			}
		}
		return null;
	}

	private String outputXML(Element el) {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		return xmlOutput.outputString(el);
	}

}
