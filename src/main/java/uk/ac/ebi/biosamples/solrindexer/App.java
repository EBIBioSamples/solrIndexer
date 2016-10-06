package uk.ac.ebi.biosamples.solrindexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.solrindexer.service.BioSDDAO;
import uk.ac.ebi.biosamples.solrindexer.service.CSVMappingService;

@Component
public class App implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${solrindexer.threadcount:0}")
	private int poolThreadCount;

	@Value("${solrindexer.fetchstep.samples:1000}")
	private int samplesFetchStep;

	@Value("${solrindexer.fetchstep.groups:1000}")
	private int groupsFetchStep;

	@Value("${solrindexer.solr.corepath.groups}")
	private String solrIndexGroupsCorePath;
	@Value("${solrindexer.solr.corepath.samples}")
	private String solrIndexSamplesCorePath;
	@Value("${solrindexer.solr.corepath.merged}")
	private String solrIndexMergedCorePath;
	@Value("${solrindexer.solr.corepath.autosuggest}")
	private String solrIndexAutosuggestCorePath;

	@Value("${solrindexer.solr.queuesize:1000}")
	private int solrIndexQueueSize;
	@Value("${solrindexer.solr.threadcount:4}")
	private int solrIndexThreadCount;

	@Value("${solrindexer.autosuggest.field:onto_suggest}")
	private String autosuggestField;

	@Value("${solrindexer.autosuggest.mincount:100}")
	private int autosuggestMinCount;
	
	//Note this is a 1-n value not 0-(n-1)
	@Value("${solrindexer.offset.count:0}")
	private int offsetCount = 0;
	@Value("${solrindexer.offset.total:0}")
	private int offsetTotal = -1;

	private ExecutorService threadPool = null;
	private List<Future<Integer>> futures = new LinkedList<>();
	private int callableCount = 0;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private BioSDDAO jdbcdao;
	
	@Autowired
	private CSVMappingService csvService;

	private ConcurrentUpdateSolrClient groupsClient = null;
	private ConcurrentUpdateSolrClient samplesClient = null;
	private ConcurrentUpdateSolrClient mergedClient = null;
	private List<String> groupAccs = new ArrayList<>();
	private List<String> sampleAccs = new ArrayList<>();;

	private boolean doGroups = true;
	private boolean doSamples = true;
	private boolean doCSV = false;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		log.info("Entering application.");
		long startTime = System.nanoTime();

		doGroups = !args.containsOption("notgroups");
		doSamples = !args.containsOption("notsamples");
		doCSV = args.containsOption("csv");		
		
		if (doGroups) {
			log.info("Will process groups");
		}	
		if (doSamples) {
			log.info("Will process samples");
		}
		if (doCSV) {
			log.info("Will output csv files");
		}
		

		// When provided a file with accessions to index
		if (args.containsOption("sourcefile")) {
			//if -- is a filename then read stdin
			handleFilenames(args.getOptionValues("sourcefile"));
		}

		// Merged core client initialization
		mergedClient = new ConcurrentUpdateSolrClient(solrIndexMergedCorePath, solrIndexQueueSize, solrIndexThreadCount);

		// only bother getting accessions from db if we will actually use them
		if (doGroups) {
			groupsClient = new ConcurrentUpdateSolrClient(solrIndexGroupsCorePath, solrIndexQueueSize, solrIndexThreadCount);
			// don't get from db if we were given a file with them
			if (groupAccs.size() == 0) {
				if (offsetTotal > 0) {
					int count = jdbcdao.getGroupCount();
					int offsetSize = count / offsetTotal;
					int start = offsetSize * (offsetCount-1);
					log.info("Getting group accessions for chunk " + offsetCount + " of " + offsetTotal);
					groupAccs = jdbcdao.getGroupAccessions(start, offsetSize);
					log.info("got " + groupAccs.size() + " groups");
				} else {
					log.info("Getting group accessions");
					groupAccs = jdbcdao.getGroupAccessions();
					log.info("got " + groupAccs.size() + " groups");
				}
			}
		}
		// only bother getting accessions from db if we will actually use them
		if (doSamples) {
			samplesClient = new ConcurrentUpdateSolrClient(solrIndexSamplesCorePath, solrIndexQueueSize, solrIndexThreadCount);
			// don't get from db if we were given a file with them
			if (sampleAccs.size() == 0) {
				if (offsetTotal > 0) {
					int count = jdbcdao.getSampleCount();
					int offsetSize = count / offsetTotal;
					int start = offsetSize * (offsetCount-1);
					log.info("Getting sample accessions for chunk " + offsetCount + " of " + offsetTotal);
					sampleAccs = jdbcdao.getSampleAccessions(start, offsetSize);
					log.info("got " + sampleAccs.size() + " samples");
				} else {
					log.info("Getting sample accessions");
					sampleAccs = jdbcdao.getSampleAccessions();
					log.info("Counted " + sampleAccs.size() + " samples");
				}
			}
		}

		try {
			// create solr index
			// maybe we want this, maybe not?
			// client.setParser(new XMLResponseParser());


			// create the thread stuff if required
			try {
				if (poolThreadCount > 0) {
					log.info("creating thread pool of "+poolThreadCount+" threads");
					threadPool = Executors.newFixedThreadPool(poolThreadCount);
				}

				// process things
				if (mergedClient != null) {
					if (doGroups && groupsClient != null) {
						runGroups(groupAccs);
					}
					if (doSamples && samplesClient != null) {
						runSamples(sampleAccs);
					}
				}

				// wait for all other futures to finish
				log.info("Waiting for futures...");
				for (Future<Integer> future : futures) {
					Integer countOfFuture = future.get();
					if (countOfFuture != null) {
						callableCount += countOfFuture;
						log.trace("" + callableCount + " documents so far, " + futures.size() + " futures remaining");
						// after each finished callable make the solr client
						// commit
						// populates the index as we go, and doing them all here
						// reduces collision risk
						// if collisions do occur, increase samples.fetchStep
						// and groups.fetchStep
						// client.commit();
						// removing this in favour of commit within parameter on
						// add
					}
				}
			} finally {
				// handle closing of thread pool in case of error
				if (threadPool != null && !threadPool.isShutdown()) {
					log.info("Shutting down thread pool");
					// allow a second to cleanly terminate before forcing
					threadPool.shutdown();
					threadPool.awaitTermination(60, TimeUnit.MINUTES);
					threadPool.shutdownNow();
				}
			}

		} finally {

			// finish the solr clients
			log.info("Closing solr clients");
			if (groupsClient != null) {
				groupsClient.commit();
				groupsClient.blockUntilFinished();
				groupsClient.close();
			}

			if (samplesClient != null) {
				samplesClient.commit();
				samplesClient.blockUntilFinished();
				samplesClient.close();
			}

			if (mergedClient != null) {
				mergedClient.commit();
				mergedClient.blockUntilFinished();
				mergedClient.close();
			}
			
			//always log how many documents we did in how long
			//so we have at least partial progress to compare
			long elapsedNanoseconds = System.nanoTime() - startTime;
			log.info("Generated " + callableCount + " documents in " + Formater.formatTime(elapsedNanoseconds/1000000));
			if (callableCount > 0) {
				log.info("Average time of "+(elapsedNanoseconds/callableCount)+"ns per document");
			}
			
		}

		log.info("Indexing finished!");
		if (offsetCount >= offsetTotal) {
			populateAutosuggestCore();
		}
		return;
	}

	private void populateAutosuggestCore() {
		log.info("Starting autosuggest core population");

		SolrClient sourceClient = new HttpSolrClient(solrIndexMergedCorePath);
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*")
				.setFacet(true)
				.setFacetLimit(-1)
				.setFacetMinCount(autosuggestMinCount)
				.setFacetSort("count")
				.setParam("facet.field",autosuggestField);

		try {
			QueryResponse response = sourceClient.query(query);
            FacetField suggestFacets = response.getFacetField("onto_suggest");
            suggestFacets.getValues().forEach(f -> {
                log.info(String.format("%s - %d", f.getName(), f.getCount()));
            });
            List<String> suggestTerms = suggestFacets.getValues().stream()
                    .map(FacetField.Count::getName)
                    .collect(Collectors.toList());

			if (suggestTerms.isEmpty()) {
				throw new IOException("No terms for autosuggestion has been returned from the origin core");
			}

            SolrClient destClient = new HttpSolrClient(solrIndexAutosuggestCorePath);
            List<SolrInputDocument> docs = suggestTerms.stream()
                    .map(t -> {
                        SolrInputDocument doc = new SolrInputDocument();
                        doc.addField("autosuggest_term_label", t);
                        return doc;
                    }).collect(Collectors.toList());
			destClient.add(docs);
			destClient.commit();

		} catch (IOException e) {
			log.error("There was a problem while retrieving documents from the merged core",e);
		} catch (SolrServerException e) {
			log.error("A problem occurred with solr server",e);
		}

		log.info("Population of autosuggest core finished");
	}

	private void runGroups(List<String> groupAccs) throws Exception {
		if (doGroups) {
			// Handle Groups
			log.info("Handling Groups");
			for (int i = 0; i < groupAccs.size(); i += groupsFetchStep) {
				List<String> theseGroupAccs = groupAccs.subList(i, Math.min(i + groupsFetchStep, groupAccs.size()));
				// have to create multiple beans via context so they all have
				// their own dao object
				// this is apparently bad Inversion Of Control but I can't see a
				// better way to do it
				GroupRepoCallable callable;
				if (doCSV) {
					callable = context.getBean(GroupRepoCallable.class, groupsClient, mergedClient, theseGroupAccs, csvService);
				} else {
					callable = context.getBean(GroupRepoCallable.class, groupsClient, mergedClient, theseGroupAccs, null);					
				}

				if (threadPool == null) {
					callable.call();
				} else {
					Future<Integer> future = threadPool.submit(callable);
					if (future == null) throw new NullPointerException("Future should not be null");
					futures.add(future);
				}
			}
		}
	}

	private void runSamples(List<String> sampleAccs) throws Exception {
		if (doSamples) {
			// Handle samples
			log.info("Handling samples");
			for (int i = 0; i < sampleAccs.size(); i += samplesFetchStep) {
				List<String> theseSampleAccs = sampleAccs.subList(i, Math.min(i + samplesFetchStep, sampleAccs.size()));
				// have to create multiple beans via context so they all have
				// their own dao object
				// this is apparently bad Inversion Of Control but I can't see a
				// better way to do it
				SampleRepoCallable callable;
				if (doCSV) {
					callable = context.getBean(SampleRepoCallable.class, samplesClient, mergedClient, theseSampleAccs, csvService);
				} else {
					callable = context.getBean(SampleRepoCallable.class, samplesClient, mergedClient, theseSampleAccs, null);
				}

				if (threadPool == null) {
					callable.call();
				} else {
					Future<Integer> future = threadPool.submit(callable);
					if (future == null) throw new NullPointerException("Future should not be null");
					futures.add(future);
				}
			}
		}
	}

	/**
	 * Reads a list of named files or (stdin) and extracts sample and group accessions into the appropriate
	 * lists on this object.
	 * @param filenames
	 */
	private void handleFilenames(List<String> filenames) {
		Set<String> sampleAccs =  new HashSet<>();
		Set<String> groupAccs =  new HashSet<>();
		
		for (String filename : filenames) {
			//read from standard in if a filename is --
			if (filename.equals("--")) {
				log.info("Reading accessions from standard input");
				try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
					handleBufferedReader(br, sampleAccs, groupAccs);
				} catch (IOException e) {
					log.error("Unable to read "+filename, e);
				}
			} else {
				log.info("Reading accessions from "+filename);
				File file = new File(filename);
				if (file.exists() && file.isFile()) {		
					try (BufferedReader br = new BufferedReader(new FileReader(file))) {
						handleBufferedReader(br, sampleAccs, groupAccs);
				    } catch (FileNotFoundException e) {
						log.error("Unable to find "+filename, e);
					} catch (IOException e) {
						log.error("Unable to read "+filename, e);
					}
				}
			}
		}
		//now we have accessions as sets
		//this is so that we can check for membership and add new ones efficiently
		
		//need to convert them to lists so they can be sorted and then sliced
		// But only if there are any samples and/or groups accessions
		int offsetSize, start;

		if (!sampleAccs.isEmpty()) {
			this.sampleAccs = new ArrayList<>(sampleAccs);
			Collections.sort(this.sampleAccs);
			if (offsetTotal > 0) {
				//slice down to section specified by arguments
				offsetSize = this.sampleAccs.size() / offsetTotal;
				start = offsetSize * (offsetCount-1);
				this.sampleAccs = this.sampleAccs.subList(start, start+offsetSize);
			}
		} else {
			this.doSamples = false;
		}

		if (!groupAccs.isEmpty()) {
			this.groupAccs = new ArrayList<>(groupAccs);
			Collections.sort(this.groupAccs);
			if (offsetTotal > 0) {
				offsetSize = this.groupAccs.size() / offsetTotal;
				start = offsetSize * (offsetCount-1);
				this.groupAccs = this.groupAccs.subList(start, start + offsetSize);
			}
		} else {
			this.doGroups = false;
		}

	}

	/**
	 * Reads from a buffered reader and extracts sample and group accessions into the sets provided.
	 */
	private void handleBufferedReader(BufferedReader br, Set<String> sampleAccSet, Set<String> groupAccSet) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			log.debug("reading line '"+line+"'");
	        if (line.matches("^SAM[END]A?[0-9]+$")) {
	        	if (!sampleAccSet.contains(line)) {
	        		log.debug("adding sample accession "+line);
	        		sampleAccSet.add(line);
	        	}
	        } else if(line.matches("^SAM[END]G[0-9]+$")) {
				if (!groupAccSet.contains(line)) {
	        		log.debug("adding group accession "+line);
	        		groupAccSet.add(line);
	        	}
	        }  
		}
	}
		
}
