package uk.ac.ebi.solrIndexer.mainTest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;
import uk.ac.ebi.solrIndexer.main.App;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class DataBaseConnectionTest {
	private static Logger log = LoggerFactory.getLogger (App.class.getName());

	private DataBaseConnection dbc1 = null, dbc2 = null;

	@Test
	public void testUnique() {
		setUp();
		log.info("Checking singletons for equalty");
		Assert.assertEquals(true, dbc1 == dbc2);
	}

	public void setUp() {
		log.info("Getting singleton...");
		dbc1 = DataBaseConnection.getInstance();
		log.info("...got singleton: " + dbc1);

		log.info("Getting singleton...");
		dbc2 = DataBaseConnection.getInstance();
		log.info("...got singleton: " + dbc2);
	}
}
