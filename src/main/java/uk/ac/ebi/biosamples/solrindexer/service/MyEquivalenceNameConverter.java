package uk.ac.ebi.biosamples.solrindexer.service;

public class MyEquivalenceNameConverter {

    public static String convert(String dbName) {

        switch(dbName) {
            case "ebi.ena.samples":
            case "ebi.ena.groups":
            case "ENA Sample Service":
                return "ENA";
            case "ebi.biosamples.samples":
            case "ebi.biosamples.groups":
            case "BioSamples Sample Service":
                return "BioSamples";
            case "ebi.arrayexpress.samples":
            case "ebi.arrayexpress.groups":
            case "ArrayExpress Sample Service":
                return "ArrayExpress";
            case "ebi.metabolights.samples":
            case "ebi.metabolights.groups":
                return "MetaboLights";
            case "sanger.cosmic.groups":
            case "COSMIC Sample Service":
                return "COSMIC";
            case "ebi.pride.samples":
            case "PRIDE Sample Service":
                return "PRIDE";
            default:
                return dbName;

        }

    }
}