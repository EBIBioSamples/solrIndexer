
CREATE CONSTRAINT ON (sample:Sample) ASSERT sample.accession IS UNIQUE;
CREATE CONSTRAINT ON (group:Group) ASSERT group.accession IS UNIQUE;
CREATE CONSTRAINT ON (link:ExternalLink) ASSERT link.url IS UNIQUE;
