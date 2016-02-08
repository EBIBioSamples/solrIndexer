package uk.ac.ebi.service.nodematchers;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlunit.diff.ElementSelector;

import java.util.List;

/**
 * Created by lucacherubin on 21/01/2016.
 */
public abstract class BaseElementSelector implements ElementSelector {

    @Override
    public boolean canBeCompared(Element control, Element test) {
        return areComparable(control,test);
    }

    protected abstract boolean areComparable(Element control, Element test);

    protected boolean bothNullOrEqual(Object ob1, Object ob2) {
        return ob1 == null ? ob2 == null : ob1.equals(ob2);
    }

    protected String getNodeContent(Element element, String nodeName) {
        Node tempNode = element.getElementsByTagName(nodeName).item(0);
        return tempNode == null ? null : tempNode.getTextContent();
    }

    protected boolean checkFieldsCorrespondence(Element control, Element test, String[] fields) {

        String controlField,testField;
        for (String field: fields) {

            controlField = getNodeContent(control,field);
            testField    = getNodeContent(test,field);

            if (!bothNullOrEqual(controlField, testField)) {
                return false;
            }
        }

        return true;

    }
}
