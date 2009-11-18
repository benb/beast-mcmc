package dr.app.beagle.evomodel.substmodel;

import dr.evolution.datatype.DataType;
import dr.inference.model.Parameter;
import dr.inference.model.Likelihood;
import dr.inference.model.BayesianStochasticSearchVariableSelection;
import dr.inference.model.Model;
import dr.inference.loggers.LogColumn;
import dr.inference.loggers.MatrixEntryColumn;
import dr.math.matrixAlgebra.Vector;

import java.util.Arrays;

/**
 * @author Marc Suchard
 */
public class ComplexSubstitutionModel extends GeneralSubstitutionModel implements Likelihood {

    public ComplexSubstitutionModel(String name, DataType dataType, FrequencyModel freqModel, Parameter parameter) {
        super(name, dataType, freqModel, parameter, -1);
    }

    protected EigenSystem getDefaultEigenSystem(int stateCount) {
        return new ComplexColtEigenSystem();
    }

    /**
     * get the complete transition probability matrix for the given distance
     *
     * @param distance the expected number of substitutions
     * @param matrix   an array to store the matrix
     */
    public void getTransitionProbabilities(double distance, double[] matrix) {
        double temp;

        EigenDecomposition eigen = getEigenDecomposition();

        if (eigen == null) {
            Arrays.fill(matrix, 0.0);
            return;
        }

        double[] Evec = eigen.getEigenVectors();
        double[] Eval = eigen.getEigenValues();
        double[] EvalImag = new double[stateCount];
        System.arraycopy(Eval,stateCount,EvalImag,0,stateCount);
        double[] Ievc = eigen.getInverseEigenVectors();

        double[][] iexp = new double[stateCount][stateCount];

// Eigenvalues and eigenvectors of a real matrix A.
//
// If A is symmetric, then A = V*D*V' where the eigenvalue matrix D is diagonal
// and the eigenvector matrix V is orthogonal. I.e. A = V D V^t and V V^t equals
// the identity matrix.
//
// If A is not symmetric, then the eigenvalue matrix D is block diagonal with
// the real eigenvalues in 1-by-1 blocks and any complex eigenvalues,
// lambda + i*mu, in 2-by-2 blocks, [lambda, mu; -mu, lambda]. The columns
// of V represent the eigenvectors in the sense that A*V = V*D. The matrix
// V may be badly conditioned, or even singular, so the validity of the
// equation A = V D V^{-1} depends on the conditioning of V.

        for (int i = 0; i < stateCount; i++) {

            if (EvalImag[i] == 0) {
                // 1x1 block
                temp = Math.exp(distance * Eval[i]);
                for (int j = 0; j < stateCount; j++) {
                    iexp[i][j] = Ievc[i * stateCount + j] * temp;
                }
            } else {
                // 2x2 conjugate block
                // If A is 2x2 with complex conjugate pair eigenvalues a +/- bi, then
                // exp(At) = exp(at)*( cos(bt)I + \frac{sin(bt)}{b}(A - aI)).
                int i2 = i + 1;
                double b = EvalImag[i];
                double expat = Math.exp(distance * Eval[i]);
                double expatcosbt = expat * Math.cos(distance * b);
                double expatsinbt = expat * Math.sin(distance * b);

                for (int j = 0; j < stateCount; j++) {
                    iexp[i ][j] = expatcosbt * Ievc[i  * stateCount + j] +
                                  expatsinbt * Ievc[i2 * stateCount + j];
                    iexp[i2][j] = expatcosbt * Ievc[i2 * stateCount + j] -
                                  expatsinbt * Ievc[i  * stateCount + j];
                }
                i++; // processed two conjugate rows
            }
        }

        int u = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                temp = 0.0;
                for (int k = 0; k < stateCount; k++) {
                    temp += Evec[i * stateCount + k] * iexp[k][j];
                }
                matrix[u] = Math.abs(temp);
                u++;
            }
        }
    }

    protected int getRateCount(int stateCount) {
        return (stateCount - 1) * stateCount;
    }

    protected void setupRelativeRates(double[] rates) {
        for (int i = 0; i < rates.length; i++)
            rates[i] = ratesParameter.getParameterValue(i);
    }

    //    protected void setupQMatrix(double[] rates, double[][] matrix) {
    protected void setupQMatrix(double[] rates, double[] pi, double[][] matrix) {
        int i, j, k = 0;
        for (i = 0; i < stateCount; i++) {
            for (j = i + 1; j < stateCount; j++) {
                matrix[i][j] = rates[k++];
            }
        }
        // Copy lower triangle in column-order form (transposed)
        for (j = 0; j < stateCount; j++) {
            for (i = j + 1; i < stateCount; i++) {
                matrix[i][j] = rates[k++];
            }
        }
    }

    public boolean canReturnComplexDiagonalization() {
        return true;
    }

    protected double getNormalizationValue(double[][] matrix, double[] pi) {
        if (doNormalization)
            return super.getNormalizationValue(matrix, pi);
        return 1.0;
    }

    public double getLogLikelihood() {
        if (BayesianStochasticSearchVariableSelection.Utils.connectedAndWellConditioned(probability,this))
            return 0;
        return Double.NEGATIVE_INFINITY;
    }

    public String prettyName() {
        return Abstract.getPrettyName(this);
    }

    public void setNormalization(boolean doNormalization) {
        this.doNormalization = doNormalization;
    }

    public void makeDirty() {

    }

    @Override
    public boolean isUsed() {
        return super.isUsed() && isUsed;
    }

    public void setUsed() {
        isUsed = true;
    }

    private boolean isUsed = false;
    private double[] probability;

    public Model getModel() {
        return this;
    }

    public LogColumn[] getColumns() {

        LogColumn[] columnList = new LogColumn[stateCount * stateCount];
        int index = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++)
                columnList[index++] = new MatrixEntryColumn(getId(), i, j, getQ());
        }
        return columnList;
    }

    private boolean doNormalization = true;
}
