package cz.muni.fi.pv168.transactionmanager;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * This entity class represents Account.
 * Account has some number, holder and balance
 * 
 * @author Viktória Tóthová
 */
public class Account {
    private Long id;
    private String number;
    private String holder;
    private BigDecimal balance;

    public Account() {
        this.id = null;
        this.number = null;
        this.holder = null;
        this.balance = null;
    }
    
    public Account(String number, String holder, BigDecimal balance) {
        this.id = null;
        this.number = number;
        this.balance = balance;
        this.holder = holder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Account{" + "id=" + id + ", number=" + number + ", holder=" + holder + ", balance=" + balance + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.id);
        hash = 67 * hash + Objects.hashCode(this.number);
        hash = 67 * hash + Objects.hashCode(this.holder);
        hash = 67 * hash + Objects.hashCode(this.balance);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Account other = (Account) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.number, other.number)) {
            return false;
        }
        if (!Objects.equals(this.holder, other.holder)) {
            return false;
        }
        if (this.balance.compareTo(other.balance) != 0) {
            return false;
        }
        return true;
    }
}
