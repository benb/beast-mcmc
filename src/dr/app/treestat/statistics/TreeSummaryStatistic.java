/*
 * TreeSummaryStatistic.java
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

package dr.app.treestat.statistics;

import dr.evolution.tree.Tree;
import dr.evolution.util.TaxonList;

import java.util.Map;

/**
 * An interface and collection of tree summary statistics.
 *
 * @version $Id: TreeSummaryStatistic.java,v 1.2 2005/09/28 13:50:56 rambaut Exp $
 *
 * @author Alexei Drummond
 */
public interface TreeSummaryStatistic extends SummaryStatisticDescription {

	int getStatisticDimensions(Tree tree);

	String getStatisticLabel(Tree tree, int i);

	/**
	 * @return the value of this summary statistic for the given tree.
	 */
	double[] getSummaryStatistic(Tree tree);

	public abstract class Factory implements SummaryStatisticDescription {
		public TreeSummaryStatistic createStatistic() {
			throw new RuntimeException("This factory method is not implemented");
		}

		public TreeSummaryStatistic createStatistic(Map<String, Object> characterMap, String characterState) {
			throw new RuntimeException("This factory method is not implemented");
		}

		public TreeSummaryStatistic createStatistic(TaxonList taxonList) {
			throw new RuntimeException("This factory method is not implemented");
		}

		public TreeSummaryStatistic createStatistic(int value) {
			throw new RuntimeException("This factory method is not implemented");
		}

		public TreeSummaryStatistic createStatistic(double value) {
			throw new RuntimeException("This factory method is not implemented");
		}

		public boolean allowsWholeTree() { return true; }
		public boolean allowsCharacter() { return false; }
		public boolean allowsCharacterState() { return false; }
		public boolean allowsTaxonList() { return false; }
		public boolean allowsInteger() { return false; }
		public boolean allowsDouble() { return false; }

		public String getValueName() { return ""; }
	}
}

