package cz.muni.fi.pv168.transactionmanager;

import java.util.List;

/**
 * Interface Account Manager 
 * 
 * @author Viktória Tóthová
 */
public interface AccountManager {

    /**
     * Stores new account into database.
     * Id for the new account is automatically generated and stored into 
     * id attribute.
     * @param account account to be created
     * @throws IllegalArgumentException when account id null or account has already assigned id.
     */
    void createAccount(Account account);
    
    /**
     * Updates account in database.
     * @param account updated account to be stored into database
     * @throws IllegalArgumentException when account is null or account has null id.
     */
    void updateAccount(Account account);
    
    /**
     * Deletes account from database.
     * @param account account to be deleted from database.
     * @throws IllegalArgumentException when account is null or account has null id.
     */
    void deleteAccount(Account account);
    
    /**
     * Returns account with given id. 
     * @param id primary key with requested account.
     * @return account with given id or null if such account does not exist.
     * @throws IllegalArgumentException when given id is null.
     */
    Account getAccountById(Long id);
    
    /**
     * Return list of all accounts in the database.
     * @return list of all accounts in the database.
     */
    List<Account> getAllAccounts();
    
    /**
     * Returns account with given number. 
     * @param number of requested account.
     * @return account with given number or null if such account does not exist.
     * @throws IllegalArgumentException when given id is null.
     */
    Account getAccountByNumber(String number);
}
