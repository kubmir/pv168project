package cz.muni.fi.pv168.transactionmanager.swing;

import cz.muni.fi.pv168.transactionmanager.Account;
import cz.muni.fi.pv168.transactionmanager.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Miroslav Kubus
 */
public class PaymentTableModel extends AbstractTableModel {

    private List<Payment> payments = new ArrayList<>();
        
    //Co je lepsie ... casova zlozitost kvoli ziskavaniu dat z DB alebo duplicita dat ??
    //Cize 27.riadok tak ako ho mam vs. acconts.add(account);
    public void addPayment(Payment payment) {
        int lastRowIndex = payments.size();
        payments.add(payment);
        fireTableRowsInserted(lastRowIndex, lastRowIndex);
    }
    
    @Override
    public int getRowCount() {
        return payments.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Payment payment = payments.get(rowIndex);
        switch(columnIndex) {
            case 0:
                return payment.getId();
            case 1:
                return payment.getFrom().getNumber();
            case 2: 
                return payment.getTo().getNumber();
            case 3:
                return payment.getAmount();
            case 4:
                return payment.getDate();
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Id";
            case 1:
                return "From";
            case 2:
                return "To";
            case 3:
                return "Amount";
            case 4:
                return "Date";
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
                return Account.class;
            case 3:
                return BigDecimal.class;
            case 4:
                return LocalDate.class;
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Payment payment = payments.get(rowIndex);
        switch (columnIndex) {
            case 0:
                throw new IllegalArgumentException("Update of ID is not allowed");
            case 1:
                payment.setFrom((Account) aValue);
                break;
            case 2:
                payment.setTo((Account) aValue);
                break;
            case 3:
                payment.setAmount((BigDecimal) aValue);
                break;
            case 4:
                payment.setDate((LocalDate) aValue);
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
            case 4:
                return false;
            default:
                throw new IllegalArgumentException("ColumnIndex out of numbers of columns");
        }
    }
    
    public void removeRow(int rowNumber) {
        if(rowNumber < payments.size()) {
            payments.remove(rowNumber);
            fireTableRowsDeleted(rowNumber, rowNumber);
        }
    }
}