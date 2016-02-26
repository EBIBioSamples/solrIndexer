package uk.ac.ebi.solrIndexer.main;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.common.PropertiesManager;
import uk.ac.ebi.solrIndexer.threads.ThreadGroup;
import uk.ac.ebi.solrIndexer.threads.ThreadSample;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class App {
    private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println(
                    "Please specify a single command, 'samples' or 'groups', to specify whether samples or groups should be indexed");
            System.exit(2);
        }
        else {
            if (!args[0].equals("samples") && !args[0].equals("groups")) {
                System.err.println("Unrecognised command '" + args[0] + "' - please specify 'samples' or 'groups'");
                System.exit(2);
            }
        }

        log.info("Entering application - indexing " + args[0]);
        long startTime = System.currentTimeMillis();

        Set<Future<Integer>> set = new HashSet<>();
        ExecutorService scheduler = Executors.newFixedThreadPool(16);
        ExecutorService indexer = Executors.newFixedThreadPool(8);

        int sum;
        if (args[0].equals("groups")) {
            final SolrClient client =
                    new ConcurrentUpdateSolrClient(PropertiesManager.getSolrCorePath() + "/groups", 10, 8);
            try {
                /* -- Handle Groups -- */
                log.info("Handling Groups");
                int max = 50000; // todo - replace this with group count query
                int start = 0;
                Set<Future<?>> tasks = new HashSet<>();
                while (start < max) {
                    final int from = start;
                    tasks.add(scheduler.submit(() -> {
                        int to = from + PropertiesManager.getSamplesFetchStep();
                        log.info("Scheduling groups from " + from + " to " + to + "...");
                        List<BioSampleGroup> groups = DataBaseManager.getAllIterableGroups(from, to);
                        final List<BioSampleGroup> groupsForThread = groups;
                        Future<Integer> future =
                                indexer.submit(new ThreadGroup(groupsForThread, client, from));
                        set.add(future);
                    }));
                    start += PropertiesManager.getGroupsFetchStep();
                }

                for (Future<?> f : tasks) {
                    f.get();
                }
                log.info("Scheduling of group indexing tasks complete!");

                sum = 0;
                for (Future<Integer> future : set) {
                    sum += future.get();
                }

                log.info("Group documents generated. " + sum + " threads finished successfully.");
                /* -------------------- */
            }
            catch (Exception e) {
                log.error("Error creating index", e);
            }
            finally {
                try {
                    client.close();
                }
                catch (IOException e) {
                    // tried our best
                }
                long duration = (System.currentTimeMillis() - startTime);
                log.info("Running time: " + Formater.formatTime(duration));
            }
        }
        else {
            final SolrClient client =
                    new ConcurrentUpdateSolrClient(PropertiesManager.getSolrCorePath() + "/samples", 10, 8);
            try {
                /* -- Handle Samples -- */
                log.info("Handling Samples");
                int max = 4_000_000; // todo - replace this with sample count query
                int start = 0;
                Set<Future<?>> tasks = new HashSet<>();
                while (start < max) {
                    final int from = start;
                    tasks.add(scheduler.submit(() -> {
                        int to = from + PropertiesManager.getSamplesFetchStep();
                        log.info("Scheduling samples from " + from + " to " + to + "...");
                        List<BioSample> samples = DataBaseManager.getAllIterableSamples(from, to);
                        final List<BioSample> samplesForThread = samples;
                        Future<Integer> future =
                                indexer.submit(new ThreadSample(samplesForThread, client, from));
                        set.add(future);
                    }));
                    start += PropertiesManager.getSamplesFetchStep();
                }

                for (Future<?> f : tasks) {
                    f.get();
                }
                log.info("Scheduling of sample indexing tasks complete!");

                sum = 0;
                for (Future<Integer> future : set) {
                    sum += future.get();
                }

                log.info("Sample documents generated. " + sum + " threads finished successfully.");
                /* -------------------- */
            }
            catch (Exception e) {
                log.error("Error creating index", e);

            }
            finally {
                try {
                    client.close();
                }
                catch (IOException e) {
                    // tried our best
                }
                long duration = (System.currentTimeMillis() - startTime);
                log.info("Running time: " + Formater.formatTime(duration));
            }
        }
        log.info("Indexing finished!");
    }
}