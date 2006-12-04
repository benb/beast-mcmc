package dr.math;

import org.apache.commons.math.MathException;

/**
 * @author Alexei Drummond
 *
 * @version $Id$
 */
public class PoissonDistribution implements Distribution {

    org.apache.commons.math.distribution.PoissonDistribution distribution;

    public PoissonDistribution(double mean) {
        distribution = new org.apache.commons.math.distribution.PoissonDistributionImpl(mean);
    }

    public double pdf(double x) {
        return distribution.probability(x);
    }

    public double logPdf(double x) {
        return Math.log(distribution.probability(x));
        
    }

    public double cdf(double x) {
        try {
            return distribution.cumulativeProbability(x);
        } catch (MathException e) {
            throw new RuntimeException(e);
        }
    }

    public double quantile(double y) {
        try {
            return distribution.inverseCumulativeProbability(y);
        } catch (MathException e) {
            throw new RuntimeException(e);
        }
    }

    public double mean() {
        return distribution.getMean();
    }

    public double variance() {
        return distribution.getMean();
    }

    public UnivariateFunction getProbabilityDensityFunction() {
        throw new RuntimeException();
    }
}
