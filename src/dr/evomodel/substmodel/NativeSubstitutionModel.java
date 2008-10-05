package dr.evomodel.substmodel;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import dr.evolution.datatype.*;
import dr.inference.loggers.LogColumn;
import dr.inference.loggers.Loggable;
import dr.inference.loggers.MatrixEntryColumn;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.math.matrixAlgebra.Vector;
import dr.xml.*;

import java.util.logging.Logger;

/**
 * <b>A general irreversible class for any
 * data type; allows complex eigenstructures.
 * Eigendecomposition is done in colt.  Finite-time
 * transition probabilities are caches in native memory</b>
 *
 * @author Marc Suchard
 */

public class NativeSubstitutionModel extends AbstractSubstitutionModel
		implements  Loggable {

	public static final String NATIVE_SUBSTITUTION_MODEL = "nativeSubstitutionModel";
	public static final String RATES = "rates";
	public static final String ROOT_FREQUENCIES = "rootFrequencies";
	public static final String INDICATOR = "rateIndicator";

//	private static final double minProb = Property.DEFAULT.tolerance();

	public NativeSubstitutionModel(String name, DataType dataType,
	                                FrequencyModel rootFreqModel, Parameter parameter) {

		super(name, dataType, rootFreqModel);
		this.infinitesimalRates = parameter;

		rateCount = stateCount * (stateCount - 1);

		stateCountSquared = stateCount * stateCount;

		if (rateCount != infinitesimalRates.getDimension()) {
			throw new RuntimeException("Dimension of '" + infinitesimalRates.getId() + "' ("
					+ infinitesimalRates.getDimension() + ") must equal " + rateCount);
		}

		stationaryDistribution = new double[stateCount];
		storedStationaryDistribution = new double[stateCount];

		addParameter(infinitesimalRates);

        illConditionedProbabilities = new double[stateCount*stateCount];

		Logger.getLogger("dr.evomodel.substmodel").info("Trying a native substitution model. Best of luck to you!") ;

    }

	protected void handleModelChangedEvent(Model model, Object object, int index) {
		if (model == freqModel)
			return; // freqModel only affects the likelihood calculation at the tree root
		super.handleModelChangedEvent(model, object, index);
	}

	protected void restoreState() {


		double[] tmp3 = storedStationaryDistribution;
		storedStationaryDistribution = stationaryDistribution;
		stationaryDistribution = tmp3;

		normalization = storedNormalization;

		updateMatrix = storedUpdateMatrix;

		nativeRestoreState();

	}




	protected void storeState() {

		storedUpdateMatrix = updateMatrix;

		System.arraycopy(stationaryDistribution, 0, storedStationaryDistribution, 0, stateCount);
		storedNormalization = normalization;

		nativeStoreState();

	}

	public void getTransitionProbabilities(double distance, long ptrMatrix) {

//		synchronized (this) {
			if (updateMatrix) {
				setupMatrix();
			}
//		}

		nativeGetTransitionProbabilities(ptrCache, distance, ptrMatrix, stateCount);

	}

	public void getTransitionProbabilities(double distance, double[] matrix) {
			throw new RuntimeException("Should only get here in debug mode");

	}



	public double[] getStationaryDistribution() {
		return stationaryDistribution;
	}

	protected void computeStationaryDistribution() {

		int i;
		final int end = stateCount - 1;
		for (i = 0; i < end; i++) {
			if (Math.abs(Eval[i]) < 1E-12)
				break;
		}

		double total = 0.0;

		for (int k = 0; k < stateCount; k++) {
			double value = Evec[k][i];
			total += value;
			stationaryDistribution[k] = value;
		}

		for (int k = 0; k < stateCount; k++)
			stationaryDistribution[k] /= total;

	}


	protected double[] getRates() {
		return infinitesimalRates.getParameterValues();
	}


	protected void setupMatrix() {

		if (!eigenInitialised) {

			amat = new double[stateCount][stateCount];
			q = new double[stateCount][stateCount];

			nativeInitialiseEigen();
			eigenInitialised = true;
			updateMatrix = true;

		}

		int i, j, k = 0;

		double[] rates = getRates();

		// Set the instantaneous rate matrix
		for (i = 0; i < stateCount; i++) {
			for (j = 0; j < stateCount; j++) {
				if (i != j)
					amat[i][j] = rates[k++];
			}
		}

		makeValid(amat, stateCount);

		// compute eigenvalues and eigenvectors
		EigenvalueDecomposition eigenDecomp = new EigenvalueDecomposition(new DenseDoubleMatrix2D(amat));

		DoubleMatrix2D eigenV = eigenDecomp.getV();
		DoubleMatrix1D eigenVReal = eigenDecomp.getRealEigenvalues();
		DoubleMatrix1D eigenVImag = eigenDecomp.getImagEigenvalues();

        DoubleMatrix2D eigenVInv;

        try {
            eigenVInv = alegbra.inverse(eigenV);
        } catch (IllegalArgumentException e) {
            wellConditioned = false;
            return;
//            throw e;
        }

        // fill AbstractSubstitutionModel parameters

		Ievc = eigenVInv.toArray();
		Evec = eigenV.toArray();
		Eval = eigenVReal.toArray();
		EvalImag = eigenVImag.toArray();

		checkComplexSolutions();

		// compute normalization and rescale eigenvalues

		computeStationaryDistribution();

		double subst = 0.0;

		for (i = 0; i < stateCount; i++)
			subst += -amat[i][i] * stationaryDistribution[i];

		normalization = subst;

		for (i = 0; i < stateCount; i++) {
			Eval[i] /= subst;
			EvalImag[i] /= subst;
		}

		nativeSetup(ptrCache,Ievc,Evec,Eval,EvalImag,stateCount);

        wellConditioned = true;
        updateMatrix = false;
	}



	protected void checkComplexSolutions() {
		boolean complex = false;
		for (int i = 0; i < stateCount && !complex; i++) {
			if (EvalImag[i] != 0)
				complex = true;
		}
		isComplex = complex;
	}

	public boolean getIsComplex() {
		return isComplex;
	}

	protected void frequenciesChanged() {
	}

	protected void ratesChanged() {
	}

	protected void setupRelativeRates() {
	}

	protected Parameter infinitesimalRates;

	public LogColumn[] getColumns() {

		LogColumn[] columnList = new LogColumn[stateCount * stateCount];
		int index = 0;
		for (int i = 0; i < stateCount; i++) {
			for (int j = 0; j < stateCount; j++)
				columnList[index++] = new MatrixEntryColumn(getId(), i, j, amat);
		}
		return columnList;
	}


	public static void main(String[] arg) {

		Parameter rates = new Parameter.Default(new double[]{5.0, 1.0, 1.0, 0.1, 5.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0});
//		Parameter rates = new Parameter.Default(new double[] {5.0, 1.0, 1.0, 1.0, 5.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0});
//		Parameter rates = new Parameter.Default(new double[] {1.0, 1.0});

		NativeSubstitutionModel substModel = new NativeSubstitutionModel("test",
//				TwoStates.INSTANCE,
				Nucleotides.INSTANCE,
				null,
				rates);

		double[] finiteTimeProbs = new double[substModel.getDataType().getStateCount() * substModel.getDataType().getStateCount()];
		double time = 1.0;
//		substModel.getTransitionProbabilities(time, finiteTimeProbs);

		long ptrMatrix = substModel.allocateNativeMemoryArray(substModel.getDataType().getStateCount()*substModel.getDataType().getStateCount());

		substModel.getTransitionProbabilities(time, ptrMatrix);

		System.out.println("Results:");
//		System.out.println(new Vector(finiteTimeProbs));
//		finiteTimeProbs = new double[substModel.getDataType().getStateCount()*substModel.getDataType().getStateCount()];

		substModel.getNativeMemoryArray(ptrMatrix,0,finiteTimeProbs,0,substModel.getDataType().getStateCount()*substModel.getDataType().getStateCount());
		System.out.println(new Vector(finiteTimeProbs));

	}



	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() {
			return NATIVE_SUBSTITUTION_MODEL;
		}

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

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

			XMLObject cxo = (XMLObject) xo.getChild(RATES);

			Parameter ratesParameter = (Parameter) cxo.getChild(Parameter.class);

			int rateCount = (dataType.getStateCount() - 1) * dataType.getStateCount();

			if (ratesParameter.getDimension() != rateCount) {
				throw new XMLParseException("Rates parameter in " + getParserName() + " element should have " + (rateCount) + " dimensions.  However parameter dimension is " + ratesParameter.getDimension());
			}


			cxo = (XMLObject) xo.getChild(ROOT_FREQUENCIES);
			FrequencyModel rootFreq = (FrequencyModel) cxo.getChild(FrequencyModel.class);

			if (dataType != rootFreq.getDataType()) {
				throw new XMLParseException("Data type of " + getParserName() + " element does not match that of its rootFrequencyModel.");
			}

			Parameter indicators = null;

			if (xo.hasChildNamed(INDICATOR)) {
				indicators = (Parameter) ((XMLObject) xo.getChild(INDICATOR)).getChild(Parameter.class);
				if (ratesParameter.getDimension() != indicators.getDimension())
					throw new XMLParseException("Rate parameter dimension must match indicator parameter dimension");
			}

			if (indicators == null)
				return new NativeSubstitutionModel(xo.getId(), dataType, rootFreq, ratesParameter);
			else
				return new NativeSubstitutionModel(xo.getId(), dataType, rootFreq, ratesParameter);

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
				new ElementRule(ROOT_FREQUENCIES, FrequencyModel.class),
				new ElementRule(RATES,
						new XMLSyntaxRule[]{
								new ElementRule(Parameter.class)}
				),
				new ElementRule(INDICATOR,
						new XMLSyntaxRule[]{
								new ElementRule(Parameter.class)
						}),
		};

	};

	private boolean isComplex = false;
	private double[] stationaryDistribution = null;
	private double[] storedStationaryDistribution;
	private Double normalization;
	private Double storedNormalization;

	protected double[] EvalImag;

    private boolean wellConditioned = true;
    private double[] illConditionedProbabilities;

    private static final Algebra alegbra = new Algebra();

	protected void nativeStoreState() {
		copyNativeMemoryArray(ptrCache,ptrStoredCache, 2*(stateCount + stateCountSquared));
	}

	protected void nativeRestoreState() {
		long tmpPtr = ptrCache;
		ptrCache = ptrStoredCache;
		ptrStoredCache = tmpPtr;
	}

	protected void nativeInitialiseEigen() {
		ptrCache = allocateNativeMemoryArray(2*stateCountSquared + 2*stateCount);
		ptrStoredCache = allocateNativeMemoryArray(2*stateCountSquared + 2*stateCount);
	}


	protected native void nativeSetup(long ptr, double[][] Ievc, double[][] Evec, double[] Eval,
	                                  double[] EvalImag, int stateCount);

	protected native long allocateNativeMemoryArray(int length);
	protected native void copyNativeMemoryArray(long from, long to, int length);

	protected native int getNativeRealSize();

	protected native void getNativeMemoryArray(long from, int fromOffset, double[] to, int toOffset, int length);
	
	protected native void nativeGetTransitionProbabilities(long ptrCache, double distance,
	                                                       long ptrMatrix, int stateCount);

	static {

		try {
			System.loadLibrary("NativeMemoryLikelihoodCore");
		} catch (UnsatisfiedLinkError e) {

		}

	}

	int stateCountSquared;

	long ptrCache;
//  long ptrIevc;
//	long ptrEvec;
//	long ptrEval;
//	long ptrEvalImag;

	long ptrStoredCache;
//	long ptrStoredIevc;
//	long ptrStoredEvec;
//	long ptrStoredEval;
//	long ptrStoredEvalImag;

}