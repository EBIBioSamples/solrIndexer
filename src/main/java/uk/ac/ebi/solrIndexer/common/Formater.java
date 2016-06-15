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

	/**
	 * Format Date variables to Solr Date format
	 * @param Date date
	 * @return "2015-09-03T12:00:00.00Z"
	 */
	public static String formatDateToSolr(Date date) throws IllegalArgumentException{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		if (date == null)
			throw new IllegalArgumentException("Null date provided to formatter");
		return df.format(date);
	}

	/**
	 * Formats Characteristics Names to Field Names Accepted by Solr
	 * @param  strimg
	 * @return String trimmed and with spaces replaced by '_' in the middle
	 */
	public static String formatCharacteristicFieldNameToSolr(String string) {
		return string.trim().toLowerCase().replace(' ', '_') + "_crt";
	}

	/**
	 * Returns the ontology uri associated with the ontology term when present
	 * in an OntologyEntry
	 * 
	 * If no url can be found, returns an empty optional object to caller.
	 * 
	 * @param onto
	 * @param acc 
	 * @return
	 */
	public static Optional<URI> getOntologyTermURI (OntologyEntry onto) {
		String acc = null;
		
		try {
			int number = Integer.parseInt(onto.getAcc());
			//its a complete number, assume NCBI taxonomy
			//TODO
			return Optional.empty();
		} catch (NumberFormatException e) {
			//do nothing, carry on 
		}
		
		//see if it is already a valid URI
		URI uri = null;
		try {
			uri = new URI(onto.getAcc());
		} catch (URISyntaxException e) {
			log.warn("OntologyEntry has non-URI acession : "+onto);
			uri = null;
		}
		if (uri != null) {
			return Optional.of(uri);
		}
		return Optional.empty();
		
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
