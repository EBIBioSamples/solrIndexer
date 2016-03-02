package uk.ac.ebi.solrIndexer.main;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingSearchResult;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.ManagerFactory;
import uk.ac.ebi.fg.myequivalents.model.Entity;
import uk.ac.ebi.fg.myequivalents.resources.Resources;

/**
 * Created by lucacherubin on 26/01/2016.
 */
public class MyEquivalenceManager {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private ManagerFactory managerFactory = Resources.getInstance().getMyEqManagerFactory();
	
	private MyEquivalenceManager() {
	}

	public Set<Entity> getGroupExternalEquivalences(String groupAccession) {
		Set<Entity> otherEquivalences = new HashSet<>();
		EntityMappingManager entityMappingManager = null;
		try {
			entityMappingManager = managerFactory.newEntityMappingManager();
			
			Collection<EntityMappingSearchResult.Bundle> bundles = entityMappingManager
					.getMappings(false, "ebi.biosamples.groups:" + groupAccession).getBundles();
	
			if (!bundles.isEmpty()) {
	
				Set<Entity> entities = bundles.iterator().next().getEntities();
	
				for (Entity entity : entities) {
	
					// if (!entity.isPublic()) {
					// continue;
					// }
	
					if (entity.getServiceName().equals("ebi.biosamples.groups")) {
	
						String entityAccession = entity.getAccession();
	
						if (entityAccession.equals(groupAccession)) {
							continue;
						}
						// else if (!
						// DataBaseStorage.isGroupPublic(entityAccession)) {
						// log.debug("Equivalence with private or non existent
						// group not inserted");
						// continue;
						// }
					}
	
					otherEquivalences.add(entity);
				}
	
			}
		} finally {
			if (entityMappingManager != null ){
				entityMappingManager.close();
			}
		}

		return otherEquivalences;
	}

	public Set<Entity> getSampleExternalEquivalences(String sampleAccession) {
		Set<Entity> otherEquivalences = new HashSet<>();
		EntityMappingManager entityMappingManager = null;
		try {
			entityMappingManager = managerFactory.newEntityMappingManager();
			
			Collection<EntityMappingSearchResult.Bundle> bundles = entityMappingManager
					.getMappings(false, "ebi.biosamples.samples:" + sampleAccession).getBundles();
	
			if (!bundles.isEmpty()) {
	
				otherEquivalences = bundles.iterator().next().getEntities().stream().filter(entity -> {
	
					// if (!entity.isPublic()) {
					// return false;
					// }
					if (entity.getServiceName().equals("ebi.biosamples.samples")) {
	
						String entityAccession = entity.getAccession();
	
						if (entityAccession.equals(sampleAccession)) {
							return false;
						}
						// TODO check if accession is public
						// else if
						// (!DataBaseStorage.isSamplePublic(entityAccession)){
						// log.debug("Equivalence with private or not existent
						// sample not inserted");
						// return false;
						// }
					}
	
					return true;
	
				}).collect(Collectors.toSet());
	
			}
		} finally {
			if (entityMappingManager != null ){
				entityMappingManager.close();
			}
		}

		return otherEquivalences;
	}

}
