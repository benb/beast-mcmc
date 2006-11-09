/*
 * MarkovChain.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.inference.markovchain;

import dr.inference.model.Likelihood;
import dr.inference.model.Model;
import dr.inference.operators.*;
import dr.inference.prior.Prior;

import java.util.ArrayList;

/**
 * A concrete markov chain. This is final as the only things that should need overriding
 * are in the delegates (prior, likelihood, schedule and acceptor). The design of this
 * class is to be fairly immutable as far as settings goes.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * @version $Id: MarkovChain.java,v 1.10 2006/06/21 13:34:42 rambaut Exp $
 */
public final class MarkovChain {

	private final OperatorSchedule schedule;
    private final Acceptor acceptor;
    private final Prior prior;
    private final Likelihood likelihood;

    private boolean pleaseStop = false;
    private boolean isStopped = false;
    private double bestScore, currentScore, initialScore;
    private int currentLength;

    private boolean useCoercion = true;

    private static final int FULL_EVALUTATION_STATES = 1000;

    public MarkovChain(Prior prior,
                       Likelihood likelihood,
                       OperatorSchedule schedule,
                       Acceptor acceptor,
                       boolean useCoercion) {
        currentLength = 0;
        this.prior = prior;
        this.likelihood = likelihood;
        this.schedule = schedule;
        this.acceptor = acceptor;
        this.useCoercion = useCoercion;

        currentScore = evaluate(likelihood, prior);
    }

    /**
     * Resets the markov chain
     */
    public void reset() {
        currentLength = 0;

	    // reset operator acceptance levels
	    for (int i = 0; i < schedule.getOperatorCount(); i++) {
	        schedule.getOperator(i).reset();
	    }
    }

    /**
     * Run the chain for a given number of states.
     * @param length number of states to run the chain.
     * @param length number of states to run the chain.
     */
    public int chain(int length, boolean disableCoerce) {

        currentScore = evaluate(likelihood, prior);

        int currentState = currentLength;

        if (currentState == 0) {
            initialScore = currentScore;
            bestScore = currentScore;
            fireBestModel(currentState, likelihood.getModel());
        }

        if (currentScore == Double.NEGATIVE_INFINITY) {
            throw new IllegalArgumentException("The initial model is invalid because it has zero likelihood!");
        }

        pleaseStop = false;
        isStopped = false;

        double[] logr = new double[] {0.0};

        while (!pleaseStop && (currentState < (currentLength + length))) {

            // periodically log states
            fireCurrentModel(currentState, likelihood.getModel());

            if (pleaseStop) {
                isStopped = true;
                break;
            }

            // Get the operator
            int op = schedule.getNextOperatorIndex();
            MCMCOperator mcmcOperator = schedule.getOperator(op);

            double oldScore = currentScore;

//            assert Profiler.startProfile("Store");

            // The current model is stored here in case the proposal fails
            if (likelihood.getModel() != null) {
                likelihood.getModel().storeModelState();
            }

//            assert Profiler.stopProfile("Store");

            boolean operatorSucceeded = true;
            double hastingsRatio = 1.0;
            boolean accept = false;

            logr[0] = -Double.MAX_VALUE;

            try {
                // The new model is proposed
//                assert Profiler.startProfile("Operate");

//                System.out.println("Operator: " + mcmcOperator.getOperatorName());

                hastingsRatio = mcmcOperator.operate();

//                assert Profiler.stopProfile("Operate");
            } catch (OperatorFailedException e) {
                operatorSucceeded = false;
            }

            double score = 0.0;
            double deviation = 0.0;

            if (operatorSucceeded) {

                // The new model is proposed
//                    assert Profiler.startProfile("Evaluate");

                // The new model is evaluated
                score = evaluate(likelihood, prior);

//                    assert Profiler.stopProfile("Evaluate");

                if (score > bestScore) {
                    bestScore = score;
                    fireBestModel(currentState, likelihood.getModel());
                }

                if (mcmcOperator instanceof GibbsOperator) {
                    // by definition Gibbs operators always accept
                    accept = true;
                } else {
                    accept = acceptor.accept(oldScore, score, hastingsRatio, logr);
                }

                deviation = score - oldScore;
            }

            // The new model is accepted or rejected
            if (accept) {
 //               System.out.println("Move accepted: new score = " + score + ", old score = " + oldScore);

                mcmcOperator.accept(deviation);
                likelihood.getModel().acceptModelState();
                currentScore = score;

            } else {
 //               System.out.println("Move rejected: new score = " + score + ", old score = " + oldScore);

                mcmcOperator.reject();

 //               assert Profiler.startProfile("Restore");

                likelihood.getModel().restoreModelState();

//                assert Profiler.stopProfile("Restore");


                // This is a test that the state is correctly restored. The restored
                // state is fully evaluated and the likelihood compared with that before
                // the operation was made.
                if (currentState < FULL_EVALUTATION_STATES) {
                    likelihood.makeDirty();
                    double testScore = evaluate(likelihood, prior);

                    if (Math.abs(testScore - oldScore) >  1e-6) {
                        System.err.println("State was not correctly restored after reject step.");
                        System.err.println("Likelihood before: " + oldScore + " Likelihood after: " + testScore);
                    }
                }
            }

            if (!disableCoerce && mcmcOperator instanceof CoercableMCMCOperator) {
                coerceAcceptanceProbability((CoercableMCMCOperator)mcmcOperator, logr[0]);
            }

            currentState += 1;
        }

        currentLength = currentState;

        fireFinished(currentLength);

//        Profiler.report();

        return currentLength;
    }

    public Prior getPrior() {
        return prior;
    }

    public Likelihood getLikelihood() {
        return likelihood;
    }

    public Model getModel() {
        return likelihood.getModel();
    }

	public OperatorSchedule getSchedule() {
		return schedule;
	}

	public Acceptor getAcceptor() {
		return acceptor;
	}

	public double getInitialScore() {
        return initialScore;
    }

    public double getBestScore() {
        return bestScore;
    }

    public int getCurrentLength() {
        return currentLength;
    }

    public double getCurrentScore() {
        return currentScore;
    }

    public void pleaseStop() {
        pleaseStop = true;
    }

    public boolean isStopped() {
        return isStopped;
    }

    private double evaluate(Likelihood likelihood, Prior prior) {

        double logPosterior = 0.0;

        if (prior != null) {
            double logPrior = prior.getLogPrior(likelihood.getModel());

            if (logPrior == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            }


            logPosterior += logPrior;
        }

        double logLikelihood = likelihood.getLogLikelihood();

        if (Double.isNaN(logLikelihood)) {
            return Double.NEGATIVE_INFINITY;
        }

        logPosterior += logLikelihood;

        return logPosterior;
    }

    /**
     * Updates the proposal parameter, based on the target acceptance probability
     * This method relies on the proposal parameter being a decreasing function of
     * acceptance probability.
     * @param op
     * @param logr
     */
    private void coerceAcceptanceProbability(CoercableMCMCOperator op, double logr) {

        if (isCoercable(op)) {
            double p = op.getCoercableParameter();
            int i = MCMCOperator.Utils.getOperationCount(op);
            double target = op.getTargetAcceptanceProbability();

            double newp = p + ((1.0 / (i + 1.0)) * (Math.exp(logr)-target));

            if (newp > -Double.MAX_VALUE && newp < Double.MAX_VALUE) {
                op.setCoercableParameter(newp);
            } else {

            }
        }
    }

    private boolean isCoercable(CoercableMCMCOperator op) {

        if (op.getMode() == CoercableMCMCOperator.COERCION_ON) return true;
        if (op.getMode() == CoercableMCMCOperator.COERCION_OFF) return false;
        return useCoercion;
    }

    public void addMarkovChainListener(MarkovChainListener listener) {
        listeners.add(listener);
    }

    public void removeMarkovChainListener(MarkovChainListener listener) {
        listeners.remove(listener);
    }

    public void fireBestModel(int state, Model bestModel) {

        for (int i = 0; i < listeners.size(); i++) {
            ((MarkovChainListener)listeners.get(i)).bestState(state, bestModel);
        }
    }

    public void fireCurrentModel(int state, Model currentModel) {
        for (int i = 0; i < listeners.size(); i++) {
            ((MarkovChainListener)listeners.get(i)).currentState(state, currentModel);
        }
    }

    public void fireFinished(int chainLength) {

        for (int i = 0; i < listeners.size(); i++) {
            ((MarkovChainListener)listeners.get(i)).finished(chainLength);
        }
    }

    private final ArrayList listeners = new ArrayList();
}
