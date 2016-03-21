package cz.muni.fi.pv168.transactionmanager;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * Class which implements Payment Manager. Supports creating, updating, getting 
 * and deleting payments from manager.
 * @author Miroslav Kubus
 */
public class PaymentManagerImpl implements PaymentManager {

    private final DataSource dataSource;
    private final AccountManagerImpl accountManager;
    
    public PaymentManagerImpl(DataSource dataSource, AccountManagerImpl manager) {
        this.dataSource = dataSource;
        this.accountManager = manager;
    }
    
    @Override
    public void createPayment(Payment payment) {
        
        validate(payment);
        if(payment.getId() != null) {
            throw new IllegalArgumentException("Payment ID is already set");
        }
        
        try ( Connection connection = dataSource.getConnection();
              PreparedStatement st = connection.prepareStatement(
                "INSERT INTO payment (fromAccount,toAccount,amount,date) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
        
            st.setLong(1,payment.getFrom().getId());
            st.setLong(2,payment.getTo().getId());
            st.setBigDecimal(3,payment.getAmount());
            st.setDate(4,Date.valueOf(payment.getDate()));
            
            int added = st.executeUpdate();
            
            if(added != 1) {
                throw new ServiceFailureException("Internal Error: More rows ("
                        + added + ") inserted when trying to insert payment " + payment);
            }
            
            ResultSet keyRS = st.getGeneratedKeys();
            payment.setId(getKey(keyRS, payment));

        } catch(SQLException ex) {
            throw new ServiceFailureException("Error when inserting payment " + payment + ex.getLocalizedMessage());
        }
    }
    
    private void validate(Payment payment) throws IllegalArgumentException {
        if(payment == null) {
            throw new IllegalArgumentException("Payment is null");
        }
        
        if(payment.getFrom() == null) {
            throw new IllegalArgumentException("Null fromAccount of payment");
        }
        
        if(payment.getTo() == null) {
            throw new IllegalArgumentException("Null toAccount of payment");
        }
        
        if(payment.getDate() == null) {
            throw new IllegalArgumentException("Null date of payment");
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
        validate(payment);
        
        if(payment.getId() == null) {
            throw new IllegalArgumentException("Null id of payment to update");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "UPDATE payment SET fromAccount = ?, toAccount = ? , amount = ?, date = ? WHERE id = ?")) {
            
            st.setLong(1, payment.getFrom().getId());
            st.setLong(2, payment.getTo().getId());
            st.setBigDecimal(3, payment.getAmount());
            st.setDate(4, Date.valueOf(payment.getDate()));
            st.setLong(5, payment.getId());
            
            int updated = st.executeUpdate();
            
            if(updated == 0) {
                throw new EntityNotFoundException("No payment " + payment + " in database");
            } 
            
            if(updated != 1) {
                throw new ServiceFailureException("Invalid updated rows count detected "
                                            + "(one row should be updated): " + updated);
            }
            
        } catch(SQLException ex) {
            throw new ServiceFailureException("Error while updating payment " + payment, ex);
        }
    }

    @Override
    public void deletePayment(Payment payment) {
        validate(payment);
        
        if(payment.getId() == null) {
            throw new IllegalArgumentException("Null id of payment for deleting");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement("DELETE FROM payment WHERE id = ?")) {
            
            st.setLong(1,payment.getId());
            
            int removed = st.executeUpdate();
            
            if(removed == 0) {
                throw new EntityNotFoundException(payment + " was not find in database");
            }
            
            if(removed != 1) {
                throw new ServiceFailureException("Invalid deleted rows count detected: " + removed);
            }
            
        } catch (SQLException ex) {
            throw new ServiceFailureException("Error while deleting payment " + payment,ex);
        }
    }       

    @Override
    public Payment getPaymentByID(Long id) {
        
        if(id == null) {
            throw new IllegalArgumentException("Null id of payment in getPaymentByID");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "SELECT * FROM payment WHERE id = ?")) {
            
            st.setLong(1,id);
            ResultSet rs = st.executeQuery();
            
            if(rs.next()) {
                Payment payment = resultSetToPayment(rs);
                
                if (rs.next()) {
                    throw new ServiceFailureException("Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + payment + " and " + resultSetToPayment(rs));
                }
                
                return payment;
            } else {
                return null;
            }   
        } catch (SQLException ex) {
            throw new ServiceFailureException("Error while getting payment with id " + id, ex);
        }
    }
    
    private Payment resultSetToPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setId(rs.getLong("id"));
        payment.setFrom(accountManager.getAccountById(rs.getLong("fromAccount")));
        payment.setTo(accountManager.getAccountById(rs.getLong("toAccount")));
        payment.setDate(rs.getDate("date").toLocalDate());
        
        return payment;
    }

    @Override
    public List<Payment> getAllPayments() {
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM payment")) {
            List<Payment> toReturn = new ArrayList<>();
            ResultSet rs = st.executeQuery();
            
            while(rs.next()) {
                toReturn.add(resultSetToPayment(rs));
            }
            
            return toReturn;
        } catch (SQLException ex) {
            throw new ServiceFailureException("Error while getting all payments from database", ex);
        }
    }

    @Override
    public List<Payment> getPaymentsFromAccount(Account account) {
        validate(account);
        
        if(account.getId() == null) {
            throw new IllegalArgumentException("Null id of account in getPaymentsFromAccount");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "SELECT * FROM payment WHERE fromAccount = ?")) {
            
            st.setLong(1, account.getId());
            List<Payment> toReturn = new ArrayList<>();
            ResultSet rs = st.executeQuery();
            
            while(rs.next()) {
                toReturn.add(resultSetToPayment(rs));
            }
            
            return toReturn;
        } catch(SQLException ex) {
            throw new ServiceFailureException("Error while getting all payment "
                                              + "from account " + account,ex);
        }
    }

    @Override
    public List<Payment> getPaymentsToAcoount(Account account) {
        validate(account);
        
        if(account.getId() == null) {
            throw new IllegalArgumentException("Null id of account in getPaymentsToAccount");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "SELECT * FROM payment WHERE toAccount = ?")) {
            
            st.setLong(1, account.getId());
            ResultSet rs = st.executeQuery();
            List<Payment> toReturn = new ArrayList<>();
            
            while(rs.next()) {
                toReturn.add(resultSetToPayment(rs));
            }
            
            return toReturn;
            
        } catch (SQLException ex) {
            throw new ServiceFailureException("Error while getting all payment "
                                              + "to account " + account,ex);
        }
    }
    
    private void validate(Account account) throws IllegalArgumentException {
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
