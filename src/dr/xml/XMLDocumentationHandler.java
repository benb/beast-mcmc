/*
 * XMLDocumentationHandler.java
 *
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

package dr.xml;

import dr.app.tools.BeastParserDoc;

import java.io.PrintWriter;
import java.util.*;

public class XMLDocumentationHandler {

    private final Random random = new Random();

    public XMLDocumentationHandler(XMLParser parser) {

        this.parser = parser;

        Iterator iterator = parser.getParsers();
        while (iterator.hasNext()) {
            XMLObjectParser xmlparser = (XMLObjectParser) iterator.next();

            XMLSyntaxRule[] rules = xmlparser.getSyntaxRules();

            if (rules != null) {
                for (XMLSyntaxRule rule : rules) {
                    Set<Class> requiredTypesForRule = rule.getRequiredTypes();
                    requiredTypes.addAll(requiredTypesForRule);
                }
            }
        }
    }

    public void outputElements(PrintWriter writer) {

        writer.println("<html>");
        BeastParserDoc.printDocTitle(writer, BeastParserDoc.DEATAIL_HTML);
        writer.println("<p>");
        writer.println("The following is a list of valid elements in a beast file.<br>");
        writer.println("<span class=\"required\">&nbsp;&nbsp;&nbsp;&nbsp;</span> required<br>");
        writer.println("<span class=\"optional\">&nbsp;&nbsp;&nbsp;&nbsp;</span> optional<br>");
        writer.println("</p>");

        Iterator iterator = parser.getParsers();
        while (iterator.hasNext()) {
            XMLObjectParser xmlParser = (XMLObjectParser) iterator.next();
            writer.println(xmlParser.toHTML(this));
            System.out.println("  outputting HTML for element " + xmlParser.getParserName());
        }

        writer.println("</body>");
        writer.println("</html>");
    }

    /**
     * Outputs an example of a particular element, using the syntax information.
     * @param writer     PrintWriter
     * @param parser     XMLObjectParser
     */
    public void outputExampleXML(PrintWriter writer, XMLObjectParser parser) {

        writer.println("<pre>");
        if (parser.hasExample()) {
            outputHTMLSafeText(writer, parser.getExample());
        } else {
            outputExampleXML(writer, parser, 0);
        }
        writer.println("</pre>");
    }

    public void outputHTMLSafeText(PrintWriter writer, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<':
                    writer.print("&lt;");
                    break;
                case '>':
                    writer.print("&gt;");
                    break;
                case '&':
                    writer.print("&amp;");
                    break;
                default:
                    writer.print(c);
                    break;
            }
        }
    }

    /**
     * Outputs an example of a particular element, using the syntax information.
     * @param writer   PrintWriter
     * @param parser   XMLObjectParser
     * @param level    int
     */
    public void outputExampleXML(PrintWriter writer, XMLObjectParser parser, int level) {
        outputElementRules(writer, parser.getParserName(), parser.getSyntaxRules(), level);
    }

    public void stochasticCollectRules(XMLSyntaxRule[] allRules, ArrayList<XMLSyntaxRule> attributeList, ArrayList<ElementRule> elementList) {

        if (allRules != null) {
            for (XMLSyntaxRule rule : allRules) {

                if (rule instanceof AttributeRule) {
                    attributeList.add(rule);
                } else if (rule instanceof ElementRule) {
                    int min = ((ElementRule) rule).getMin();
                    int max = Math.max(min, Math.min(5, ((ElementRule) rule).getMax()));

                    int numRules = min;
                    if (max != min) numRules = random.nextInt(max - min) + min;
                    for (int j = 0; j < numRules; j++) {
                        elementList.add((ElementRule) rule);
                    }
                } else if (rule instanceof XORRule) {
                    XORRule xorRule = (XORRule) rule;
                    XMLSyntaxRule[] rules = xorRule.getRules();
                    int ruleIndex = random.nextInt(rules.length);
                    stochasticCollectRules(new XMLSyntaxRule[]{rules[ruleIndex]}, attributeList, elementList);
                } else if (rule instanceof OrRule) {
                    OrRule orRule = (OrRule) rule;
                    XMLSyntaxRule[] rules = orRule.getRules();
                    int ruleIndex = random.nextInt(rules.length);
                    stochasticCollectRules(new XMLSyntaxRule[]{rules[ruleIndex]}, attributeList, elementList);
                }
            }
        }
    }

    /**
     * Outputs an example of a rule, using the syntax information.
     * @param writer   PrintWriter
     * @param rule     AttributeRule
     */
    public void outputExampleXML(PrintWriter writer, AttributeRule rule) { //, int level) {
        writer.print(" " + rule.getName() + "=\"");
        if (rule.hasExample()) {
            writer.print(rule.getExample());
        } else {
            outputAttributeValue(writer, rule.getAttributeClass());
        }
        writer.print("\"");
    }

    /**
     * Outputs an example of a rule, using the syntax information.
     * @param writer  PrintWriter
     * @param rule    ElementRule
     * @param level   int
     */
    public void outputExampleXML(PrintWriter writer, ElementRule rule, int level) {

        if (rule.getElementClass() == null) {
            if (rule.getName() == null) System.err.println(rule + " has a null name");
            outputElementRules(writer, rule.getName(), rule.getRules(), level);
        } else {
            if (rule.hasExample()) {
                writer.println(spaces(level + 1) + rule.getExample());
            } else {
                outputExampleXML(writer, rule.getElementClass(), level + 1);
            }
        }
    }

    /**
     *
     * @param writer   PrintWriter
     * @param name     String
     * @param rules    XMLSyntaxRule[]
     * @param level    int
     */
    public void outputElementRules(PrintWriter writer, String name, XMLSyntaxRule[] rules, int level) {

        ArrayList<XMLSyntaxRule> attributeList = new ArrayList<XMLSyntaxRule>();
        ArrayList<ElementRule> elementList = new ArrayList<ElementRule>();
        stochasticCollectRules(rules, attributeList, elementList);

        writer.print(spaces(level) + "&lt;" + name);
        // write out the attributes
        for (XMLSyntaxRule rule : attributeList) {
            outputExampleXML(writer, (AttributeRule) rule); //, level + 1);
        }
        if (elementList.size() > 0) {
            writer.println("&gt;");
            // write out the elements
            for (ElementRule rule : elementList) {
                outputExampleXML(writer, rule, level + 1);
            }
            writer.println(spaces(level) + "&lt;/" + name + "&gt;");
        } else {
            writer.println("/&gt;");
        }
    }

    public void outputExampleXML(PrintWriter writer, Class c, int level) {

        if (c == String.class) {
            writer.println(spaces(level) + "foo");
        } else if (c == Double.class) {
            writer.println(spaces(level) + "1.0");
        } else if (c == Integer.class) {
            writer.println(spaces(level) + "1");
        } else if (c == Boolean.class) {
            writer.println(spaces(level) + "true");
        } else if (c == Double[].class) {
            writer.println(spaces(level) + "0.5 1.0");
        } else if (c == String[].class) {
            writer.println(spaces(level) + "foo bar");
        } else {
            if (c == null) {
                throw new RuntimeException("Class is null");
            }
            XMLObjectParser randomParser = getRandomParser(c);
            if (randomParser == null) {
                writer.println(spaces(level) + "ERROR!");
            } else {
                if (level > 1) {
                    writer.println(spaces(level) + "&lt;" + randomParser.getParserName() +
                            " idref=\"" + randomParser.getParserName() + (random.nextInt(10) + 1) + "\"/&gt;");
                } else {
                    outputExampleXML(writer, randomParser, level);
                }
            }
        }

    }

    public void outputAttributeValue(PrintWriter writer, Class c) {
        if (c == String.class) {
            writer.print("foo");
        } else if (c == Double.class) {
            writer.print("1.0");
        } else if (c == Integer.class) {
            writer.print("1");
        } else if (c == Boolean.class) {
            writer.print("true");
        } else if (c == Double[].class) {
            writer.print("0.5 1.0");
        } else if (c == Integer[].class) {
            writer.print("1 2 4 8");
        } else if (c == String[].class) {
            writer.print("foo bar");
        } else {
            throw new RuntimeException("Class " + c + " not allowed as attribute value");
        }
    }

    private String spaces(int level) {
        StringBuffer buffer = new StringBuffer("");
        for (int i = 0; i < level; i++) {
            buffer.append(' ');
        }
        return buffer.toString();
    }

    public XMLObjectParser getRandomParser(Class c) {

        ArrayList<XMLObjectParser> matchingParsers = getMatchingParsers(c);

        if (matchingParsers.size() == 0) return null;
        return matchingParsers.get(random.nextInt(matchingParsers.size()));
    }

    public final ArrayList<XMLObjectParser> getMatchingParsers(Class c) {

        ArrayList<XMLObjectParser> matchingParsers = new ArrayList<XMLObjectParser>();
        // find all parsers that match this required type
        Iterator i = parser.getParsers();
        while (i.hasNext()) {
//            final XMLObjectParser xmlParser = (XMLObjectParser) i.next();
//            final Class returnType = xmlParser.getReturnType();
            XMLObjectParser xmlParser = (XMLObjectParser) i.next();
            Class returnType = xmlParser.getReturnType();
            if (c.isAssignableFrom(returnType)) {
                matchingParsers.add(xmlParser);
            }
        }
        return matchingParsers;
    }

    /**
     * Outputs all types that appear as required attributes or elements in an HTML table to the given writer.
     * @param writer  PrintWriter
     */
    public void outputIndex(PrintWriter writer) {

        writer.println("<html>");
        BeastParserDoc.printDocTitle(writer, BeastParserDoc.INDEX_HTML);
        writer.println("<p>");
        writer.println("The following is a list of generic types that elements represent in a beast file.<br>");
        writer.println("</p>");


        // iterate through the types
        //Iterator iterator = requiredTypes.iterator();
        for (Class requiredType : requiredTypes) {
            if (requiredType != Object.class) {

                String name = ClassComparator.getName(requiredType);

                System.out.println("  outputting HTML for generic type " + name);

//                TreeSet<XMLObjectParser> matchingParserNames = new TreeSet<XMLObjectParser>();
                ArrayList<String> matchingParserNames = new ArrayList<String>();

                // find all parsers that match this required type
                Iterator i = parser.getParsers();
                while (i.hasNext()) {
                    XMLObjectParser xmlParser = (XMLObjectParser) i.next();
                    Class returnType = xmlParser.getReturnType();
                    if (returnType == null) {
                        System.out.println("find null Class for parser : " + xmlParser.getParserName());
                    } else if (requiredType.isAssignableFrom(returnType)) {
                        if (matchingParserNames.size() == 0) {
                            writer.println("<div id=\"" + name + "\"><h2>" + name + "</h2>");
                            writer.println("<p>");
                            writer.println("Elements of this type include:");
                            writer.println("</p>");
                        }

                        if (!matchingParserNames.contains(xmlParser.getParserName())) {
                            matchingParserNames.add(xmlParser.getParserName());                                                        
//                            writer.println(xmlParser.toHTML(this));

//                        writer.println("<div><a href=\"" + BeastParserDoc.INDEX_HTML + "#" + xmlParser.getParserName() + "\"> &lt;"
//                                + xmlParser.getParserName() + "&gt;</a></div>");
                            writer.println("<div id=\"" + xmlParser.getParserName() + "\" class=\"element\">");
//                            writer.println("  <div class=\"elementheader\">");
                            writer.println("    <span class=\"elementname\"><a href=\"" + BeastParserDoc.DEATAIL_HTML
                                     + "#" + xmlParser.getParserName() + "\"> <h3>&lt;" + xmlParser.getParserName()
                                    + "&gt;</h3></a></span>");
                            writer.println("    <div class=\"description\"><b>Description:</b><br>");
                            writer.println(xmlParser.getParserDescription());
                            writer.println("    </div>");
                            writer.println("  </div>");

//                            // print rules
//                            if (xmlParser.hasSyntaxRules()) {
//                                XMLSyntaxRule[] rules = xmlParser.getSyntaxRules();
//                                writer.println("  <div class=\"rules\"><b>Rule:</b>");
//                                for (XMLSyntaxRule rule : rules) {
//                                    writer.println(rule.htmlRuleString(this));
//                                }
//                                writer.println("  </div>");
//                            }
//
//                            // print examples
//                            if (xmlParser.hasExample()) {
//                                writer.println("<div class=\"example\"><b>Example:</b>");
//                                outputExampleXML(writer, xmlParser);
//                                writer.println("</div>");
//                            }
                            
                            writer.println("<p/>");
                        }
                    }
                }
                if (matchingParserNames.size() > 0) writer.println("</div>");
                writer.println("<p/>");

//                if (matchingParserNames.size() > 1 ||
//                        (matchingParserNames.size() == 1 && (!matchingParserNames.iterator().next().getParserName().equals(name)))) {
//                    // output table row containing the type and the matching parser names
//                    writer.println("<div id=\"" + name + "\"><h2>" + name + "</h2>");
//                    writer.println("<p>");
//                    writer.println("Elements of this type include:");
//                    writer.println("</p>");
//                    i = matchingParserNames.iterator();
//                    while (i.hasNext()) {
//                        XMLObjectParser parser = i.next();
//                        writer.println("<div><a href=\"" + BeastParserDoc.INDEX_HTML + "#" + parser.getParserName() + "\"> &lt;"
//                                + parser.getParserName() + "&gt;</a></div>");
//                        writer.println("<div>" + parser.getParserDescription() + "</div>");
//                        if (parser.hasExample()) writer.println("<div>" + parser.getExample() + "</div>");
//                        for (XMLSyntaxRule rule: parser.getSyntaxRules()) {
//                            writer.println("<div>" + rule.ruleString() + "</div>");
//                        }
//                    }
//                    writer.println("</div>");
//                }
            }

        }
        writer.println("</body>");
        writer.println("</html>");
    }

/*

	public Set getParsersForClass(Class returnType) {

		TreeSet set = new TreeSet();
		return set;
	}
*/

    public String getHTMLForClass(Class c) {
        String name = ClassComparator.getName(c);
//        return "<A HREF=\"" + BeastParserDoc.DEATAIL_HTML + "#" + name + "\">" + name + "</A>";
        return "<A HREF=\"" + BeastParserDoc.INDEX_HTML + "#" + name + "\">" + name + "</A>";
    }
/*
	class SetHash {

		private HashMap table;

		public SetHash() { table = new HashMap(); }

		public final void put(Object key, XMLObjectParser o) {
			Set set = (Set)table.get(key);

			if (set != null) {
				set.add(o);
			} else {
				TreeSet newSet = new TreeSet();
				newSet.add(o);
				table.put(key, newSet);
			}
		}

		public final Set keySet() { return table.keySet(); }

		public final Object[] getArray(Object key) { return getSortedSet(key).toArray(); }

		public final SortedSet getSortedSet(Object key) { return (SortedSet)table.get(key); }
	}*/

    /*class XMLObjectParserComparator implements Comparator<XMLObjectParser> {

         public int compare(XMLObjectParser c1, XMLObjectParser c2) {

             final String name1 = c1.getParserName().toUpperCase();
             final String name2 = c2.getParserName().toUpperCase();

             return name1.compareTo(name2);
         }
     }*/

    protected Set<Class> requiredTypes = new TreeSet<Class>(ClassComparator.INSTANCE);
    protected XMLParser parser = null;
}
