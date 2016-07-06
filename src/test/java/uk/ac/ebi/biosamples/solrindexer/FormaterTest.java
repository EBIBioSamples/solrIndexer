package uk.ac.ebi.biosamples.solrindexer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.ac.ebi.biosamples.solrindexer.Formater;

public class FormaterTest {

	@Test
	public void testFormatTime() {
		assertEquals("01:46:39",Formater.formatTime(6399411));
		assertEquals("00:30:00",Formater.formatTime(1800000));
		assertEquals("00:00:05",Formater.formatTime(5000));
	}

	@Test
	public void testFormatOntologyTermURL() {
		//TODO
	}
}
