/*
 * TreeLikelihood.java
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

package dr.app.beagle.evomodel.treelikelihood;

import beagle.*;
import dr.app.beagle.evomodel.parsers.TreeLikelihoodParser;
import dr.app.beagle.evomodel.sitemodel.BranchSiteModel;
import dr.app.beagle.evomodel.sitemodel.SiteRateModel;
import dr.app.beagle.evomodel.substmodel.EigenDecomposition;
import dr.evolution.alignment.PatternList;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.TaxonList;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.branchratemodel.DefaultBranchRateModel;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * BeagleTreeLikelihoodModel - implements a Likelihood Function for sequences on a tree.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Marc Suchard
 * @version $Id$
 */

public class BeagleTreeLikelihood extends AbstractTreeLikelihood {

    // This property is a comma-delimited list of resource numbers (0 == CPU) to
    // allocate each BEAGLE instance to. If less than the number of instances then
    // will wrap around.
    private static final String RESOURCE_ORDER_PROPERTY = "beagle.resource.order";
    private static final String PREFERRED_FLAGS_PROPERTY = "beagle.preferred.flags";
    private static final String REQUIRED_FLAGS_PROPERTY = "beagle.required.flags";
    private static final String SCALING_PROPERTY = "beagle.scaling";

    private static int instanceCount = 0;
    private static List<Integer> resourceOrder = null;

    private static final int RESCALE_FREQUENCY = 10000;
    private static final int RESCALE_TIMES = 1;

    public BeagleTreeLikelihood(PatternList patternList,
                                TreeModel treeModel,
                                BranchSiteModel branchSiteModel,
                                SiteRateModel siteRateModel,
                                BranchRateModel branchRateModel,
                                boolean useAmbiguities,
                                PartialsRescalingScheme rescalingScheme
    ) {

        super(TreeLikelihoodParser.TREE_LIKELIHOOD, patternList, treeModel);

        try {
            final Logger logger = Logger.getLogger("dr.evomodel");

            logger.info("Using BEAGLE TreeLikelihood");

            this.siteRateModel = siteRateModel;
            addModel(this.siteRateModel);

            this.branchSiteModel = branchSiteModel;
            addModel(branchSiteModel);

            if (branchRateModel != null) {
                this.branchRateModel = branchRateModel;
                logger.info("  Branch rate model used: " + branchRateModel.getModelName());
            } else {
                this.branchRateModel = new DefaultBranchRateModel();
            }
            addModel(this.branchRateModel);

            this.categoryCount = this.siteRateModel.getCategoryCount();

            this.tipCount = treeModel.getExternalNodeCount();

            internalNodeCount = nodeCount - tipCount;

            int compactPartialsCount = tipCount;
            if (useAmbiguities) {
                // if we are using ambiguities then we don't use tip partials
                compactPartialsCount = 0;
            }

            // one partials buffer for each tip and two for each internal node (for store restore)
            partialBufferHelper = new BufferIndexHelper(nodeCount, tipCount);

            // two eigen buffers: for store and restore.
            eigenBufferHelper = new BufferIndexHelper(1, 0);

            // two matrices for each node less the root
            matrixBufferHelper = new BufferIndexHelper(nodeCount, 0);

            // one scaling buffer for each internal node plus an extra for the accumulation, then doubled for store/restore
            scaleBufferHelper = new BufferIndexHelper(getScaleBufferCount(), 0);

            // Attempt to get the resource order from the System Property
            if (resourceOrder == null) {
                resourceOrder = new ArrayList<Integer>();
                String r = System.getProperty(RESOURCE_ORDER_PROPERTY);
                if (r != null) {
                    String[] parts = r.split(",");
                    for (String part : parts) {
                        try {
                            int n = Integer.parseInt(part.trim());
                            resourceOrder.add(n);
                        } catch (NumberFormatException nfe) {
                            System.err.println("Invalid entry '"+part+"' in "+RESOURCE_ORDER_PROPERTY);
                        }
                    }
                }
            }

            // first set the rescaling scheme to use from the parser
            this.rescalingScheme = rescalingScheme;

            // then allow it to be overriden from the command line
            if (System.getProperty(SCALING_PROPERTY) != null) {
                this.rescalingScheme = PartialsRescalingScheme.parseFromString(System.getProperty(SCALING_PROPERTY));
            }

            int[] resourceList = null;
            long preferenceFlags = 0;
            long requirementFlags = 0;

            if (resourceOrder.size() > 0) {
                // added the zero on the end so that a CPU is selected if requested resource fails
                resourceList = new int[] { resourceOrder.get(instanceCount % resourceOrder.size()), 0 };
                if (resourceList[0] > 0) {
                    preferenceFlags |= BeagleFlag.PROCESSOR_GPU.getMask(); // Add preference weight against CPU
                }
            }

            if (System.getProperty(PREFERRED_FLAGS_PROPERTY) != null) {
                preferenceFlags = Long.valueOf(System.getProperty(PREFERRED_FLAGS_PROPERTY));
            }

            if (System.getProperty(REQUIRED_FLAGS_PROPERTY) != null) {
                requirementFlags = Long.valueOf(System.getProperty(REQUIRED_FLAGS_PROPERTY));
            }

            // Define default behaviour here
            if (this.rescalingScheme == PartialsRescalingScheme.DEFAULT) {
                //if GPU: the default is dynamic scaling in BEAST
                if (resourceList != null && resourceList[0] > 1) {
                    this.rescalingScheme = PartialsRescalingScheme.DYNAMIC;
                } else { // if CPU: just run as fast as possible
                    this.rescalingScheme = PartialsRescalingScheme.NONE;
                }
            }

            if (this.rescalingScheme == PartialsRescalingScheme.AUTO) {
                preferenceFlags |= BeagleFlag.SCALING_AUTO.getMask();
                useAutoScaling = true;              
            } else {
//                preferenceFlags |= BeagleFlag.SCALING_MANUAL.getMask();
            }

            if (preferenceFlags == 0 && resourceList == null) { // else determine dataset characteristics
                if ( stateCount == 4 && patternList.getPatternCount() < 10000) // TODO determine good cut-off
                    preferenceFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
            }

            if (branchSiteModel.canReturnComplexDiagonalization()) {
                requirementFlags |= BeagleFlag.EIGEN_COMPLEX.getMask();
            }

            instanceCount ++;

            beagle = BeagleFactory.loadBeagleInstance(
                    tipCount,
                    partialBufferHelper.getBufferCount(),
                    compactPartialsCount,
                    stateCount,
                    patternCount,
                    eigenBufferHelper.getBufferCount(),            // eigenBufferCount
                    matrixBufferHelper.getBufferCount(),
                    categoryCount,
                    scaleBufferHelper.getBufferCount(), // Always allocate; they may become necessary
                    resourceList,
                    preferenceFlags,
                    requirementFlags
            );

            InstanceDetails instanceDetails = beagle.getDetails();
            ResourceDetails resourceDetails = null;

            if (instanceDetails != null) {
                resourceDetails = BeagleFactory.getResourceDetails(instanceDetails.getResourceNumber());
                if (resourceDetails != null) {
                    StringBuilder sb = new StringBuilder("  Using BEAGLE resource ");
                    sb.append(resourceDetails.getNumber()).append(": ");
                    sb.append(resourceDetails.getName()).append("\n");
                    if (resourceDetails.getDescription() != null) {
                        String[] description = resourceDetails.getDescription().split("\\|");
                        for (String desc : description) {
                            if (desc.trim().length() > 0) {
                                sb.append("    ").append(desc.trim()).append("\n");
                            }
                        }
                    }
                    sb.append("    with instance flags: ").append(instanceDetails.toString());
                    logger.info(sb.toString());
                } else {
                    logger.info("  Error retrieving BEAGLE resource for instance: " + instanceDetails.toString());
                }
            } else {
                logger.info("  No external BEAGLE resources available, or resource list/requirements not met, using Java implementation");
            }
            logger.info("  " + (useAmbiguities ? "Using" : "Ignoring") + " ambiguities in tree likelihood.");
            logger.info("  With " + patternList.getPatternCount() + " unique site patterns.");

            for (int i = 0; i < tipCount; i++) {
                // Find the id of tip i in the patternList
                String id = treeModel.getTaxonId(i);
                int index = patternList.getTaxonIndex(id);

                if (index == -1) {
                    throw new TaxonList.MissingTaxonException("Taxon, " + id + ", in tree, " + treeModel.getId() +
                            ", is not found in patternList, " + patternList.getId());
                } else {
                    if (useAmbiguities) {
                        setPartials(beagle, patternList, index, i);
                    } else {
                        setStates(beagle, patternList, index, i);
                    }
                }
            }

            beagle.setPatternWeights(patternWeights);

            if (this.rescalingScheme == PartialsRescalingScheme.DEFAULT &&
                    resourceDetails != null &&
                    (resourceDetails.getFlags() & BeagleFlag.SCALING_AUTO.getMask()) == 0) {
                // If auto scaling in BEAGLE is not supported then do it here
//                this.rescalingScheme = PartialsRescalingScheme.NONE;
//                this.rescalingScheme = PartialsRescalingScheme.ALWAYS;
                this.rescalingScheme = PartialsRescalingScheme.DYNAMIC;
                logger.info("  Auto rescaling not supported in BEAGLE, using : " + this.rescalingScheme.getText());
            } else {
                logger.info("  Using rescaling scheme : " + this.rescalingScheme.getText());
            }

            if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                everUnderflowed = true; // If commented out, BEAST does not rescale until first under-/over-flow.
            }
                        
            updateSubstitutionModel = true;
            updateSiteModel = true;

        } catch (TaxonList.MissingTaxonException mte) {
            throw new RuntimeException(mte.toString());
        }
        hasInitialized = true;
    }

    public SiteRateModel getSiteRateModel() {
        return siteRateModel;
    }

    public BranchRateModel getBranchRateModel() {
        return branchRateModel;
    }

    protected int getScaleBufferCount() {
        return internalNodeCount + 1;
    }

    /**
     * Sets the partials from a sequence in an alignment.
     * @param beagle beagle
     * @param patternList patternList
     * @param sequenceIndex sequenceIndex
     * @param nodeIndex nodeIndex
     */
    protected final void setPartials(Beagle beagle,
                                     PatternList patternList,
                                     int sequenceIndex,
                                     int nodeIndex) {
        double[] partials = new double[patternCount * stateCount * categoryCount];

        boolean[] stateSet;

        int v = 0;
        for (int i = 0; i < patternCount; i++) {

            int state = patternList.getPatternState(sequenceIndex, i);
            stateSet = dataType.getStateSet(state);

            for (int j = 0; j < stateCount; j++) {
                if (stateSet[j]) {
                    partials[v] = 1.0;
                } else {
                    partials[v] = 0.0;
                }
                v++;
            }
        }

        // if there is more than one category then replicate the partials for each
        int n = patternCount * stateCount;
        int k = n;
        for (int i = 1; i < categoryCount; i++) {
            System.arraycopy(partials, 0, partials, k, n);
            k += n;
        }

        beagle.setPartials(nodeIndex, partials);
    }

    /**
     * Sets the partials from a sequence in an alignment.
     * @param beagle beagle
     * @param patternList patternList
     * @param sequenceIndex sequenceIndex
     * @param nodeIndex nodeIndex
     */
    protected final void setStates(Beagle beagle,
                                   PatternList patternList,
                                   int sequenceIndex,
                                   int nodeIndex) {
        int i;

        int[] states = new int[patternCount];

        for (i = 0; i < patternCount; i++) {

            states[i] = patternList.getPatternState(sequenceIndex, i);
        }

        beagle.setTipStates(nodeIndex, states);
    }

    // **************************************************************
    // ModelListener IMPLEMENTATION
    // **************************************************************

    /**
     * Handles model changed events from the submodels.
     */
    protected void handleModelChangedEvent(Model model, Object object, int index) {

        if (model == treeModel) {
            if (object instanceof TreeModel.TreeChangedEvent) {

                if (((TreeModel.TreeChangedEvent) object).isNodeChanged()) {
                    // If a node event occurs the node and its two child nodes
                    // are flagged for updating (this will result in everything
                    // above being updated as well. Node events occur when a node
                    // is added to a branch, removed from a branch or its height or
                    // rate changes.
                    updateNodeAndChildren(((TreeModel.TreeChangedEvent) object).getNode());

                } else if (((TreeModel.TreeChangedEvent) object).isTreeChanged()) {
                    // Full tree events result in a complete updating of the tree likelihood
                    // This event type is now used for EmpiricalTreeDistributions.
//                    System.err.println("Full tree update event - these events currently aren't used\n" +
//                            "so either this is in error or a new feature is using them so remove this message.");
                    updateAllNodes();
                } else {
                    // Other event types are ignored (probably trait changes).
                    //System.err.println("Another tree event has occured (possibly a trait change).");
                }
            }

        } else if (model == branchRateModel) {
            if (index == -1) {
                if (COUNT_TOTAL_OPERATIONS)
                    totalRateUpdateAllCount++;
                updateAllNodes();
            } else {
                if (COUNT_TOTAL_OPERATIONS)
                    totalRateUpdateSingleCount++;
                updateNode(treeModel.getNode(index));
            }

        } else if (model == branchSiteModel) {

            updateSubstitutionModel = true;
            updateAllNodes();

        } else if (model == siteRateModel) {

            updateSiteModel = true;
            updateAllNodes();

        } else {

            throw new RuntimeException("Unknown componentChangedEvent");
        }

        super.handleModelChangedEvent(model, object, index);
    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    /**
     * Stores the additional state other than model components
     */
    protected void storeState() {
        partialBufferHelper.storeState();
        eigenBufferHelper.storeState();
        matrixBufferHelper.storeState();

        if (useScaleFactors || useAutoScaling) { // Only store when actually used
            scaleBufferHelper.storeState();
            System.arraycopy(scaleBufferIndices, 0, storedScaleBufferIndices, 0, scaleBufferIndices.length);
//            storedRescalingCount = rescalingCount;
        }

        super.storeState();

    }

    /**
     * Restore the additional stored state
     */
    protected void restoreState() {
        updateSiteModel = true; // this is required to upload the categoryRates to BEAGLE after the restore

        partialBufferHelper.restoreState();
        eigenBufferHelper.restoreState();
        matrixBufferHelper.restoreState();

        if (useScaleFactors || useAutoScaling) {
            scaleBufferHelper.restoreState();
            int[] tmp = storedScaleBufferIndices;
            storedScaleBufferIndices = scaleBufferIndices;
            scaleBufferIndices = tmp;
//            rescalingCount = storedRescalingCount;
        }

        super.restoreState();

    }

//    int marcCount = 0;
    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    protected double calculateLogLikelihood() {

        if (patternLogLikelihoods == null) {
            patternLogLikelihoods = new double[patternCount];
        }

        if (matrixUpdateIndices == null) {
            matrixUpdateIndices = new int[nodeCount];
            branchLengths = new double[nodeCount];
            scaleBufferIndices = new int[internalNodeCount];
            storedScaleBufferIndices = new int[internalNodeCount];
        }

        if (operations == null) {
            operations = new int[internalNodeCount * Beagle.OPERATION_TUPLE_SIZE];
        }

        recomputeScaleFactors = false;

        if (this.rescalingScheme == PartialsRescalingScheme.ALWAYS) {
            useScaleFactors = true;
            recomputeScaleFactors = true;
        } else if (this.rescalingScheme == PartialsRescalingScheme.DYNAMIC && everUnderflowed) {
            useScaleFactors = true;
            if (rescalingCountInner < RESCALE_TIMES) {
                recomputeScaleFactors = true;
                makeDirty();
//                System.err.println("Recomputing scale factors");
            }

            rescalingCountInner++;
            rescalingCount ++;
            if (rescalingCount > RESCALE_FREQUENCY) {
                rescalingCount = 0;
                rescalingCountInner = 0;
            }
        }

        branchUpdateCount = 0;
        operationCount = 0;

        final NodeRef root = treeModel.getRoot();
        traverse(treeModel, root, null, true);

        if (updateSubstitutionModel) {
            // we are currently assuming a homogeneous model...
            EigenDecomposition ed = branchSiteModel.getEigenDecomposition(0, 0);

            eigenBufferHelper.flipOffset(0);

            beagle.setEigenDecomposition(
                    eigenBufferHelper.getOffsetIndex(0),
                    ed.getEigenVectors(),
                    ed.getInverseEigenVectors(),
                    ed.getEigenValues());
        }

        if (updateSiteModel) {
            double[] categoryRates = this.siteRateModel.getCategoryRates();
            beagle.setCategoryRates(categoryRates);
        }

        beagle.updateTransitionMatrices(
                eigenBufferHelper.getOffsetIndex(0),
                matrixUpdateIndices,
                null,
                null,
                branchLengths,
                branchUpdateCount);

        if (COUNT_TOTAL_OPERATIONS) {
            totalMatrixUpdateCount += branchUpdateCount;
        }

        if (COUNT_TOTAL_OPERATIONS) {
            totalOperationCount += operationCount;
        }

        double logL;
        boolean done = true;

        do {
            beagle.updatePartials(operations, operationCount, Beagle.NONE);

            int rootIndex = partialBufferHelper.getOffsetIndex(root.getNumber());

            double[] categoryWeights = this.siteRateModel.getCategoryProportions();
            double[] frequencies = branchSiteModel.getStateFrequencies(0);

            int cumulateScaleBufferIndex = Beagle.NONE;
            if (useScaleFactors) {

                if (recomputeScaleFactors) {
                    scaleBufferHelper.flipOffset(internalNodeCount);
                    cumulateScaleBufferIndex = scaleBufferHelper.getOffsetIndex(internalNodeCount);
                    beagle.resetScaleFactors(cumulateScaleBufferIndex);
                    beagle.accumulateScaleFactors(scaleBufferIndices, internalNodeCount, cumulateScaleBufferIndex);
                } else {
                    cumulateScaleBufferIndex = scaleBufferHelper.getOffsetIndex(internalNodeCount);
                }
            } else if (useAutoScaling) {
                beagle.accumulateScaleFactors(scaleBufferIndices, internalNodeCount, Beagle.NONE);
            }

            // these could be set only when they change but store/restore would need to be considered
            beagle.setCategoryWeights(0, categoryWeights);
            beagle.setStateFrequencies(0, frequencies);

            double[] sumLogLikelihoods = new double[1];

            beagle.calculateRootLogLikelihoods(new int[] { rootIndex }, new int[] { 0 }, new int[] { 0 },
                    new int[] { cumulateScaleBufferIndex }, 1, sumLogLikelihoods);

            logL = sumLogLikelihoods[0];

            if  (Double.isNaN(logL) || Double.isInfinite(logL) ) {
                everUnderflowed = true;
                logL = Double.NEGATIVE_INFINITY;

                if (!done) {
                    if (rescalingScheme == PartialsRescalingScheme.DYNAMIC) {
                        // we have had a potential under/over flow so attempt a rescaling
                        recomputeScaleFactors = true;

                        // traverse again but without flipping partials indices as we
                        // just want to overwrite the last attempt. We will flip the
                        // scale buffer indices though as we are recomputing them.
                        traverse(treeModel, root, null, false);

                        done = false;
                    } else {
                        // either we are not rescaling or always rescaling
                        // so just return the likelihood...
                        done = true;
                    }
                } else {
                    // we have already tried a rescale
                    // so just return the likelihood...
                    done = true;
                }
            } else {
                done = true;
            }

        } while (!done);

        // If these are needed...
        //beagle.getSiteLogLikelihoods(patternLogLikelihoods);

        //********************************************************************
        // after traverse all nodes and patterns have been updated --
        //so change flags to reflect this.
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = false;
        }

        updateSubstitutionModel = false;
        updateSiteModel = false;
        //********************************************************************

        return logL;
    }

    /**
     * Traverse the tree calculating partial likelihoods.
     * @param tree tree
     * @param node node
     * @param operatorNumber operatorNumber
     * @param flip flip
     * @return boolean
     */
    private boolean traverse(Tree tree, NodeRef node, int[] operatorNumber, boolean flip) {

        boolean update = false;

        int nodeNum = node.getNumber();

        NodeRef parent = tree.getParent(node);

        if (operatorNumber != null) {
            operatorNumber[0] = -1;
        }

        // First update the transition probability matrix(ices) for this branch
        if (parent != null && updateNode[nodeNum]) {

            final double branchRate = branchRateModel.getBranchRate(tree, node);

            // Get the operational time of the branch
            final double branchTime = branchRate * (tree.getNodeHeight(parent) - tree.getNodeHeight(node));

            if (branchTime < 0.0) {
                throw new RuntimeException("Negative branch length: " + branchTime);
            }

            if (flip) {
                // first flip the matrixBufferHelper
                matrixBufferHelper.flipOffset(nodeNum);
            }

            // then set which matrix to update
            matrixUpdateIndices[branchUpdateCount] = matrixBufferHelper.getOffsetIndex(nodeNum);

            branchLengths[branchUpdateCount] = branchTime;
            branchUpdateCount++;

            update = true;
        }

        // If the node is internal, update the partial likelihoods.
        if (!tree.isExternal(node)) {

            // Traverse down the two child nodes
            NodeRef child1 = tree.getChild(node, 0);
            final int[] op1 = { -1 };
            final boolean update1 = traverse(tree, child1, op1, flip);

            NodeRef child2 = tree.getChild(node, 1);
            final int[] op2 = { -1 };
            final boolean update2 = traverse(tree, child2, op2, flip);

            // If either child node was updated then update this node too
            if (update1 || update2) {

                int x = operationCount * Beagle.OPERATION_TUPLE_SIZE;

                if (flip) {
                    // first flip the partialBufferHelper
                    partialBufferHelper.flipOffset(nodeNum);
                }

                operations[x] = partialBufferHelper.getOffsetIndex(nodeNum);

                if (useScaleFactors) {
                    // get the index of this scaling buffer
                    int n = nodeNum - tipCount;

                    if (recomputeScaleFactors) {
                        // flip the indicator: can take either n or (internalNodeCount + 1) - n
                        scaleBufferHelper.flipOffset(n);

                        // store the index
                        scaleBufferIndices[n] = scaleBufferHelper.getOffsetIndex(n);

                        operations[x + 1] = scaleBufferIndices[n]; // Write new scaleFactor
                        operations[x + 2] = Beagle.NONE;

                    } else {
                        operations[x + 1] = Beagle.NONE;
                        operations[x + 2] = scaleBufferIndices[n]; // Read existing scaleFactor
                    }

                } else {

                    if (useAutoScaling) {
                        scaleBufferIndices[nodeNum - tipCount] = partialBufferHelper.getOffsetIndex(nodeNum);
                    }
                    operations[x + 1] = Beagle.NONE; // Not using scaleFactors
                    operations[x + 2] = Beagle.NONE;
                }

                operations[x + 3] = partialBufferHelper.getOffsetIndex(child1.getNumber()); // source node 1
                operations[x + 4] = matrixBufferHelper.getOffsetIndex(child1.getNumber()); // source matrix 1
                operations[x + 5] = partialBufferHelper.getOffsetIndex(child2.getNumber()); // source node 2
                operations[x + 6] = matrixBufferHelper.getOffsetIndex(child2.getNumber()); // source matrix 2

                operationCount ++;

                update = true;
            }
        }

        return update;

    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    private int[] matrixUpdateIndices;
    private double[] branchLengths;
    private int branchUpdateCount;
    private int[] scaleBufferIndices;
    private int[] storedScaleBufferIndices;

    private int[] operations;
    private int operationCount;

    protected BufferIndexHelper partialBufferHelper;
    private final BufferIndexHelper eigenBufferHelper;
    protected BufferIndexHelper matrixBufferHelper;
    protected BufferIndexHelper scaleBufferHelper;

    protected final int tipCount;
    protected final int internalNodeCount;

    private PartialsRescalingScheme rescalingScheme;
    protected boolean useScaleFactors = false;
    private boolean useAutoScaling = false;
    private boolean recomputeScaleFactors = false;
    private boolean everUnderflowed = false;
    private int rescalingCount = 0;
    private int rescalingCountInner = 0;
//    private int storedRescalingCount;

    /**
     * the branch-site model for these sites
     */
    protected final BranchSiteModel branchSiteModel;

    /**
     * the site model for these sites
     */
    protected final SiteRateModel siteRateModel;
    /**
     * the branch rate model
     */
    protected final BranchRateModel branchRateModel;

    /**
     * the pattern likelihoods
     */
    protected double[] patternLogLikelihoods = null;

    /**
     * the number of rate categories
     */
    protected int categoryCount;

    /**
     * an array used to transfer tip partials
     */
    protected double[] tipPartials;

    /**
     * the BEAGLE library instance
     */
    protected Beagle beagle;

    /**
     * Flag to specify that the substitution model has changed
     */
    protected boolean updateSubstitutionModel;
    protected boolean storedUpdateSubstitutionModel;

    /**
     * Flag to specify that the site model has changed
     */
    protected boolean updateSiteModel;
    protected boolean storedUpdateSiteModel;

//    /***
//     * Flag to specify if LikelihoodCore supports dynamic rescaling
//     */
//    private boolean dynamicRescaling = false;

    protected class BufferIndexHelper {
        /**
         *
         * @param maxIndexValue the number of possible input values for the index
         * @param minIndexValue the minimum index value to have the mirrored buffers
         */
        BufferIndexHelper(int maxIndexValue, int minIndexValue) {
            this.maxIndexValue = maxIndexValue;
            this.minIndexValue = minIndexValue;

            offsetCount = maxIndexValue - minIndexValue;
            indexOffsets = new int[offsetCount];
            storedIndexOffsets = new int[offsetCount];
        }

        public int getBufferCount() {
            return 2 * offsetCount + minIndexValue;
        }

        void flipOffset(int i) {
            if (i >= minIndexValue) {
                indexOffsets[i - minIndexValue] = offsetCount - indexOffsets[i - minIndexValue];
            } // else do nothing
        }

        int getOffsetIndex(int i) {
            if (i < minIndexValue) {
                return i;
            }
            return indexOffsets[i - minIndexValue] + i;
        }

        void getIndices(int[] outIndices) {
            for (int i = 0; i < maxIndexValue; i++) {
                outIndices[i] = getOffsetIndex(i);
            }
        }

        void storeState() {
            System.arraycopy(indexOffsets, 0, storedIndexOffsets, 0, indexOffsets.length);

        }

        void restoreState() {
            int[] tmp = storedIndexOffsets;
            storedIndexOffsets = indexOffsets;
            indexOffsets = tmp;
        }

        private final int maxIndexValue;
        private final int minIndexValue;
        private final int offsetCount;

        private int[] indexOffsets;
        private int[] storedIndexOffsets;

    }
}