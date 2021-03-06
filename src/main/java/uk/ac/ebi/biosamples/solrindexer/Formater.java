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
	 * @return Space removed and words cammelCased
	 */
	public static String formatCharacteristicFieldNameToSolr(String string) {
		StringBuilder builder = new StringBuilder();
		
		boolean spaced = false;
		
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
		
		builder.append("_crt");
		
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
			if (onto.getAcc().matches("^EFO[:_][0-9]+$")) {
				uri = new URI("http://www.ebi.ac.uk/efo/"+onto.getAcc());
			} else if (onto.getAcc().matches("^LBO[:_][0-9]+$")
					|| onto.getAcc().matches("^OBI[:_][0-9]+$")
					|| onto.getAcc().matches("^PO[:_][0-9]+$")
					|| onto.getAcc().matches("^UBERON[:_][0-9]+$")
					|| onto.getAcc().matches("^BTO[:_][0-9]+$")
					|| onto.getAcc().matches("^CL[:_][0-9]+$")
					|| onto.getAcc().matches("^PRODE[:_][0-9]+$")
					|| onto.getAcc().matches("^GO[:_][0-9]+$")
					|| onto.getAcc().matches("^MOD[:_][0-9]+$")
					|| onto.getAcc().matches("^DOID[:_][0-9]+$")
					|| onto.getAcc().matches("^UO[:_][0-9]+$")
					|| onto.getAcc().matches("^LBO[:_][0-9]+$")
					|| onto.getAcc().matches("^PATO[:_][0-9]+$")
					|| onto.getAcc().matches("^PO[:_][0-9]+$")
					|| onto.getAcc().matches("^MS[:_][0-9]+$")
					|| onto.getAcc().matches("^OBI[:_][0-9]+$")
					|| onto.getAcc().matches("^Orphanet[:_][0-9]+$")
					|| onto.getAcc().matches("^MIRO[:_][0-9]+$")
					|| onto.getAcc().matches("^MI[:_][0-9]+$")
					|| onto.getAcc().matches("^WBbt[:_][0-9]+$")
					|| onto.getAcc().matches("^ZFS[:_][0-9]+$")
					|| onto.getAcc().matches("^HP[:_][0-9]+$")
					|| onto.getAcc().matches("^EO[:_][0-9]+$")
					|| onto.getAcc().matches("^MA[:_][0-9]+$")
					|| onto.getAcc().matches("^MP[:_][0-9]+$")
					|| onto.getAcc().matches("^sep[:_][0-9]+$")
					|| onto.getAcc().matches("^CHEBI[:_][0-9]+$")
					|| onto.getAcc().matches("NCBITaxon^[:_][0-9]+$")
					|| onto.getAcc().matches("^FIX[:_][0-9]+$")
					|| onto.getAcc().matches("^IDQ[:_][0-9]+$")
					|| onto.getAcc().matches("^SO[:_][0-9]+$")
					|| onto.getAcc().matches("^TO[:_][0-9]+$")
					|| onto.getAcc().matches("^OGG[:_][0-9]+$")
					|| onto.getAcc().matches("^OMIT[:_][0-9]+$")
					|| onto.getAcc().matches("^ERO[:_][0-9]+$")
					|| onto.getAcc().matches("^ZFA[:_][0-9]+$")
					|| onto.getAcc().matches("^CLO[:_][0-9]+$")) {
				uri = new URI("http://purl.obolibrary.org/obo/"+onto.getAcc());
			} else if (onto.getAcc().matches("^ATOL[:_][0-9]+$")) {
					uri = new URI("http://opendata.inra.fr/ATOL/"+onto.getAcc());
			} else if (onto.getAcc().matches("^[a-zA-Z]+[:_][0-9]+$")) {
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
