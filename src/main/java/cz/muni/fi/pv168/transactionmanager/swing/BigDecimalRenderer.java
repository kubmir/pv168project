package cz.muni.fi.pv168.transactionmanager.swing;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Display a <tt>Number</tt> in a table cell in the format defined by
 * {@link NumberFormat#getCurrencyInstance()}, and aligned to the right.
 */
final class BigDecimalRenderer extends DefaultTableCellRenderer {

    BigDecimalRenderer() {
//        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public void setValue(Object aValue) {
        Object result = aValue;
        if ((aValue != null) && (aValue instanceof BigDecimal)) {            
            BigDecimal numberValue = (BigDecimal) aValue;
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
//            Currency eur = Currency.getInstance("EUR");            //TODO fixna currency cez vsetky locale alebo nie?
//            formatter.setCurrency(eur);
            result = formatter.format(numberValue);
        }
        super.setValue(result);
    }
}
