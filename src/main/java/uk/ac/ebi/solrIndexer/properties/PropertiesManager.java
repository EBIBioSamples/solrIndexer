package uk.ac.ebi.solrIndexer.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesManager {
	private static Logger log = LoggerFactory.getLogger (PropertiesManager.class.getName());

	private static PropertiesManager propertiesManager = null;
	private static Properties properties = null;

	private PropertiesManager () {
		properties = new Properties();
		FileInputStream file;

		String filePath = "/Users/fpenim/Desktop/solrIndexer.properties";
		try {
			file = new FileInputStream(filePath);
			properties.load(file);
			file.close();

		} catch (FileNotFoundException e) {
			log.error("Properties file not found: ", e);
			System.exit(0);
		} catch (IOException e) {
			log.error("Error reading propertiesManager file: ", e);
			System.exit(0);
		}
	}

	/**
	 * Returns a pointer to the sigleton properties.
	 * @return properties
	 */
	public synchronized static Properties getProperties() {
		if (propertiesManager == null) {
			propertiesManager = new PropertiesManager();
		}
		return properties;
	}

	/**
	 * Fetches the value of property 'solrIndexer.corePath'
	 * @return String
	 */
	public static String getSolrCorePath() {
		return getProperties().getProperty("solrIndexer.corePath");
	}
}
