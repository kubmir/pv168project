package cz.muni.fi.pv168.transactionmanager.swing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author artemis
 */
public class DateRenderer extends DefaultTableCellRenderer{
    
    @Override
    public void setValue(Object aValue) {
        Object result = aValue;
        if ((aValue != null) && (aValue instanceof LocalDate)) {
            LocalDate localDate = (LocalDate) aValue;
            DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
            result = dateFormat.format(localDate);
        }
        super.setValue(result);
    }
    
}
