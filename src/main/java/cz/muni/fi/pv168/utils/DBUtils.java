package cz.muni.fi.pv168.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Class which proccess SQL scripts
 * @author Miroslav Kubus
 */
public class DBUtils {
    /**
     * Executes SQL script.
     * 
     * @param dataSource datasource
     * @param scriptUrl url of sql script to be executed
     * @throws SQLException when operation fails
     */
    public static void executeSqlScript(DataSource dataSource, URL scriptUrl) throws SQLException {
       
        try (Connection connection = dataSource.getConnection()) {
            for (String sqlStatement : readSqlStatements(scriptUrl)) {
                if (!sqlStatement.trim().isEmpty()) {
                    connection.prepareStatement(sqlStatement).executeUpdate();
                }
            }
        }
    }
    
    /**
     * Reads SQL statements from file. SQL commands in file must be separated by 
     * a semicolon.
     * 
     * @param url url of the file
     * @return array of command  strings
     */
    private static String[] readSqlStatements(URL url) {
        try (InputStreamReader reader = new InputStreamReader(url.openStream(), "UTF-8")) {
            char buffer[] = new char[256];
            StringBuilder result = new StringBuilder();
            
            while (true) {
                int count = reader.read(buffer);
                if (count < 0) {
                    break;
                }
                result.append(buffer, 0, count);
            }
            return result.toString().split(";");
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read " + url, ex);
        }
    }
}