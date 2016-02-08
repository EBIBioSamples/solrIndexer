package uk.ac.ebi.database;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingSearchResult;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.ManagerFactory;
import uk.ac.ebi.fg.myequivalents.model.Entity;
import uk.ac.ebi.fg.myequivalents.resources.Resources;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
                        if (entity.getAccession().equals(groupAccession)) {
                            continue;
                        } else {
                            try {
                                BioSampleGroup eqGroup = DataBaseManager.fetchGroup(entity.getAccession());
                                if (!eqGroup.isPublic()) {
                                    continue;
                                }
                            } catch (NoResultException e) {
                                log.error("Equivalence with not existent group not inserted");
                                continue;
                            }
                        }
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
                            if (entity.getAccession().equals(sampleAccession)) {
                                return false;
                            } else {
                                try {
                                    BioSample eqSample = DataBaseManager.fetchSample(entity.getAccession());
                                    if (!eqSample.isPublic()) {
                                        return false;
                                    }
                                } catch (NoResultException e) {
                                    log.error("Equivalence with not existent group not inserted");
                                    return false;
                                }
                            }
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
