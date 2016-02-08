package uk.ac.ebi.solrIndexer.mainTest;

import static org.junit.Assert.fail;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class FetchOTFromAnnotatorTest {

	@Test
	@SuppressWarnings("rawtypes")
	public void AnnotatorTest() {
		System.out.println("////////////////////////////// Annotator Test START //////////////////////////////");

		DataBaseConnection connection = null;
		try {
			connection = new DataBaseConnection();
			EntityManager manager = connection.getEntityManager();
			AnnotatorAccessor ancestor = new AnnotatorAccessor(manager);

			List<BioSample> samples = getRandomSamples(manager, 2000052, 2);
			for (BioSample sample : samples) {
				System.out.println("---- Sample ACC: " + sample.getAcc());

				for (ExperimentalPropertyValue epv : sample.getPropertyValues()) {
					System.out.println("Term: " + epv.getTermText());

					ancestor.getAllOntologyEntries(epv).forEach(oe -> System.out.println("       ACC: " + oe.getAcc() + " Source: " + oe.getSource()));
				}

			}

		} catch (Exception e) {
			fail("Ups, somethin went wrong...");

		} finally {
			connection.closeDataBaseConnection();
			System.out.println("////////////////////////////// Annotator Test END //////////////////////////////");
		}

	}

	private List<BioSample> getRandomSamples(EntityManager manager, int offset, int max) {
		CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
		CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
		Root<BioSample> root = criteriaQuery.from(BioSample.class);
		criteriaQuery.select(root);
		List<BioSample> result = manager.createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();

		return result;
	}
}
