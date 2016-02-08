package uk.ac.ebi.service.nodematchers;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.diff.ElementSelector;

/**
 * Created by lucacherubin on 21/01/2016.
 */
public class PersonNodeMatcher extends BaseElementSelector {

    @Override
    protected boolean areComparable(Element control, Element test) {

        String[] fields = {"FirstName","LastName","Email","MidInitial","Role"};
        return checkFieldsCorrespondence(control,test,fields);


    }
}
