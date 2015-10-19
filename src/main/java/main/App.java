package main;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class App {
	private static Logger log = LogManager.getLogger (App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");

    	DataBaseInteraction dbi = null;
    	try {
    		dbi = new DataBaseInteraction();
        	List<BioSampleGroup> groups = dbi.fetchGroups();
        	//debugging purposes
        	int i = 0;
        	for (BioSampleGroup bsg : groups) {
        		System.out.println(bsg.getReleaseDate());
        		if (++i == 10) break;
        	}

        	List<BioSample> samples = dbi.fetchSamples();
        	//debugging purposes
        	int j = 0;
        	for (BioSample bs : samples) {
        		System.out.println(bs.getAcc());
        		if (++j == 10) break;
        	}

    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
        	if (dbi.getEntityManager()!= null && dbi.getEntityManager().isOpen()) {
        		dbi.getEntityManager().close();
    		}
    	}
    }

}
