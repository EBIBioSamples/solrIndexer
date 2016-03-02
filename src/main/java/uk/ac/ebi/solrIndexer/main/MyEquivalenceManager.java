package uk.ac.ebi.solrIndexer.main;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingSearchResult;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.ManagerFactory;
import uk.ac.ebi.fg.myequivalents.model.Entity;
import uk.ac.ebi.fg.myequivalents.resources.Resources;

/**
 * Created by lucacherubin on 26/01/2016.
 */
public class MyEquivalenceManager {

    private static Logger log = LoggerFactory.getLogger (MyEquivalenceManager.class.getName());

    private ManagerFactory myEqManagerFactory = null;
    private EntityMappingManager myEqMappingManager = null;
    private static MyEquivalenceManager equivalenceManager = null;


    private MyEquivalenceManager() {
        log.debug("Creating MyEquivalenceManager");
        try {
            myEqManagerFactory = Resources.getInstance().getMyEqManagerFactory();
            myEqMappingManager = myEqManagerFactory.newEntityMappingManager();

        } catch (Exception e) {
            log.error("Error while creating MyEquivalenceManager: ", e);
        }
    }

    private synchronized static EntityMappingManager getEntityManagerMapping() {
        if (equivalenceManager == null) {
            equivalenceManager = new MyEquivalenceManager();
        }
        return equivalenceManager.myEqMappingManager;
    }

    public static Set<Entity> getGroupExternalEquivalences(String groupAccession) {

        Set<Entity> otherEquivalences = new HashSet<>();

        try {
            Collection<EntityMappingSearchResult.Bundle> bundles = getEntityManagerMapping().getMappings(false, "ebi.biosamples.groups:" + groupAccession).getBundles();


            if (!bundles.isEmpty()) {

                Set<Entity> entities = bundles.iterator().next().getEntities();

                for(Entity entity: entities) {

//                        if (!entity.isPublic()) {
//                            continue;
//                        }

                    if (entity.getServiceName().equals("ebi.biosamples.groups")) {

                        String entityAccession = entity.getAccession();

                        if (entityAccession.equals(groupAccession)) {
                            continue;
                        } 
                        //else if (! DataBaseStorage.isGroupPublic(entityAccession)) {
                        //    log.debug("Equivalence with private or non existent group not inserted");
                        //    continue;
                        //}
                    }

                    otherEquivalences.add(entity);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return otherEquivalences;

    }

    public static Set<Entity> getSampleExternalEquivalences(String sampleAccession) {

        Set<Entity> otherEquivalences = new HashSet<>();

        try {
            Collection<EntityMappingSearchResult.Bundle> bundles = getEntityManagerMapping().getMappings(false, "ebi.biosamples.samples:" + sampleAccession).getBundles();

            if (!bundles.isEmpty()) {

                otherEquivalences = bundles.iterator().next().getEntities().stream()
                        .filter(entity -> {

//                        if (!entity.isPublic()) {
//                            return false;
//                        }
                        if (entity.getServiceName().equals("ebi.biosamples.samples")) {

                            String entityAccession = entity.getAccession();

                            if (entityAccession.equals(sampleAccession)) {
                                return false;
                            } 
                            //TODO check if accession is public
                            //else if (!DataBaseStorage.isSamplePublic(entityAccession)){
                            //    log.debug("Equivalence with private or not existent sample not inserted");
                            //    return false;
                            //}
                        }

                        return true;


                }).collect(Collectors.toSet());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return otherEquivalences;
    }


}
