package uk.ac.ebi.solrIndexer.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import uk.ac.ebi.fg.myequivalents.managers.impl.db.DbManagerFactory;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingSearchResult;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.ManagerFactory;
import uk.ac.ebi.fg.myequivalents.model.Entity;

@Component
public class MyEquivalenceManager {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private ManagerFactory managerFactory = null;

	private MyEquivalenceManager() {
	}
	
	private MyEquivalenceManager(ManagerFactory managerFactory) {
		this.managerFactory = managerFactory;
	}	

	@PostConstruct
	public void doSetup() throws IOException {
		if (managerFactory == null) {
			Properties properties = new Properties();
			InputStream is = null;
			try {
				is = this.getClass().getResourceAsStream("/myeq.properties");
				if (is == null) {
					throw new IOException("Unable to find myeq.properties");
				}
				properties.load(is);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// do nothing
					}
				}
			}
			managerFactory = new DbManagerFactory(properties);
		}
	}

	public ManagerFactory getManagerFactory() {
		return managerFactory;
	}

	public void setManagerFactory(ManagerFactory managerFactory) {
		this.managerFactory = managerFactory;
	}

	public Set<Entity> getGroupExternalEquivalences(String groupAccession, EntityMappingManager entityMappingManager) {
		Set<Entity> otherEquivalences = new HashSet<>();
		
		entityMappingManager = getManagerFactory().newEntityMappingManager();

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

		return otherEquivalences;
	}

	public Set<Entity> getSampleExternalEquivalences(String sampleAccession, EntityMappingManager entityMappingManager) {
		Set<Entity> otherEquivalences = new HashSet<>();

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

		return otherEquivalences;
	}

}
