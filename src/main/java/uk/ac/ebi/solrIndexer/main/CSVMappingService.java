package uk.ac.ebi.solrIndexer.main;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.model.Entity;

import java.util.*;

/*
* The class is responsible for actually creating the csv files that are used in the neo importer. For every node as well as relationship
* an own csv file is created. To create each csv files, the database is queried via Hibernate, the sample and group objects are run through
* as well as their relationships and the data stored in csv files accordingly. As additional property, Database references for sample/groups are
* extracted from the object or the myEquivalent database
* */
@Service
public class CSVMappingService implements AutoCloseable {

	@Autowired
	private MyEquivalenceManager myEquivalenceManager;

	@Value("${neo4jIndexer.outpath:output}")
	private File outpath;

	private CSVPrinter samplePrinter;
	private CSVPrinter groupPrinter;
	private CSVPrinter membershipPrinter;
	private CSVPrinter derivationPrinter;
	private CSVPrinter sameAsPrinter;
	private CSVPrinter childOfPrinter;
	private CSVPrinter recurationPrinter;
	private CSVPrinter externalLinkPrinter;
	private CSVPrinter hasExternalLinkGroupPrinter;
	private CSVPrinter hasExternalLinkSamplePrinter;

	private int offsetCount = -1;

	public void setOffsetCount(int offsetCount) {
		this.offsetCount = offsetCount;
	}

	/**
	 * If using offset stepping then incorporate that into the output files for csvs
	 * so they can be easily joined together later
	 * @param prefix
	 * @return
	 */
	private File getFile(String prefix) {
		if (offsetCount >= 0) {
			return new File(outpath, "sample." + offsetCount + ".csv");
		} else {
			return new File(outpath, "sample.csv");
		}
	}

	/*
	 * Sets up csv printers for every sample/relationship by using FileWriter
	 * and BufferedWriter
	 */
	@PostConstruct
	public void doSetup() throws IOException {
		// TODO create in temp dirs, then atomically swap into file location
		outpath.mkdirs();
		samplePrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("sample"))), CSVFormat.DEFAULT);
		groupPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("group"))), CSVFormat.DEFAULT);
		membershipPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("membership"))),
				CSVFormat.DEFAULT);
		derivationPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("derivation"))),
				CSVFormat.DEFAULT);
		sameAsPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("sameas"))), CSVFormat.DEFAULT);
		childOfPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("childof"))), CSVFormat.DEFAULT);
		recurationPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("recuratedfrom"))),
				CSVFormat.DEFAULT);
		externalLinkPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("links"))), CSVFormat.DEFAULT);
		hasExternalLinkGroupPrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("haslink_group"))),
				CSVFormat.DEFAULT);
		hasExternalLinkSamplePrinter = new CSVPrinter(new BufferedWriter(new FileWriter(getFile("haslink_sample"))),
				CSVFormat.DEFAULT);
	}

	/*
	 * Close every printer when finished
	 */
	@PreDestroy
	public void close() throws IOException {
		samplePrinter.close();
		groupPrinter.close();
		membershipPrinter.close();
		derivationPrinter.close();
		sameAsPrinter.close();
		childOfPrinter.close();
		recurationPrinter.close();
		externalLinkPrinter.close();
		hasExternalLinkGroupPrinter.close();
		hasExternalLinkSamplePrinter.close();
	}

	private boolean valid(BioSampleGroup group) {
		if (group == null)
			return false;
		// must be owned by one msi and one msi only
		if (group.getMSIs().size() != 1)
			return false;
		return true;
	}

	private boolean valid(BioSample sample) {
		if (sample == null)
			return false;
		// must be owned by one msi and one msi only
		if (sample.getMSIs().size() != 1)
			return false;
		return true;
	}

	/*
	 * Print Sample Node with db reference
	 * 
	 * @param acc accession for a Sample node
	 * 
	 * @param dbrefstring Set<String> that defines the db urls
	 */
	private void printSample(String acc) throws IOException {
		if (acc == null)
			return;
		if (acc.trim().length() == 0)
			return;
		synchronized (samplePrinter) {
			samplePrinter.print(acc);
			samplePrinter.println();
		}
	}

	/*
	 * Print group Node with database reference
	 * 
	 * @param acc accession for a Group node
	 * 
	 * @param dbrefstring Set<String> that defines the db urls
	 */
	private void printGroup(String acc) throws IOException {
		if (acc == null)
			return;
		if (acc.trim().length() == 0)
			return;
		synchronized (groupPrinter) {
			groupPrinter.print(acc);
			groupPrinter.println();
		}
	}

	/*
	 * Print sample to group Membership
	 * 
	 * @param sampleAcc accession of the sample
	 * 
	 * @param groupAcc accession of the group
	 */
	private void printMembership(String sampleAcc, String groupAcc) throws IOException {
		if (sampleAcc == null)
			return;
		if (groupAcc == null)
			return;
		if (sampleAcc.trim().length() == 0)
			return;
		if (groupAcc.trim().length() == 0)
			return;
		synchronized (membershipPrinter) {
			membershipPrinter.print(sampleAcc);
			membershipPrinter.print(groupAcc);
			membershipPrinter.println();
		}
	}

	/*
	 * Prints derivation relationship
	 * 
	 * @param productAcc sample accession
	 * 
	 * @param sourceAcc sample accession
	 */
	private void printDerivation(String productAcc, String sourceAcc) throws IOException {
		if (productAcc == null)
			return;
		if (sourceAcc == null)
			return;
		if (productAcc.trim().length() == 0)
			return;
		if (sourceAcc.trim().length() == 0)
			return;
		synchronized (derivationPrinter) {
			derivationPrinter.print(sourceAcc);
			derivationPrinter.print(productAcc);
			derivationPrinter.println();
		}
	}

	/*
	 * Prints Recurated From relationship
	 * 
	 * @param sample accession
	 * 
	 * @param sample accession
	 */
	private void printRecuration(String target, String original) throws IOException {
		if (target == null)
			return;
		if (original == null)
			return;
		if (target.trim().length() == 0)
			return;
		if (original.trim().length() == 0)
			return;
		synchronized (recurationPrinter) {
			recurationPrinter.print(original);
			recurationPrinter.print(target);
			recurationPrinter.println();
		}
	}

	/*
	 * Prints as as relationship
	 * 
	 * @param sample accession
	 * 
	 * @param sample accession
	 */
	private void printSameAs(String acc, String otherAcc) throws IOException {
		if (acc == null)
			return;
		if (otherAcc == null)
			return;
		if (acc.trim().length() == 0)
			return;
		if (otherAcc.trim().length() == 0)
			return;
		synchronized (sameAsPrinter) {
			sameAsPrinter.print(acc);
			sameAsPrinter.print(otherAcc);
			sameAsPrinter.println();
		}
	}

	/*
	 * Prints childOf relationship
	 * 
	 * @param sample accession
	 * 
	 * @param sample accession
	 */
	private void printChildOf(String child, String parent) throws IOException {
		if (child == null)
			return;
		if (parent == null)
			return;
		if (child.trim().length() == 0)
			return;
		if (parent.trim().length() == 0)
			return;
		synchronized (childOfPrinter) {
			childOfPrinter.print(child);
			childOfPrinter.print(parent);
			childOfPrinter.println();
		}
	}

	/*
	 * @param url url of a database link
	 */
	private void printExternalLink(String url) throws IOException {
		if (url == null)
			return;
		if (url.trim().length() == 0)
			return;
		synchronized (externalLinkPrinter) {
			externalLinkPrinter.print(url);
			externalLinkPrinter.println();
		}
	}

	/*
	 * Saves the relationship between db url and sample
	 */
	private void printSampleHasExternalLink(String acc, String url) throws IOException {
		if (acc == null)
			return;
		if (url == null)
			return;
		if (acc.trim().length() == 0)
			return;
		if (url.trim().length() == 0)
			return;
		synchronized (hasExternalLinkSamplePrinter) {
			hasExternalLinkSamplePrinter.print(acc);
			hasExternalLinkSamplePrinter.print(url);
			hasExternalLinkSamplePrinter.println();
		}
	}

	/*
	 * Saves the relationship between db url and group
	 */
	private void printGroupHasExternalLink(String acc, String url) throws IOException {
		if (acc == null)
			return;
		if (url == null)
			return;
		if (acc.trim().length() == 0)
			return;
		if (url.trim().length() == 0)
			return;
		synchronized (hasExternalLinkGroupPrinter) {
			hasExternalLinkGroupPrinter.print(acc);
			hasExternalLinkGroupPrinter.print(url);
			hasExternalLinkGroupPrinter.println();
		}
	}

	/*
	 * Converts DbRefs coming from an Entity as stored in the myEquivalence
	 * model
	 * 
	 * @param dbRefs A Set<Entity> as stored in myEquivalence
	 */
	private Set<String> covertMyEquiEntity(Set<Entity> dbRefs) {
		Set<String> list = new HashSet<String>();
		for (Entity entity : dbRefs) {
			list.add(entity.getURI().toString());
		}
		return list;
	}

	/*
	 * Converts DbRefs coming from the Hibernate model into Set of Strings
	 * 
	 * @param dbRefs A Set <DatabaseRecordRef> as stored in the model
	 */
	private Set<String> convertDbRefs(Set<DatabaseRecordRef> dbRefs) {
		Set<String> list = new HashSet<String>();
		for (DatabaseRecordRef ref : dbRefs) {
			list.add(ref.getUrl());
		}
		return list;
	}

	public void handle(BioSample sample, EntityMappingManager entityMappingManager) throws IOException {

		if (valid(sample)) {

			Set<String> dburls = new HashSet<String>();
			dburls.addAll(convertDbRefs(sample.getDatabaseRecordRefs()));
			dburls.addAll(covertMyEquiEntity(
					myEquivalenceManager.getSampleExternalEquivalences(sample.getAcc(), entityMappingManager)));

			printSample(sample.getAcc());

			/*
			 * To model the db links as own relationships: Run over dburls and
			 * create for every string an entry in DbNode as well as a
			 * connection between the group and the url
			 */
			for (String url : dburls) {
				// skip internal links, these should be captured as other
				// relationships
				if (url.contains("ebi.ac.uk/biosamples"))
					continue;
				printSampleHasExternalLink(sample.getAcc(), url);
				printExternalLink(url);
			}

			// this is the slow join
			for (ExperimentalPropertyValue<?> epv : sample.getPropertyValues()) {
				ExperimentalPropertyType ept = epv.getType();

				if ("derived from".equals(ept.getTermText().toLowerCase())) {
					printDerivation(sample.getAcc(), epv.getTermText());
				}

				if ("same as".equals(ept.getTermText().toLowerCase())) {
					printSameAs(sample.getAcc(), epv.getTermText());
				}

				// to convert to lower case does not seem to work. no
				// idea why. for the sake of it I keep in the if clause
				// just to make sure we catch all
				if ("Child Of".equals(ept.getTermText()) || "child of".equals(ept.getTermText().toLowerCase())) {
					printChildOf(sample.getAcc(), epv.getTermText());
				}

				if ("recurated from".equals(ept.getTermText().toLowerCase())) {
					printRecuration(sample.getAcc(), epv.getTermText());
				}
			}

		}
	}

	public void handle(BioSampleGroup group, EntityMappingManager entityMappingManager) throws IOException {
		if (valid(group)) {

			Set<String> dburls = new HashSet<String>();
			dburls.addAll(convertDbRefs(group.getDatabaseRecordRefs()));
			dburls.addAll(covertMyEquiEntity(
					myEquivalenceManager.getGroupExternalEquivalences(group.getAcc(), entityMappingManager)));

			printGroup(group.getAcc());

			/*
			 * To model the db links as own relationships: Run over dburls and
			 * create for every string an entry in DbNode as well as a
			 * connection between the group and the url
			 */
			for (String url : dburls) {
				// skip internal links, these should be captured as other
				// relationships
				if (url.contains("ebi.ac.uk/biosamples"))
					continue;
				printGroupHasExternalLink(group.getAcc(), url);
				printExternalLink(url);
			}

			for (BioSample sample : group.getSamples()) {
				if (valid(sample) && valid(group)) {
					printMembership(sample.getAcc(), group.getAcc());
				}
			}
		}

	}
}
