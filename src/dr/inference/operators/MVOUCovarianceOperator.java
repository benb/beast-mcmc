package dr.inference.operators;

import dr.inference.model.MatrixParameter;
import dr.inference.model.Parameter;
import dr.math.WishartDistribution;
import dr.math.matrixAlgebra.Matrix;
import dr.xml.*;

/**
 * @author Marc Suchard
 */
public class MVOUCovarianceOperator extends AbstractCoercableOperator {

    public static final String MVOU_OPERATOR = "mvouOperator";
    public static final String MIXING_FACTOR = "mixingFactor";
    public static final String VARIANCE_MATRIX = "varMatrix";
    public static final String PRIOR_DF = "priorDf";

    private double mixingFactor;
    private MatrixParameter varMatrix;
    private int dim;

    private MatrixParameter precisionParam;
    private WishartDistribution priorDistribution;
    private int priorDf;
    private double[][] I;
    private Matrix Iinv;


    public MVOUCovarianceOperator(double mixingFactor,
                                  MatrixParameter varMatrix,
                                  int priorDf,
                                  double weight, CoercionMode mode) {
        super(mode);
        this.mixingFactor = mixingFactor;
        this.varMatrix = varMatrix;
        this.priorDf = priorDf;
        setWeight(weight);
        dim = varMatrix.getColumnDimension();
        I = new double[dim][dim];
        for (int i = 0; i < dim; i++)
            I[i][i] = 1.0;
//			I[i][i] = i;
        Iinv = new Matrix(I).inverse();
    }

    public double doOperation() throws OperatorFailedException {

        double[][] draw = WishartDistribution.nextWishart(priorDf, I);
//		double[][] good = varMatrix.getParameterAsMatrix();
//		double[][] saveOld = varMatrix.getParameterAsMatrix();

//		System.err.println("draw:\n"+new Matrix(draw));
        double[][] oldValue = varMatrix.getParameterAsMatrix();
        for (int i = 0; i < dim; i++) {
            Parameter column = varMatrix.getParameter(i);
            for (int j = 0; j < dim; j++)
                column.setParameterValue(j,
                        mixingFactor * oldValue[j][i] + (1.0 - mixingFactor) * draw[j][i]
                );

        }
//        varMatrix.fireParameterChangedEvent();
        // calculate Hastings ratio

//		System.err.println("oldValue:\n"+new Matrix(oldValue).toString());
//		System.err.println("newValue:\n"+new Matrix(varMatrix.getParameterAsMatrix()).toString());

        Matrix forwardDrawMatrix = new Matrix(draw);
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
//				saveOld[i][j] *= - mixingFactor;
//				saveOld[i][j] += varMatrix.getParameterValue(i,j);
//				saveOld[i][j] /= 1.0 - mixingFactor;
                oldValue[i][j] -= mixingFactor * varMatrix.getParameterValue(i, j);
                oldValue[i][j] /= 1.0 - mixingFactor;
            }
        }

//		double[][] saveNew = varMatrix.getParameterAsMatrix();

        Matrix backwardDrawMatrix = new Matrix(oldValue);

//		System.err.println("forward:\n"+forwardDrawMatrix);
//		System.err.println("backward:\n"+backwardDrawMatrix);

//		System.err.println("calc start");

//		if( Math.abs(backwardDrawMatrix.component(0,0) + 0.251) < 0.001 ) {
//			System.err.println("found:\n"+backwardDrawMatrix);
//
//			System.err.println("original:\n"+new Matrix(good));
//			System.err.println("draw:\n"+new Matrix(draw));
//			System.err.println("proposed:\n"+new Matrix(varMatrix.getParameterAsMatrix()));
//			System.err.println("mixing = "+mixingFactor);
//			System.err.println("back[0][0] = "+backwardDrawMatrix.component(0,0));
//			System.err.println("saveOld[0][0] = "+saveOld[0][0]);
//
//
//		}

        double bProb = WishartDistribution.logPdf(backwardDrawMatrix, Iinv, priorDf, dim,
//				WishartDistribution.computeNormalizationConstant(Iinv,priorDf,dim));
                0);

        if (bProb == Double.NEGATIVE_INFINITY)
            throw new OperatorFailedException("Not reversible");

        double fProb = WishartDistribution.logPdf(forwardDrawMatrix, Iinv, priorDf, dim,
//				WishartDistribution.computeNormalizationConstant(Iinv,priorDf,dim));
                0);

//		System.err.println("calc end");

//		if( fProb == Double.NEGATIVE_INFINITY ) {
//			System.err.println("forwards is problem");
//			System.exit(-1);
//		}

//		if( bProb == Double.NEGATIVE_INFINITY ) {
//			System.err.println("backwards is problem");
//			System.exit(-1);
//		}

//		System.err.println("fProb = "+fProb);
//		System.err.println("bProb = "+bProb);

//		System.exit(-1);

        return bProb - fProb;
    }

    //MCMCOperator INTERFACE
    public final String getOperatorName() {
        return MVOU_OPERATOR + "(" +
                varMatrix.getId() + ")";
    }

    public double getCoercableParameter() {
        return Math.log(mixingFactor / (1.0 - mixingFactor));
//		return Math.log((1.0 - mixingFactor) / mixingFactor);
    }

    public void setCoercableParameter(double value) {
        mixingFactor = Math.exp(value) / (1.0 + Math.exp(value));
//		mixingFactor = Math.exp(-value) / (1.0 + Math.exp(-value));
    }

    public double getRawParameter() {
        return mixingFactor;
    }

    public double getMixingFactor() {
        return mixingFactor;
    }

    public double getTargetAcceptanceProbability() {
        return 0.234;
    }

    public double getMinimumAcceptanceLevel() {
        return 0.1;
    }

    public double getMaximumAcceptanceLevel() {
        return 0.4;
    }

    public double getMinimumGoodAcceptanceLevel() {
        return 0.20;
    }

    public double getMaximumGoodAcceptanceLevel() {
        return 0.30;
    }

    public final String getPerformanceSuggestion() {

        double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
        double targetProb = getTargetAcceptanceProbability();
        dr.util.NumberFormatter formatter = new dr.util.NumberFormatter(5);
        double sf = OperatorUtils.optimizeWindowSize(mixingFactor, prob, targetProb);
        if (prob < getMinimumGoodAcceptanceLevel()) {
            return "Try setting mixingFactor to about " + formatter.format(sf);
        } else if (prob > getMaximumGoodAcceptanceLevel()) {
            return "Try setting mixingFactor to about " + formatter.format(sf);
        } else return "";
    }

    public static dr.xml.XMLObjectParser PARSER = new dr.xml.AbstractXMLObjectParser() {

        public String getParserName() {
            return MVOU_OPERATOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            CoercionMode mode = CoercionMode.parseMode(xo);
            double weight = xo.getDoubleAttribute(WEIGHT);
            double mixingFactor = xo.getDoubleAttribute(MIXING_FACTOR);
            int priorDf = xo.getIntegerAttribute(PRIOR_DF);

            if (mixingFactor <= 0.0 || mixingFactor >= 1.0) {
                throw new XMLParseException("mixingFactor must be greater than 0.0 and less thatn 1.0");
            }

//            Parameter parameter = (Parameter) xo.getChild(Parameter.class);

//            XMLObject cxo = (XMLObject) xo.getChild(VARIANCE_MATRIX);
            MatrixParameter varMatrix = (MatrixParameter) xo.getChild(MatrixParameter.class);

            // Make sure varMatrix is square and dim(varMatrix) = dim(parameter)

            if (varMatrix.getColumnDimension() != varMatrix.getRowDimension())
                throw new XMLParseException("The variance matrix is not square");

//            if (varMatrix.getColumnDimension() != parameter.getDimension())
//                throw new XMLParseException("The parameter and variance matrix have differing dimensions");

            return new MVOUCovarianceOperator(mixingFactor, varMatrix, priorDf, weight, mode);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns junk.";
        }

        public Class getReturnType() {
            return MCMCOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(MIXING_FACTOR),
                AttributeRule.newIntegerRule(PRIOR_DF),
                AttributeRule.newDoubleRule(WEIGHT),
                AttributeRule.newBooleanRule(AUTO_OPTIMIZE, true),
//                new ElementRule(Parameter.class),
//                new ElementRule(VARIANCE_MATRIX,
//                        new XMLSyntaxRule[]{new ElementRule(MatrixParameter.class)}),

                new ElementRule(MatrixParameter.class)

        };

    };
}
