package test.dr.integration;

import dr.evolution.alignment.SitePatterns;
import dr.evolution.datatype.Nucleotides;
import dr.evolution.tree.SimpleNode;
import dr.evolution.tree.SimpleTree;
import dr.evolution.tree.Tree;
import dr.evolution.util.Units;
import dr.evomodel.sitemodel.GammaSiteModel;
import dr.evomodel.substmodel.FrequencyModel;
import dr.evomodel.substmodel.HKY;
import dr.evomodel.tree.TreeModel;
import dr.evomodel.treelikelihood.TreeLikelihood;
import dr.evomodelxml.HKYParser;
import dr.inference.model.Parameter;
import dr.math.MathUtils;
import junit.framework.Test;
import junit.framework.TestSuite;
import test.dr.inference.trace.TraceCorrelationAssert;


/**
 * @author Walter Xie
 * convert testLikelihood.xml in the folder /example
 */

public class LikelihoodTest extends TraceCorrelationAssert {

    private TreeModel treeModel;

    public LikelihoodTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();

        MathUtils.setSeed(666);

        createAlignment(HOMINID_TAXON_SEQUENCE, Nucleotides.INSTANCE);

    }

    public void testNewickTree() {

        SimpleNode[] nodes = new SimpleNode[10];
        for (int n=0; n < 10; n++) {
            nodes[n] = new SimpleNode();
        }

//        nodes[0].setHeight(0);
        nodes[0].setTaxon(taxa[0]); // human

        nodes[1].setTaxon(taxa[1]); // chimp

        nodes[2].setTaxon(taxa[2]); // bonobo

        nodes[3].setHeight(0.010772);
        nodes[3].addChild(nodes[1]);
        nodes[3].addChild(nodes[2]);

        nodes[4].setHeight(0.024003);
        nodes[4].addChild(nodes[0]);
        nodes[4].addChild(nodes[3]);

        nodes[5].setTaxon(taxa[3]); // gorilla

        nodes[6].setHeight(0.036038);
        nodes[6].addChild(nodes[4]);
        nodes[6].addChild(nodes[5]);

        nodes[7].setTaxon(taxa[4]); // orangutan

        nodes[8].setHeight(0.069125);
        nodes[8].addChild(nodes[6]);
        nodes[8].addChild(nodes[7]);

        nodes[9].setTaxon(taxa[5]); // siamang

        SimpleNode root = new SimpleNode();
        root.setHeight(0.099582);
        root.addChild(nodes[8]);
        root.addChild(nodes[9]);

        Tree tree = new SimpleTree(root);
        tree.setUnits(Units.Type.YEARS);

        treeModel = new TreeModel(tree); //treeModel

        String expectedNewickTree = "((((human:0.024003,(chimp:0.010772,bonobo:0.010772):0.013231):0.012035," +
                "gorilla:0.036038):0.033087,orangutan:0.069125):0.030457,siamang:0.099582);";
        
        assertEquals("Fail to covert the correct tree !!!", expectedNewickTree, Tree.Utils.newick(treeModel, 6));
    }



    public void testLikelihoodJC69() { 
        // Sub model
        Parameter freqs = new Parameter.Default(new double[]{0.25, 0.25, 0.25, 0.25});
        Parameter kappa = new Parameter.Default(HKYParser.KAPPA, 1.0, 0, 100);

        FrequencyModel f = new FrequencyModel(Nucleotides.INSTANCE, freqs);
        HKY hky = new HKY(kappa, f);

        //siteModel
        GammaSiteModel siteModel = new GammaSiteModel(hky);
        Parameter mu = new Parameter.Default(GammaSiteModel.MUTATION_RATE, 1.0, 0, Double.POSITIVE_INFINITY);
        siteModel.setMutationRateParameter(mu);

        //treeLikelihood
        SitePatterns patterns = new SitePatterns(alignment, null, 0, -1, 1, true);

        TreeLikelihood treeLikelihood = new TreeLikelihood(patterns, treeModel, siteModel, null, null,
                false, false, true, false, false);
        treeLikelihood.setId("treeLikelihoodJC69");

//      <expectation name="likelihood" value="-1992.20564"/>
//        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, treeLikelihood, -1992.20564);

    }

    public void testLikelihoodK80() {
        // Sub model
        Parameter freqs = new Parameter.Default(new double[]{0.25, 0.25, 0.25, 0.25});
        Parameter kappa = new Parameter.Default(HKYParser.KAPPA, 27.402591, 0, 100);

        FrequencyModel f = new FrequencyModel(Nucleotides.INSTANCE, freqs);
        HKY hky = new HKY(kappa, f);

        //siteModel
        GammaSiteModel siteModel = new GammaSiteModel(hky);
        Parameter mu = new Parameter.Default(GammaSiteModel.MUTATION_RATE, 1.0, 0, Double.POSITIVE_INFINITY);
        siteModel.setMutationRateParameter(mu);

        //treeLikelihood
        SitePatterns patterns = new SitePatterns(alignment, null, 0, -1, 1, true);

        TreeLikelihood treeLikelihood = new TreeLikelihood(patterns, treeModel, siteModel, null, null,
                false, false, true, false, false);
        treeLikelihood.setId("treeLikelihoodK80");

//      <expectation name="likelihood" value="-1856.30305"/>
//        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, treeLikelihood, -1856.30305);

    }

    public void testLikelihoodHKY85() {
        // Sub model
        Parameter freqs = new Parameter.Default(alignment.getStateFrequencies());
        Parameter kappa = new Parameter.Default(HKYParser.KAPPA, 29.739445, 0, 100);

        FrequencyModel f = new FrequencyModel(Nucleotides.INSTANCE, freqs);
        HKY hky = new HKY(kappa, f);

        //siteModel
        GammaSiteModel siteModel = new GammaSiteModel(hky);
        Parameter mu = new Parameter.Default(GammaSiteModel.MUTATION_RATE, 1.0, 0, Double.POSITIVE_INFINITY);
        siteModel.setMutationRateParameter(mu);

        //treeLikelihood
        SitePatterns patterns = new SitePatterns(alignment, null, 0, -1, 1, true);

        TreeLikelihood treeLikelihood = new TreeLikelihood(patterns, treeModel, siteModel, null, null,
                false, false, true, false, false);
        treeLikelihood.setId("treeLikelihoodHKY85");

//      <expectation name="likelihood" value="-1825.21317"/>
//        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, treeLikelihood, -1825.21317);

    }

    public static Test suite() {
        return new TestSuite(LikelihoodTest.class);
    }
}
