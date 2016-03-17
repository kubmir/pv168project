/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.transactionmanager;

import cz.muni.fi.pv168.transactionmanager.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Class which represent one payment
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
}
