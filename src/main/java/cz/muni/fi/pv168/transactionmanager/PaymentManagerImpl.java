package cz.muni.fi.pv168.transactionmanager;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;

/**
 * Class which implements Payment Manager. Supports creating, updating, getting 
 * and deleting payments from manager.
 * @author Miroslav Kubus
 */
public class PaymentManagerImpl implements PaymentManager {

    private final DataSource dataSource;
    
    public PaymentManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void createPayment(Payment payment) {
        
        validate(payment);
        if(payment.getId() != null) {
            throw new IllegalArgumentException("Payment ID is already set");
        }
        
        try ( Connection connection = dataSource.getConnection();
              PreparedStatement st = connection.prepareStatement(
                "INSERT INTO PAYMENTS (from,to,amount,date) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
        
            st.setObject(1,payment.getFrom());
            st.setObject(2,payment.getTo());
            st.setBigDecimal(3,payment.getAmount());
            st.setObject(4,payment.getDate(),Types.DATE);
            int added = st.executeUpdate();
            
            if(added != 1) {
                throw new ServiceFailureException("Internal Error: More rows ("
                        + added + ") inserted when trying to insert payment " + payment);
            }
            
            ResultSet keyRS = st.getGeneratedKeys();
            payment.setId(getKey(keyRS, payment));

        } catch(SQLException ex) {
            throw new ServiceFailureException("Error when inserting payment " + payment, ex);
        }
    }
    
    private void validate(Payment payment) throws IllegalArgumentException {
        if(payment == null) {
            throw new IllegalArgumentException("Payment is null");
        }
        
        if(payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Negative or zero amount of payment");
        }
        
        if(payment.getFrom().equals(payment.getTo())) {
            throw new IllegalArgumentException("Payment with same From and To account");
        }
        
        if(payment.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date of payment from past");
        }
    }
    
   
    private Long getKey(ResultSet keyRS, Payment payment) throws ServiceFailureException, SQLException {
        if (keyRS.next()) {
            if (keyRS.getMetaData().getColumnCount() != 1) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert payment " + payment
                        + " - wrong key fields count: " + keyRS.getMetaData().getColumnCount());
            }
            
            Long result = keyRS.getLong(1);
            
            if (keyRS.next()) {
                throw new ServiceFailureException("Internal Error: Generated key"
                        + "retriving failed when trying to insert payment " + payment
                        + " - more keys found");
            }
            return result;
        } else {
            throw new ServiceFailureException("Internal Error: Generated key"
                    + "retriving failed when trying to insert payment " + payment
                    + " - no key found");
        }
    }
    
    @Override
    public void updatePayment(Payment payment) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deletePayment(Payment payment) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Payment getPaymentByID(Long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Payment> getAllPayments() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Payment> getPaymentsFromAccount(Account account) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Payment> getPaymentsToAcoount(Account account) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
