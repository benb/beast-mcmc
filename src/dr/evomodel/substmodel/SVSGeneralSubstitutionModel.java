package dr.evomodel.substmodel;

import dr.evolution.datatype.*;
import dr.inference.model.Parameter;
import dr.xml.*;

/**
 * <b>A general model of sequence substitution with stochastic variable selection</b>. A general reversible class for any
 * data type.
 *
 * @author Marc Suchard
 * @version $Id: SVSGeneralSubstitutionModel.java,v 1.37 2006/05/05 03:05:10 msuchard Exp $
 */

public class SVSGeneralSubstitutionModel extends GeneralSubstitutionModel {

	public static final String SVS_GENERAL_SUBSTITUTION_MODEL = "svsGeneralSubstitutionModel";
	public static final String INDICATOR = "rateIndicator";


	public SVSGeneralSubstitutionModel(DataType dataType, FrequencyModel freqModel, Parameter parameter,
	                                   Parameter indicator) { //, int relativeTo) {
		super(dataType, freqModel, parameter, 1);

		rateIndicator = indicator;
		addParameter(rateIndicator);
	}

	protected SVSGeneralSubstitutionModel(String name, DataType dataType, FrequencyModel freqModel, int relativeTo) {
		super(name, dataType, freqModel, relativeTo);
	}

	public Parameter getRateIndicators() {
		return rateIndicator;
	}

	public boolean myIsValid() {
		boolean valid = true;
		//	setupMatrix();
		updateMatrix = true;
		int stateCount = dataType.getStateCount();
		int stateCountSquare = stateCount * stateCount;
		double[] probs = new double[stateCountSquare];
		getTransitionProbabilities(1.0, probs);
		for (int i = 0; i < stateCountSquare; i++) {
			if (probs[i] == 0)
				valid = false; // must be fully connected
		}

//		if (!valid) {
//			double sum = 0;
//			for(int i=0;i<rateIndicator.getDimension();i++)
//				sum += rateIndicator.getParameterValue(i);
//			System.err.println("total = "+sum);
//			for(int i=0;i<stateCount;i++) {
//				for(int j=0; j<stateCount;j++)
//					System.err.printf(" %5.3f",probs[i*stateCount+j]);
//				System.err.println("");
//			}
//			System.err.println("");
//		}

		return valid;
	}

	protected void setupRelativeRates() {

//		boolean allZero = true;

		for (int i = 0; i < relativeRates.length; i++) {
//			if (i == ratesRelativeTo) {
//				relativeRates[i] = 1.0;
//			} else if (i < ratesRelativeTo) {
//				relativeRates[i] = ratesParameter.getParameterValue(i) * rateIndicator.getParameterValue(i);
//			} else {
//				relativeRates[i] = ratesParameter.getParameterValue(i - 1) * rateIndicator.getParameterValue(i - 1);
//			}
			relativeRates[i] = ratesParameter.getParameterValue(i) * rateIndicator.getParameterValue(i);

//			if (rateIndicator.getParameterValue(i) != 0.0)
//				allZero = false;
//
		}
//		if (allZero)
//			throw new RuntimeException("All rates went to zero");
//		fireModelChanged();
	}

//	public void restoreState() {
//		setupMatrix();
//		updateMatrix = true;
//		System.err.println("Restoring");
//		setupRelativeRates();
//		fireModelChanged();
//	}

	void normalize(double[][] matrix, double[] pi) {
		double subst = 0.0;
		int dimension = pi.length;

		final int dim = rateIndicator.getDimension();
		int sum = 0;
		for (int i = 0; i < dim; i++)
			sum += rateIndicator.getParameterValue(i);


		for (int i = 0; i < dimension; i++)
			subst += -matrix[i][i] * pi[i];

		for (int i = 0; i < dimension; i++) {
			for (int j = 0; j < dimension; j++) {
				matrix[i][j] = matrix[i][j] / subst; // / sum;
			}
		}
	}

	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() {
			return SVS_GENERAL_SUBSTITUTION_MODEL;
		}

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			Parameter ratesParameter;
			Parameter indicatorParameter;

			XMLObject cxo = (XMLObject) xo.getChild(FREQUENCIES);
			FrequencyModel freqModel = (FrequencyModel) cxo.getChild(FrequencyModel.class);

			DataType dataType = null;

			if (xo.hasAttribute(DataType.DATA_TYPE)) {
				String dataTypeStr = xo.getStringAttribute(DataType.DATA_TYPE);
				if (dataTypeStr.equals(Nucleotides.DESCRIPTION)) {
					dataType = Nucleotides.INSTANCE;
				} else if (dataTypeStr.equals(AminoAcids.DESCRIPTION)) {
					dataType = AminoAcids.INSTANCE;
				} else if (dataTypeStr.equals(Codons.DESCRIPTION)) {
					dataType = Codons.UNIVERSAL;
				} else if (dataTypeStr.equals(TwoStates.DESCRIPTION)) {
					dataType = TwoStates.INSTANCE;
				}
			}

			if (dataType == null) dataType = (DataType) xo.getChild(DataType.class);

			cxo = (XMLObject) xo.getChild(RATES);

//			int relativeTo = cxo.getIntegerAttribute(RELATIVE_TO) - 1;
//			if (relativeTo < 0) throw new XMLParseException(RELATIVE_TO + " must be 1 or greater");

			ratesParameter = (Parameter) cxo.getChild(Parameter.class);

			if (dataType != freqModel.getDataType()) {
				throw new XMLParseException("Data type of " + getParserName() + " element does not match that of its frequencyModel.");
			}

			int rateCount = ((dataType.getStateCount() - 1) * dataType.getStateCount()) / 2;

			if (ratesParameter == null) {

				if (rateCount == 1) {
					// simplest model for binary traits...
				} else {
					throw new XMLParseException("No rates parameter found in " + getParserName());
				}
			} else if (ratesParameter.getDimension() != rateCount) {
				throw new XMLParseException("Rates parameter in " + getParserName() + " element should have " + (rateCount) + " dimensions.");
			}

			cxo = (XMLObject) xo.getChild(INDICATOR);

			indicatorParameter = (Parameter) cxo.getChild(Parameter.class);

			if (indicatorParameter.getDimension() != ratesParameter.getDimension())
				throw new XMLParseException("Rates and indicator parameters in " + getParserName() + " element must be the same dimension.");


			return new SVSGeneralSubstitutionModel(dataType, freqModel, ratesParameter, indicatorParameter);//, relativeTo);
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "A general reversible model of sequence substitution for any data type with stochastic variable selection.";
		}

		public Class getReturnType() {
			return SubstitutionModel.class;
		}

		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
				new XORRule(
						new StringAttributeRule(DataType.DATA_TYPE, "The type of sequence data", new String[]{Nucleotides.DESCRIPTION, AminoAcids.DESCRIPTION, Codons.DESCRIPTION, TwoStates.DESCRIPTION}, false),
						new ElementRule(DataType.class)
				),
				new ElementRule(FREQUENCIES, FrequencyModel.class),
				new ElementRule(RATES,
						new XMLSyntaxRule[]{
//								AttributeRule.newIntegerRule(RELATIVE_TO, false, "The index of the implicit rate (value 1.0) that all other rates are relative to. In DNA this is usually G<->T (6)"),
								new ElementRule(Parameter.class)}
				),
				new ElementRule(INDICATOR,
						new XMLSyntaxRule[]{
								new ElementRule(Parameter.class)
						})
		};

	};

	private Parameter rateIndicator;
}
