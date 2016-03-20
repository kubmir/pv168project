package cz.muni.fi.pv168.transactionmanager;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface Payment Manager
 * @author Miroslav Kubus
 */
public interface PaymentManager {
   
    /**
     * Stores new payment into the database.
     * Id for new payment is automatically generated and stored into
     * id attribute.
     * @param payment represents payment to be created
     * @throws IllegalArgumentException when payment id null or payment has already assigned id.
     */
    void createPayment(Payment payment);
    
    /**
     * Update payment in the database.
     * @param payment updated payment to be stored into the database.
     * @throws IllegalArgumentException when some attribute is invalid
     */
    void updatePayment(Payment payment);
    
    /**
     * Delete payment from the database.
     * @param payment represents payment to be deleted from the database.
     * @throws IllegalArgumentException when payment is null or payment has null id.
     */
    void deletePayment(Payment payment);
    
    /**
     * Returns payment with given id. 
     * @param id represents primary key of requested payment.
     * @return payment with given id or null if such payment does not exist.
     * @throws IllegalArgumentException when given id is null.
     */
    Payment getPaymentByID(Long id);
    
    /**
     * Return list of all payments in the database.
     * @return list of all payments in the database.
     */
    List<Payment> getAllPayments();
    
    /**
     * Return list of payments where from account is equal to parameter account
     * @param account represents FROM account of searched payments 
     * @return list of payments where from account is equal to parameter account
     */
    List<Payment> getPaymentsFromAccount(Account account);
    
    /**
     * Return list of payments where TO account is equal to parameter account
     * @param account represents TO account of searched payments 
     * @return list of payments where TO account is equal to parameter account
     */
    List<Payment> getPaymentsToAcoount(Account account);
}
