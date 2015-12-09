package uk.ac.ebi.solrIndexer.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesManager {
	private static Logger log = LoggerFactory.getLogger (PropertiesManager.class.getName());

	/**
	 * Gets the solr.path property value from
	 * the ./solrIndexer.properties
	 * @return String corePath
	 * @throws IOException
	 */
	public static String getSolrCorePath() {

		Properties properties = new Properties();
		FileInputStream file;

		String filePath = "./solrIndexer.properties";
		try {
			file = new FileInputStream(filePath);
			properties.load(file);
			file.close();

		} catch (FileNotFoundException e) {
			log.error("Properties file not found: ", e);
			System.exit(0);
		} catch (IOException e) {
			log.error("Error reading properties file: ", e);
			System.exit(0);
		}

		return properties.getProperty("solrIndexer.corePath");
	}
}
