package uk.ac.ebi.solrIndexer.commonTest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.ac.ebi.solrIndexer.common.Formater;

public class FormaterTest {

	@Test
	public void testFormatTime() {
		assertEquals("01:46:39",Formater.formatTime(6399411));
		assertEquals("00:30:00",Formater.formatTime(1800000));
		assertEquals("00:00:05",Formater.formatTime(5000));
	}

}
