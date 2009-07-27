package dr.evomodel.continuous;

import dr.evolution.tree.Tree;
import dr.evolution.tree.NodeRef;
import dr.xml.*;
import dr.evomodel.tree.TreeModel;
import dr.evomodel.tree.TreeStatistic;
import dr.inference.model.Statistic;

/**
 * @author Marc Suchard
 * @author Philippe Lemey
 */
public class TreeDispersionStatistic extends Statistic.Abstract implements TreeStatistic {

    public static final String TREE_DISPERSION_STATISTIC = "treeDispersionStatistic";
    public static final String BOOLEAN_OPTION = "greatCircleDistance";

    public TreeDispersionStatistic(String name, TreeModel tree, SampledMultivariateTraitLikelihood traitLikelihood,
                                   boolean genericOption) {
        super(name);
        this.tree = tree;
        this.traitLikelihood = traitLikelihood;
        this.genericOption = genericOption;
    }

    public void setTree(Tree tree) {
        this.tree = (TreeModel) tree;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        return 1;
    }

    /**
     * @return whatever Philippe wants
     */
    public double getStatisticValue(int dim) {

        String traitName = traitLikelihood.getTraitName();        
        double treelength = 0;
        double treeDistance = 0;

        for (int i = 0; i < tree.getNodeCount(); i++) {
            NodeRef node = tree.getNode(i);
            double[] trait = tree.getMultivariateNodeTrait(node, traitName);

            if (node != tree.getRoot()) {

                double[] parentTrait = tree.getMultivariateNodeTrait(tree.getParent(node), traitName);
                treelength += tree.getBranchLength(node);

                if (genericOption) {
                    treeDistance += getKilometerGreatCircleDistance(trait,parentTrait);
                } else {
                    treeDistance += getNativeDistance(trait,parentTrait);
                }

            }
        }
        return treeDistance/treelength;
    }

    private double getNativeDistance(double[] location1, double[] location2) {
        return Math.sqrt(Math.pow((location2[0]-location1[0]),2.0)+Math.pow((location2[1]-location1[1]),2.0));
    }
    private double getKilometerGreatCircleDistance(double[] location1, double[] location2) {
        double R = 6371; // km
        double dLat = Math.toRadians(location2[0]-location1[0]);
        double dLon = Math.toRadians(location2[1]-location1[1]);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(location1[0])) * Math.cos(Math.toRadians(location2[0])) * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return TREE_DISPERSION_STATISTIC;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            String name = xo.getAttribute(NAME, xo.getId());
            TreeModel tree = (TreeModel) xo.getChild(Tree.class);

            boolean option = xo.getAttribute(BOOLEAN_OPTION,false); // Default value is false

            SampledMultivariateTraitLikelihood traitLikelihood = (SampledMultivariateTraitLikelihood)
                    xo.getChild(SampledMultivariateTraitLikelihood.class);

            return new TreeDispersionStatistic(name, tree, traitLikelihood, option);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A statistic that returns the average of the branch rates";
        }

        public Class getReturnType() {
            return TreeStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newStringRule(NAME, true),
                AttributeRule.newBooleanRule(BOOLEAN_OPTION,true),
                new ElementRule(TreeModel.class),
                new ElementRule(SampledMultivariateTraitLikelihood.class),
        };
    };

    private TreeModel tree = null;
    private boolean genericOption;
    private SampledMultivariateTraitLikelihood traitLikelihood;
}
