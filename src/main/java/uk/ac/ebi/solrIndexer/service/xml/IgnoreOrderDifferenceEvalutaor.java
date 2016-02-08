package uk.ac.ebi.solrIndexer.service.xml;

import org.h2.expression.Comparison;

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