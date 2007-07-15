/*
 * ContentRule.java
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

package dr.xml;

import java.util.Collections;
import java.util.Set;

/**
 * A syntax rule to ensure that allows one to document arbitrary content.
 */
public class ContentRule implements XMLSyntaxRule {

	/**
	 * Creates a required element rule.
	 */
	public ContentRule(String htmlDescription) {
		this.htmlDescription = htmlDescription;
	}

	/**
	 * @return true
	 */
	public boolean isSatisfied(XMLObject xo) { return true; }

    public boolean containsAttribute(String name) {
        return false;
    }

    /**
	 * @return a string describing the rule.
	 */
	public String ruleString() { return htmlDescription; }

	/**
	 * @return a string describing the rule.
	 */
	public String htmlRuleString(XMLDocumentationHandler handler) {
		return htmlDescription;
	}

	/**
	 * @return a string describing the rule.
	 */
	public String wikiRuleString(XMLDocumentationHandler handler) {
		return ":" + htmlDescription;
	}

	/**
	 * @return a string describing the rule.
	 */
	public String ruleString(XMLObject xo) { return null; }

	/**
	 * @return a set containing the required types of this rule.
	 */
	public Set<Class> getRequiredTypes() { return Collections.EMPTY_SET; }


	public boolean isAttributeRule() { return false; }

	private String htmlDescription;
}
