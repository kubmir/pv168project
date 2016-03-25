package cz.muni.fi.pv168.utils;

import cz.muni.fi.pv168.transactionmanager.Account;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class which provides validation of account and transform resultSet to account
 * @author Miroslav Kubus
 */
public class AccountHelper {
    
    /**
     * Method which retrieve account from resultSet
     * @param rs represents resultSet from which we retrieve account
     * @return account retrieved from resultSet
     * @throws SQLException in case of any error when retrieving data from resultSet
     */
    public Account resultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setHolder(rs.getString("holder"));
        account.setNumber(rs.getString("number"));
        
        return account;        
    }

    /**
     * Method which validate parameters of account
     * @param account represents account to be validate
     * @throws IllegalArgumentException in case of invalid value of argument of account
     */
    public void validate(Account account) throws IllegalArgumentException {
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
}
