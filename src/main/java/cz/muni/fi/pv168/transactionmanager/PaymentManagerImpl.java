package cz.muni.fi.pv168.transactionmanager;

import cz.muni.fi.pv168.utils.AccountHelper;
import cz.muni.fi.pv168.utils.EntityNotFoundException;
import cz.muni.fi.pv168.utils.ServiceFailureException;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Class which implements Payment Manager. Supports creating, updating, getting 
 * and deleting payments from manager.
 * @author Miroslav Kubus
 */
public class PaymentManagerImpl implements PaymentManager {

    private static final Logger logger = Logger.getLogger(PaymentManagerImpl.class.getName());
    private final DataSource dataSource;
    private final AccountHelper accountHelper;
    
    public PaymentManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
        accountHelper = new AccountHelper();
    }
    
    @Override
    public void createPayment(Payment payment) {
        logger.log(Level.INFO, "Creating new payment");
        
        validate(payment);
        if(payment.getId() != null) {
            throw new IllegalArgumentException("Payment ID is already set");
        }
                
        try ( Connection connection = dataSource.getConnection();
              PreparedStatement updateFromAcc = connection.prepareStatement("UPDATE account SET balance = ? WHERE id = ?");
              PreparedStatement updateToAcc = connection.prepareStatement("UPDATE account SET balance = ? WHERE id = ?");
              PreparedStatement st = connection.prepareStatement("INSERT INTO payment (fromAccount,toAccount,amount,date) VALUES (?,?,?,?)",
              Statement.RETURN_GENERATED_KEYS)) {
                       
            changeAmountOfFromAccount(updateFromAcc, payment, null);
            changeAmountOfToAccount(updateToAcc, payment, null);
                   
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
            logger.log(Level.SEVERE, "Error while creating new payment", ex);
            throw new ServiceFailureException("Error when inserting payment " + payment + ex.getLocalizedMessage());
        }
    }
    
    private void validate(Payment payment) throws IllegalArgumentException {
        logger.log(Level.INFO, "Validation of payment{0}", payment);
        
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
        logger.log(Level.INFO, "Setting key of new payment");
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
        logger.log(Level.INFO, "Updating payment{0}", payment);
        
        validate(payment);
        
        if(payment.getId() == null) {
            throw new IllegalArgumentException("Null id of payment to update");
        }
        
        Payment paymentBeforeUpdate = getPaymentByID(payment.getId());

        
        try(Connection connection = dataSource.getConnection()) {
                 
            connection.setAutoCommit(false);

            try {
                if((payment.getFrom().equals(paymentBeforeUpdate.getFrom()) && payment.getTo().equals(paymentBeforeUpdate.getTo()))) {
                
                    try(PreparedStatement st = connection.prepareStatement("UPDATE payment SET amount = ?, date = ? WHERE id = ?")) {
                        executeUpdateOfPayment(st, payment);
                    }

                    try(PreparedStatement st = connection.prepareStatement("UPDATE account SET balance = ? WHERE id = ?")) {
                        changeAmountOfFromAccount(st, payment, paymentBeforeUpdate);
                    }

                    try(PreparedStatement st = connection.prepareStatement("UPDATE account SET balance = ? WHERE id = ?")) {
                        changeAmountOfToAccount(st, payment, paymentBeforeUpdate);
                    }
                
                }
                
                connection.commit();                
            } catch (Exception ex) {
                connection.rollback();
                logger.log(Level.SEVERE, "Error while updating payment " + payment , ex);
                throw new ServiceFailureException("Error while updating payment " + payment, ex);                
            }
                        
        } catch(SQLException ex) {
            logger.log(Level.SEVERE, "Connection error while updating payment " + payment, ex);
            throw new ServiceFailureException("Error while updating payment " + payment, ex);
        }
    }
    
    private static void executeUpdateOfPayment(PreparedStatement st, Payment payment) throws SQLException {
        logger.log(Level.INFO, "Executing update of payment{0}", payment);
        st.setBigDecimal(1, payment.getAmount());
        st.setDate(2, Date.valueOf(payment.getDate()));
        st.setLong(3, payment.getId());
            
        int updated = st.executeUpdate();
            
        if(updated == 0) {
            throw new EntityNotFoundException("No payment " + payment + " in database");
        } 
            
        if(updated != 1) {
            throw new ServiceFailureException("Invalid updated rows count detected "
                                        + "(one row should be updated): " + updated);
        }
    }
    
    private static void changeAmountOfFromAccount(PreparedStatement updateFromAccSt, Payment payment, Payment paymentBeforeUpdate) throws SQLException {
        logger.log(Level.INFO, "Changing amount of from account of payment{0}", payment);
        
        if(paymentBeforeUpdate != null) {
            BigDecimal help = paymentBeforeUpdate.getFrom().getBalance().add(paymentBeforeUpdate.getAmount());
            updateFromAccSt.setBigDecimal(1, help.subtract(payment.getAmount()));
        } else {
            updateFromAccSt.setBigDecimal(1, payment.getFrom().getBalance().subtract(payment.getAmount()));
        }
        
        updateFromAccSt.setLong(2, payment.getFrom().getId());
        int updated = updateFromAccSt.executeUpdate();
         
        if(updated == 0) {
            throw new EntityNotFoundException("No account " + payment.getFrom() + " in database");
        } 
            
        if(updated != 1) {
            throw new ServiceFailureException("Invalid updated rows count detected "
                                        + "(one row should be updated): " + updated);
        }
    }
    
    private static void changeAmountOfToAccount(PreparedStatement updateToAccSt, Payment payment, Payment paymentBeforeUpdate) throws SQLException {
        logger.log(Level.INFO, "Changing amount of to account of payment{0}", payment);

        if(paymentBeforeUpdate != null) {
            BigDecimal help = paymentBeforeUpdate.getTo().getBalance().subtract(paymentBeforeUpdate.getAmount());
            updateToAccSt.setBigDecimal(1, help.add(payment.getAmount()));
        } else {
            updateToAccSt.setBigDecimal(1, payment.getTo().getBalance().add(payment.getAmount()));
        }
        
        updateToAccSt.setLong(2, payment.getTo().getId());
            
        int updated = updateToAccSt.executeUpdate();
        
        if(updated == 0) {
            throw new EntityNotFoundException("No account " + payment.getTo() + " in database");
        } 
            
        if(updated != 1) {
            throw new ServiceFailureException("Invalid updated rows count detected "
                                        + "(one row should be updated): " + updated);
        }
    }

    @Override
    public void deletePayment(Payment payment) {
        logger.log(Level.INFO, "Deleting payment{0}", payment);

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
            logger.log(Level.SEVERE, "Error while deleting payment", ex);
            throw new ServiceFailureException("Error while deleting payment " + payment,ex);
        }
    }       
    //Accounty v jednom SQL dotaze
    @Override
    public Payment getPaymentByID(Long id) {
        logger.log(Level.INFO, "Getting payment by ID:{0}", id);

        if(id == null) {
            throw new IllegalArgumentException("Null id of payment in getPaymentByID");
        }
        
        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(
                "SELECT * FROM payment WHERE id = ?")) {
            
            st.setLong(1,id);
            ResultSet rs = st.executeQuery();
            
            if(rs.next()) {
                Payment payment = resultSetToPayment(connection, rs);
                
                if (rs.next()) {
                    throw new ServiceFailureException("Internal error: More entities with the same id found "
                            + "(source id: " + id + ", found " + payment + " and " + resultSetToPayment(connection, rs));
                }
                
                return payment;
            } else {
                return null;
            }   
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while getting payment with ID " + id, ex);
            throw new ServiceFailureException("Error while getting payment with id " + id, ex);
        }
    }
    
    private Payment resultSetToPayment(Connection connection, ResultSet rs) throws SQLException {
        logger.log(Level.INFO, "Transforming ResultSet to payment");
        Payment payment = setAttributeOfPayment(rs);
        payment.setFrom(loadAccountOfPayment(connection,rs.getLong("fromAccount")));
        payment.setTo(loadAccountOfPayment(connection,rs.getLong("toAccount")));
        
        return payment;
    }
    
    private Payment setAttributeOfPayment(ResultSet rs) throws SQLException {
        logger.log(Level.INFO, "Setting of attributes of payment from ResultSet");
        Payment payment = new Payment();
        
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setId(rs.getLong("id"));
        payment.setDate(rs.getDate("date").toLocalDate());
        
        return payment;
    }
    
    private Account loadAccountOfPayment(Connection connection, Long idOfAccount) {
        logger.log(Level.INFO, "Loading account of payment from database");

        if (idOfAccount == null) {
            throw new IllegalArgumentException("Null id of account when retrieving account from database!");
        }
        
        try( PreparedStatement st = connection.prepareStatement
             ("SELECT id, balance, holder, number FROM account WHERE id = ?")) {
            
            st.setLong(1, idOfAccount);
            ResultSet rs = st.executeQuery();
            
            if(rs.next()) {
                
                Account account = accountHelper.resultSetToAccount(rs);
       
                if(rs.next()) {
                    throw new IllegalArgumentException("Too much accounts with ID " + idOfAccount);
                }
                
                return account;
            } else {
                throw new IllegalArgumentException("No account with ID " + idOfAccount);
            }
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while loading account of payment with ID " + idOfAccount, ex);
            throw new ServiceFailureException("Error while retrieving account with ID " + idOfAccount + ex);
        }
    }

    @Override
    public List<Payment> getAllPayments() {
        logger.log(Level.INFO, "Getting all payments from database");

        try( Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement("SELECT * FROM payment")) {
            List<Payment> toReturn = new ArrayList<>();
            ResultSet rs = st.executeQuery();
            
            while(rs.next()) {
                toReturn.add(resultSetToPayment(connection,rs));
            }
            
            return toReturn;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while getting all payments", ex);
            throw new ServiceFailureException("Error while getting all payments from database", ex);
        }
    }

    @Override
    public List<Payment> getPaymentsFromAccount(Account account) {
        logger.log(Level.INFO, "Getting all payment with same from account");
        accountHelper.validate(account);
        
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
                toReturn.add(resultSetToPayment(connection,rs));
            }
            
            return toReturn;
        } catch(SQLException ex) {
            logger.log(Level.SEVERE, "Error while getting all payments from same account", ex);
            throw new ServiceFailureException("Error while getting all payment "
                                              + "from account " + account,ex);
        }
    }

    @Override
    public List<Payment> getPaymentsToAcoount(Account account) {
        logger.log(Level.INFO, "Getting all payment with same to account");

        accountHelper.validate(account);
        
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
                toReturn.add(resultSetToPayment(connection,rs));
            }
            
            return toReturn;
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error while getting all payment to same account", ex);
            throw new ServiceFailureException("Error while getting all payment "
                                              + "to account " + account,ex);
        }
    }    
}
