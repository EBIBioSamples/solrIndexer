package uk.ac.ebi.solrIndexer.main.repo;

import org.springframework.context.annotation.Scope;
import org.springframework.data.repository.PagingAndSortingRepository;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

@Scope("prototype")
public interface BioSampleGroupRepository extends PagingAndSortingRepository<BioSampleGroup, Long> {

	BioSampleGroup findOneByAcc(String acc);
}