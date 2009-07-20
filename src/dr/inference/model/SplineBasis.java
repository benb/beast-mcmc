package dr.inference.model;

import dr.math.IntegrableUnivariateFunction;
import dr.xml.*;
import no.uib.cipr.matrix.BandMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import java.util.logging.Logger;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Marc A. Suchard
 */
public class SplineBasis extends AbstractModel implements IntegrableUnivariateFunction {

    public static final String SPLINE_BASIS = "splineFunction";
    public static final String KNOT_POINTS = "knotLocations";
    public static final String KNOT_VALUES = "knotValues";
    public static final String DEGREE = "degree";

    public SplineBasis(String name, Variable<Double> knotLocations, Variable<Double> knotValues, int degree) {
        super(name);
        this.knotLocations = knotLocations;
        this.knotValues = knotValues;
        addVariable(knotLocations);
        addVariable(knotValues);
        this.degree = degree;
        updateBasis = true;

        n = knotValues.getSize();
        h = new double[n - 1];
        deltaY = new double[n - 1];

        hMatrix = new BandMatrix(n, 1, 1);
        yByH = new DenseVector(n);
        z = new DenseVector(n);

        calculateBasis();

        StringBuilder buffer = new StringBuilder();
        buffer.append("Constructing spline basis:\n");
        buffer.append("\tDegree: ").append(degree).append("\n");
        buffer.append("\tRange: [").append(getLowerBound()).append(", ").append(getUpperBound()).append("\n");

        Logger.getLogger("dr.math").info(buffer.toString());

    }

    public int getDegree() {
        return degree;
    }

    public double evaluate(double location) {
        calculateBasis();

        int i = 0;
        double xi   = knotLocations.getValue(i);

        while( xi < location) {
            i++;
            xi = knotLocations.getValue(i);
        } // TODO Keep a sorted list of knotLocations for a O(log N) tree search.
                                   
        double xip1 = knotLocations.getValue(i+1);
        double yi   = knotValues.getValue(i);
        double yip1 = knotValues.getValue(i+1);
        double zi   = z.get(i);
        double zip1 = z.get(i+1);
        double hi   = xip1 - xi;

        return
                zip1 * Math.pow(location - xi, 3) +
                zi   * Math.pow(zip1 - location, 3) +
                (yip1/hi - hi/6*zip1)*(location - xi) +
                (yi/hi - hi/6*zi)*(xip1 - location);

    }

    public double getLowerBound() {
        return rangeMin;
    }

    public double getUpperBound() {
        return rangeMax;
    }

    public double evaluateIntegral(double startLocation, double endLocation) {
        calculateBasis();
        // TODO
        return 0;
    }

    private void calculateBasis() {
        if (updateBasis) {

            Double[] x = knotLocations.getValues();
            Double[] y = knotValues.getValues();

            rangeMin = x[0];
            rangeMax = x[x.length-1];

            for (int i = 0; i < n - 1; i++) {
                h[i] = x[i + 1] - x[i];
                deltaY[i] = y[i + 1] - y[i];
            }

            hMatrix.set(0, 0, 1.0); // TODO Do not need to update
            yByH.set(0, 0.0);

            for (int i = 1; i < n - 2; i++) {
                hMatrix.set(i, i - 1, h[i - 1]);
                hMatrix.set(i, i, 2 * (h[i] + h[i - 1]));
                hMatrix.set(i, i + 1, h[i]);
                yByH.set(i, 6 * (deltaY[i]/h[i] - deltaY[i - 1]/h[i - 1]) );
            }

            hMatrix.set(n - 1, n - 1, 1.0); // TODO Do not need to update
            yByH.set(n - 1, 0.0);

            hMatrix.solve(yByH, z);

            updateBasis = false;
        }
    }

    public void addModelListener(ModelListener listener) {

    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {

    }

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        updateBasis = true;
    }

    protected void storeState() {

    }

    protected void restoreState() {

    }

    protected void acceptState() {

    }

    /**
     * The XML parser
     */
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return SPLINE_BASIS;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            int degree = xo.getAttribute(DEGREE, 3);
            Parameter knotLocations = (Parameter) xo.getElementFirstChild(KNOT_POINTS);
            Parameter knotValues = (Parameter) xo.getElementFirstChild(KNOT_VALUES);

            if (knotLocations.getDimension() != knotValues.getDimension())
                throw new XMLParseException("Spline basis knot locations and values must have the same dimension");

            List<XY> xyList = new ArrayList<XY>();
            for(int i=0; i<knotLocations.getDimension(); i++)
                xyList.add(new XY(knotLocations.getParameterValue(i),knotValues.getParameterValue(i)));

            Collections.sort(xyList);

            for(int i=0; i<knotLocations.getDimension(); i++) {
                XY xy = xyList.get(i);
                knotLocations.setParameterValue(i,xy.x);
                knotValues.setParameterValue(i,xy.y);
            }

            return new SplineBasis(xo.getId(), knotLocations, knotValues, degree);
        }

        class XY implements Comparable {

            private double x;
            private double y;

            public XY(double x, double y) {
                this.x = x;
                this.y = y;
            }

            public int compareTo(Object o) {
                double z = ((XY)o).x;
                if (z == x)
                    throw new RuntimeException("No ties accepted in spline basis");
                return Double.compare(x, z);
            }

        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the a spline interpolation of discrete data.";
        }

        public Class getReturnType() {
            return SplineBasis.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newIntegerRule(DEGREE, true),
                new ElementRule(KNOT_POINTS, new XMLSyntaxRule[]{
                        new ElementRule(Parameter.class)
                }),
                new ElementRule(KNOT_VALUES, new XMLSyntaxRule[]{
                        new ElementRule(Parameter.class)
                }),
        };
    };

    private int degree;
    private int n;
    private Variable<Double> knotLocations;
    private Variable<Double> knotValues;
//    private double[] splineCoefficients;
//    private double[] storedSplineCoefficients;
    private boolean updateBasis;
    private double rangeMax;
    private double rangeMin;

    private double[] h;
    private double[] deltaY;

    private Matrix hMatrix;
    private Vector yByH;
    private Vector z;

}
