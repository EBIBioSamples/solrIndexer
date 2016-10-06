package uk.ac.ebi.biosamples.solrindexer;

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
	 * @return Space removed and words cammelcased
	 */
	public static String formatCharacteristicFieldNameToSolr(String string) {
		StringBuilder builder = new StringBuilder();
		
		boolean spaced = true;
		
		for (int i=0;i < string.length(); i++) {
			//process as String objects to handle UTF-8
			String c = string.substring(i,i+1);
			if (c.trim().isEmpty()) {
				spaced = true;
			} else if (c.matches("[^a-zA-Z0-9]")) {
				//non-character
				spaced = true;
			} else if (spaced) {
				spaced = false;
				builder.append(c.toUpperCase());
			} else {
				builder.append(c.toLowerCase());
			}
		}
		return builder.toString();
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
			URI uri = new URI("http://purl.obolibrary.org/obo/NCBITaxon_"+number);
			//TODO validate this
			return Optional.of(uri);
		} catch (NumberFormatException e) {
			//do nothing, carry on 
		} catch (URISyntaxException e) {
			//invalid uri, carry on
		}
		
		//see if it is already a valid URI
		URI uri = null;
		try {
			//technically, a string like BTO:0001182 or EFO_0000462 is a valid URI...
			if (onto.getAcc().matches("^[A-Z]+[:_][0-9]+$")) {
				//so explicitly remove it
				uri = null;
				log.warn("OntologyEntry has non-URI acession : "+onto);
				//TODO try to resolve these against OLS
			} else {
				uri = new URI(onto.getAcc());
			}
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
