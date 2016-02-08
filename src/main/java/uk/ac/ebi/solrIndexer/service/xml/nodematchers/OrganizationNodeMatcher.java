package uk.ac.ebi.service.nodematchers;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.diff.ElementSelector;

/**
 * Created by lucacherubin on 19/01/2016.
 */
public class OrganizationNodeMatcher extends BaseElementSelector {

    @Override
    protected boolean areComparable(Element control, Element test) {

        String[] fields = {"Name","Address","URI","Email","Role"};
        return checkFieldsCorrespondence(control,test,fields);

    }
}
