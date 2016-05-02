package cz.muni.fi.pv168.transactionmanager;

import cz.muni.fi.pv168.utils.AccountHelper;
import cz.muni.fi.pv168.utils.EntityNotFoundException;
import cz.muni.fi.pv168.utils.ServiceFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * This class implements interface AccountManager
 * 
 * @author Viktória Tóthová, Miroslav Kubus
 */
public class AccountManagerImpl implements AccountManager {
    
    private static final Logger logger = Logger.getLogger(AccountManagerImpl.class.getName());
    private final DataSource dataSource;
    private final AccountHelper accountHelper;
    
    public AccountManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
        accountHelper = new AccountHelper();
    }
        
    @Override
    public void createAccount(Account account) throws ServiceFailureException {
        logger.log(Level.INFO, "Creating new account");
        accountHelper.validate(account);
        
        if(account.getId() != null) {
            throw new IllegalArgumentException("Account ID is already set");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                     "INSERT INTO ACCOUNT(number,holder,balance) VALUES (?,?,?)", 
                     Statement.RETURN_GENERATED_KEYS)) {
            
            st.setString(1,account.getNumber());
            st.setString(2,account.getHolder());
            st.setBigDecimal(3,account.getBalance());
            
            int addedRows = st.executeUpdate();
            
            if(addedRows != 1) {
                 throw new ServiceFailureException("Internal Error: More rows ("
                        + addedRows + ") inserted when trying to insert account " + account);
            }
            
            ResultSet keyRS = st.getGeneratedKeys();
            account.setId(getKey(keyRS,account));
                         
        } catch(SQLException ex) {
            logger.log(Level.SEVERE, "Error when creating new account" + account, ex);
            throw new ServiceFailureException("Error when inserting account " + account + ex, ex);
        }
    }
      
    private Long getKey(ResultSet keyRS, Account account) throws ServiceFailureException, SQLException {
        logger.log(Level.INFO, "Setting primary key of new account");
        
        if (keyRS.next()) {
            if (keyRS.getMetaData().getColumnCount() != 1) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert account " + account
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            
            Long result = keyRS.getLong(1);
            
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert account " + account
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + "retriving failed when trying to insert account " + account
                    + " - no key found");
        }
    }

    @Override
    public void updateAccount(Account account) throws ServiceFailureException, EntityNotFoundException {
        logger.log(Level.INFO, "Updating account {0}", account);
        accountHelper.validate(account);
        
        if(account.getId() == null) {
            throw new IllegalArgumentException("Null id of account to upadate");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "UPDATE account SET number = ?, holder = ?, balance = ? WHERE id = ?")) {
            
            st.setString(1, account.getNumber());
            st.setString(2, account.getHolder());
            st.setBigDecimal(3, account.getBalance());
            st.setLong(4, account.getId());
            
            int updated = st.executeUpdate();
            
            if(updated == 0) {
                throw new EntityNotFoundException("Account " + account + "were not find in database");
            }
            
            if(updated != 1) {
                throw new ServiceFailureException("Invalid updated rows count detected "
                                                 + "(one row should be updated): " + updated);
            }
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while updating account " + account, ex);
            throw new ServiceFailureException("Error while updating of account " + account, ex);
        }
    }

    @Override
    public void deleteAccount(Account account) throws ServiceFailureException, EntityNotFoundException {
        logger.log(Level.INFO, "Deleting account {0}", account);
        
        if(account == null) {
            throw new IllegalArgumentException("Non existing account to delete");
        }
        
        if(account.getId() == null) {
            throw new IllegalArgumentException("Null id of account to delete");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
             "DELETE FROM account WHERE id = ?")) {
            
            st.setLong(1, account.getId());
            int removed = st.executeUpdate();
            
            if(removed == 0) {
                throw new EntityNotFoundException(account + "was not find in database");
            } 
            
            if(removed != 1) {
                throw new ServiceFailureException("Invalid deleted rows count detected: " + removed);
            }
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while deleting account with id " 
                                              + account.getId(), ex);
            throw new ServiceFailureException("Error when deleting account with id "
                                              + account.getId(), ex);
        }
    }

    @Override
    public Account getAccountById(Long id)  throws ServiceFailureException {
        logger.log(Level.INFO, "Getting account with ID: {0}", id);
        
        if(id == null) {
            throw new IllegalArgumentException("Null id of account in getAccountByID");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "SELECT id,number,holder,balance FROM account WHERE id = ?")) {
            
            st.setLong(1, id);
            ResultSet rs = st.executeQuery();
            
            if(rs.next()) {
                Account account = accountHelper.resultSetToAccount(rs);
                
                if(rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + account + " and " + accountHelper.resultSetToAccount(rs));
                }
                
                return account;
            } else {
                return null;
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while getting account with ID " + id , ex);
            throw new ServiceFailureException("Error when retrieving account with id " + id, ex);
        }
    }
    
    @Override
    public List<Account> getAllAccounts() throws ServiceFailureException {
        logger.log(Level.INFO, "Getting all accounts from database");
        
        List<Account> accounts = new ArrayList<>();
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM account")) {
            
            ResultSet rs = st.executeQuery();
            
            while(rs.next()) {
                accounts.add(accountHelper.resultSetToAccount(rs));
            }
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while getting all accounts", ex);
            throw new ServiceFailureException("Error when retrieving all accounts",ex);
        }
   
        return accounts;
    }
}
