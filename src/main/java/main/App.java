package main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.sampletab.loader.Loader;
import uk.ac.ebi.fg.biosd.sampletab.persistence.Persister;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");

    	DataBaseInteraction dbi = null;
    	
    	
    	try {
    		// --- Populate the in-memory DB ---
    		MSI msi = null;
    		String path = args [0];
    		String msiAcc = null;

			try {
				if(msi == null) {
					msi = loadSampleTab (path);
					msiAcc = msi.getAcc ();
					if(msi.getSamples().size() + msi.getSampleGroups().size() > 0) {
						log.info ("Now persisting data, MSI acc: " + msiAcc);
						new Persister ().persist (msi);
					}
				}

			} catch ( RuntimeException ex ) {
				msi = null;
				ex.printStackTrace();
			}
    		// --- Populate the in-memory DB ---
    		
    		
    		dbi = new DataBaseInteraction();
/*
        	List<BioSampleGroup> groups = dbi.fetchGroups();
        	//debugging purposes
        	int i = 0;
        	for (BioSampleGroup bsg : groups) {
        		System.out.println(bsg.getReleaseDate());
        		if (++i == 10) break;
        	}
*/
        	List<BioSample> samples = dbi.fetchSamples();
        	//debugging purposes
        	int j = 0;
        	for (BioSample bs : samples) {
        		log.info(bs.getAcc());
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

	private static MSI loadSampleTab (String path) throws ParseException {
    	Loader loader = new Loader();
    	MSI msi = loader.fromSampleData (path);
		return msi;
    }

}
