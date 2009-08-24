/*
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

package dr.app.beauti.enumTypes;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public enum FixRateType {
	FIX_MEAN("Estimate relative rate by fixing mean"), // 
    RElATIVE_TO("Estimate rate relative to ?th partition rate"),
	TIP_CALIBRATED("Tip time is calibrated"), // 
	NODE_CALIBRATED("Tree root height is calibrated"), // 
	RATE_CALIBRATED("Rate is calibrated"); // 
    
	FixRateType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    private final String name;
}
