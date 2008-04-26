/*
 * Statistic.java
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

import dr.inference.loggers.LogColumn;
import dr.inference.loggers.Loggable;
import dr.inference.loggers.NumberColumn;
import dr.util.Attribute;
import dr.util.Identifiable;

/**
 * @version $Id: Statistic.java,v 1.8 2005/05/24 20:26:00 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public interface Statistic extends Attribute<double[]>, Identifiable, Loggable {
	
	public static final String NAME = "name";

	/** @return the name of this statistic */
	String getStatisticName();
	
	/** @return the statistic's name for a given dimension */
	String getDimensionName(int dim);

	/** @return the number of dimensions that this statistic has. */
	int getDimension();
	
	/** @return the statistic's scalar value in the given dimension */
	double getStatisticValue(int dim);
	
	
	/**
	 * Abstract base class for Statistics
	 *
	 */
	public abstract class Abstract implements Statistic {
		
		private String name = null;
	
		public Abstract() { this.name = null; }
		public Abstract(String name) { this.name = name; }
		
		public String getStatisticName() {
			if (name != null) {
				return name;
			} else if (id != null) {
				return id;
			} else {
				return getClass().toString();
			}
		}	
			
		public String getDimensionName(int dim) {
			if (getDimension() == 1) {
				return getStatisticName();
			} else {
				return getStatisticName() + Integer.toString(dim+1);
			}
		}	
			
		public String toString() {
			StringBuffer buffer = new StringBuffer(String.valueOf(getStatisticValue(0)));
			
			for (int i = 1; i < getDimension(); i++) {
                buffer.append(", ").append(String.valueOf(getStatisticValue(i)));
			}
			return buffer.toString();
		}

        // **************************************************************
        // Attribute IMPLEMENTATION
        // **************************************************************

        public final String getAttributeName() {
            return getStatisticName();
        }
        
        public final double[] getAttributeValue() {
            double[] stats = new double[getDimension()];
            for (int i = 0; i < stats.length; i++) {
                stats[i] = getStatisticValue(i);
            }

            return stats;
        }

	    // **************************************************************
	    // Identifiable IMPLEMENTATION
	    // **************************************************************

		protected String id = null;

		/**
		 * @return the id.
		 */
		public String getId() {
			return id;
		}

		/**
		 * Sets the id.
		 */
		public void setId(String id) {
			this.id = id;
		}

	    // **************************************************************
	    // Loggable IMPLEMENTATION
	    // **************************************************************

		/**
		 * @return the log columns.
		 */
		public LogColumn[] getColumns() {
			LogColumn[] columns = new LogColumn[getDimension()];
			for (int i = 0; i < getDimension(); i++) {
				columns[i] = new StatisticColumn(getDimensionName(i), i);
			}
			return columns;
		}

		private class StatisticColumn extends NumberColumn {
			private int dim;
			public StatisticColumn(String label, int dim) { super(label); this.dim = dim; }
			public double getDoubleValue() { return getStatisticValue(dim); }
		}
	}
}
