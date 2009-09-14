/*
 * TreeModelGenerator.java
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.beauti.generator;

import dr.app.beauti.components.ComponentFactory;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.enumTypes.ClockType;
import dr.app.beauti.enumTypes.PriorType;
import dr.app.beauti.options.PartitionTreeModel;
import dr.app.beauti.options.Parameter;
import dr.app.beauti.util.XMLWriter;
import dr.evomodel.coalescent.CoalescentSimulator;
import dr.evomodel.tree.TreeModel;
import dr.evomodel.tree.MonophylyStatistic;
import dr.evomodelxml.TreeModelParser;
import dr.evoxml.UPGMATreeParser;
import dr.inference.model.ParameterParser;
import dr.inference.model.BooleanLikelihood;
import dr.inference.model.OneOnXPrior;
import dr.util.Attribute;
import dr.xml.XMLParser;
import dr.evolution.util.Taxa;
import dr.inferencexml.PriorParsers;

import java.util.Map;
import java.util.ArrayList;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public class ParameterPriorGenerator extends Generator {

    public ParameterPriorGenerator(BeautiOptions options, ComponentFactory[] components) {
        super(options, components);
    }

    /**
     * Write the priors for each parameter
     *
     * @param writer the writer
     */
    void writeParameterPriors(XMLWriter writer) {
        boolean first = true;
        for (Map.Entry<Taxa, Boolean> taxaBooleanEntry : options.taxonSetsMono.entrySet()) {
            if (taxaBooleanEntry.getValue()) {
                if (first) {
                    writer.writeOpenTag(BooleanLikelihood.BOOLEAN_LIKELIHOOD);
                    first = false;
                }
                final String taxaRef = "monophyly(" + taxaBooleanEntry.getKey().getId() + ")";
                writer.writeIDref(MonophylyStatistic.MONOPHYLY_STATISTIC, taxaRef);
            }
        }
        if (!first) {
            writer.writeCloseTag(BooleanLikelihood.BOOLEAN_LIKELIHOOD);
        }

        ArrayList<Parameter> parameters = options.selectParameters();
        for (Parameter parameter : parameters) {
            if (parameter.priorType != PriorType.NONE) {
                if (parameter.priorType != PriorType.UNIFORM_PRIOR || parameter.isNodeHeight) {
                    writeParameterPrior(parameter, writer);
                }
            }
        }
    }

    /**
     * Write the priors for each parameter
     *
     * @param parameter the parameter
     * @param writer    the writer
     */
    private void writeParameterPrior(Parameter parameter, XMLWriter writer) {
        switch (parameter.priorType) {
            case UNIFORM_PRIOR:
                writer.writeOpenTag(PriorParsers.UNIFORM_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.LOWER, "" + parameter.lower),
                                new Attribute.Default<String>(PriorParsers.UPPER, "" + parameter.upper)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.UNIFORM_PRIOR);
                break;
            case EXPONENTIAL_PRIOR:
                writer.writeOpenTag(PriorParsers.EXPONENTIAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.MEAN, "" + parameter.mean),
                                new Attribute.Default<String>(PriorParsers.OFFSET, "" + parameter.offset)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.EXPONENTIAL_PRIOR);
                break;
            case LAPLACE_PRIOR:
                throw new IllegalArgumentException("Laplace prior has not been implemented in PriorParsers !");//TODO

            case NORMAL_PRIOR:
                writer.writeOpenTag(PriorParsers.NORMAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.MEAN, "" + parameter.mean),
                                new Attribute.Default<String>(PriorParsers.STDEV, "" + parameter.stdev)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.NORMAL_PRIOR);
                break;
            case LOGNORMAL_PRIOR:
                writer.writeOpenTag(PriorParsers.LOG_NORMAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.MEAN, "" + parameter.mean),
                                new Attribute.Default<String>(PriorParsers.STDEV, "" + parameter.stdev),
                                new Attribute.Default<String>(PriorParsers.OFFSET, "" + parameter.offset),

                                // this is to be implemented...
                                new Attribute.Default<String>(PriorParsers.MEAN_IN_REAL_SPACE, "false")
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.LOG_NORMAL_PRIOR);
                break;
            case GAMMA_PRIOR:
                writer.writeOpenTag(PriorParsers.GAMMA_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.SHAPE, "" + parameter.shape),
                                new Attribute.Default<String>(PriorParsers.SCALE, "" + parameter.scale),
                                new Attribute.Default<String>(PriorParsers.OFFSET, "" + parameter.offset)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.GAMMA_PRIOR);
                break;
            case JEFFREYS_PRIOR:
                writer.writeOpenTag(OneOnXPrior.ONE_ONE_X_PRIOR);
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(OneOnXPrior.ONE_ONE_X_PRIOR);
                break;
            case POISSON_PRIOR:
                writer.writeOpenTag(PriorParsers.POISSON_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.MEAN, "" + parameter.mean),
                                new Attribute.Default<String>(PriorParsers.OFFSET, "" + parameter.offset)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.POISSON_PRIOR);
                break;
            case TRUNC_NORMAL_PRIOR:
                writer.writeOpenTag(PriorParsers.UNIFORM_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.LOWER, "" + parameter.lower),
                                new Attribute.Default<String>(PriorParsers.UPPER, "" + parameter.upper)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.UNIFORM_PRIOR);
                writer.writeOpenTag(PriorParsers.NORMAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(PriorParsers.MEAN, "" + parameter.mean),
                                new Attribute.Default<String>(PriorParsers.STDEV, "" + parameter.stdev)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(PriorParsers.NORMAL_PRIOR);
                break;
            default:
                throw new IllegalArgumentException("Unknown priorType");
        }
    }

    private void writeParameterIdref(XMLWriter writer, dr.app.beauti.options.Parameter parameter) {
        if (parameter.isStatistic) {
            writer.writeIDref("statistic", parameter.getName());
        } else {
            writer.writeIDref(ParameterParser.PARAMETER, parameter.getName());
        }
    }

}