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
 */
public enum ClockType {

    STRICT_CLOCK("Strict Clock"),
    UNCORRELATED_EXPONENTIAL("Relaxed Clock: Uncorrelated Exp"),
    UNCORRELATED_LOGNORMAL("Relaxed Clock: Uncorrelated Lognormal"),
    RANDOM_LOCAL_CLOCK("Random local clock model"),
    AUTOCORRELATED_LOGNORMAL("Relaxed Clock: Autocorrelated Lognormal");


    ClockType(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return displayName;
    }

    private final String displayName;

    final public static String LOCAL_CLOCK = "localClock";
    final public static String UCED_MEAN = "uced.mean";
    final public static String UCLD_MEAN = "ucld.mean";
    final public static String UCLD_STDEV = "ucld.stdev";
}
