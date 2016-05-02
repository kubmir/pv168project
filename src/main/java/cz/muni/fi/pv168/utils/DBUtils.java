package cz.muni.fi.pv168.utils;

import cz.muni.fi.pv168.transactionmanager.Account;
import cz.muni.fi.pv168.transactionmanager.AccountManager;
import cz.muni.fi.pv168.transactionmanager.AccountManagerImpl;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDriver;


/**
 * Class which proccess SQL scripts
 * @author Miroslav Kubus
 */
public class DBUtils {

    private static final Logger logger = Logger.getLogger(DBUtils.class.getName());

    /**
     * Method which creates in memory database with table account and payment
     * @return datasource of database
     */
    public static DataSource createMemoryDatabase() {
        BasicDataSource bds = new BasicDataSource();
        
        bds.setDriverClassName(EmbeddedDriver.class.getName());
        bds.setUrl("jdbc:derby:memory:transactionDB;create=true");
        logger.info("Database created");
        
        new ResourceDatabasePopulator(
                new ClassPathResource("cz/muni/fi/pv168/transactionmanager/createAccountTable.sql"),
                new ClassPathResource("cz/muni/fi/pv168/transactionmanager/createPaymentTable.sql"),
                new ClassPathResource("cz/muni/fi/pv168/transactionmanager/test-data.sql")).execute(bds);
          
        logger.info("SQL scripts executed");
        return bds;
    }
    
    /**
     * Help main for testing createMemoryDatabase - can be deleted
     * @param args
     * @throws SQLException 
     */
    public static void main(String[] args) throws SQLException {
        logger.info("Executing main function");
        DataSource dataSource = createMemoryDatabase();
        AccountManager accountManager = new AccountManagerImpl(dataSource);

        List<Account> allAccounts = accountManager.getAllAccounts();
        System.out.println("allAccounts = " + allAccounts);
    }

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
        
        logger.log(Level.INFO, "Script{0}executed", scriptUrl.toString());
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
            logger.log(Level.SEVERE, "Error while reading sql statement", ex);
            throw new RuntimeException("Cannot read " + url, ex);
        }
    }
}