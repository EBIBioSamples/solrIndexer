package uk.ac.ebi.solrIndexer.main;

import java.util.Set;

/**
 * Created by lucacherubin on 12/02/2016.
 */
public class DataBaseStorage {

    private static Set<String> publicSamplesAccessions;
    private static Set<String> publicGroupsAccession;

    public static Set<String> getPublicSamplesAccessions() {
        if (publicSamplesAccessions == null) {
            publicSamplesAccessions = DataBaseManager.getPublicSamplesAccessionSet();
        }
        return publicSamplesAccessions;

    }

    public static boolean isSamplePublic(String acc) {
        return getPublicSamplesAccessions().contains(acc);
    }

    public static Set<String> getPublicGroupsAccessions() {
        if (publicGroupsAccession == null) {
            publicGroupsAccession = DataBaseManager.getPublicGroupsAccessionSet();
        }
        return publicGroupsAccession;

    }

    public static boolean isGroupPublic(String acc) {
        return getPublicGroupsAccessions().contains(acc);
    }

}
