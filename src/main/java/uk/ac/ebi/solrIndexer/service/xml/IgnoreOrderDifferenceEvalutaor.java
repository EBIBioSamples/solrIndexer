package uk.ac.ebi.service;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DifferenceEvaluator;

/**
 * Created by lucacherubin on 25/01/2016.
 */
public class IgnoreOrderDifferenceEvalutaor implements DifferenceEvaluator {


    public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.DIFFERENT ) {

            if ( comparison.getType() == ComparisonType.CHILD_NODELIST_SEQUENCE ) {
                return ComparisonResult.EQUAL;
            }

            // Unable to understand why this is different

            /*
            else if ( comparison.getControlDetails().getValue().equals( "{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}BioSample" ) ){
                return ComparisonResult.EQUAL;
            }
            */

        }



        return outcome;
    }
}