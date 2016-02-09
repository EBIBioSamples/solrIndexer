package uk.ac.ebi.solrIndexer.main;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

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
		connection.closeDataBaseConnection();
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
            return samples.get(0);
        } else {
            throw new NoResultException("No Biosamples with Accession: " + acc + " was found in the database");
        }
    }

}
