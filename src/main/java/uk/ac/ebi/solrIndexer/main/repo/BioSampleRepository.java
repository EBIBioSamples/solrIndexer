package uk.ac.ebi.solrIndexer.main.repo;

import org.springframework.data.repository.PagingAndSortingRepository;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;

public interface BioSampleRepository extends PagingAndSortingRepository<BioSample, Long> {

	BioSample findOneByAcc(String acc);
}