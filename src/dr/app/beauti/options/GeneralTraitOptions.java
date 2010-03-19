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

package dr.app.beauti.options;

import java.util.List;

/**
 * @author Walter Xie
 */
public class GeneralTraitOptions extends TraitsOptions {

    public static final String TREE_FILE_NAME = "trees";


    public GeneralTraitOptions(BeautiOptions options) {
         super(options);
    }

    @Override
    protected void initTraitParametersAndOperators() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    /**
     * return a list of parameters that are required
     *
     * @param params the parameter list
     */
    public void selectParameters(List<Parameter> params) {



    }

    /**
     * return a list of operators that are required
     *
     * @param ops the operator list
     */
    public void selectOperators(List<Operator> ops) {

    }

    /////////////////////////////////////////////////////////////

    public boolean isPhylogeographic() {
        return containTrait(TraitGuesser.Traits.TRAIT_LOCATIONS.toString());
    }

}
