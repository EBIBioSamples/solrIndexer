package uk.ac.ebi.solrIndexer.service.xml;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.DataBaseManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lucacherubin on 07/12/2015.
 */
public class XMLServiceTester {

    private List<String> accessions;
    private static final HashMap<Class,XMLService> xmlServices;
    private static Logger log = LoggerFactory.getLogger(XMLServiceTest.class.getName());

    static {

        xmlServices = new HashMap<>();
        xmlServices.put(BioSampleGroup.class, new BioSampleGroupXMLService());
        xmlServices.put(BioSample.class, new BioSampleXMLService());

    }

    public static void main(String[] args) {

        String filePath = args[0];
        BufferedReader br = null;
        ArrayList<String> accessions = new ArrayList<>();

        try {

            String currentLine;

            br = new BufferedReader( new FileReader( filePath ) );
            while( ( currentLine = br.readLine() ) != null ) {
                accessions.add(currentLine);
            }
        } catch ( IOException e ) {

            e.printStackTrace();

        } finally {

            try {

                if ( br != null)  {
                    br.close();
                }

            } catch ( IOException ex ) {

                ex.printStackTrace();

            }
        }

        if ( !accessions.isEmpty() ) {
            XMLServiceTester tester = new XMLServiceTester(accessions);
            tester.test();
            System.exit( 0 );
        } else {
            System.exit( -1 );
        }


    }

    public XMLServiceTester(List<String> accessionList) {
        this.accessions = accessionList;
    }

    private void test() {

        if ( !this.accessions.isEmpty() ) {

            XMLService xmlService;
            Document testDocument;
            Document refDocument;

            for (String accession : this.accessions) {

                if ( accession.matches("SAMEG\\d+") ) {

                    xmlService = XMLServiceTester.xmlServices.get(BioSampleGroup.class);

                    BioSampleGroup group = DataBaseManager.fetchGroup(accession);

                    testDocument = xmlService.getXMLDocument(group);
                    refDocument = getGroupReferenceXML(accession);

                } else if ( accession.matches("SAMEA\\d+") ) {

                    xmlService = XMLServiceTester.xmlServices.get(BioSample.class);

                    BioSample sample = DataBaseManager.fetchSample(accession);

                    testDocument = xmlService.getXMLDocument(sample);
                    refDocument = getGroupReferenceXML(accession);

                } else {
                    continue;
                }

                log.info( render( testDocument ) );
                log.info( render( refDocument ) );


            }
        }

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

    private Document buildDocumentFromUrl(String url) throws Exception {
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
