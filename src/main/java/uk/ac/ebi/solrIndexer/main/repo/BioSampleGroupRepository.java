package uk.ac.ebi.solrIndexer.main.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public interface BioSampleGroupRepository extends PagingAndSortingRepository<BioSampleGroup, Long> {

	BioSampleGroup findOneByAcc(String acc);
}