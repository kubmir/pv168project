package cz.muni.fi.pv168.transactionmanager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Tests for class AccountManagerImpl
 * 
 * @author Viktória Tóthová
 */
public class AccountManagerImplTest {
    
    private AccountManagerImpl manager;
    private DataSource dataSource;
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("CREATE TABLE ACCOUNT ("
                    + "id bigint primary key generated always as identity,"
                    + "number varchar(255),"
                    + "holder varchar(255),"
                    + "balance decimal(12,4))").executeUpdate();
        }
        manager = new AccountManagerImpl(dataSource);
    }
    
    @After
    public void tearDown() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("DROP TABLE ACCOUNT").executeUpdate();
        }
    }

    private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:accountmgr-test");
        ds.setCreateDatabase("create");
        return ds;
    }

    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateAccountWithNull() {
        manager.createAccount(null);
    }
    
    @Test
    public void testCreateAccDbSetId() {
        Account account = newAccount("1", "1", BigDecimal.ZERO);
        manager.createAccount(account);
        assertNotNull(account.getId());
    }
    
    @Test
    public void testCreateAccSavedAccEqualToLoaded() {
        Account account = newAccount("1", "1", BigDecimal.ZERO);
        manager.createAccount(account);       
        
        Account result = manager.getAccountById(account.getId());
        assertThat(result, is(equalTo(account)));
    }
    
    @Test
    public void testCreateAccSameInstance() {
        Account account = newAccount("1", "1", BigDecimal.ZERO);
        manager.createAccount(account);
        
        Account result = manager.getAccountById(account.getId());
        assertThat(result, is(not(sameInstance(account))));
    }
    
    @Test
    public void testCreateAccountDeepEquals() {                                         
        Account account = newAccount("1", "1", BigDecimal.ZERO);
        manager.createAccount(account);
                
        Account result = manager.getAccountById(account.getId());
        assertDeepEquals(account, result);        
    }
    
    @Test
    public void testCreateAccWithSetId() {
        Account account = newAccount("1", "1", BigDecimal.ZERO);
        account.setId(1L);        
        expectedException.expect(IllegalArgumentException.class);
        manager.createAccount(account); 
    }
    
    @Test
    public void testCreateAccNonzeroBalance() {
        Account account = newAccount("1", "1", BigDecimal.valueOf(-15));        
        expectedException.expect(IllegalArgumentException.class);
        manager.createAccount(account);
    }
        
    @Test
    public void testCreateAccNullNumber() {
        Account account = newAccount(null, "1", BigDecimal.ZERO);        
        expectedException.expect(IllegalArgumentException.class);
        manager.createAccount(account);
    }
    
    @Test
    public void testCreateAccNullHolder() {        
        Account account = newAccount("1", null, BigDecimal.ZERO);
        expectedException.expect(IllegalArgumentException.class);
        manager.createAccount(account);
    }
    
    @Test
    public void testCreateAccWithDecimalBalance() {        
        Account account = newAccount("1", "Holder", BigDecimal.valueOf(500.56));
        manager.createAccount(account);
        Account fromDB = manager.getAccountById(account.getId());

        assertEquals(account,fromDB);
        assertThat(account,is(not(sameInstance(fromDB))));
        assertDeepEquals(account, fromDB);    
    }
    
    @Test
    public void testUpdateChangeNumber() {
        Account account = newAccount("1", "a", BigDecimal.ZERO);
        manager.createAccount(account);
        Long graveId = account.getId();
                
        account.setNumber("10");
        manager.updateAccount(account);
        account = manager.getAccountById(graveId);
        assertEquals("10", account.getNumber());
        assertEquals("a", account.getHolder());
        assertTrue(account.getBalance().compareTo(BigDecimal.ZERO) == 0);     
    }
    
    @Test
    public void testUpdateChangeHolder() {
        Account account = newAccount("10", "a", BigDecimal.ZERO);
        manager.createAccount(account);
        Long graveId = account.getId();
                
        account.setHolder("asd");
        manager.updateAccount(account);
        account = manager.getAccountById(graveId);
        assertEquals("10", account.getNumber());
        assertEquals("asd", account.getHolder());
        assertTrue(account.getBalance().compareTo(BigDecimal.ZERO) == 0);     
    }
    
    @Test
    public void testUpdateChangeBalance() {
        Account account = newAccount("10", "asd", BigDecimal.ZERO);
        manager.createAccount(account);
        Long accountId = account.getId();
                
        account.setBalance(BigDecimal.valueOf(100.01));
        manager.updateAccount(account);
        account = manager.getAccountById(accountId);
        assertEquals("10", account.getNumber());
        assertEquals("asd", account.getHolder());
        assertTrue(account.getBalance().compareTo(BigDecimal.valueOf(100.01)) == 0);     
    }

    @Test
    public void testUpdateAccCheckOthers() {
        Account account = newAccount("10", "asd", BigDecimal.ZERO);
        Account acc2 = newAccount("2", "b", BigDecimal.ZERO);
        manager.createAccount(account);
        manager.createAccount(acc2);
        Long graveId = account.getId();
                
        account.setBalance(BigDecimal.valueOf(100.01));
        manager.updateAccount(account);
        account = manager.getAccountById(graveId);
        assertEquals("10", account.getNumber());
        assertEquals("asd", account.getHolder());
        assertTrue(BigDecimal.valueOf(100.01).compareTo(account.getBalance()) == 0);     
        
        assertDeepEquals(acc2, manager.getAccountById(acc2.getId()));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateAccNullParam() {
        manager.updateAccount(null);
    }
    
    @Test
    public void testUpdateAccNullId() {
        Account account= newAccount("2", "f", BigDecimal.ZERO);
        manager.createAccount(account);
        account = manager.getAccountById(account.getId());
        account.setId(null);        
        expectedException.expect(IllegalArgumentException.class);
        manager.updateAccount(account);
    }
    
    @Test
    public void testUpdateAcc() {
        Account account= newAccount("2", "f", BigDecimal.ZERO);
        manager.createAccount(account);
        Long accountId = account.getId();        
        account = manager.getAccountById(accountId);
        account.setId(accountId - 1);        
        expectedException.expect(EntityNotFoundException.class);
        manager.updateAccount(account);
    }
    
    @Test
    public void testUpdateAccNullNumber() {
        Account account= newAccount("2", "f", BigDecimal.ZERO);
        manager.createAccount(account);        
        account = manager.getAccountById(account.getId());
        account.setNumber(null);        
        expectedException.expect(IllegalArgumentException.class);
        manager.updateAccount(account);
    }
    
    @Test
    public void testUpdateAccNullHolder() {
        Account account= newAccount("2", "f", BigDecimal.ZERO);
        manager.createAccount(account);        
        account = manager.getAccountById(account.getId());
        account.setHolder(null);        
        expectedException.expect(IllegalArgumentException.class);
        manager.updateAccount(account);
    }

    @Test
    public void testDeleteAccount() {                             
        Account acc1 = newAccount("1", "1", BigDecimal.ZERO);
        Account acc2 = newAccount("2", "2", BigDecimal.ZERO);
        manager.createAccount(acc1);
        manager.createAccount(acc2);
        
        assertNotNull(manager.getAccountById(acc1.getId()));
        assertNotNull(manager.getAccountById(acc2.getId()));
        
        manager.deleteAccount(acc1);
        
        assertNull(manager.getAccountById(acc1.getId()));
        assertNotNull(manager.getAccountById(acc2.getId()));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDeleteAccNullParam() {
        manager.deleteAccount(null);
    }
    
    @Test
    public void testDeleteAccWithoutId() {
        Account account = newAccount("1", "1", BigDecimal.ZERO);
        account.setId(null);
        expectedException.expect(IllegalArgumentException.class);
        manager.deleteAccount(null);
    }
    
    @Test
    public void testDeleteAccWithNonExistingId() {
        Account account = newAccount("1", "1", BigDecimal.ZERO);
        account.setId(1L);
        expectedException.expect(EntityNotFoundException.class);
        manager.deleteAccount(account);
    }
    
    @Test
    public void testGetAllAccEmpty() {
        assertTrue(manager.getAllAccounts().isEmpty());
    }

    @Test
    public void testGetAllAccounts() {                     
        Account acc1 = newAccount("1", "1", BigDecimal.ZERO);
        Account acc2 = newAccount("2", "2", BigDecimal.ZERO);
        manager.createAccount(acc1);
        manager.createAccount(acc2);
        
        List<Account> expected = Arrays.asList(acc1, acc2);
        List<Account> actual = manager.getAllAccounts();
        
        idComparator idComp = new idComparator();
        Collections.sort(actual, idComp);
        Collections.sort(expected, idComp);
        
        assertEquals(expected, actual);
        assertDeepEquals(expected, actual);
    }
    
    class idComparator implements Comparator<Account> {
        @Override
        public int compare(Account o1, Account o2) {
            return o1.getId().compareTo(o2.getId());
        }
    }
            
    private void assertDeepEquals(Account expected, Account actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getNumber(), actual.getNumber());
        assertEquals(expected.getHolder(), actual.getHolder());
        assertTrue(expected.getBalance().compareTo(actual.getBalance()) == 0);     
    }
    
    private void assertDeepEquals(List<Account> expectedList, List<Account> actualList) {
        assertEquals(expectedList.size(), actualList.size());
        
        for (int i = 0; i < expectedList.size(); ++i) {
            Account expected = expectedList.get(i);
            Account actual = actualList.get(i);
            assertDeepEquals(expected, actual);
        }
    }
    
    private Account newAccount(String number, String holder, BigDecimal balance) {
        Account account = new Account();
        account.setNumber(number);
        account.setHolder(holder);
        account.setBalance(balance);
        return account;
    }
}
