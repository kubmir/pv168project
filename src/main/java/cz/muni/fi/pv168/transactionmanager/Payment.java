package cz.muni.fi.pv168.transactionmanager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * This entity class represents Payment.
 * Payment has id, from Account, to Account, amout and date of payment
 * @author Miroslav Kubus
 */
public class Payment {
    private Long id;
    private Account from;
    private Account to;
    private BigDecimal amount;
    private LocalDate date;
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return this.id;
    }
    
    public void setFrom(Account from) {
        this.from = from;
    }
    
    public Account getFrom() {
        return this.from;
    }
    
    public void setTo(Account to) {
        this.to = to;
    }
    
    public Account getTo() {
        return this.to;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getAmount() {
        return this.amount;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public LocalDate getDate() {
        return this.date;
    }    
    
    @Override
    public String toString() {
        return "Payment{id = " + this.id + ",from account = " + this.from + ",to account = " 
                + this.to + ",amount = " + this.amount + ",date = " + this.date + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final Payment other = (Payment) obj;

        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        
        if (!Objects.equals(this.from, other.from)) {
            return false;
        }
        
        if (!Objects.equals(this.to, other.to)) {
            return false;
        }
        
        if (!Objects.equals(this.amount, other.amount)) {
            return false;
        }
        
        return Objects.equals(this.date, other.date);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.from);
        hash = 37 * hash + Objects.hashCode(this.to);
        hash = 37 * hash + Objects.hashCode(this.amount);
        hash = 37 * hash + Objects.hashCode(this.date);
        return hash;
    }
}
