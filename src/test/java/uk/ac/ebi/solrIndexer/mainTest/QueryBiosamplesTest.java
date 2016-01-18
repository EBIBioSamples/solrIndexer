package uk.ac.ebi.solrIndexer.mainTest;

import static org.junit.Assert.fail;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class QueryBiosamplesTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		try {
			List<BioSample> samples = getRandomSamples(1000, 1001);
			for (BioSample sample : samples) {
				System.out.println("--------------------------");
				for (ExperimentalPropertyValue epv : sample.getPropertyValues()) {
					System.out.println(Formater.formatCharacteristicFieldNameToSolr(epv.getType().getTermText()) + " : " + epv.getTermText());
				}
			}
		} catch (Exception e) {
			fail("Ups, somethin went wrong...");
		}
	}

	private List<BioSample> getRandomSamples(int offset, int max) {
		DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
		Root<BioSample> root = criteriaQuery.from(BioSample.class);
		
		criteriaQuery.select(root);
		List<BioSample> result = connection.getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();
		connection.closeDataBaseConnection();
		return result;
	}
}
