/*
 * ElementRule.java
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
import java.util.TreeSet;

/**
 * A syntax rule to ensure that exactly one of the given element
 * appears as a child.
 *
 * @version $Id: ElementRule.java,v 1.22 2005/05/24 20:26:01 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class ElementRule implements XMLSyntaxRule {
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(Class type) {
		this(type, null, null, 1, 1);
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(Class type, boolean optional) {
		this(type, null, null, (optional ? 0 : 1), 1);
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(Class type, String description) {
		this(type, description, null, 1, 1);
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(Class type, int min, int max) {
		this(type, null, null, min, max);
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(Class type, String description, String example) {
		this(type, description, example, 1, 1);
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(Class type, String description, int min, int max) {
		this(type, description, null, min, max);
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(Class type, String description, String example, int min, int max) {
		if (type == null) throw new IllegalArgumentException("Class cannot be null!");
		this.c = type;
		this.min = min;
		this.max = max;
		this.description = description;
		this.example = example;
	}
	
	
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(String name, Class type) {
		this.name = name;
		this.rules = new XMLSyntaxRule[] { new ElementRule(type)};
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(String name, Class type, String description) {
		this.name = name;
		this.description = description;
		this.rules = new XMLSyntaxRule[] { new ElementRule(type)};
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(String name, Class type, String description, boolean optional) {
		this.name = name;
		this.description = description;
		this.rules = new XMLSyntaxRule[] { new ElementRule(type)};
		this.min = 1;
		this.max = 1;
		if (optional) this.min = 0;
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(String name, Class type, String description, int min, int max) {
		this.name = name;
		this.description = description;
		this.rules = new XMLSyntaxRule[] { new ElementRule(type)};
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(String name, XMLSyntaxRule[] rules) {
		this.name = name;
		this.rules = rules;
	}
	
	/**
	 * Creates an element rule.
	 */
	public ElementRule(String name, XMLSyntaxRule[] rules, boolean optional) {
		this.name = name;
		this.rules = rules;
		this.min = 1;
		this.max = 1;
		if (optional) this.min = 0;
	}
	
	/**
	 * Creates an element rule.
	 */
	public ElementRule(String name, XMLSyntaxRule[] rules, int min, int max) {
		this.name = name;
		this.rules = rules;
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Creates a required element rule.
	 */
	public ElementRule(String name, XMLSyntaxRule[] rules, String description) {
		this.name = name;
		this.rules = rules;
		this.description = description;
	}
	
	public Class getElementClass() { return c; }
	
	public String getDescription() {
		return description;
	}
	
	public boolean hasDescription() { return description != null; }
	
	public String getExample() {
		return example;
	}
	
	public boolean hasExample() { return example != null; }
	

	/**
	 * @return true if the required attribute of the correct type is present.
	 */
	public boolean isSatisfied(XMLObject xo) {
		
		// first check if no matches and its optional
		int nameCount = 0;
		for (int i = 0; i < xo.getChildCount(); i++) {
			Object xoc = xo.getChild(i);
			if (xoc instanceof XMLObject && ((XMLObject)xoc).getName().equals(name)) {
				nameCount += 1;
			}
		}
		
		if (min == 0 && nameCount == 0) return true;
		
		// if !optional or nameCount > 0 then check if exactly one match exists
		int matchCount = 0;
		for (int i = 0; i < xo.getChildCount(); i++) {
			Object xoc = xo.getChild(i);
			if (isCompatible(xoc)) {
				matchCount += 1;
			}
		}
		
		return (matchCount >= min && matchCount <= max);
	}
	
	/**
	 * @return a string describing the rule.
	 */
	public String ruleString() {
		if (c != null) {
			return "ELEMENT of type " + getTypeName() + " REQUIRED";
		} else {
			StringBuffer buffer = new StringBuffer("Exactly one ELEMENT of name " + name + " REQUIRED containing");
			for (int i =0;i < rules.length; i++) {
				buffer.append("\n    " + rules[i].ruleString());
			}
			return buffer.toString();
		}
	}
	
	/**
	 * @return a string describing the rule.
	 */
	public String htmlRuleString(XMLDocumentationHandler handler) {
		if (c != null) {
			String html = "<div class=\"" + (min == 0 ? "optional" : "required") + "rule\">" + handler.getHTMLForClass(c);
			if (max > 1) {
				html += " elements (";
				if (min == 0) {
					html += "zero"; 
				} else if (min == 1) { 
					html += "one"; 
				} else if (min == max) {
					html += "exactly " + min; 
				}	
				if (max != min) {
					if (max < Integer.MAX_VALUE) {
						html += " to " + max;
					} else {
						html += " or more";
					}
				}
			} else {
				html += " element (";
				if (min == 0) {
					html += "zero or one";
				} else {
					html += "exactly one";
				}
			}
			html += ")";
			
			if (hasDescription()) {
				html += "<div class=\"description\">" + getDescription() + "</div>\n";
			}
			return html + "</div>\n";
			
		} else {
			StringBuffer buffer = new StringBuffer("<div class=\"" + (min == 0 ? "optional" : "required") + "compoundrule\">Element named <span class=\"elemname\">" + name + "</span> containing:");
			for (int i =0;i < rules.length; i++) {
				buffer.append(rules[i].htmlRuleString(handler));
			}
			if (hasDescription()) {
				buffer.append("<div class=\"description\">" + getDescription() + "</div>\n");
			}
			buffer.append("</div>\n");
			return buffer.toString();
		}
	
		
	}
	
	/**
	 * @return a string describing the rule.
	 */
	public String ruleString(XMLObject xo) {
		return ruleString();
	}

	
	public boolean isAttributeRule() { return false; }
	
	public String getName() { return name; }
	
	public XMLSyntaxRule[] getRules() { return rules; }

	/**
	 * @return true if the given object is compatible with the required class.
	 */
	private final boolean isCompatible(Object o) {
		
		if (rules != null) {
			
			if (o instanceof XMLObject) {
				XMLObject xo = (XMLObject)o;
				
				if (xo.getName().equals(name)) {
					for (int i =0; i < rules.length; i++) {
						if (!rules[i].isSatisfied(xo)) {
							return false;
						}
					}
					
					return true;
				}
			}
		} else {
			
			if (c == null) {
				return true;
			}
			
			if (c.isInstance(o)) { 
				return true; 
			}
			
			if (o instanceof String) {
				
				if (c == Double.class) {
					try {
						double d = Double.parseDouble((String)o);
						return true;
					} catch (NumberFormatException nfe) { return false; }
				}
				if (c == Integer.class) {
					try {
						int j = Integer.parseInt((String)o);
						return true;
					} catch (NumberFormatException nfe) { return false; }
				}
				if (c == Float.class) {
					try {
						double f = Float.parseFloat((String)o);
						return true;
					} catch (NumberFormatException nfe) { return false; }
				}
				if (c == Boolean.class) {
					return (o.equals("true") || o.equals("false"));	
				}
				if (c == Number.class) {
					try {
						double d = Double.parseDouble((String)o);
						return true;
					} catch (NumberFormatException nfe) { return false; }
				}
			}
		}
				
		return false;
	}
	
	/**
	 * @return a pretty name for a class.
	 */
	private final String getTypeName() {
	
		if (c == null) return "Object";
		String name = c.getName();
		return name.substring(name.lastIndexOf('.')+1);
	}
	
	/**
	 * @return a set containing the required types of this rule.
	 */
	public Set getRequiredTypes() { 
		if (c != null) {
			return Collections.singleton(c); 
		} else {
			Set set = new TreeSet(ClassComparator.INSTANCE);
		
			for (int i = 0; i < rules.length; i++) {
				set.addAll(rules[i].getRequiredTypes());
			}
			return set;
		}
	}
	
	public int getMin() { return min; }
	public int getMax() { return max; }
	
	public String toString() { return ruleString(); }
		
	private Class c = null;
	private String name = null;
	private XMLSyntaxRule[] rules = null;

	private int min = 1;
	private int max = 1;

	private String description = null;
	private String example = null;
	
}
