package dr.evomodel.treelikelihood;

import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.evolution.alignment.PatternList;
import dr.evolution.util.TaxonList;
import dr.evomodel.tree.TreeModel;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id$
 */
public abstract class TipPartialsModel extends AbstractModel {

	/**
	 * @param name Model Name
	 */
	public TipPartialsModel(String name, TreeModel treeModel, TaxonList includeTaxa, TaxonList excludeTaxa) {
		super(name);

		this.treeModel = treeModel;

		int extNodeCount = treeModel.getExternalNodeCount();

		excluded = new boolean[extNodeCount];
		if (includeTaxa != null) {
			for (int i = 0; i < extNodeCount; i++) {
				if (includeTaxa.getTaxonIndex(treeModel.getNodeTaxon(treeModel.getExternalNode(i))) == -1) {
					excluded[i] = true;
				}
			}
		}

		if (excludeTaxa != null) {
			for (int i = 0; i < extNodeCount; i++) {
				if (excludeTaxa.getTaxonIndex(treeModel.getNodeTaxon(treeModel.getExternalNode(i))) != -1) {
					excluded[i] = true;
				}
			}

		}

		states = new int[extNodeCount][];
		partials = new double[extNodeCount][];
	}


	public final void setStates(PatternList patternList, int sequenceIndex, int nodeIndex) {
		if (patternCount == 0) {
			patternCount = patternList.getPatternCount();
			stateCount = patternList.getDataType().getStateCount();
		}

		int[] states = new int[patternCount];

		for (int i = 0; i < patternCount; i++) {

			states[i] = patternList.getPatternState(sequenceIndex, i);
		}

		if (this.states[nodeIndex] == null) {
			this.states[nodeIndex] = new int[patternCount];
			this.partials[nodeIndex] = new double[patternCount * stateCount];
		}
		System.arraycopy(states, 0, this.states[nodeIndex], 0, patternCount);
	}

	protected void handleModelChangedEvent(Model model, Object object, int index) {
		fireModelChanged();
	}

	/**
	 * This method is called whenever a parameter is changed.
	 * <p/>
	 * It is strongly recommended that the model component sets a "dirty" flag and does no
	 * further calculations. Recalculation is typically done when the model component is asked for
	 * some information that requires them. This mechanism is 'lazy' so that this method
	 * can be safely called multiple times with minimal computational cost.
	 */
	protected void handleParameterChangedEvent(Parameter parameter, int index) {
		updatePartials = true;
		fireModelChanged();
	}

	/**
	 * Additional state information, outside of the sub-model is stored by this call.
	 */
	protected void storeState() {
	}

	/**
	 * After this call the model is guaranteed to have returned its extra state information to
	 * the values coinciding with the last storeState call.
	 * Sub-models are handled automatically and do not need to be considered in this method.
	 */
	protected void restoreState() {
		updatePartials = true;
	}

	/**
	 * This call specifies that the current state is accept. Most models will not need to do anything.
	 * Sub-models are handled automatically and do not need to be considered in this method.
	 */
	protected void acceptState() {
	}


	public abstract double[] getTipPartials(int nodeIndex);

	protected int[][] states;
	protected double[][] partials;
	protected boolean[] excluded;

	protected int patternCount = 0;
	protected int stateCount;

	protected final TreeModel treeModel;

	protected boolean updatePartials = true;
}
