package uk.ac.ebi.solrIndexer.main;


import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataBaseManager {
	private static Logger log = LoggerFactory.getLogger (DataBaseManager.class.getName());

	public static List<BioSampleGroup> getAllIterableGroups (int offset, int max) {
		log.debug("Fetching Groups . . .");

		DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
		Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);

		criteriaQuery.select(root);
		List<BioSampleGroup> result = connection.getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();

		// Force eager initialization
		result.forEach(bsg -> bsg.getMSIs().forEach(m -> Hibernate.initialize( m.getDatabaseRecordRefs() )));
		result.forEach(bsg -> bsg.getPropertyValues().forEach(epv -> Hibernate.initialize( epv.getTermText() )));
		result.forEach(bsg -> bsg.getPropertyValues().forEach(epv -> Hibernate.initialize( epv.getType() )));
		result.forEach(bsg -> bsg.getPropertyValues().forEach(epv -> epv.getOntologyTerms().forEach(oe -> Hibernate.initialize( oe.getAcc() ))));

		connection.closeDataBaseConnection();
		return result;
	}

	public static List<BioSample> getAllIterableSamples (int offset, int max) {
		log.debug("Fetching Samples . . .");

		DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
		CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
		Root<BioSample> root = criteriaQuery.from(BioSample.class);

		criteriaQuery.select(root);
		List<BioSample> result = connection.getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();

		// Force eager initialization
		result.forEach(bs -> bs.getMSIs().forEach(m -> Hibernate.initialize(m.getDatabaseRecordRefs())));
		result.forEach(bs -> bs.getPropertyValues().forEach(epv -> Hibernate.initialize(epv.getTermText())));
		result.forEach(bs -> bs.getPropertyValues().forEach(epv -> Hibernate.initialize( epv.getType().getTermText() )));
		result.forEach(bs -> bs.getPropertyValues().forEach(epv -> epv.getOntologyTerms().forEach(oe -> Hibernate.initialize( oe.getAcc() ))));

		connection.closeDataBaseConnection();
		return result;
	}

    public static List<BioSampleGroup> getAllIterableGroups (DataBaseConnection connection, int offset, int max) {
        log.debug("Fetching Groups . . .");

        CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
        Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);

        criteriaQuery.select(root);
        List<BioSampleGroup> result = connection.getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();
        return result;
    }

    public static List<BioSample> getAllIterableSamples (DataBaseConnection connection, int offset, int max) {
        log.debug("Fetching Samples . . .");

        CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
        Root<BioSample> root = criteriaQuery.from(BioSample.class);

        criteriaQuery.select(root);
        List<BioSample> result = connection.getEntityManager().createQuery(criteriaQuery).setFirstResult(offset).setMaxResults(max).getResultList();
        return result;
    }

    /**
     * Queries the Biosamples DB retrieving the sample with the accession acc.
     * @param acc Accession
     * @return BioSample
     */
    public static BioSampleGroup fetchGroup (String acc) {
        log.debug("Fetching Group with accession: " + acc);

        DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BioSampleGroup> criteriaQuery = criteriaBuilder.createQuery(BioSampleGroup.class);
        Root<BioSampleGroup> root = criteriaQuery.from(BioSampleGroup.class);

        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("acc"), acc));
        TypedQuery<BioSampleGroup> query = connection.getEntityManager().createQuery(criteriaQuery);
        BioSampleGroup group = query.getSingleResult();
        connection.closeDataBaseConnection();
        return group;
    }

	/**
     * Queries the Biosamples DB retrieving the sample with the accession acc.
     * @param acc Accession
     * @return BioSample
     */
    public static BioSample fetchSample (String acc) {
        log.debug("Fetching Sample with accession: " + acc);

        DataBaseConnection connection = new DataBaseConnection();
		CriteriaBuilder criteriaBuilder = connection.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BioSample> criteriaQuery = criteriaBuilder.createQuery(BioSample.class);
        Root<BioSample> root = criteriaQuery.from(BioSample.class);

        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("acc"), acc));
        TypedQuery<BioSample> query = connection.getEntityManager().createQuery(criteriaQuery);
        List<BioSample> samples =  query.getResultList();
        if ( !samples.isEmpty() ) {
        	connection.closeDataBaseConnection();
            return samples.get(0);
        } else {
        	connection.closeDataBaseConnection();
            throw new NoResultException("No Biosamples with Accession: " + acc + " was found in the database");
        }
    }

    public static Set<String> getPublicSamplesAccessionSet() {
        log.debug("Fetching Public Samples Accessions . . .");

        Set<String> publicAccessions = new HashSet<>();
        try {
            JDBCConnection connection = new JDBCConnection();
            Statement stmt = connection.getConnection().createStatement();
            stmt.setFetchSize(1000);
            String query = "SELECT SA,COUNT(SA) AS NS\n" +
                    "  FROM (\n" +
                    "    SELECT b.ACC AS SA, b.PUBLIC_FLAG AS PF, b.RELEASE_DATE AS SRD, m.RELEASE_DATE AS MRD \n" +
                    "    FROM BIO_PRODUCT b LEFT JOIN MSI_SAMPLE ms ON b.ID = ms.SAMPLE_ID LEFT JOIN MSI m ON m.ID = ms.MSI_ID \n" +
                    "    WHERE (\n" +
                    "      (b.PUBLIC_FLAG IS NULL OR b.PUBLIC_FLAG = 1) AND \n" +
                    "      (b.RELEASE_DATE IS NULL OR b.RELEASE_DATE < CURRENT_DATE)\n" +
                    "    )\n" +
                    "  ) \n" +
                    "  WHERE\n" +
                    "    (SRD < CURRENT_DATE) OR\n" +
                    "    (SRD IS NULL AND MRD < CURRENT_DATE)\n" +
                    "  GROUP BY SA";
            ResultSet rs = stmt.executeQuery(query);


            while(rs.next()) {
                String acc = rs.getString(1);
                if (rs.getInt(2) > 1) {
                    log.warn(String.format("Multiple MSIs for %s - Sample not public",acc));
                    continue;
                }
                publicAccessions.add(acc);
            }


            return publicAccessions;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return publicAccessions;
    }

    public static Set<String> getPublicGroupsAccessionSet() {

        log.debug("Fetching Public Groups Accessions . . .");

        Set<String> publicAccessions = new HashSet<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = new JDBCConnection().getConnection();
            stmt = conn.createStatement();
            stmt.setFetchSize(1000);
            String query = "  SELECT GA,COUNT(GA) AS NS\n" +
                    "  FROM (\n" +
                    "    SELECT bsg.ACC AS GA, bsg.PUBLIC_FLAG AS PF, bsg.RELEASE_DATE AS GRD, m.RELEASE_DATE AS MRD \n" +
                    "    FROM BIO_SMP_GRP bsg LEFT JOIN MSI_SAMPLE_GROUP msg ON bsg.ID = msg.GROUP_ID LEFT JOIN MSI m ON msg.MSI_ID = m.ID \n" +
                    "    WHERE (\n" +
                    "      (bsg.PUBLIC_FLAG IS NULL OR bsg.PUBLIC_FLAG = 1) AND \n" +
                    "      (bsg.RELEASE_DATE IS NULL OR bsg.RELEASE_DATE < CURRENT_DATE)\n" +
                    "    )\n" +
                    "  ) \n" +
                    "  WHERE\n" +
                    "    (GRD < CURRENT_DATE) OR\n" +
                    "    (GRD IS NULL AND MRD < CURRENT_DATE)\n" +
                    "  GROUP BY GA";
            rs = stmt.executeQuery(query);

            while(rs.next()) {
                String acc = rs.getString(1);
                if (rs.getInt(2) > 1) {
                    log.warn(String.format("Multiple MSIs for %s - Group not public",acc));
                    continue;
                }
                publicAccessions.add(acc);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {};
            try { if (stmt != null) stmt.close(); } catch (Exception e) {};
            try { if (conn != null) conn.close(); } catch (Exception e) {};
        }

        return publicAccessions;

    }


	@SuppressWarnings("rawtypes")
	public static List<String> getOntologyFromAnnotator(ExperimentalPropertyValue epv) {
		DataBaseConnection connection = new DataBaseConnection();
		EntityManager manager = connection.getEntityManager();
		AnnotatorAccessor annotator = new AnnotatorAccessor(manager);

		List<String> urls = new ArrayList<String>();
		List<OntologyEntry> ontologies = annotator.getAllOntologyEntries(epv);
		ontologies.forEach(oe -> urls.add(oe.getAcc()));

		connection.closeDataBaseConnection();
		return urls;
	}
}
