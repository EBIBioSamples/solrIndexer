package uk.ac.ebi.solrIndexer.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.sampletab.loader.Loader;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");
    	/*
    	DataBaseInteraction dbi = null;
    	
    	
    	try {

    		// --- Populate the in-memory DB ---
    		MSI msi1 = null;
    		String path1 = args [0];
    		String msiAcc1 = null;

    		MSI msi2 = null;
    		String path2 = args [1];
    		String msiAcc2 = null;

			try {
				if(msi1 == null) {
					msi1 = loadSampleTab (path1);
					msiAcc1 = msi1.getAcc ();
					if(msi1.getSamples().size() + msi1.getSampleGroups().size() > 0) {
						log.info ("Now persisting data, MSI acc: " + msiAcc1);
						new Persister ().persist (msi1);
					}
				}

				if(msi2 == null) {
					msi2 = loadSampleTab (path2);
					msiAcc2 = msi2.getAcc ();
					if(msi2.getSamples().size() + msi2.getSampleGroups().size() > 0) {
						log.info ("Now persisting data, MSI acc: " + msiAcc2);
						new Persister ().persist (msi2);
					}
				}

			} catch ( RuntimeException ex ) {
				msi1 = null;
				msi2 = null;
				ex.printStackTrace();
			}
    		// --- Populate the in-memory DB ---
    		
    		
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
        	BioSample bs = samples.get(0);
        	
    		System.out.println("ID: " + bs.getId().toString());
    		System.out.println("ACC: " + bs.getAcc());
    		//System.out.println("Release Date: " + bs.getReleaseDate() != null ? bs.getReleaseDate().toString() : "--");

        	for(ExperimentalPropertyValue epv : dbi.fetchExperimentalPropertyValues(bs)) {
        		System.out.println("TermText: " + epv.getTermText());
        		System.out.println("Type: " + epv.getType());
        		System.out.println("Unit: " + epv.getUnit());
        	}

    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
        	if (dbi.getEntityManager()!= null && dbi.getEntityManager().isOpen()) {
        		dbi.getEntityManager().close();
    		}
    	}
    	*/
    }

	private static MSI loadSampleTab (String path) throws ParseException {
    	Loader loader = new Loader();
    	MSI msi = loader.fromSampleData (path);
		return msi;
    }

}
