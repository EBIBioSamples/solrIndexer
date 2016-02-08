package uk.ac.ebi.service.nodematchers;

import org.w3c.dom.Element;

/**
 * Created by lucacherubin on 26/01/2016.
 */
public class PublicationNodeMatcher extends BaseElementSelector {
    @Override
    protected boolean areComparable(Element control, Element test) {

        String[] fields = {"PubMedID","DOI"};
        return checkFieldsCorrespondence(control,test,fields);



    }
}
