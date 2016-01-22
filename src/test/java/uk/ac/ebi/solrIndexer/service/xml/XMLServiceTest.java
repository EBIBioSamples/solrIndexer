package uk.ac.ebi.solrIndexer.service.xml;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.DataBaseManager;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertTrue;


public class XMLServiceTest {

    private static Logger log = LoggerFactory.getLogger(XMLServiceTest.class.getName());


    @Test
    public void testRandom() throws Exception{

        String accession             = "SAMEG14161";
        BioSampleGroupXMLService xmlService = new BioSampleGroupXMLService();

        BioSampleGroup group = DataBaseManager.fetchGroup(accession);

        Document testDocument      = xmlService.getXMLDocument(group);
        Document referenceDocument = getGroupReferenceXML(accession);


        assertTrue(render(testDocument).equals(render(referenceDocument)));

    }

    private Document getGroupReferenceXML(String accession) {
        String url = "https://www.ebi.ac.uk/biosamples/xml/group/" + accession;
        Document xml = new Document();

        try {
            xml = buildDocumentFromUrl(url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return xml;
    }

    private Document getSampleReferenceXML(String accession) {

        String url = "https://www.ebi.ac.uk/biosamples/xml/group/" + accession;
        Document xml = new Document();

        try {
            xml = buildDocumentFromUrl(url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return xml;
    }

    private Document buildDocumentFromUrl(String url) throws Exception{
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        HttpURLConnection connection = (HttpURLConnection) uc;

        InputStream in = connection.getInputStream();
        SAXBuilder parser = new SAXBuilder();
        Document xmlDocument = parser.build(in);
        in.close();

        return xmlDocument;
    }

    private String render(Document doc) {
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        return xmlOutput.outputString(doc);
    }

}