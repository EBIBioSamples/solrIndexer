package uk.ac.ebi.solrIndexer.mainTest;

import static org.junit.Assert.fail;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class FetchOTFromSubmissionTest {

	//@Test
	public void getOntologiesFromRandomSamplesTest() {
		System.out.println("////////////////////////////// Ontology Terms Test START //////////////////////////////");

		try {
			List<BioSample> samples = getRandomSamples(1052, 1);
			for (BioSample sample : samples) {
				System.out.println("--------------------------");
				System.out.println("Sample ACC: " + sample.getAcc());
				for (ExperimentalPropertyValue<?> epv : sample.getPropertyValues()) {
					System.out.println("-" + epv.getType().getTermText()
							+ " [" + epv.getType().getSingleOntologyTerm() + "]"
							+ " : " + epv.getTermText()
							+ " [" + epv.getSingleOntologyTerm() + "]");

					OntologyEntry onto = epv.getSingleOntologyTerm();
					if (onto != null) {
						System.out.println("  + Formater: " + Formater.formatOntologyTermURL(epv.getSingleOntologyTerm()));
					}
					
				}
			}

		} catch (Exception e) {
			fail("Ups, somethin went wrong...");
		} finally {
			System.out.println("//////////////////////////////  Ontology Terms Test END  //////////////////////////////");
		}
	}

	@Test
	public void getOntologiesFromGroupTest() {
		System.out.println("////////////////////////////// Ontology Terms Test START //////////////////////////////");
		try {
			BioSampleGroup group = getGroupByAccession("SAMEG298568");
			System.out.println("Group: " + group.getAcc());

			for (ExperimentalPropertyValue<?> epv : group.getPropertyValues()) {
				System.out.println(" - " + epv.getType().getTermText()
						+ " : " + epv.getTermText()
						+ " [" + epv.getSingleOntologyTerm() + "]");

				OntologyEntry onto = epv.getSingleOntologyTerm();
				if (onto != null) {
					System.out.println("  + Source: " + onto.getSource());
					System.out.println("  + Formater: " + Formater.formatOntologyTermURL(epv.getSingleOntologyTerm()));
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Ups, somethin went wrong...");
		} finally {
			System.out.println("//////////////////////////////  Ontology Terms Test END  //////////////////////////////");
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

	private BioSampleGroup getGroupByAccession(String acc) {
		DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
		Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);

		criteriaQuery.select(root).where(criteriaBuilder.equal(root.get("acc"), acc));
		BioSampleGroup group = connection.getEntityManager().createQuery(criteriaQuery).getSingleResult();
		return group;
	}
}
