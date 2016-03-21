package cz.muni.fi.pv168.transactionmanager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.After;



/**
 * Tests for class PaymentManagerImpl
 * @author Viktória Tóthová, Miroslav Kubus
 */
public class PaymentManagerImplTest {
    
    private PaymentManagerImpl manager;
    private AccountManagerImpl accountManager;
    private Account from = null;
    private Account to = null;
    private LocalDate date = null;
    private Payment payment = null;
    private DataSource dataSource;
    
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("CREATE TABLE account ("
                    + "id bigint primary key generated always as identity,"
                    + "number varchar(255),"
                    + "holder varchar(255),"
                    + "balance decimal(12,4))").executeUpdate();
        }
        
        accountManager = new AccountManagerImpl(dataSource);
        from = newAccount("111","from",new BigDecimal(1000));
        accountManager.createAccount(from);
        to = newAccount("222","to",new BigDecimal(100));
        accountManager.createAccount(to);
        
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("CREATE TABLE payment ("
                    + "id BIGINT primary key generated always as identity,"
                    + "fromAccount BIGINT REFERENCES account(id),"
                    + "toAccount BIGINT REFERENCES account(id),"
                    + "amount DECIMAL(12,4),"
                    + "date DATE)").executeUpdate();
        }
        
        manager = new PaymentManagerImpl(dataSource,accountManager);
        date = LocalDate.of(2016, 3, 23);
        payment = newPayment(from,to,new BigDecimal(500),date);
    }
    
    @After
    public void tearDown() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("DROP TABLE payment").executeUpdate();
            connection.prepareStatement("DROP TABLE account").executeUpdate();
        }
    }

    
     private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        //we will use in memory database
        ds.setDatabaseName("memory:paymentmanager-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
    @Rule
    public ExpectedException expectedException= ExpectedException.none();
    
    @Test
    public void testCreatePayment() {
        manager.createPayment(payment);
        Payment resultPayment = manager.getPaymentByID(payment.getId());
        
        assertEquals(resultPayment,payment);
        assertThat(resultPayment,is(not(sameInstance(payment))));
        assertDeepEqualsOfPayment(payment,resultPayment);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePaymentWithNull() throws Exception {
        manager.createPayment(null);
    }
    
    @Test
    public void testCreatePaymentWithExistingId() {
        payment.setId(new Long(5));
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
       
    @Test
    public void testCreatePaymentWithSameFromToAccount() {
        payment.setTo(from);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithNullFromAccount() {
        payment.setFrom(null);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithNullToAccount() {
        payment.setTo(null);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithNegativeAmount() {
        payment.setAmount(BigDecimal.valueOf(-5000));
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithZeroAmount() {
        payment.setAmount(BigDecimal.valueOf(0));
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithDateFromPast() {
        LocalDate past = LocalDate.of(1999, Month.MARCH, 25);
        payment.setDate(past);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testGetPaymentByID() {
        Payment paymentB = newPayment(to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        Payment expected = payment;
        Payment actual = manager.getPaymentByID(payment.getId());
        
        assertEquals(expected, actual);
        assertThat(expected,is(not(sameInstance(actual))));
        assertDeepEqualsOfPayment(expected, actual);       
    }
    
    @Test
    public void testGetPaymentByIdWIthNonExistentId() {
        assertTrue(manager.getAllPayments().isEmpty());
        manager.createPayment(payment);
        
        assertNull(manager.getPaymentByID(payment.getId() - 1));
    }
    
    @Test
    public void testGetAllPayments() {
        assertTrue(manager.getAllPayments().isEmpty());

        Payment paymentB = newPayment(to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        List<Payment> expected = Arrays.asList(payment, paymentB);
        List<Payment> actual = manager.getAllPayments();
        
        Collections.sort(actual, idComparator);
        Collections.sort(expected, idComparator);
        
        assertEquals(expected, actual);
        assertDeepEquals(expected, actual);
    }
    
     @Test
    public void testGetPaymentsFromAccount() {
        assertTrue(manager.getAllPayments().isEmpty());

        Payment paymentB = newPayment(to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        List<Payment> expected = Arrays.asList(payment);
        List<Payment> actual = manager.getPaymentsFromAccount(from);
        
        Collections.sort(actual, idComparator);
        
        assertEquals(expected,actual);
        assertDeepEquals(expected, actual);        
    }
    
    @Test
    public void testGetPaymentNonExistentFromAccount() {
        assertTrue(manager.getAllPayments().isEmpty());
        manager.createPayment(payment);
        
        expectedException.expect(EntityNotFoundException.class);
        manager.getPaymentsFromAccount(to);
    }
    
    @Test
    public void testGetPaymentsToAcoount() {
        assertTrue(manager.getAllPayments().isEmpty());
           
        Payment paymentB = newPayment(to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        List<Payment> expected = Arrays.asList(payment);
        List<Payment> actual = manager.getPaymentsToAcoount(to);
        
        Collections.sort(actual, idComparator);
        
        assertEquals(expected,actual);
        assertDeepEquals(expected, actual);
    }
    
    @Test
    public void testGetPaymentNonExistentToAccount() {
        assertTrue(manager.getAllPayments().isEmpty());
        manager.createPayment(payment);
        
        expectedException.expect(EntityNotFoundException.class);
        manager.getPaymentsToAcoount(from);
    }
    
    @Test
    public void testDeletePayment() {
        Payment paymentB = newPayment(to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        assertNotNull(manager.getPaymentByID(payment.getId()));
        assertNotNull(manager.getPaymentByID(paymentB.getId()));
        
        manager.deletePayment(paymentB);
         
        assertNotNull(manager.getPaymentByID(payment.getId()));
        assertNull(manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDeleteNullPayment() {
       manager.deletePayment(null);
    }
    
    @Test
    public void testDeletePaymentWithNullId() {
       payment.setId(null);
       
       expectedException.expect(IllegalArgumentException.class);
       manager.deletePayment(payment);
    }
    
    @Test
    public void testDeletePaymentWithNonExistentId() {
       manager.createPayment(payment);
       payment.setId(payment.getId() + 1);
       
       expectedException.expect(EntityNotFoundException.class);
       manager.deletePayment(payment);
    }
    
    @Test
    public void testUpdateFromAccountOfPayment() {
        Payment paymentB = newPayment(to,from,BigDecimal.valueOf(7989),date);
        Account helpAccount = newAccount("444","help",BigDecimal.valueOf(9999));

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        accountManager.createAccount(helpAccount);

        Long id = payment.getId();
        payment = manager.getPaymentByID(id);
        payment.setFrom(helpAccount);
        manager.updatePayment(payment);
        
        assertEquals(payment.getFrom(),helpAccount);
        assertEquals(payment.getTo(),to);
        assertTrue((payment.getAmount().compareTo(BigDecimal.valueOf(500))) == 0);     
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateToAccountOfPayment() {
        Payment paymentB = newPayment(to,from,new BigDecimal(7989),date);
        Account helpAccount = newAccount("444","help",new BigDecimal(9999));
        
        manager.createPayment(payment);
        manager.createPayment(paymentB);
        accountManager.createAccount(helpAccount);
        
        Long id = payment.getId();
        payment = manager.getPaymentByID(id);

        payment.setTo(helpAccount);
        manager.updatePayment(payment);
        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),helpAccount);
        assertTrue((payment.getAmount().compareTo(BigDecimal.valueOf(500))) == 0);     
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateAmountOfPayment() {
        Payment paymentB = newPayment(to,from,new BigDecimal(7989),date);
        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        BigDecimal newAmount = BigDecimal.valueOf(5000);
        payment.setAmount(newAmount);
        manager.updatePayment(payment);

        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),to);
        assertTrue((payment.getAmount().compareTo(newAmount)) == 0);     
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateDateOfPayment() {
        Payment paymentB = newPayment(to,from,new BigDecimal(7989),date);
        manager.createPayment(payment);
        manager.createPayment(paymentB);
        LocalDate newDate = LocalDate.of(2016, Month.SEPTEMBER, 5);

        Long id = payment.getId();
        payment = manager.getPaymentByID(id);

        payment.setDate(newDate);
        manager.updatePayment(payment);
        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),to);
        assertTrue((payment.getAmount().compareTo(BigDecimal.valueOf(500))) == 0);     
        assertEquals(payment.getDate(),newDate);
        
        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testUpdateOfNullPayment() {
        manager.updatePayment(null);
    }
        
    @Test
    public void testUpdateOfPaymentWithNullId() {
        manager.createPayment(payment);
        payment.setId(null);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.updatePayment(payment);
    }
    
    @Test
    public void testUpdateOfPaymentWithNonExistingId() {
        manager.createPayment(payment);
        payment.setId(payment.getId() + 1);
        
        expectedException.expect(EntityNotFoundException.class);
        manager.updatePayment(payment);
    }
    
    @Test
    public void testUpdateOfPaymentWithNegativeAmount() {
        manager.createPayment(payment);
        payment.setAmount(BigDecimal.valueOf(-5));
        
        expectedException.expect(IllegalArgumentException.class);
        manager.updatePayment(payment);
    }
    
    @Test
    public void testUpdateOfPaymentWithZeroAmount() {
        manager.createPayment(payment);
        payment.setAmount(BigDecimal.valueOf(0));
        
        expectedException.expect(IllegalArgumentException.class);
        manager.updatePayment(payment);
    }
    
    @Test
    public void testUpdateOfPaymentWithSameFromAndToAccount() {
        manager.createPayment(payment);
        payment.setFrom(to);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.updatePayment(payment);
    }
    
    @Test
    public void testUpdateOfPaymentWithNullFromAccount() {
        manager.createPayment(payment);
        payment.setFrom(null);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.updatePayment(payment);
    }
    
    @Test
    public void testUpdateOfPaymentWithNullToAccount() {
        manager.createPayment(payment);
        payment.setTo(null);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.updatePayment(payment);
    }
    
    @Test
    public void testUpdateOfPaymentWithNullDate() {
        manager.createPayment(payment);
        payment.setDate(null);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.updatePayment(payment);
    }
    
    
    private static Payment newPayment(Account from, Account to, BigDecimal amount, LocalDate date) {
        Payment payment = new Payment();
        payment.setFrom(from);
        payment.setTo(to);
        payment.setAmount(amount);
        payment.setDate(date);
        
        return payment;
    }
    
    private static Account newAccount(String number, String holder, BigDecimal balance) {
        Account account = new Account();
        account.setNumber(number);
        account.setHolder(holder);
        account.setBalance(balance);
        
        return account;
    }
    
    private static void assertDeepEqualsOfPayment(Payment expected, Payment actual) {
       assertEquals(expected.getId(),actual.getId());
       assertTrue(expected.getAmount().compareTo(actual.getAmount()) == 0);     
       assertEquals(expected.getDate(),actual.getDate());
       assertEquals(expected.getFrom(),actual.getFrom());
       assertEquals(expected.getTo(),actual.getTo());
       
       assertDeepEqualsOfAccounts(expected.getFrom(),actual.getFrom());
       assertDeepEqualsOfAccounts(expected.getTo(),actual.getTo());
        
    }
    
    private static void assertDeepEqualsOfAccounts(Account expected, Account actual) {
        assertEquals(expected.getId(),actual.getId());
        assertEquals(expected.getNumber(),actual.getNumber());
        assertEquals(expected.getHolder(),actual.getHolder());
        assertTrue(expected.getBalance().compareTo(actual.getBalance()) == 0);     
    }
    
    private void assertDeepEquals(List<Payment> expectedList, List<Payment> actualList) {
        for (int i = 0; i < expectedList.size(); i++) {
            Payment expected = expectedList.get(i);
            Payment actual = actualList.get(i);
            assertDeepEqualsOfPayment(expected, actual);
        }
    }
    
    private static Comparator<Payment> idComparator = new Comparator<Payment>() {
        @Override
        public int compare(Payment o1, Payment o2) {
            return o1.getId().compareTo(o2.getId());
        }
    };
}
