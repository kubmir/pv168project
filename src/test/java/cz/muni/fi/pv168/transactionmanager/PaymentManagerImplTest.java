/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transfersmanager.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

/**
 *
 * @author Miroslav Kubus
 */
public class PaymentManagerImplTest {
    
    private PaymentManagerImpl manager;
    private Account from = null;
    private Account to = null;
    private LocalDate date = null;
    private Payment payment = null;
    
    @Before
    public void setUp() {
        manager = new PaymentManagerImpl();
        from = newAccount(new Long(1),"111","from",new BigDecimal(1000));
        to = newAccount(new Long(2),"222","to",new BigDecimal(100));
        date = LocalDate.of(2016, 3, 13);
        payment = newPayment(new Long(7),from,to,new BigDecimal(500),date);
    }
    
    @Rule
    public ExpectedException expectedException= ExpectedException.none();
    
    @Test
    public void testCreatePayment() {
        Long paymentId = payment.getId();
        assertNotNull(paymentId);
        
        Payment resultPayment = manager.getPaymentByID(paymentId);
        
        assertEquals(resultPayment,payment);
        assertThat(resultPayment,is(not(sameInstance(payment))));
        
        assertDeepEqualsOfPayment(payment,resultPayment);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePaymentWithNull() throws Exception {
        manager.createPayment(null);
    }
    
    @Test
    public void testCreatePaymentWithNegativeId() {
        payment.setId(new Long(-5));
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithNullId() {
        payment.setId(null);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithSameFromToAccount() {
        payment.setFrom(from);
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
        payment.setAmount(new BigDecimal(-5000));
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testCreatePaymentWithDateFromPast() {
        LocalDate past = LocalDate.of(5, Month.MARCH, 1999);
        payment.setDate(past);
        
        expectedException.expect(IllegalArgumentException.class);
        manager.createPayment(payment);
    }
    
    @Test
    public void testGetPaymentByID() {
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        Payment expected = payment;
        Payment actual = manager.getPaymentByID(payment.getId());
        
        assertEquals(expected, actual);
        assertThat(expected,is(not(sameInstance(actual))));
        assertDeepEqualsOfPayment(expected, actual);       
    }
    
    @Test
    public void testGetAllPayments() {
        assertTrue(manager.getAllPayments().isEmpty());

        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(1000),date);

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

        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        List<Payment> expected = Arrays.asList(payment);
        List<Payment> actual = manager.getPaymentsFromAccount(from);
        
        Collections.sort(actual, idComparator);
        
        assertEquals(expected,actual);
        assertDeepEquals(expected, actual);
        
    }
    
    @Test
    public void testGetPaymentsToAcoount() {
        assertTrue(manager.getAllPayments().isEmpty());
           
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(1000),date);

        manager.createPayment(payment);
        manager.createPayment(paymentB);
        
        List<Payment> expected = Arrays.asList(payment);
        List<Payment> actual = manager.getPaymentsToAcoount(to);
        
        Collections.sort(actual, idComparator);
        
        assertEquals(expected,actual);
        assertDeepEquals(expected, actual);
        
    }
    
    @Test
    public void testDeletePayment() {
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(1000),date);

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
    public void testDeletePaymentWithNegativeId() {
       payment.setId(new Long(-5));
       
       expectedException.expect(IllegalArgumentException.class);
       manager.deletePayment(payment);
    }
    
    @Test
    public void testUpdateIdOfPayment() {
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(7989),date);
        manager.createPayment(paymentB);
        
        payment.setId(new Long(777));
        manager.updatePayment(payment);
        assertEquals(payment.getId(),new Long(777));
        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),to);
        assertEquals(payment.getAmount(),500);
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateFromAccountOfPayment() {
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(7989),date);
        manager.createPayment(paymentB);
        Long id = payment.getId();
        Account helpAccount = newAccount(new Long(9),"444","help",new BigDecimal(9999));
        
        payment.setFrom(helpAccount);
        manager.updatePayment(payment);
        assertEquals(payment.getId(),id);
        assertEquals(payment.getFrom(),helpAccount);
        assertEquals(payment.getTo(),to);
        assertEquals(payment.getAmount(),500);
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateToAccountOfPayment() {
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(7989),date);
        manager.createPayment(paymentB);
        Long id = payment.getId();
        Account helpAccount = newAccount(new Long(9),"444","help",new BigDecimal(9999));
        
        payment.setTo(helpAccount);
        manager.updatePayment(payment);
        assertEquals(payment.getId(),id);
        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),helpAccount);
        assertEquals(payment.getAmount(),500);
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateAmountOfPayment() {
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(7989),date);
        manager.createPayment(paymentB);
        Long id = payment.getId();
        BigDecimal newAmount = new BigDecimal(5000);
        
        payment.setAmount(newAmount);
        manager.updatePayment(payment);
        assertEquals(payment.getId(),id);
        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),to);
        assertEquals(payment.getAmount(),newAmount);
        assertEquals(payment.getDate(),date);

        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    @Test
    public void testUpdateDateOfPayment() {
        Payment paymentB = newPayment(new Long(8),to,from,new BigDecimal(7989),date);
        manager.createPayment(paymentB);
        Long id = payment.getId();
        LocalDate newDate = LocalDate.of(5, Month.SEPTEMBER, 2016);
        
        payment.setDate(newDate);
        manager.updatePayment(payment);
        assertEquals(payment.getId(),id);
        assertEquals(payment.getFrom(),from);
        assertEquals(payment.getTo(),to);
        assertEquals(payment.getAmount(),500);
        assertEquals(payment.getDate(),newDate);
        
        assertDeepEqualsOfPayment(paymentB,manager.getPaymentByID(paymentB.getId()));
    }
    
    //TODO Wrong Arguments of UPDATE
    
    private static Payment newPayment(Long id, Account from, Account to, BigDecimal amount, LocalDate date) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setFrom(from);
        payment.setTo(to);
        payment.setAmount(amount);
        payment.setDate(date);
        
        return payment;
    }
    
    private static Account newAccount(Long id, String number, String holder, BigDecimal balance) {
        Account account = new Account();
        account.setId(id);
        account.setNumber(number);
        account.setHolder(holder);
        account.setBalance(balance);
        
        return account;
    }
    
    private static void assertDeepEqualsOfPayment(Payment expected, Payment actual) {
       //Deep test of parameters of payment
       assertEquals(expected.getId(),actual.getId());
       assertEquals(expected.getAmount(),actual.getAmount());
       assertEquals(expected.getDate(),actual.getDate());
       assertEquals(expected.getFrom(),actual.getFrom());
       assertEquals(expected.getTo(),actual.getTo());
       
       //Deep test of parameters of account
       assertDeepEqualsOfAccounts(expected.getFrom(),actual.getFrom());
       assertDeepEqualsOfAccounts(expected.getTo(),actual.getTo());
        
    }
    
    private static void assertDeepEqualsOfAccounts(Account expected, Account actual) {
        //Deep test of parameters of account
        assertEquals(expected.getId(),actual.getId());
        assertEquals(expected.getNumber(),actual.getNumber());
        assertEquals(expected.getHolder(),actual.getHolder());
        assertEquals(expected.getBalance(),actual.getBalance());
    }
    
    //Deep test for each payment in List
    private void assertDeepEquals(List<Payment> expectedList, List<Payment> actualList) {
        for (int i = 0; i < expectedList.size(); i++) {
            Payment expected = expectedList.get(i);
            Payment actual = actualList.get(i);
            assertDeepEqualsOfPayment(expected, actual);
        }
    }
    
    //Comparator for test which use List of payments
    private static Comparator<Payment> idComparator = new Comparator<Payment>() {
        @Override
        public int compare(Payment o1, Payment o2) {
            return o1.getId().compareTo(o2.getId());
        }
    };
}
