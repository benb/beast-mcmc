package dr.evomodel.tree;

import dr.evolution.tree.NodeAttributeProvider;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marc A. Suchard
 */
public class NodeTraitLogger implements NodeAttributeProvider {

	public static final String TRAIT_LOGGER = "logAllTraits";

	private TreeModel treeModel;

	public NodeTraitLogger(TreeModel treeModel) {
		this.treeModel = treeModel;
	}

	public String[] getNodeAttributeLabel() {
		return getAllNodeTraitLabels(treeModel);
	}

	public String[] getAttributeForNode(Tree tree, NodeRef node) {
		return getAllNodeTraitValues(treeModel, node);
	}

	public static String[] getAllNodeTraitLabels(TreeModel tree) {

		Map<String, Parameter> traits = tree.getTraitMap(tree.getRoot());
		List<String> labels = new ArrayList<String>();
		for (String traitName : traits.keySet()) {

			Parameter traitParameter = traits.get(traitName);
			if (traitParameter.getDimension() == 1)
				labels.add(traitName);
			else {
				for (int i = 1; i <= traitParameter.getDimension(); i++)
					labels.add(traitName + i);
			}
		}
		return labels.toArray(new String[labels.size()]);
	}

	public static String[] getAllNodeTraitValues(TreeModel tree, NodeRef node) {

		Map<String, Parameter> traits = tree.getTraitMap(node);
		List<String> values = new ArrayList<String>();
		for (String traitName : traits.keySet()) {

			Parameter traitParameter = traits.get(traitName);

			for (int i = 0; i < traitParameter.getDimension(); i++)
				values.add(Double.toString(traitParameter.getParameterValue(i)));
		}
		return values.toArray(new String[values.size()]);
	}

	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() {
			return TRAIT_LOGGER;
		}

		/**
		 * @return an object based on the XML element it was passed.
		 */
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);

			try {

				//Map<String, Parameter> traits = treeModel.getTraitMap(treeModel.getRoot());
				return new NodeTraitLogger(treeModel);

			} catch (IllegalArgumentException e) {

				throw new XMLParseException("Tree " + treeModel.getId() + " contains no traits to log");
			}


		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{

				new ElementRule(TreeModel.class, "The tree which is to be logged")
		};

		public String getParserDescription() {
			return null;
		}

		public String getExample() {
			return null;
		}

		public Class getReturnType() {
			return NodeAttributeProvider.class;
		}
	};


}
