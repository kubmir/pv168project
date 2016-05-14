package cz.muni.fi.pv168.transactionmanager.swing;

import cz.muni.fi.pv168.transactionmanager.Account;
import cz.muni.fi.pv168.transactionmanager.AccountManagerImpl;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.table.AbstractTableModel;

/**
 * Table model for account table
 * @author Miroslav Kubus
 */
public class AccountTableModel extends AbstractTableModel {

    private List<Account> accounts = new ArrayList<>();
    private AccountManagerImpl accountManager;
    
    public AccountTableModel(AccountManagerImpl accMan) {
        accountManager = accMan;
    }
    
    public void addAccount(Account account) {
        int lastRowIndex = accounts.size();
        accounts.add(account);
        fireTableRowsInserted(lastRowIndex, lastRowIndex);
    }
    
    public void updateAccounts() {
        int lastRowIndex = accounts.size();
        accounts = accountManager.getAllAccounts();
        fireTableRowsInserted(lastRowIndex, lastRowIndex);
    }
    
    @Override
    public int getRowCount() {
        return accounts.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Account account = accounts.get(rowIndex);
        switch(columnIndex) {
            case 0:
                return account.getId();
            case 1:
                return account.getNumber();
            case 2: 
                return account.getHolder();
            case 3:
                return account.getBalance();
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("ID");
            case 1:
                return java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("NUMBER");
            case 2:
                return java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("HOLDER");
            case 3:
                return java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("BALANCE");
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Long.class;
            case 1:
            case 2:
                return String.class;
            case 3:
                return BigDecimal.class;
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Account account = accounts.get(rowIndex);
        switch (columnIndex) {
            case 0:
                throw new IllegalArgumentException("Update of ID is not allowed");
            case 1:
                account.setNumber((String) aValue);
                break;
            case 2:
                account.setHolder((String) aValue);
                break;
            case 3:
                account.setBalance((BigDecimal) aValue);
                break;
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0: 
            case 1:
            case 2:
            case 3:
                return false;
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
    }
    
    public void removeRow(int rowNumber) {
        accounts.remove(rowNumber);
        fireTableRowsDeleted(rowNumber, rowNumber);
    }
}