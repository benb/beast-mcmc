/*
 * MCMCParser.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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

package dr.inference.mcmc;

import dr.inference.loggers.Logger;
import dr.inference.markovchain.MarkovChain;
import dr.inference.model.CompoundLikelihood;
import dr.inference.model.Likelihood;
import dr.inference.operators.OperatorSchedule;
import dr.inference.prior.Prior;
import dr.xml.*;

import java.util.ArrayList;

public class MCMCParser extends AbstractXMLObjectParser {

    public String getParserName() {
        return MCMC;
    }

    /**
     * @return a tree object based on the XML element it was passed.
     */
    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        MCMC mcmc = new MCMC("mcmc1");
        MCMCOptions options = new MCMCOptions();
        OperatorSchedule opsched = (OperatorSchedule) xo.getChild(OperatorSchedule.class);
        Likelihood likelihood = (Likelihood) xo.getChild(Likelihood.class);
        ArrayList<Logger> loggers = new ArrayList<Logger>();

        options.setChainLength(xo.getIntegerAttribute(CHAIN_LENGTH));
        options.setUseCoercion(xo.getAttribute(COERCION, true));
        options.setPreBurnin(xo.getAttribute(PRE_BURNIN, options.getChainLength() / 100));
        options.setTemperature(xo.getAttribute(TEMPERATURE, 1.0));
        options.setFullEvaluationCount(xo.getAttribute(FULL_EVALUATION, 2000));
        options.setminOperatorCountForFullEvaluation(xo.getAttribute(MIN_OPS_EVALUATIONS, 1));

        for (int i = 0; i < xo.getChildCount(); i++) {
            Object child = xo.getChild(i);
            if (child instanceof Logger) {
                loggers.add((Logger) child);
            }
        }

        mcmc.setShowOperatorAnalysis(true);

        Logger[] loggerArray = new Logger[loggers.size()];
        loggers.toArray(loggerArray);

        java.util.logging.Logger.getLogger("dr.inference").info("Creating the MCMC chain:" +
                "\n  chainLength=" + options.getChainLength() +
                "\n  autoOptimize=" + options.useCoercion());

        mcmc.init(options, likelihood, Prior.UNIFORM_PRIOR, opsched, loggerArray);

        MarkovChain mc = mcmc.getMarkovChain();
        double initialScore = mc.getCurrentScore();

        if (initialScore == Double.NEGATIVE_INFINITY) {
            String message = "The initial posterior is zero";
            if (likelihood instanceof CompoundLikelihood) {
                message += ": " + ((CompoundLikelihood) likelihood).getDiagnosis();
            } else {
                message += "!";
            }
            throw new IllegalArgumentException(message);
        }

        if (!xo.getAttribute(SPAWN, true))
            mcmc.setSpawnable(false);

        return mcmc;
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "This element returns an MCMC chain and runs the chain as a side effect.";
    }

    public Class getReturnType() {
        return MCMC.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            AttributeRule.newIntegerRule(CHAIN_LENGTH),
            AttributeRule.newBooleanRule(COERCION, true),
            AttributeRule.newIntegerRule(PRE_BURNIN, true),
            AttributeRule.newDoubleRule(TEMPERATURE, true),
            AttributeRule.newIntegerRule(FULL_EVALUATION, true),
            AttributeRule.newIntegerRule(MIN_OPS_EVALUATIONS, true),
            AttributeRule.newBooleanRule(SPAWN, true),
            new ElementRule(OperatorSchedule.class),
            new ElementRule(Likelihood.class),
            new ElementRule(Logger.class, 1, Integer.MAX_VALUE)
    };

    public static final String COERCION = "autoOptimize";
    public static final String PRE_BURNIN = "preBurnin";
    public static final String MCMC = "mcmc";
    public static final String CHAIN_LENGTH = "chainLength";
    public static final String FULL_EVALUATION = "fullEvaluation";
    public static final String MIN_OPS_EVALUATIONS = "minOpsFullEvaluations";
    public static final String WEIGHT = "weight";
    public static final String TEMPERATURE = "temperature";
    public static final String SPAWN = "spawn";

}
