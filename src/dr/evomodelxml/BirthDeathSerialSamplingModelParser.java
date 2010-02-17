/*
 * BirthDeathModelParser.java
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

package dr.evomodelxml;

import dr.evolution.util.Units;
import dr.evomodel.speciation.BirthDeathGernhard08Model;
import dr.evomodel.speciation.BirthDeathSerialSamplingModel;
import dr.evoxml.util.XMLUnits;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.logging.Logger;

/**
 * @author Alexei Drummond
 * @author Joseph Heled
 */
public class BirthDeathSerialSamplingModelParser extends AbstractXMLObjectParser {

    public static final String BIRTH_DEATH_SERIAL_MODEL = "birthDeathSerialSampling";
    public static final String LAMBDA = "birthRate";
    public static final String MU = "deathRate";
    public static final String PSI = "psi";
    public static final String SAMPLE_PROBABILITY = "sampleProbability";
    public static final String TREE_TYPE = "type";
    public static final String CONDITIONAL_ON_ROOT = "conditionalOnRoot";

    public String getParserName() {
        return BIRTH_DEATH_SERIAL_MODEL;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        final Units.Type units = XMLUnits.Utils.getUnitsAttr(xo);

        final Parameter lambda = (Parameter) xo.getElementFirstChild(LAMBDA);
        final Parameter mu = (Parameter) xo.getElementFirstChild(MU);
        final Parameter psi = (Parameter) xo.getElementFirstChild(PSI);
        final Parameter p = (Parameter) xo.getElementFirstChild(SAMPLE_PROBABILITY);


        Logger.getLogger("dr.evomodel").info("Using birth-death serial sampling model: Stadler et al (2010) in prep.");

        final String modelName = xo.getId();

        return new BirthDeathSerialSamplingModel(modelName, lambda, mu, psi, p, units);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "Stadler et al (2010; in press) model of speciation.";
    }

    public Class getReturnType() {
        return BirthDeathGernhard08Model.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            AttributeRule.newStringRule(TREE_TYPE, true),
            AttributeRule.newBooleanRule(CONDITIONAL_ON_ROOT, true),
            new ElementRule(LAMBDA, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
            new ElementRule(MU, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
            new ElementRule(PSI, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
            new ElementRule(SAMPLE_PROBABILITY, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
            XMLUnits.SYNTAX_RULES[0]
    };
}