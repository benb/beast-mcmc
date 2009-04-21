package dr.app.beauti;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * @author Andrew Rambaut
* @version $Id$
*/
public class ComboBoxRenderer extends JComboBox implements TableCellRenderer {
    public ComboBoxRenderer() {
        super();
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        if (isSelected) {
            this.setForeground(table.getSelectionForeground());
            this.setBackground(table.getSelectionBackground());
        } else {
            this.setForeground(table.getForeground());
            this.setBackground(table.getBackground());
        }

        if (value != null) {
            removeAllItems();
            addItem(value);
        }
        return this;
    }

    public void revalidate() {
    }

}
