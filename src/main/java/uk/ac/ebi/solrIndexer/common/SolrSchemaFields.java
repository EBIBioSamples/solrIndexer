package uk.ac.ebi.solrIndexer.common;

public class SolrSchemaFields {

	public static final String ACC = "accession";
	/*
		Release date and update date are formatted without "_"
	 	to maintain compatibility with old queries
	  */
	public static final String UPDATE_DATE = "updatedate";
	public static final String RELEASE_DATE = "releasedate";
	public static final String DESCRIPTION = "description";

	/* ------------------------------- */
	/* --         BIOSAMPLE         -- */
	/* ------------------------------- */
	public static final String SAMPLE_GRP_ACC = "sample_grp_accessions";

	/* ------------------------------- */
	/* --           GROUP           -- */
	/* ------------------------------- */
	public static final String GRP_SAMPLE_ACC = "grp_sample_accessions";

	/* ------------------------------- */
	/* --            MSI            -- */
	/* ------------------------------- */
	public static final String SUBMISSION_ACC = "submission_acc";
	public static final String SUBMISSION_TITLE = "submission_title";

	/* ------------------------------- */
	/* --         REFERENCES        -- */
	/* ------------------------------- */
	public static final String REFERENCES = "external_references_json";
	public static final String REFERENCES_ACC = "external_references_acc";
	public static final String REFERENCES_NAME = "external_references_name";
	public static final String REFERENCES_URL = "external_references_url";

	/* ------------------------------- */
	/* --        Solr Schema        -- */
	/* ------------------------------- */
	public static final String CONTENT_TYPE = "content_type";
	public static final String NUMBER_OF_SAMPLES = "number_of_samples";
	public static final String CRT_TYPE = "crt_type";

	/* --- Other --- */
	public static final String XML = "api_xml";

	/* --- BioSolr Plugin Field ---*/
	public static final String BIO_SOLR_FIELD = "ontology_uri";
}
