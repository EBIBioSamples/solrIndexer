package uk.ac.ebi.service.utilities;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by lucacherubin on 02/02/2016.
 */
public class ExternalDBReferenceChecker {

    public static boolean isReferenceValid(String stringUrl) {

        try {

            URL url = new URL(stringUrl);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            return false;
        }





    }

}
