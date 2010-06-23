package dr.inference.markovjumps;

import dr.math.MathUtils;
import dr.math.GammaFunction;
import dr.math.matrixAlgebra.Vector;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * A class to represent a Poisson process and discrete-time Markov chain that subordinate
 * a continuous-time Markov chain in the interval [0,T]. The subordinator drives the Uniformization method
 * for simulating end-conditioned realizations
 * <p/>
 * This work is supported by NSF grant 0856099
 * <p/>
 * Hobolth A and Stone E (2009) Simulation from endpoint-conditioned, continuous-time Markov chains on a finite
 * state space, with applications to molecular evolution. Annals of Applied Statistics, 3, 1204-1231.
 *
 * @author Marc A. Suchard
 */

public class SubordinatedProcess {

    public SubordinatedProcess(double[] Q, int stateCount) {
        this.stateCount = stateCount;
        poissonRate = getMaxRate(Q, stateCount);
        dtmcCache = new ArrayList<double[]>();
        dtmcCache.add(makeIndentityMatrx(stateCount));
        dtmcCache.add(constructDtmcMatrix(Q, stateCount));
        tmp = new double[stateCount];
    }

    public double getPoissonRate() {
        return poissonRate;
    }

    private double getCachedExp(double x) {
        if (x != cachedXForExp) {
            cachedXForExp = x;
            cachedExpValue = Math.exp(x);
        }
        return cachedExpValue;
    }

    /**
     * Compute the n-step discrete-time transition probabilities
     *
     * @param nSteps which step
     * @return a pointer to the cached matrix
     */

    public double[] getDtmcProbabilities(int nSteps) {
        if (nSteps > dtmcCache.size() - 1) {
            double[] dtmcOneStep = dtmcCache.get(1);
            for (int step = dtmcCache.size() - 1; step <= nSteps; step++) {
                double[] lastDtmcMatrix = dtmcCache.get(step);
                double[] nextDtmcMatrix = new double[stateCount * stateCount];
                MarkovJumpsCore.matrixMultiply(lastDtmcMatrix, dtmcOneStep, stateCount, nextDtmcMatrix);
                dtmcCache.add(nextDtmcMatrix);
            }
        }
        return dtmcCache.get(nSteps);
    }

    /**
     * Find max_i -Q_{ii}
     *
     * @param Q          ctmc rate matrix
     * @param stateCount dim
     * @return max rate
     */

    private double getMaxRate(double[] Q, int stateCount) {
        double max = -Q[0];
        for (int i = 1; i < stateCount; i++) {
            double nextRate = -Q[i * stateCount + i];
            if (nextRate > max) {
                max = nextRate;
            }
        }
        return max;
    }

    /**
     * R =  I + 1/maxRate Q
     *
     * @param lambda     Q
     * @param stateCount dim
     * @return R
     */

    private double[] constructDtmcMatrix(double[] lambda, int stateCount) {
        double[] R = new double[stateCount * stateCount];
        double maxRate = getMaxRate(lambda, stateCount);
        int index = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                R[index] = lambda[index] / maxRate;

                if (i == j) {
                    R[index] += 1;
                }
                index++;
            }
        }
        return R;
    }

    /**
     * Simulate transition times, uniformly distributed before sorting
     *
     * @param timeDuration         T
     * @param totalNumberOfChanges total number of changes
     * @return the transition times of the subordinated process
     */
    public double[] drawTransitionTimes(double timeDuration, int totalNumberOfChanges) {
        double[] times = new double[totalNumberOfChanges];
        for (int i = 0; i < totalNumberOfChanges; i++) {
            times[i] = timeDuration * MathUtils.nextDouble();
        }
        if (times.length > 1) {
            Arrays.sort(times);
        }
        return times;
    }

    /**
     * Simulate the next transition in the subordinated process, equation in remark 7
     *
     * @param currentState         current state of the subordinated process
     * @param endingState          ending state of CTMC
     * @param totalNumberOfChanges number of subordinated changes
     * @param thisChangeNumber     this transition number
     * @return the next state of the subordinated process
     */
    public int drawNextChainState(int currentState, int endingState, int totalNumberOfChanges, int thisChangeNumber) {
        computePdfNextChainState(currentState, endingState, totalNumberOfChanges, thisChangeNumber, tmp);
        return MathUtils.randomChoicePDF(tmp);
    }

    public void computePdfNextChainState(int currentState, int endingState, int totalNumberOfChanges, int thisChangeNumber,
                                         double[] pdf) {
        double[] R = getDtmcProbabilities(1);
        double[] RnMinusI = getDtmcProbabilities(totalNumberOfChanges - thisChangeNumber);

        for (int i = 0; i < stateCount; i++) {
            pdf[i] = R[currentState * stateCount + i] * RnMinusI[i * stateCount + endingState];
//                     / RnMinusIPlus1[currentState * stateCount + endingState] // No need to normalize
        }
    }

    /**
     * Simulate the number of transitions in the subordinated process, equation (2.9)
     *
     * @param startingState   starting state of CTMC
     * @param endingState     ending state of CTMC
     * @param time            length of chain
     * @param ctmcProbability the CTMC finite-time transition probability
     * @return the number of transitions in the subordinated process
     */

    public int drawNumberOfChanges(int startingState, int endingState, double time, double ctmcProbability) {
        return drawNumberOfChanges(startingState, endingState, time, ctmcProbability, MathUtils.nextDouble());
    }

    public int drawNumberOfChanges(int startingState, int endingState, double time, double ctmcProbability,
                                   double cutoff) {
        int drawnNumber = -1;
        double cdf = 0;

        double effectiveRate = getPoissonRate() * time;
//        double preFactor = Math.exp(-effectiveRate);
        double preFactor = getCachedExp(-effectiveRate);
        double scale = 1.0;
        int index = startingState * stateCount + endingState;

        double[] check;
        int maxTries = 1000;
        if (DEBUG) {
            check = new double[maxTries+1];
        }

        while (cutoff >= cdf) {
            drawnNumber++;

            double[] Rn = getDtmcProbabilities(drawnNumber);
            if (drawnNumber > 0) {
                scale *= effectiveRate;
            }
            if (drawnNumber > 1) {
                scale /= (double) drawnNumber;
            }

            cdf += preFactor * scale * Rn[index] / ctmcProbability;

            if (DEBUG) {
                check[drawnNumber] = cdf;
                if (drawnNumber == maxTries) {
                    System.err.println("cdf = " + cdf);
                    System.err.println("cutoff = " + cutoff);
                    System.err.println("ctmcProb = " + ctmcProbability);
                    System.err.println("PoissonRate = " + getPoissonRate());

                    double[] distr = computePDFDirectly(startingState, endingState, time, ctmcProbability, drawnNumber);
                    double[] checkCDF = new double[distr.length];
                    double total = 0;
                    for (int i = 0; i < distr.length; i++) {
                        total += distr[i];
                        checkCDF[i] = total;
                    }
                    System.err.println("distr = " + new Vector(distr));
                    System.err.println("cdf   = " + new Vector(checkCDF));
                    System.err.println("check = " + new Vector(check));

                    throw new RuntimeException("Oh yeah");
                }
            }
        }
        return drawnNumber;
    }

    public double[] computePDFDirectly(int startingState, int endingState, double time, double ctmcProbaility,
                                       int maxTerm) {
        double[] pdf = new double[maxTerm];

        for (int n = 0; n < maxTerm; n++) {
            double[] Rn = getDtmcProbabilities(n);
            pdf[n] = Math.exp(-getPoissonRate() * time) * Math.pow(getPoissonRate() * time, n) /
                    Math.exp(GammaFunction.lnGamma(n + 1)) * Rn[startingState * stateCount + endingState] /
                    ctmcProbaility;
        }
        return pdf;
    }

    private double[] makeIndentityMatrx(int stateCount) {
        double[] I = new double[stateCount * stateCount];
        for (int i = 0; i < stateCount; i++) {
            I[i * stateCount + i] = 1.0;
        }
        return I;
    }

    private final List<double[]> dtmcCache;
    private final double poissonRate;
    private final int stateCount;
    private final double[] tmp;

    private double cachedXForExp = Double.NaN;
    private double cachedExpValue;

    private static final boolean DEBUG = false;
}
