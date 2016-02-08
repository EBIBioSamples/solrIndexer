package uk.ac.ebi.service.nodematchers;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlunit.diff.ElementSelector;

/**
 * Created by lucacherubin on 21/01/2016.
 */
public class DatabaseNodeMatcher extends BaseElementSelector {

    @Override
    protected boolean areComparable(Element control, Element test) {

        String[] fields = {"Name","ID"};
        return checkFieldsCorrespondence(control,test,fields);

    }
}
