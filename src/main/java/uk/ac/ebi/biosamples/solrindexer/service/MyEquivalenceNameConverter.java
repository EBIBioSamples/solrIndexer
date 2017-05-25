package uk.ac.ebi.biosamples.solrindexer.service;

public class MyEquivalenceNameConverter {

    public static String convert(String dbName) {

        switch(dbName) {
            case "ebi.ena.samples":
            case "ebi.ena.groups":
                return "ENA";
            case "ebi.biosamples.samples":
            case "ebi.biosamples.groups":
                return "BioSamples";
            case "ebi.arrayexpress.samples":
            case "ebi.arrayexpress.groups":
                return "ArrayExpress";
            case "ebi.metabolights.samples":
            case "ebi.metabolights.groups":
                return "MetaboLights";
            case "sanger.cosmic.groups":
                return "COSMIC";
            case "ebi.pride.samples":
                return "PRIDE";
            default:
                return dbName;

        }

    }
}
