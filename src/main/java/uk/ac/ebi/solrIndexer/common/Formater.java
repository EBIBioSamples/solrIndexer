package uk.ac.ebi.solrIndexer.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.core_model.terms.OntologyEntry;

public class Formater {
	private static Logger log = LoggerFactory.getLogger (Formater.class.getName());

	private static final String EFO = "EFO";
	private static final String NCBI = "NCBI Taxonomy";
	private static final String ONTOBEE = "http://purl.obolibrary.org/";
	private static final String BIOONTO = "http://purl.bioontology.org/";
	private static final String ICD10 = "ICD10";
	private static final String MESH = "MeSH";

	/**
	 * Format Date variables to Solr Date format
	 * @param Date date
	 * @return "2015-09-03T12:00:00.00Z"
	 */
	public static String formatDateToSolr(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
		if (date == null) 
			return "0000-00-00T00:00:00.00Z";
		return df.format(date);
	}

	/**
	 * Formats Characteristics Names to Field Names Accepted by Solr
	 * @param  strimg
	 * @return String trimmed and with spaces replaced by '_' in the middle
	 */
	public static String formatCharacteristicFieldNameToSolr(String string) {
		return string.trim().replace(' ', '_') + "_crt";
	}

	/**
	 * Generates the ontology url associated with the ontology term for EFO and NCBI Taxonomy.
	 * 
	 * If no url can be found, returns an empty optional object to caller.
	 * 
	 * @param onto
	 * @param acc 
	 * @return
	 */
	public static Optional<String> formatOntologyTermURL (OntologyEntry onto) {
		String acc = null;
		
		//see if it is already a valid URI
		URI uri = null;
		try {
			uri = new URI(onto.getAcc());
		} catch (URISyntaxException e) {
			uri = null;
		}
		if (uri != null) {
			return Optional.of(uri.toString());
		}
		return Optional.empty();
		
	/*

		if (onto.getSource() != null) {
			acc = onto.getSource().getAcc();
			
			if (EFO.equals(acc) || acc.startsWith(ONTOBEE) || MESH.equals(acc)) {
				return Optional.of(onto.getAcc());
			} else if (NCBI.equals(acc)) {
				return Optional.of(onto.getSource().getUrl() + "?term=" + onto.getAcc());
			} else if (ICD10.equals(acc)) {
				return Optional.of(onto.getSource().getUrl());
			} else {
				log.error("Unknown ontology mapping with source: " + onto.getSource());
				return Optional.empty();
			}
		} else if (onto.getSource() == null && (onto.getAcc().startsWith(ONTOBEE) || onto.getAcc().startsWith(BIOONTO))) {
			return Optional.of(onto.getAcc());
		} else {
			acc = onto.getAcc();
			log.error("Unknown ontology mapping with null source. Ontology accession: " + acc);
			return Optional.empty();
		}
*/
	}

	/**
	 * Returns a String with the date in the format "dd-MM-yyyy"
	 * @param date
	 * @return String
	 */
	public static String formatDateToSqlRange(Date date) {
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		return df.format(date);
	}

	/**
	 * Returns a String time representation of the milliseconds received.
	 * @param long
	 * @return String
	 */
	public static String formatTime(long millis) {
		return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
			    TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
			    TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
	}
}
