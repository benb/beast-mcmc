/*
 * BirthDeathSSLikelihoodTest.java
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

package test.dr.evomodel.speciation;

import dr.evolution.io.NewickImporter;
import dr.evolution.tree.FlexibleTree;
import dr.evolution.tree.Tree;
import dr.evolution.util.Units;
import dr.evomodel.speciation.BirthDeathSerialSamplingModel;
import dr.evomodel.speciation.SpeciationLikelihood;
import dr.evomodel.speciation.SpeciationModel;
import dr.evomodelxml.tree.TreeModelParser;
import dr.inference.model.Likelihood;
import dr.inference.model.Variable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * YuleModel Tester.
 *
 * @author Alexei Drummond
 * @version 1.0
 * @since <pre>08/26/2007</pre>
 */
public class BirthDeathSSLikelihoodTest extends TestCase {

    static final String TL = "TL";
    static final String TREE_HEIGHT = TreeModelParser.ROOT_HEIGHT;

    private FlexibleTree tree;

    public BirthDeathSSLikelihoodTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();

        NewickImporter importer = new NewickImporter("((1:1.0,2:1.0):1.0,3:2.0);");
        tree = (FlexibleTree) importer.importTree(null);
    }

    final double birthRate = 1.0;
    final double deathRate = 0.5;
    final double p = 1.0;
    final double psi = 0.0;

    public void testP0() {
        assertEquals(BirthDeathSerialSamplingModel.p0(birthRate, deathRate, p, psi, 1.0), 0.28236670080320814);
    }

    public void testP1() {
        assertEquals(BirthDeathSerialSamplingModel.p1(birthRate, deathRate, p, psi, 1.0), 0.31236180503535266);
    }

    public void testPureBirthLikelihood() {

        //likelihoodTester(tree, 1, 0, -2.8219461696520542);

    }

    public void testBirthDeathLikelihood() {
        likelihoodTester(tree, birthRate, deathRate, -3.534621219768513);
    }

    private void likelihoodTester(Tree tree, double birthRate, double deathRate, double logL) {

        Variable<Double> b = new Variable.D("b", birthRate);
        Variable<Double> d = new Variable.D("d", deathRate);
        Variable<Double> psi = new Variable.D("psi", this.psi);
        Variable<Double> p = new Variable.D("p", this.p);

        SpeciationModel speciationModel = new BirthDeathSerialSamplingModel(b, d, psi, p, Units.Type.YEARS);
        Likelihood likelihood = new SpeciationLikelihood(tree, speciationModel, "bdss.like");

        assertEquals(logL, likelihood.getLogLikelihood());
    }

    public static Test suite() {
        return new TestSuite(BirthDeathSSLikelihoodTest.class);
    }
}