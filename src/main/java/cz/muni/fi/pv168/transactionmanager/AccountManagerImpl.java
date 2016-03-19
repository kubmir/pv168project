package cz.muni.fi.pv168.transactionmanager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

/**
 * This class implements interface AccountManager
 * 
 * @author Viktória Tóthová
 */
public class AccountManagerImpl implements AccountManager {
    
    private final DataSource dataSource;
    
    public AccountManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }
        
    @Override
    public void createAccount(Account account) {
        validate(account);
        
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
            throw new ServiceFailureException("Error when inserting account " + account, ex);
        }
    }
    
    private void validate(Account account) {
        if(account == null) {
            throw new IllegalArgumentException("Null Account");
        }
        
        if(account.getNumber() == null) {
            throw new IllegalArgumentException("Null number of account");
        }
        
        if(account.getHolder() == null) {
            throw new IllegalArgumentException("Null holder of account");
        }
        
        if(account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative balance of account");
        }        
    }
    
    private Long getKey(ResultSet keyRS, Account account) throws ServiceFailureException, SQLException {
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
    public void updateAccount(Account account) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteAccount(Account account) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Account getAccountById(Long id) throws IllegalArgumentException {
        if(id == null) {
            throw new IllegalArgumentException("Null id of account in getAccountByID");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "SELECT id,number,holder,balance FROM account WHERE id = ?",
                Statement.RETURN_GENERATED_KEYS)) {
            
            st.setLong(1, id);
            ResultSet rs = st.executeQuery();
            
            if(rs.next()) {
                Account account = resultSetToAccount(rs);
                
                if(rs.next()) {
                    throw new ServiceFailureException(
                            "Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + account + " and " + resultSetToAccount(rs));
                }
                
                return account;
            } else {
                return null;
            }
        } catch (SQLException ex) {
            throw new ServiceFailureException("Error when retrieving account with id " + id, ex);
        }
    }
    
    private Account resultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setHolder(rs.getString("holder"));
        account.setNumber(rs.getString("number"));
        
        return account;        
    }

    @Override
    public List<Account> getAllAccounts() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
