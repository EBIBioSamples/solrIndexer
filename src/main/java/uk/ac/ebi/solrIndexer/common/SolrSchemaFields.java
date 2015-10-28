package uk.ac.ebi.solrIndexer.common;

public class SolrSchemaFields {

	/* ------------------------------- */
	/* --        BIO_PRODUCT        -- */
	/* ------------------------------- */
	public static final String ID = "id";
	public static final String SAMPLE_ACC = "sample_acc";

	/* ------------------------------- */
	/* --            MSI            -- */
	/* ------------------------------- */
	public static final String SUBMISSION_ACC = "submission_acc";
	public static final String SUBMISSION_DESCRIPTION = "submission_description";
	public static final String SUBMISSION_TITLE = "submission_title";
	public static final String RELEASE_DATE = "sample_release_date";
	public static final String SUBMISSION_UPDATE_DATE = "submission_update_date";
	public static final String FORMATVERSION = "format_version";

	/* ------------------------------- */
	/* --         DB_REC_REF        -- */
	/* ------------------------------- */
	public static final String DB_ACC = "data_base_acc";
	public static final String DB_NAME = "data_base_name";
	public static final String DB_URL = "data_base_url";

	/* ------------------------------- */
	/* --        BIO_SMP_GRP        -- */
	/* ------------------------------- */
	public static final String GROUP_ACC = "group_acc";
	public static final String IS_REF_LAYER = "is_ref_layer";
	public static final String GROUP_UPDATE_DATE = "group_update_date";

	/* ------------------------------- */
	/* --        Solr Schema        -- */
	/* ------------------------------- */
	public static final String CONTENT_TYPE = "content_type";
	public static final String NUMBER_OF_SAMPLES = "number_of_samples";
	public static final String GROUP_SAMPLES = "group_samples";
}
