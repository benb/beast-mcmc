/*
 * PathLikelihood.java
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

import dr.xml.*;

/**
 * A likelihood function which is simply the product of a set of likelihood functions.
 *
 * @author Andrew Rambaut
 * @author Alex Alekseyenko
 * @author Marc Suchard
 * @version $Id: CompoundLikelihood.java,v 1.19 2005/05/25 09:14:36 rambaut Exp $
 */
public class PathLikelihood implements Likelihood {

    public static final String PATH_LIKELIHOOD = "pathLikelihood";
    public static final String PATH_PARAMETER = "theta";
    public static final String DIFFERENCE = "delta";
    public static final String SOURCE = "source";
    public static final String DESTINATION ="destination";
    public static final String PSUEDO_SOURCE ="sourcePseudoPrior";
    public static final String PSUEDO_DESTINATION = "destinationPseudoPrior";

    public PathLikelihood(Likelihood source, Likelihood destination) {
        this(source, destination, null, null);
    }

    public PathLikelihood(Likelihood source, Likelihood destination,
                          Likelihood pseudoSource, Likelihood pseudoDestination) {
        this.source = source;
        this.destination = destination;
        this.pseudoSource = pseudoSource;
        this.pseudoDestination = pseudoDestination;

        compoundModel.addModel(source.getModel());
        compoundModel.addModel(destination.getModel());
    }

    public double getPathParameter() {
        return pathParameter;
    }

    public void setPathParameter(double pathParameter) {
        this.pathParameter = pathParameter;
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    public Model getModel() {
        return compoundModel;
    }

    public double getLogLikelihood() {
        // Depends on complete model (include pseudo-densities)
        return (source.getLogLikelihood() * pathParameter) + (destination.getLogLikelihood() * (1.0 - pathParameter));
    }

    public void makeDirty() {
        source.makeDirty();
        destination.makeDirty();
    }

    public String toString() {

        return Double.toString(getLogLikelihood());

    }

    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

	/**
	 * @return the log columns.
	 */
    public dr.inference.loggers.LogColumn[] getColumns() {
        return new dr.inference.loggers.LogColumn[] {
                new DeltaLogLikelihoodColumn(getId() + "." + DIFFERENCE),
                new ThetaColumn(getId() + "." + PATH_PARAMETER)
        };
    }

    private class DeltaLogLikelihoodColumn extends dr.inference.loggers.NumberColumn {

        public DeltaLogLikelihoodColumn(String label) {
            super(label);
        }

        public double getDoubleValue() {
            // Remove pseudo-densities
            double logDensity =  source.getLogLikelihood() - destination.getLogLikelihood();
            if (pseudoSource != null) {
                logDensity -= pseudoSource.getLogLikelihood();
            }
            if (pseudoDestination != null) {
//                System.err.println("value = "+pseudoDestination.getLogLikelihood());
//                logDensity += pseudoDestination.getLogLikelihood();
            }
            return logDensity;
        }
    }

    private class ThetaColumn extends dr.inference.loggers.NumberColumn {

        public ThetaColumn(String label) {
            super(label);
        }

        public double getDoubleValue() {
            return pathParameter;
        }

    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed() {
        isUsed = true;
    }

    private boolean isUsed = false;

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

     public String prettyName() {
         return Abstract.getPrettyName(this);
     }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return PATH_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            Likelihood likelihood = (Likelihood) xo.getElementFirstChild(SOURCE);
            Likelihood prior = (Likelihood) xo.getElementFirstChild(DESTINATION);

            Likelihood pseudoLikelihood = null;
            if (xo.hasChildNamed(PSUEDO_SOURCE)) {
                pseudoLikelihood = (Likelihood) xo.getElementFirstChild(PSUEDO_SOURCE);
            }
            Likelihood pseudoPrior = null;
            if (xo.hasChildNamed(PSUEDO_DESTINATION)) {
                pseudoPrior = (Likelihood) xo.getElementFirstChild(PSUEDO_DESTINATION);
            }

            return new PathLikelihood(likelihood, prior, pseudoLikelihood, pseudoPrior);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A likelihood function used for estimating marginal likelihoods and Bayes factors using path sampling.";
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(SOURCE,
                        new XMLSyntaxRule[]{new ElementRule(Likelihood.class)}),
                new ElementRule(DESTINATION,
                        new XMLSyntaxRule[]{new ElementRule(Likelihood.class)}),
                new ElementRule(PSUEDO_SOURCE,
                        new XMLSyntaxRule[]{new ElementRule(Likelihood.class)},true),
                new ElementRule(PSUEDO_DESTINATION,
                        new XMLSyntaxRule[]{new ElementRule(Likelihood.class)},true),
        };

        public Class getReturnType() {
            return CompoundLikelihood.class;
        }
    };

    private final Likelihood source;
    private final Likelihood destination;
    private final Likelihood pseudoSource;
    private final Likelihood pseudoDestination;

    private double pathParameter;

    private final CompoundModel compoundModel = new CompoundModel("compoundModel");
}