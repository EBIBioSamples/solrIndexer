package uk.ac.ebi.solrIndexer.main;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.service.xml.BioSampleGroupXMLService;
import uk.ac.ebi.solrIndexer.service.xml.BioSampleXMLService;
import uk.ac.ebi.solrIndexer.service.xml.XMLService;

/**
 * Created by lucacherubin on 09/02/2016.
 */
public class XMLManager {

    private static final XMLService<BioSample> sampleXmlService = new BioSampleXMLService();
    private static final XMLService<BioSampleGroup> groupXmlService = new BioSampleGroupXMLService();

    public static String getXMLString(BioSample sample) {
        return sampleXmlService.getXMLString(sample);
    }

    public static String getXMLString(BioSampleGroup group) {
        return groupXmlService.getXMLString(group);
    }


}
