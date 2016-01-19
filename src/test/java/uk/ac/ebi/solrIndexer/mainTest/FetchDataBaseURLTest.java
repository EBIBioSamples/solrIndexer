package uk.ac.ebi.solrIndexer.mainTest;

import static org.junit.Assert.fail;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_ACC;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_NAME;
import static uk.ac.ebi.solrIndexer.common.SolrSchemaFields.DB_URL;

import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.solrIndexer.main.DataBaseConnection;

public class FetchDataBaseURLTest {

	@Test
	public void test() {
		System.out.println("////////////////////////////");
		System.out.println("/ Data Base URL Test START /");
		System.out.println("////////////////////////////");

		try {
			List<BioSample> samples = getRandomSamples(1052, 1);
			for (BioSample sample : samples) {
				System.out.println("--------------------------");
				System.out.println("Sample ACC: " + sample.getAcc());

				Set<MSI> msi = sample.getMSIs();
				if (msi.iterator().hasNext()) {

					MSI submission = msi.iterator().next();
					Set<DatabaseRecordRef> db = submission.getDatabaseRecordRefs();
					if (db.iterator().hasNext()) {
						System.out.println("From MSI");
						DatabaseRecordRef dbrr = db.iterator().next();
						System.out.println(DB_ACC + " " + dbrr.getAcc());
						System.out.println(DB_NAME + " " + dbrr.getDbName());
						System.out.println(DB_URL + " " + dbrr.getUrl());
					}
				}

				Set<DatabaseRecordRef> db = sample.getDatabaseRecordRefs();
				if (db.iterator().hasNext()) {
					System.out.println("From Sample");
					DatabaseRecordRef dbrr = db.iterator().next();
					System.out.println(DB_ACC + " " + dbrr.getAcc());
					System.out.println(DB_NAME + " " + dbrr.getDbName());
					System.out.println(DB_URL + " " + dbrr.getUrl());
				}

			}

		} catch (Exception e) {
			fail("Ups, somethin went wrong...");
		} finally {
			System.out.println("////////////////////////////");
			System.out.println("/  Data Base URL Test END  /");
			System.out.println("////////////////////////////");
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
