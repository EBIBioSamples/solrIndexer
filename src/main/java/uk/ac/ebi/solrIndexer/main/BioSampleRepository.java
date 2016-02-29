package uk.ac.ebi.solrIndexer.main;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;

public interface BioSampleRepository extends CrudRepository<BioSample, Long> {

	BioSample findOneByAcc(String acc);
}