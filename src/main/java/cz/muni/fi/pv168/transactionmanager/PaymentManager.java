/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.transactionmanager;

import java.util.List;

/**
 *
 * @author Miroslav Kubus
 */
public interface PaymentManager {
    
    void createPayment(Payment payment);
    void updatePayment(Payment payment);
    void deletePayment(Payment payment);
    Payment getPaymentByID(Long id);
    List<Payment> getAllPayments();
    List<Payment> getPaymentsFromAccount(Account account);
    List<Payment> getPaymentsToAcoount(Account account);
    
}
