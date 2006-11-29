/*
 * TransmissionHistoryModel.java
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

package dr.evomodel.transmission;

import dr.evolution.colouring.DefaultBranchColouring;
import dr.evolution.colouring.DefaultTreeColouring;
import dr.evolution.colouring.TreeColouring;
import dr.evolution.colouring.TreeColouringProvider;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Taxon;
import dr.evolution.util.Units;
import dr.evoxml.XMLUnits;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A model for defining a known transmission history. Times of transmission events
 * can optionally be obtained as parameters for sampling. In future it may be possible
 * to sample the direction of transmission where this is not known.
 *
 * @version $Id: TransmissionHistoryModel.java,v 1.3 2005/04/11 11:25:50 alexei Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class TransmissionHistoryModel extends AbstractModel implements TreeColouringProvider, Units
{

    //
    // Public stuff
    //

    public static String TRANSMISSION_HISTORY_MODEL = "transmissionHistory";
    public static String TRANSMISSION = "transmission";
    public static String DONOR = "donor";
    public static String RECIPIENT = "recipient";


    /**
     * Construct model with default settings
     */
    public TransmissionHistoryModel(int units) {

        this(TRANSMISSION_HISTORY_MODEL, units);
    }

    /**
     * Construct model with default settings
     */
    public TransmissionHistoryModel(String name, int units) {

        super(name);

        setUnits(units);
    }

    private void addTransmission(Taxon donor, Taxon recipient, Parameter parameter) {
        if (donor.equals(recipient)) {
            throw new RuntimeException("Donor and recipient are the same, " + donor);
        }

        if (parameter != null) {
            addParameter(parameter);
        }

        TransmissionEvent transmissionEvent = new TransmissionEvent(donor, recipient, parameter);
        transmissionEvents.add(transmissionEvent);
        transmissionEventMap.put(recipient, transmissionEvent);
        if (!hosts.contains(donor)) {
            hosts.add(donor);
        }
        if (!hosts.contains(recipient)) {
            hosts.add(recipient);
        }

        Logger.getLogger("dr.evomodel").info( "Transmission from " + donor + " to " + recipient + " at " + parameter.getParameterValue(0));
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        // no submodels so nothing to do
    }

    /**
     * Called when a parameter changes.
     */
    public void handleParameterChangedEvent(Parameter parameter, int index) {
    }

    public int getTransmissionEventCount() {
        return transmissionEvents.size();
    }

    public TransmissionEvent getTransmissionEvent(int index) {
        return (TransmissionEvent)transmissionEvents.get(index);
    }

    public TransmissionEvent getTransmissionEventToHost(Taxon recipient) {
        return (TransmissionEvent)transmissionEventMap.get(recipient);
    }

    // *****************************************************************
    // Interface ModelComponent
    // *****************************************************************

    /**
     * Store current state
     */
    protected void storeState() {

    }

    /**
     * Restore the stored state
     */
    protected void restoreState() {

    }

    /**
     * accept the stored state
     */
    protected void acceptState() {} // nothing to do

    // **************************************************************
    // Units IMPLEMENTATION
    // **************************************************************

    /**
     * Sets the units these coalescent intervals are
     * measured in.
     */
    public final void setUnits(int u)
    {
        units = u;
    }

    /**
     * Returns the units these coalescent intervals are
     * measured in.
     */
    public final int getUnits()
    {
        return units;
    }

    private int units;

    /**
     * Parses an element from an DOM document into a ExponentialGrowth.
     */
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() { return TRANSMISSION_HISTORY_MODEL; }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            int units = XMLParser.Utils.getUnitsAttr(xo);

            TransmissionHistoryModel history = new TransmissionHistoryModel(units);

            for (int i = 0; i < xo.getChildCount(); i++) {
                XMLObject xoc = (XMLObject)xo.getChild(i);
                if (xoc.getName().equals(TRANSMISSION)) {
                    Taxon donor = (Taxon)xoc.getSocketChild(DONOR);
                    Taxon recipient = (Taxon)xoc.getSocketChild(RECIPIENT);
                    if (donor.equals(recipient)) {
                        throw new XMLParseException("Donor and recipient in TransmissionHistoryModel are the same: " + donor);
                    }

                    // Date date = (Date)xoc.getChild(Date.class);
                    Parameter parameter = (Parameter)xoc.getChild(Parameter.class);
                    history.addTransmission(donor, recipient, parameter);
                }
            }

            return history;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "Defines a transmission history";
        }

        public Class getReturnType() { return TransmissionHistoryModel.class; }

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                XMLUnits.UNITS_RULE,
                new ElementRule(TRANSMISSION,
                        new XMLSyntaxRule[] {
                                //new ElementRule(Date.class),
                                new ElementRule(Parameter.class),
                                new ElementRule(DONOR,
                                        new XMLSyntaxRule[] {new ElementRule(Taxon.class)}),
                                new ElementRule(RECIPIENT,
                                        new XMLSyntaxRule[] {new ElementRule(Taxon.class)})
                        }, 1, Integer.MAX_VALUE
                )
        };

    };

    public int getHostIndex(Taxon host) {
        return hosts.indexOf(host);
    }

    public int getHostCount() {
        return hosts.size();
    }

    public TreeColouring getTreeColouring(Tree tree) {
        DefaultTreeColouring treeColouring = new DefaultTreeColouring(getHostCount(), tree);

        createTreeColouring(tree, tree.getRoot(), treeColouring);

        return treeColouring;
    }

    private Taxon createTreeColouring(Tree tree, NodeRef node, DefaultTreeColouring treeColouring) {
        Taxon parentHost = null;
        Taxon childHost = null;

        if (tree.isExternal(node)) {
            childHost = (Taxon)tree.getNodeTaxon(node).getAttribute("host");
            if (childHost == null) {
                throw new RuntimeException("One or more of the viruses tree's taxa are missing the 'host' attribute");
            }

        } else {
            Taxon h1 = createTreeColouring(tree, tree.getChild(node, 0), treeColouring);
            Taxon h2 = createTreeColouring(tree, tree.getChild(node, 1), treeColouring);
            if (h1 != h2) {
                throw new RuntimeException("Two children have different hosts at coalescent event");
            }

            childHost = h1;
        }

        NodeRef parent = tree.getParent(node);
        if (parent != null) {
            double height0 = tree.getNodeHeight(node);
            double height1 = tree.getNodeHeight(parent);

            TransmissionEvent event = getTransmissionEventToHost(childHost);
            DefaultBranchColouring branchColouring;

            if (event != null && event.getTransmissionTime() < height1) {
                if (event.getTransmissionTime() < height0) {
                    throw new RuntimeException("Transmission event is before the node");
                }

                List hosts = new ArrayList();
                List times = new ArrayList();

                parentHost = childHost;

                while (event != null && event.getTransmissionTime() < height1) {
                    hosts.add(parentHost);
                    times.add(new Double(event.getTransmissionTime()));

                    parentHost = event.donor;

                    event = getTransmissionEventToHost(parentHost);
                }

                int host1 = getHostIndex(childHost);
                int host2 = getHostIndex(parentHost);
                branchColouring = new DefaultBranchColouring(host2, host1);
                for (int i = hosts.size() - 1; i >= 0; i--) {
                    int host = getHostIndex((Taxon)hosts.get(i));
                    double time = ((Double)times.get(i)).doubleValue();
                    branchColouring.addEvent(host, time);
                }

            } else {
                int host = getHostIndex(childHost);
                branchColouring = new DefaultBranchColouring(host, host);
                parentHost = childHost;
            }

            treeColouring.setBranchColouring(node, branchColouring);
        }

        return parentHost;
    }

    //
    // protected stuff
    //
    class TransmissionEvent {
        Taxon donor;
        Taxon recipient;
        Parameter timeParameter = null;

        public TransmissionEvent(Taxon donor, Taxon recipient, Parameter timeParameter) {
            this.donor = donor;
            this.recipient = recipient;
            this.timeParameter = timeParameter;
        }

        public double getTransmissionTime() {
            return timeParameter.getParameterValue(0);
        }

        public Taxon getDonor() {
            return donor;
        }

        public Taxon getRecipient() {
            return recipient;
        }
    }

    private List transmissionEvents = new ArrayList();
    private Map transmissionEventMap = new HashMap();
    private List hosts = new ArrayList();
}
