package uk.ac.ebi.solrIndexer.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Formater {

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
	 * Returns a String with the date in the format "dd-MM-yyyy"
	 * @param date
	 * @return String
	 */
	public static String formatDateToSqlRange(Date date) {
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		return df.format(date);
	}
}
