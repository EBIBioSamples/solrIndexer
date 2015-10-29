package uk.ac.ebi.solrIndexer.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.sampletab.loader.Loader;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");
    	
    	DataBaseConnection dbi = null;

    	try {

    		/* --- Populate the in-memory DB --- 
    		MSI msi1 = null, msi2 = null;
        	try {
        		// SampleTab File 1
        		if(msi1 == null) {
        			msi1 = loadSampleTab (args [0]);
        			if(msi1.getSamples().size() + msi1.getSampleGroups().size() > 0) {
        				new Persister ().persist (msi1);
        			}
        		}
        		// SampleTab File 2
        		if(msi2 == null) {
        			msi2 = loadSampleTab (args [1]);
        			if(msi2.getSamples().size() + msi2.getSampleGroups().size() > 0) {
        				new Persister ().persist (msi2);
        			}
        		}
        		
        	} catch (RuntimeException | ParseException e) {
    			msi1 = null;
    			msi2 = null;
        		e.printStackTrace();
        	}
        	/* --- ------------------------- --- */
    		
    		dbi = DataBaseConnection.getInstance();

    		for (BioSampleGroup bsg : BioSDEntities.fetchGroups()) {
    			System.out.println("Group ACC: " + bsg.getAcc());
    		}

    		for (BioSample bs : BioSDEntities.fetchSamples()) {
    			System.out.println("Sample ACC: " + bs.getAcc());
    			for (ExperimentalPropertyValue epv : BioSDEntities.fetchExperimentalPropertyValues(bs)) {
    				System.out.println(epv.getType().getTermText() + ":: " +epv.getTermText());
    			}
    		}

    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
        	if (dbi.getEntityManager()!= null && dbi.getEntityManager().isOpen()) {
        		dbi.getEntityManager().close();
        		System.exit(0);
    		}
    	}

    }

	private static MSI loadSampleTab (String path) throws ParseException {
    	Loader loader = new Loader();
    	MSI msi = loader.fromSampleData (path);
		return msi;
    }

}
