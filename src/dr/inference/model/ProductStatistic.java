/*
 * ProductStatistic.java
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

import dr.xml.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: ProductStatistic.java,v 1.2 2005/05/24 20:26:00 rambaut Exp $
 */
public class ProductStatistic extends Statistic.Abstract {

    public static String PRODUCT_STATISTIC = "productStatistic";

    private int dimension = 0;
    private final boolean elementwise;

    public ProductStatistic(String name, boolean elementwise) {
        super(name);
        this.elementwise = elementwise;
    }

    public void addStatistic(Statistic statistic) {
        if (!elementwise) {
            if (dimension == 0) {
                dimension = statistic.getDimension();
            } else if (dimension != statistic.getDimension()) {
                throw new IllegalArgumentException();
            }
        } else {
            dimension = 1;
        }
        statistics.add(statistic);
    }

    public int getDimension() {
        return dimension;
    }

    /**
     * @return mean of contained statistics
     */
    public double getStatisticValue(int dim) {

        double product = 1.0;

        for (Statistic statistic : statistics) {
            if (elementwise) {
                for (int j = 0; j < statistic.getDimension(); j++) {
                    product *= statistic.getStatisticValue(j);
                }
            } else {
                product *= statistic.getStatisticValue(dim);
            }
        }

        return product;
    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String[] getParserNames() {
            return new String[]{getParserName(), "product"};
        }

        public String getParserName() {
            return PRODUCT_STATISTIC;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {


            boolean elementwise = xo.getAttribute("elementwise", false);

            ProductStatistic productStatistic = new ProductStatistic(PRODUCT_STATISTIC, elementwise);

            for (int i = 0; i < xo.getChildCount(); i++) {
                Object child = xo.getChild(i);
                if (child instanceof Statistic) {
                    try {
                        productStatistic.addStatistic((Statistic) child);
                    } catch (IllegalArgumentException iae) {
                        throw new XMLParseException("Statistic added to " + getParserName() + " element is not of the same dimension");
                    }
                } else {
                    throw new XMLParseException("Unknown element found in " + getParserName() + " element:" + child);
                }
            }

            return productStatistic;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns a statistic that is the product of the child statistics.";
        }

        public Class getReturnType() {
            return ProductStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newBooleanRule("elementwise", true),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };
    };

    // ****************************************************************
    // Private and protected stuff
    // ****************************************************************

    private final List<Statistic> statistics = new ArrayList<Statistic>();
}
