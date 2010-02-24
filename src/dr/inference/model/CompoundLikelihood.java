/*
 * CompoundLikelihood.java
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

package dr.inference.model;

import dr.util.NumberFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * A likelihood function which is simply the product of a set of likelihood functions.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: CompoundLikelihood.java,v 1.19 2005/05/25 09:14:36 rambaut Exp $
 */
public class CompoundLikelihood implements Likelihood {

    public CompoundLikelihood(int threads, Collection<Likelihood> likelihoods) {
        if (threads < 0 && likelihoods.size() > 1) {
            // asking for an automatic threadpool size
            threadCount = likelihoods.size();
        } else {
            threadCount = threads;
        }

        if (threadCount > 0) {
            pool = Executors.newFixedThreadPool(threadCount);
//        } else if (threads < 0) {
//            // create a cached thread pool which should create one thread per likelihood...
//            pool = Executors.newCachedThreadPool();
        } else {
            pool = null;
        }

        for (Likelihood l : likelihoods) {
            addLikelihood(l);
        }
    }

    private void addLikelihood(Likelihood likelihood) {

        if (!likelihoods.contains(likelihood)) {

            likelihoods.add(likelihood);
            if (likelihood.getModel() != null) {
                compoundModel.addModel(likelihood.getModel());
            }

            likelihoodCallers.add(new LikelihoodCaller(likelihood));
        }
    }

    public int getLikelihoodCount() {
        return likelihoods.size();
    }

    public final Likelihood getLikelihood(int i) {
        return likelihoods.get(i);
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    public Model getModel() {
        return compoundModel;
    }

    // todo: remove in release
    static int DEBUG = 0;

    public double getLogLikelihood() {
        double logLikelihood = 0.0;

        if (pool == null) {
            // Single threaded

            for (Likelihood likelihood : likelihoods) {
                final double l = likelihood.getLogLikelihood();
                // if the likelihood is zero then short cut the rest of the likelihoods
                // This means that expensive likelihoods such as TreeLikelihoods should
                // be put after cheap ones such as BooleanLikelihoods
                if( l == Double.NEGATIVE_INFINITY )
                    return Double.NEGATIVE_INFINITY;
                logLikelihood += l;
            }
        } else {
            try {

                List<Future<Double>> results = pool.invokeAll(likelihoodCallers);

                for (Future<Double> result : results) {
                    double logL = result.get();
                    logLikelihood += logL;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if( DEBUG > 0 ) {
            int t = DEBUG; DEBUG = 0;
            System.err.println(getId() + ": " + getDiagnosis() + " = " + logLikelihood);
            DEBUG = t;
        }
        return logLikelihood;
    }

    public void makeDirty() {
        for( Likelihood likelihood : likelihoods ) {
            likelihood.makeDirty();
        }
    }

    public String getDiagnosis() {
        String message = "";
        boolean first = true;

        final NumberFormatter nf = new NumberFormatter(6);

        for( Likelihood lik : likelihoods ) {

            if( !first ) {
                message += ", ";
            } else {
                first = false;
            }

            message += lik.prettyName() + "=";

            if( lik instanceof CompoundLikelihood ) {
                final String d = ((CompoundLikelihood) lik).getDiagnosis();
                if( d != null && d.length() > 0 ) {
                    message += "(" + d + ")";
                }
            } else {

                final double logLikelihood = lik.getLogLikelihood();
                if( logLikelihood == Double.NEGATIVE_INFINITY ) {
                    message += "-Inf";
                } else if( Double.isNaN(logLikelihood) ) {
                    message += "NaN";
                } else {
                    message += nf.formatDecimal(logLikelihood, 4);
                }
            }
        }

        return message;
    }

    public String toString() {
        return getId();
        // really bad for debugging
        //return Double.toString(getLogLikelihood());
    }

    public String prettyName() {
        return Abstract.getPrettyName(this);
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed() {
        used = true;
        for (Likelihood l : likelihoods) {
            l.setUsed();
        }
    }

    public int getThreadCount() {
        return threadCount;
    }


    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

    /**
     * @return the log columns.
     */
    public dr.inference.loggers.LogColumn[] getColumns() {
        return new dr.inference.loggers.LogColumn[]{
                new LikelihoodColumn(getId())
        };
    }

    private class LikelihoodColumn extends dr.inference.loggers.NumberColumn {
        public LikelihoodColumn(String label) {
            super(label);
        }

        public double getDoubleValue() {
            return getLogLikelihood();
        }
    }

    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

    private String id = null;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    private boolean used = false;

    private final int threadCount;

    private final ExecutorService pool;

    private final ArrayList<Likelihood> likelihoods = new ArrayList<Likelihood>();
    private final CompoundModel compoundModel = new CompoundModel("compoundModel");


    private final List<Callable<Double>> likelihoodCallers = new ArrayList<Callable<Double>>();

    class LikelihoodCaller implements Callable<Double> {

        public LikelihoodCaller(Likelihood likelihood) {
            this.likelihood = likelihood;
        }

        public Double call() throws Exception {
            return likelihood.getLogLikelihood();
        }

        private final Likelihood likelihood;
    }
}

