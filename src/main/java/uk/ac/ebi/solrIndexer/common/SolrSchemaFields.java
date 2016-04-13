package uk.ac.ebi.solrIndexer.common;

public class SolrSchemaFields {

	/* ------------------------------- */
	/* --         BIOSAMPLE         -- */
	/* ------------------------------- */
	public static final String ID = "id";
	public static final String SAMPLE_ACC = "sample_acc";
	public static final String SAMPLE_UPDATE_DATE = "sample_update_date";
	public static final String SAMPLE_RELEASE_DATE = "sample_release_date";
	public static final String SAMPLE_GRP_ACC = "sample_grp_accessions";

	/* ------------------------------- */
	/* --            MSI            -- */
	/* ------------------------------- */
	public static final String SUBMISSION_ACC = "submission_acc";
	public static final String SUBMISSION_DESCRIPTION = "submission_description";
	public static final String SUBMISSION_TITLE = "submission_title";
	public static final String SUBMISSION_UPDATE_DATE = "submission_update_date";

	/* ------------------------------- */
	/* --         DB_REC_REF        -- */
	/* ------------------------------- */
	public static final String REFERENCES = "external_references";

	/* ------------------------------- */
	/* --           GROUP           -- */
	/* ------------------------------- */
	public static final String GROUP_ACC = "group_acc";
	public static final String GROUP_UPDATE_DATE = "group_update_date";
	public static final String GROUP_RELEASE_DATE = "group_release_date";
	public static final String GRP_SAMPLE_ACC = "grp_sample_accessions";

	/* ------------------------------- */
	/* --        Solr Schema        -- */
	/* ------------------------------- */
	public static final String CONTENT_TYPE = "content_type";
	public static final String NUMBER_OF_SAMPLES = "number_of_samples";
	public static final String CRT_TYPE = "crt_type";

	/* --- Other --- */
	public static final String XML = "xmlAPI";

	/* --- BioSolr Plugin Field ---*/
	public static final String BIO_SOLR_FIELD = "ontology_uri";
}
