package uk.ac.ebi.solrIndexer.main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.common.PropertiesManager;

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

        int offset = 0;
        List<BioSample> samples;
        Set<String> publicAccessions = new HashSet<>();
        while ((samples = getAllIterableSamples(offset, PropertiesManager.getGroupsFetchStep())).size() > 0) {
            List<String> accessions = samples.stream()
                    .filter(bioSample -> {
                        try {
                            return bioSample.isPublic();
                        } catch( IllegalStateException e) {
//                            e.printStackTrace();
//                            log.debug(String.format("Sample %s with multiple MSI found, skipped from public accession collection",bioSample.getAcc()));
                        }
                        return false;
                    }).map(bioSample -> {
                        return bioSample.getAcc();
                    }).collect(Collectors.toList());

            publicAccessions.addAll(accessions);

            offset += samples.size();
        }

        return publicAccessions;
    }

    public static Set<String> getPublicGroupsAccessionSet() {

        log.debug("Fetching Public Groups Accessions . . .");

        int offset = 0;
        List<BioSampleGroup> groups;
        Set<String> publicAccessions = new HashSet<>();
        while ((groups = getAllIterableGroups(offset, PropertiesManager.getGroupsFetchStep())).size() > 0) {
            publicAccessions.addAll(groups.stream().filter(group-> {
                try {
                    return group.isPublic();
                } catch (IllegalStateException e) {
                    return false;
                }
            }).map(BioSampleGroup::getAcc).collect(Collectors.toList()));

            offset += groups.size();
        }

        return publicAccessions;
    }


}
