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
import dr.xml.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * A likelihood function which is simply the product of a set of likelihood functions.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: CompoundLikelihood.java,v 1.19 2005/05/25 09:14:36 rambaut Exp $
 */
public class PathLikelihood implements Likelihood {

    public static final String PATH_LIKELIHOOD = "pathLikelihood";
    public static final String PRIOR = "prior";
    public static final String LIKELIHOOD = "likelihood";

    public PathLikelihood(Likelihood likelihood, Likelihood prior) {
        this.likelihood = likelihood;
        this.prior = prior;

        compoundModel.addModel(likelihood.getModel());
        compoundModel.addModel(prior.getModel());
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
        double logPrior = prior.getLogLikelihood();
        return ((likelihood.getLogLikelihood() + logPrior) * pathParameter) + (logPrior * (1.0 - pathParameter));
	}

	public void makeDirty() {
        likelihood.makeDirty();
        prior.makeDirty();
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

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return PATH_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            Likelihood likelihood = (Likelihood) xo.getElementFirstChild("likelihood");
            Likelihood prior = (Likelihood) xo.getElementFirstChild("prior");

            return new PathLikelihood(likelihood, prior);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A likelihood function used for estimating marginal likelihoods using path sampling.";
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(LIKELIHOOD,
                        new XMLSyntaxRule[]{new ElementRule(Likelihood.class)}),
                new ElementRule(PRIOR,
                        new XMLSyntaxRule[]{new ElementRule(Likelihood.class)}),
        };

        public Class getReturnType() {
            return CompoundLikelihood.class;
        }
    };

	private final Likelihood likelihood;
    private final Likelihood prior;

    private double pathParameter;

    private CompoundModel compoundModel = new CompoundModel("compoundModel");
}