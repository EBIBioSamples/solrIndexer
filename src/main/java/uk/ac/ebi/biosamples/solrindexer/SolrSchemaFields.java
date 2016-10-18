package uk.ac.ebi.biosamples.solrindexer;

public class SolrSchemaFields {

	public static final String ACC = "accession";
	public static final String NAME = "name";
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
	public static final String CRT_TYPE = "crt_type";

	/* --- Other --- */
	public static final String XML = "api_xml";

	/* --- BioSolr Plugin Field ---*/
	public static final String BIO_SOLR_FIELD = "ontology_uri";

	/* --- Organizations, Contacts & Publications --- */
	public static final String ORG_NAME = "org_name";
	public static final String ORG_EMAIL = "org_email";
	public static final String ORG_ROLE = "org_role";
	public static final String ORG_URL = "org_url";
	public static final String ORG_JSON = "org_json";

	public static final String CONTACT_NAME = "contact_name";
	public static final String CONTACT_AFFILIATION = "contact_affiliation";
	public static final String CONTACT_URL = "contact_url";
	public static final String CONTACT_JSON = "contact_json";

	public static final String PUB_DOI = "pub_doi";
	public static final String PUB_PUBMED = "pub_pubmed";
	public static final String PUB_JSON = "pub_json";

}
