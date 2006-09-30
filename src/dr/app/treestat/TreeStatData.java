/*
 * TreeStatData.java
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

package dr.app.treestat;

import org.jdom.Document;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeStatData {
	public static final String version = "1.0";
	
	public TreeStatData() {
	}
	
	// Data options
	public Set allTaxa = new HashSet();
	public ArrayList taxonSets = new ArrayList();	
	public ArrayList characters = new ArrayList();	
	public ArrayList statistics = new ArrayList();	
	
	public static class TaxonSet {
		String name;
		List taxa;
		public String toString() { return name; }
	};
		
	public static class Character {
		String name;
		List states;
		public String toString() { return name; }
	};
		
	public static class State {
		String name;
		String description;
		List taxa;
		public String toString() { return name; }
	};
		
	/**
	 * Read options from a file
	 */
	public Document create() {
	
		Element root = new Element("treeTracer");
		root.setAttribute("version", version);
		
		Element taxonSetsElement = new Element("taxonSets");
		Element charactersElement = new Element("characters");
		Element statisticsElement = new Element("statistics");
		
		root.addContent(taxonSetsElement);
		root.addContent(charactersElement);
		root.addContent(statisticsElement);
		
		Document doc = new Document(root);
		return doc;
	}

	private Element createChild(String name, String value) {
		Element e = new Element(name);
		e.setText(value);
		return e;
	}
	
	private Element createChild(String name, int value) {
		Element e = new Element(name);
		e.setText(Integer.toString(value));
		return e;
	}
	
	private Element createChild(String name, double value) {
		Element e = new Element(name);
		e.setText(Double.toString(value));
		return e;
	}
	
	private Element createChild(String name, boolean value) {
		Element e = new Element(name);
		e.setText(value?"true":"false");
		return e;
	}
		
	/**
	 * Read options from a file
	 */
	public void parse(Document document) throws dr.xml.XMLParseException {
	
		Element root = document.getRootElement();
		if (!root.getName().equals("treeTracer")) {
			throw new dr.xml.XMLParseException("This document does not appear to be a TreeTracer file");
		}
		
		Element taxonSetsElement = root.getChild("taxonSets");
		Element charactersElement = root.getChild("characters");
		Element statisticsElement = root.getChild("statistics");
	}

	private String getStringChild(Element element, String childName) {
		return element.getChildTextTrim(childName);
	}
	private int getIntegerChild(Element element, String childName) {
		String value = element.getChildTextTrim(childName);
		return Integer.parseInt(value);
	}
	private double getDoubleChild(Element element, String childName) {
		String value = element.getChildTextTrim(childName);
		return Double.parseDouble(value);
	}
	private boolean getBooleanChild(Element element, String childName) {
		String value = element.getChildTextTrim(childName);
		if (value.equals("true")) return true;
		return false;
	}
}

