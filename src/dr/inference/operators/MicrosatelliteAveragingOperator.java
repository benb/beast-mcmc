package dr.inference.operators;

import dr.inference.model.Parameter;
import dr.xml.*;

/**
 * @author Chieh-Hsi Wu
 *
 *  This operator performs bitflip operation on the bit vector representing the model.
 * 
 */
public class MicrosatelliteAveragingOperator extends SimpleMCMCOperator{
    private Parameter parameter;
    private Parameter dependencies;
    public static final int PRESENT = 1;
    public static final int ABSENT = 0;
    public static final int NO_DEPENDENCY = -1;
    public static final String MODEL_CHOOSE = "modelChoose";
    public static final String DEPENDENCIES = "dependencies";


    public MicrosatelliteAveragingOperator(Parameter parameter, Parameter dependencies, double weight){
        this.parameter = parameter;
        this.dependencies = dependencies;
        if(parameter.getDimension() != dependencies.getDimension())
            throw new RuntimeException("Dimenension of the parameter ("+parameter.getDimension()+
                    ") does not equal to the dimension of the dependencies parameter("+dependencies.getDimension()+").");
        setWeight(weight);
    }

    public String getOperatorName(){
        return "msatModelSwitch(" + parameter.getParameterName() + ")";
    }

    public double doOperation() throws OperatorFailedException{

        double logq = 0.0;
        double[] bitVec = new double[parameter.getDimension()];
        for(int i = 0; i < bitVec.length; i++){
            bitVec[i] = parameter.getParameterValue(i);
        }
        int index = (int)(Math.random()*parameter.getDimension());
        int oldVal  = (int)parameter.getParameterValue(index);
        int newVal = -1;
        if(oldVal == ABSENT){
            newVal = PRESENT;
        }else if(oldVal == PRESENT){
           newVal = ABSENT;
        }else{
            throw new RuntimeException("The parameter can only take values 0 or 1.");
        }
        bitVec[index] = newVal;
        for(int i = 0; i < bitVec.length; i++){
            int dependentInd = (int)dependencies.getParameterValue(i);
            if(dependentInd > NO_DEPENDENCY){
                if(bitVec[dependentInd] == ABSENT && bitVec[i]==PRESENT){
                    throw new OperatorFailedException("");
                    //newVal = oldVal;
                }
            }

        }
        parameter.setParameterValue(index, newVal);

        return logq;
    }

    public final String getPerformanceSuggestion() {
        return "no suggestions available";
    }

    public static dr.xml.XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return "msatModelSwitchOperator";
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double weight = xo.getDoubleAttribute(WEIGHT);
            Parameter modelChoose = (Parameter) xo.getElementFirstChild(MODEL_CHOOSE);
            Parameter dependencies = (Parameter)xo.getElementFirstChild(DEPENDENCIES);

            return new MicrosatelliteAveragingOperator(modelChoose, dependencies, weight);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns a microsatellite averaging operator on a given parameter.";
        }

        public Class getReturnType() {
            return MCMCOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(WEIGHT),
                new ElementRule(MODEL_CHOOSE, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(DEPENDENCIES, new XMLSyntaxRule[]{new ElementRule(Parameter.class)})
        };

    };

}
