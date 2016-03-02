package uk.ac.ebi.solrIndexer.service.xml;

import java.util.Iterator;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filter;

public interface XMLService<E> {

	String getXMLString(E type);

	Document getXMLDocument(E type);

	Element getXMLElement(E type);

	public default void filterDescendantOf(Element element, Filter<?> filter) {

		Iterator<?> iterator = element.getDescendants().iterator();

		while(iterator.hasNext()) {
			Content child = (Content)iterator.next();
			if ( !filter.matches(child) ) {
				iterator.remove();
			}

		}
	}
	
	public default String clean(String in) {
		//handle nulls
        if (in == null){
            return in;
        }
        //trim extra whitespace at start and end
        in = in.trim();
        //XML automatically replaces consecutive spaces with single spaces
        while (in.contains("  ")) {
            in = in.replace("  ", " ");
        }
        
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF))){
                out.append(current);
            }
        }
        return out.toString();
	}

}
