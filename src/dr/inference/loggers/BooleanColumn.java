package dr.inference.loggers;

/**
 *
 * Boolean formating (C style) 0 is false, anything else true;
 * @author joseph
 *         Date: 4/06/2008
 */

public class BooleanColumn implements LogColumn {
    private NumberColumn column;

    public BooleanColumn(NumberColumn column) {
        this.column = column;
    }

    public void setLabel(String label) {
        column.setLabel(label);
    }

    public String getLabel() {
        return column.getLabel();
    }

    public void setMinimumWidth(int minimumWidth) {
        // ignore
    }

    public int getMinimumWidth() {
        return "false".length();
    }

    public String getFormatted() {
        if( column.getDoubleValue() == 0.0 ) {
            return "false";
        }
        return "true";
    }
}
