package uk.ac.ebi.solrIndexer.mainTest;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.solrIndexer.main.App;
import uk.ac.ebi.solrIndexer.main.DataBaseManager;

public class DataBaseManagerTest {

	private static Logger log = LoggerFactory.getLogger (App.class.getName());

	private DataBaseManager dbc1 = null, dbc2 = null;

	@Test
	public void testUnique() {
		try 
		{
			setUp();
			log.info("Checking singletons for equalty");
			Assert.assertEquals(true, dbc1 == dbc2);
		} 
		catch (Exception e)
		{
    		e.printStackTrace();
    	}
		finally
		{
        	if (dbc1.getEntityManager()!= null && dbc1.getEntityManager().isOpen()) {
        		dbc1.getEntityManager().close();
    		}
    	}
	}

	public void setUp() {
		log.info("Getting singleton...");
		dbc1 = DataBaseManager.getConnection();
		log.info("...got singleton: " + dbc1);

		log.info("Getting singleton...");
		dbc2 = DataBaseManager.getConnection();
		log.info("...got singleton: " + dbc2);
	}

}
