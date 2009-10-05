package dr.app.beagle.evomodel.substmodel;

import dr.math.matrixAlgebra.RobustEigenDecomposition;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * @author Marc Suchard
 */
public class ComplexColtEigenSystem extends ColtEigenSystem {

    protected double[] getAllEigenValues(RobustEigenDecomposition decomposition) {
        double[] realEval = decomposition.getRealEigenvalues().toArray();
        double[] imagEval = decomposition.getImagEigenvalues().toArray();

        final int dim = realEval.length;
        double[] merge = new double[2*dim];
        System.arraycopy(realEval,0,merge,0,dim);
        System.arraycopy(imagEval,0,merge,dim,dim);
        return merge;
    }

      protected double[] getEmptyAllEigenValues(int dim) {
        return new double[2 * dim];
    }

    protected boolean validDecomposition(DoubleMatrix2D eigenV) {
        return true;
    }
}
