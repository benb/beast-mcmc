package dr.inference.model;

import dr.xml.*;
import dr.evolution.datatype.DataType;
import dr.evomodel.substmodel.FrequencyModel;
import dr.inference.distribution.GeneralizedLinearModel;

/**
 * @author Marc A. Suchard
 */

public class MaskedParameter extends Parameter.Abstract implements ParameterListener {

    public static final String MASKED_PARAMETER = "maskedParameter";
    public static final String MASKING = "mask";

    public MaskedParameter(Parameter parameter) {
        this.parameter = parameter;
        this.map = new int[parameter.getDimension()];
        for(int i=0; i<map.length; i++)
            map[i] = i;
        int length = map.length;
    }

    public void addMask(Parameter maskParameter) {
        int index = 0;
        for(int i=0; i<maskParameter.getDimension(); i++) {
            final int maskValue = (int) maskParameter.getParameterValue(i);
            if (maskValue == 1) {
                map[index] = i;
                index++;
            }
        }
        length = index;
    }

    public int getDimension() {
        return length;
    }

    protected void storeValues() {
        parameter.storeParameterValues();
    }

    protected void restoreValues() {
        parameter.restoreParameterValues();
    }

    protected void acceptValues() {
       parameter.acceptParameterValues();
    }

    protected void adoptValues(Parameter source) {
        parameter.adoptParameterValues(source);
    }

    public double getParameterValue(int dim) {
        return parameter.getParameterValue(map[dim]);
    }

    public void setParameterValue(int dim, double value) {
        parameter.setParameterValueQuietly(map[dim],value);
    }

    public void setParameterValueQuietly(int dim, double value) {
        parameter.setParameterValueQuietly(map[dim],value);
    }

    public String getParameterName() {
        return "masked" + parameter.getParameterName();
    }

    public void addBounds(Bounds bounds) {
        parameter.addBounds(bounds);
    }

    public Bounds getBounds() {
        return parameter.getBounds();
    }

    public void addDimension(int index, double value) {
        throw new RuntimeException("Not yet implemented.");
    }

    public double removeDimension(int index) {
        throw new RuntimeException("Not yet implemented.");
    }

    public void parameterChangedEvent(Parameter parameter, int index, ChangeType type) {
//       todo !!!
    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter parameter = (Parameter) xo.getChild(Parameter.class);
            XMLObject cxo = (XMLObject) xo.getChild(MASKING);
            Parameter mask = (Parameter) cxo.getChild(Parameter.class);

            if (mask.getDimension() != parameter.getDimension())
                throw new XMLParseException("dim(" + parameter.getId() + ") != dim(" + mask.getId() + ")");

            MaskedParameter maskedParameter = new MaskedParameter(parameter);
            maskedParameter.addMask(mask);

            return maskedParameter;
        }

       public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(Parameter.class),
                new ElementRule(MASKING,
                        new XMLSyntaxRule[] {
                                new ElementRule(Parameter.class)
                        }),
        };

        public String getParserDescription() {
            return "A masked parameter.";
        }

        public Class getReturnType() {
            return Parameter.class;
        }

        public String getParserName() {
            return MASKED_PARAMETER;
        }
    };

    private Parameter parameter;
    private int[] map;
    private int length;

}
