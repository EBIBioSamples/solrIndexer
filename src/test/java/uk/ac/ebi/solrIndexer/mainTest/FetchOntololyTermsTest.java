package uk.ac.ebi.solrIndexer.mainTest;

import static org.junit.Assert.fail;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class FetchOntololyTermsTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		System.out.println("/////////////////////////////");
		System.out.println("/ Ontology Terms Test START /");
		System.out.println("/////////////////////////////");

		try {
			List<BioSample> samples = getRandomSamples(1052, 1);
			for (BioSample sample : samples) {
				System.out.println("--------------------------");
				System.out.println("Sample ACC: " + sample.getAcc());
				for (ExperimentalPropertyValue epv : sample.getPropertyValues()) {
					System.out.println("-" + epv.getType().getTermText()
							+ " [" + epv.getType().getSingleOntologyTerm() + "]"
							+ " : " + epv.getTermText()
							+ " [" + epv.getSingleOntologyTerm() + "]");

					if (epv.getSingleOntologyTerm() != null) {

						switch (epv.getSingleOntologyTerm().getSource().getAcc()) {
							case "EFO":           System.out.println("   EFO");
												  System.out.println("   " + epv.getSingleOntologyTerm().getAcc());
								                  break;
							case "NCBI Taxonomy": System.out.println("   NCBI Taxonomy");
												  System.out.println("   " + epv.getSingleOntologyTerm().getSource().getUrl() + "?term=" + epv.getSingleOntologyTerm().getAcc());
								                  break;
							default:              System.out.println("   None of the above.");
								                  break;
						}
					}
				}
			}
		} catch (Exception e) {
			fail("Ups, somethin went wrong...");
		} finally {
			System.out.println("/////////////////////////////");
			System.out.println("/  Ontology Terms Test END  /");
			System.out.println("/////////////////////////////");
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
