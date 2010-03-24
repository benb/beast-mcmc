package test.dr.math;

import dr.math.matrixAlgebra.Vector;
import junit.framework.TestCase;

/**
 * @author Marc A. Suchard
 */
public class MathTestCase extends TestCase {

    protected void assertEquals(double[] a, double[] b, double accuracy) {
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i], accuracy);
        }
    }

    protected void printSquareMatrix(double[] A, int dim) {
        double[] row = new double[dim];
        for (int i = 0; i < dim; i++) {
            System.arraycopy(A, i * dim, row, 0, dim);
            System.out.println(new Vector(row));
        }
    }
}
