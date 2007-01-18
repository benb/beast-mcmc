/*
 * Units.java
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

package dr.evolution.util;

/**
 * interface holding unit constants
 *
 * @version $Id: Units.java,v 1.17 2005/05/24 20:25:57 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public interface Units {

    public enum Type {
       SUBSTITUTIONS, GENERATIONS, DAYS, MONTHS, YEARS
    }

	/**
	 * @return the units for this object.
	 */
	Type getUnits();

	/**
	 * Sets the units for this object.
     * @param units to use
     */
	void setUnits(Type units);

    // array of unit names
    // second dimension is to allow synonyms -- first element is default
	public String[][] UNIT_NAMES = {{"substitutions","mutations"}, {"generations"}, {"days"}, {"months"}, {"years"}};

    public class Utils {

        public static String getDefaultUnitName(Type i) {
            return UNIT_NAMES[i.ordinal()][0];
        }
    }
}