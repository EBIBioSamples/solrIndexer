package uk.ac.ebi.biosamples.solrindexer.service;

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

	public Set<Entity> getGroupExternalEquivalences(String groupAccession,
			EntityMappingManager entityMappingManager) {
		return getExternalEquivalences("ebi.biosamples.groups", groupAccession, entityMappingManager);
	}

	public Set<Entity> getSampleExternalEquivalences(String sampleAccession,
			EntityMappingManager entityMappingManager) {
		return getExternalEquivalences("ebi.biosamples.samples", sampleAccession, entityMappingManager);
	}

	private Set<Entity> getExternalEquivalences(String serviceName, String accession,
												EntityMappingManager entityMappingManager) {
		Set<Entity> otherEquivalences = new HashSet<>();

		Collection<EntityMappingSearchResult.Bundle> bundles = entityMappingManager
				.getMappings(false, serviceName + ":" + accession).getBundles();

		if (!bundles.isEmpty()) {
			otherEquivalences =
					bundles.iterator().next()
							.getEntities().stream()
							.filter(entity -> !entity.getServiceName().matches("ebi.biosamples.(samples|groups)"))
							.collect(Collectors.toSet());

		}

		return otherEquivalences;
	}

}
