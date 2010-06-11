package dr.evomodelxml.substmodel;

import dr.xml.*;
import dr.evolution.datatype.Microsatellite;
import dr.inference.model.Parameter;
import dr.evomodel.substmodel.MsatAveragingSubsetModel;

import java.util.HashMap;
import java.util.ArrayList;

/**
 * @author Chieh-Hsi Wu
 *
 * Parser of MsatAveragingSubsetModel
 */
public class MsatAveragingSubsetModelParser extends AbstractXMLObjectParser{

    public static final String MODELS = "models";
    public static final String MODEL = "model";
    public static final String BINARY = "binary";
    public static final String CODE = "code";
    public static final String LOGIT = "logit";
    public static final String RATE_PROPS = "rateProps";
    public static final String RATE_PROP = "rateProp";
    public static final String BIAS_CONSTS = "biasConsts";
    public static final String BIAS_CONST = "biasConst";
    public static final String BIAS_LINS = "biasLins";
    public static final String BIAS_LIN = "biasLin";
    public static final String GEOS = "geos";
    public static final String GEO = "geo";
    public static final String IN_MODELS = "inModels";
    public static final String MODEL_CHOOSE = "modelChoose";
    public static final String MODEL_INDICATOR = "modelIndicator";
    public static final String MSAT_AVG_SUBSET_MODEL = "msatAvgSubsetModel";
    public static final int PROP_RATES_MAX_COUNT = 6;
    public static final int BIAS_CONST_MAX_COUNT = 8;
    public static final int BIAS_LIN_MAX_COUNT = 4;
    public static final int GEO_MAX_COUNT = 6;



    public String getParserName(){
        return MSAT_AVG_SUBSET_MODEL;
    }


    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        //get microsatellite data type
        Microsatellite dataType = (Microsatellite)xo.getChild(Microsatellite.class);

        //whether mutational bias is in logit space
        boolean logit = xo.getAttribute(LOGIT,true);

        XMLObject modelsXO = xo.getChild(MODELS);
        int modelCount = modelsXO.getChildCount();

        HashMap<Integer, Integer> modelBitIndMap = new HashMap<Integer, Integer>(modelCount);
        for(int i = 0; i < modelCount; i++){

            XMLObject modelXO = (XMLObject)modelsXO.getChild(i);
            String bitVec = modelXO.getStringAttribute(BINARY);
            int bitVecVal = Integer.parseInt(bitVec,2);
            int modelCode = modelXO.getIntegerAttribute(CODE);
            modelBitIndMap.put(bitVecVal,modelCode);

        }

        Parameter[][] paramModelMap = new Parameter[4][modelCount];



        XMLObject propRatesXO = xo.getChild(RATE_PROPS);
        ArrayList<Parameter> rateProps =
                processParameters(
                        propRatesXO,
                        paramModelMap,
                        MsatAveragingSubsetModel.PROP_INDEX
                );

        XMLObject biasConstsXO = xo.getChild(BIAS_CONSTS);
        ArrayList<Parameter> biasConsts =
                processParameters(
                        biasConstsXO,
                        paramModelMap,
                        MsatAveragingSubsetModel.BIAS_CONST_INDEX
                );

        XMLObject biasLinsXO = xo.getChild(BIAS_LINS);
        ArrayList<Parameter> biasLins =
                processParameters(
                        biasLinsXO,
                        paramModelMap,
                        MsatAveragingSubsetModel.BIAS_LIN_INDEX
                );

        XMLObject geosXO = xo.getChild(GEOS);
        ArrayList<Parameter> geos =
                processParameters(
                        geosXO,
                        paramModelMap,
                        MsatAveragingSubsetModel.GEO_INDEX
                );

        Parameter modelChoose = (Parameter) xo.getElementFirstChild(MODEL_CHOOSE);
        Parameter modelIndicator = (Parameter) xo.getElementFirstChild(MODEL_INDICATOR);

        printParameters(paramModelMap);
        return new MsatAveragingSubsetModel(
                dataType,
                logit,
                rateProps,
                biasConsts,
                biasLins,
                geos,
                paramModelMap,
                modelChoose,
                modelIndicator,
                modelBitIndMap
        );
    }

    public ArrayList<Parameter> processParameters(
            XMLObject paramsXO,
            Parameter[][] paramModelMap,
            int paramIndex)throws XMLParseException{

        ArrayList<Parameter> paramList = new ArrayList<Parameter>();
        int paramsCount = paramsXO.getChildCount();

        for(int i = 0; i < paramsCount; i++){

            XMLObject paramXO = (XMLObject) paramsXO.getChild(i);
            int[] inModels = paramXO.getIntegerArrayAttribute(IN_MODELS);
            Parameter param = (Parameter)paramXO.getChild(Parameter.class);

            for(int j = 0; j < inModels.length; j++){
                if(paramModelMap[paramIndex][inModels[j]] == null){
                    paramModelMap[paramIndex][inModels[j]] = param;
                }else{
                    throw new RuntimeException("Different objects cannot be assigned to the same parameter in a model");
                }
            }

            paramList.add(param);

        }
        return paramList;
    }

    public void printParameters(Parameter[][] paramModelMap){
        for(int i = 0; i < paramModelMap.length; i++){
            for(int j = 0; j < paramModelMap[i].length; j++){
                System.out.print(paramModelMap[i][j]+" ");
            }
            System.out.println();
        }
    }




    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
            new ElementRule(Microsatellite.class),
            AttributeRule.newBooleanRule(LOGIT,true),
            new ElementRule(
                    MODELS,
                    new XMLSyntaxRule[]{
                            new ElementRule(
                                    MODEL,
                                    new XMLSyntaxRule[]{
                                            AttributeRule.newStringRule(BINARY),
                                            AttributeRule.newStringRule(CODE)
                                    },
                                    1,
                                    12
                            )
                    }
            ),
            new ElementRule(
                    RATE_PROPS,
                    new XMLSyntaxRule[]{
                        new ElementRule(
                                RATE_PROP,
                                new XMLSyntaxRule[]{
                                        AttributeRule.newIntegerArrayRule(IN_MODELS, false),
                                        new ElementRule(Parameter.class)
                                },
                                1,
                                6
                        )
                    }
            ),
            new ElementRule(
                    BIAS_CONSTS,
                    new XMLSyntaxRule[]{
                        new ElementRule(
                                BIAS_CONST,
                                new XMLSyntaxRule[]{
                                        AttributeRule.newIntegerArrayRule(IN_MODELS, false),
                                        new ElementRule(Parameter.class)
                                },
                                1,
                                8
                        )
                    }
            ),
            new ElementRule(
                    BIAS_LINS,
                    new XMLSyntaxRule[]{
                        new ElementRule(
                                BIAS_LIN,
                                new XMLSyntaxRule[]{
                                        AttributeRule.newIntegerArrayRule(IN_MODELS, false),
                                        new ElementRule(Parameter.class)
                                },
                                1,
                                4
                        )
                    }
            ),
            new ElementRule(
                    GEOS,
                    new XMLSyntaxRule[]{
                        new ElementRule(
                                GEO,
                                new XMLSyntaxRule[]{
                                        AttributeRule.newIntegerArrayRule(IN_MODELS, false),
                                        new ElementRule(Parameter.class)
                                },
                                1,
                                6
                        )
                    }
            ),
            new ElementRule(MODEL_CHOOSE, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
            new ElementRule(MODEL_INDICATOR, new XMLSyntaxRule[]{new ElementRule(Parameter.class)})

    };

    public String getParserDescription() {
        return "This element represents an instance of the Microsatellite Averaging Model of microsatellite evolution.";
    }

    public Class getReturnType(){
        return MsatAveragingSubsetModel.class;
    }
}
