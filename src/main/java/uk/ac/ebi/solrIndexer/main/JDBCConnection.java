package uk.ac.ebi.solrIndexer.main;

import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.solrIndexer.common.PropertiesManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by lucacherubin on 18/02/2016.
 */
public class JDBCConnection {

    private Connection connection;

    public JDBCConnection(/*Database informations*/) throws SQLException{

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String url = PropertiesManager.getDatabaseUrl();
        String user = PropertiesManager.getDatabaseUser();
        String password = PropertiesManager.getDatabasePassword();
        this.connection = DriverManager.getConnection(url,user,password);
    }

    public Connection getConnection() {
        return this.connection;
    }
}
