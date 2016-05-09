package cz.muni.fi.pv168.transactionmanager;

import cz.muni.fi.pv168.utils.DBUtils;
import cz.muni.fi.pv168.utils.EntityNotFoundException;
import cz.muni.fi.pv168.utils.ServiceFailureException;
import java.math.BigDecimal;
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
    
    private static PaymentManagerImpl manager;
    private static AccountManagerImpl accountManager;
    private Account from = null;
    private Account to = null;
    private static LocalDate date = null;
    private static Payment payment = null;
    private static DataSource dataSource;
     
    
    @Before
    public void setUp() throws SQLException {
        dataSource = prepareDataSource();
        
        DBUtils.executeSqlScript(dataSource, AccountManager.class.getResource("createAccountTable.sql"));        
        accountManager = new AccountManagerImpl(dataSource);
        from = newAccount("111","from",new BigDecimal(1000));
        accountManager.createAccount(from);
        to = newAccount("222","to",new BigDecimal(100));
        accountManager.createAccount(to);
        
        DBUtils.executeSqlScript(dataSource, PaymentManager.class.getResource("createPaymentTable.sql"));
        manager = new PaymentManagerImpl(dataSource);
        date = LocalDate.now();
        payment = newPayment(from,to,new BigDecimal(500),date);
    }
    
    @After
    public void tearDown() throws SQLException {
        DBUtils.executeSqlScript(dataSource,PaymentManager.class.getResource("dropPaymentTable.sql"));
        DBUtils.executeSqlScript(dataSource,AccountManager.class.getResource("dropAccountTable.sql"));
    }

    
     private static DataSource prepareDataSource() throws SQLException {
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:paymentmanager-test");
        ds.setCreateDatabase("create");
        return ds;
    }
    
    @Rule
    public ExpectedException expectedException= ExpectedException.none();
    
    @Test
    public void testCreatePayment() {
        manager.createPayment(payment);
        payment = updateAmountsOfAccounts(payment);
        
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
        Payment paymentB = preparePaymentB();
        
        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        payment = updateAmountsOfAccounts(payment);
        updateAmountsOfAccounts(paymentB);
        
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

        Payment paymentB = preparePaymentB();
        
        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        payment = updateAmountsOfAccounts(payment);
        paymentB = updateAmountsOfAccounts(paymentB);
        
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

        Payment paymentB = preparePaymentB();

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        payment = updateAmountsOfAccounts(payment);
        paymentB = updateAmountsOfAccounts(paymentB);
        
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
        
        assertTrue(manager.getPaymentsFromAccount(to).isEmpty());
    }
    
    @Test
    public void testGetPaymentsToAcoount() {
        assertTrue(manager.getAllPayments().isEmpty());
           
        Payment paymentB = preparePaymentB();

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        payment = updateAmountsOfAccounts(payment);
        paymentB = updateAmountsOfAccounts(paymentB);
        
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
        
        assertTrue(manager.getPaymentsToAcoount(from).isEmpty());
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
    public void testUpdateAmountOfPayment() {
        Payment paymentB = preparePaymentB();        
        manager.createPayment(payment);
        manager.createPayment(paymentB);
        payment = updateAmountsOfAccounts(payment);
        paymentB = updateAmountsOfAccounts(paymentB);
        
        BigDecimal newAmount = BigDecimal.valueOf(5000);
        BigDecimal oldAmount = payment.getAmount();
      
        payment.setAmount(newAmount);
        manager.updatePayment(payment);
        
        payment.getFrom().setBalance(payment.getFrom().getBalance().add(oldAmount));
        payment.getTo().setBalance(payment.getTo().getBalance().subtract(oldAmount));
        
        from.setBalance(from.getBalance().subtract(newAmount));
        to.setBalance(to.getBalance().add(newAmount));
        
        
        payment = manager.getPaymentByID(payment.getId());
        
        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),to);
        assertTrue((payment.getAmount().compareTo(newAmount)) == 0);     
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(payment,manager.getPaymentByID(payment.getId()));
        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateDateOfPayment() {
        Payment paymentB = preparePaymentB();
        manager.createPayment(payment);
        manager.createPayment(paymentB);
        payment = updateAmountsOfAccounts(payment);
        paymentB = updateAmountsOfAccounts(paymentB);
        
        from = payment.getFrom();
        to = payment.getTo();
        LocalDate newDate = LocalDate.of(2016, Month.SEPTEMBER, 5);

        Long id = payment.getId();
        payment = manager.getPaymentByID(id);
        payment.setDate(newDate);
        manager.updatePayment(payment);
        payment = manager.getPaymentByID(id);

        assertEquals(payment.getTo(),to);
        assertEquals(payment.getFrom(),from);
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
        
        expectedException.expect(ServiceFailureException.class);
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
        Payment newPayment = new Payment();
        newPayment.setFrom(from);
        newPayment.setTo(to);
        newPayment.setAmount(amount);
        newPayment.setDate(date);
        
        return newPayment;
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
       
       assertAmountChangesOfAccounts(expected, actual);  
    }
    
    private static void assertDeepEqualsOfAccounts(Account expected, Account actual) {
        assertEquals(expected.getId(),actual.getId());
        assertEquals(expected.getNumber(),actual.getNumber());
        assertEquals(expected.getHolder(),actual.getHolder());
    }
    
    private static void assertAmountChangesOfAccounts(Payment expected, Payment actual) {
        assertTrue(actual.getFrom().getBalance().compareTo(expected.getFrom().getBalance()) == 0);
        assertTrue(actual.getTo().getBalance().compareTo(expected.getTo().getBalance()) == 0);
    }
    
    private void assertDeepEquals(List<Payment> expectedList, List<Payment> actualList) {
        for (int i = 0; i < expectedList.size(); i++) {
            Payment expected = expectedList.get(i);
            Payment actual = actualList.get(i);
            assertDeepEqualsOfPayment(expected, actual);
        }
    }
    
    private static Payment updateAmountsOfAccounts(Payment toUpdatePayment) {
        toUpdatePayment.getFrom().setBalance(toUpdatePayment.getFrom().getBalance().subtract(toUpdatePayment.getAmount()));
        toUpdatePayment.getTo().setBalance(toUpdatePayment.getTo().getBalance().add(toUpdatePayment.getAmount()));
        
        return toUpdatePayment;
    }
    
    private static Payment preparePaymentB() {
        Account fromB = newAccount("14785","fromB",new BigDecimal(444));
        Account toB = newAccount("14786","toB",new BigDecimal(555));
        accountManager.createAccount(fromB);
        accountManager.createAccount(toB);

        Payment paymentB = newPayment(fromB,toB,new BigDecimal(1000),date);
        
        return paymentB;
    }
    
    private static final Comparator<Payment> idComparator = (Payment o1, Payment o2) -> o1.getId().compareTo(o2.getId());
}
