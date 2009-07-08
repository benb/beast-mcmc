package dr.app.beauti.generator;

import dr.app.beauti.util.XMLWriter;
import dr.app.beauti.components.ComponentFactory;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.ClockType;
import dr.app.beauti.options.ModelOptions;
import dr.app.beauti.options.PartitionClockModel;
import dr.app.beauti.options.PartitionData;
import dr.app.beauti.options.PartitionSubstitutionModel;
import dr.app.beauti.options.PartitionTreeModel;
import dr.evolution.datatype.DataType;
import dr.evolution.datatype.Nucleotides;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.branchratemodel.StrictClockBranchRates;
import dr.evomodel.clock.ACLikelihood;
import dr.evomodel.sitemodel.GammaSiteModel;
import dr.evomodel.sitemodel.SiteModel;
import dr.evomodel.tree.TreeModel;
import dr.evomodel.treelikelihood.TreeLikelihood;
import dr.evomodelxml.DiscretizedBranchRatesParser;
import dr.evoxml.AlignmentParser;
import dr.evoxml.SitePatternsParser;
import dr.util.Attribute;
import dr.xml.XMLParser;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Walter Xie
 */
public class TreeLikelihoodGenerator extends Generator {

	public TreeLikelihoodGenerator(BeautiOptions options, ComponentFactory[] components) {
        super(options, components);
    }

    /**
     * Write the tree likelihood XML block.
     *
     * @param model  the partition model to write likelihood block for
     * @param writer the writer
     */
    void writeTreeLikelihood(PartitionData partition, XMLWriter writer) {
    	
    	PartitionSubstitutionModel model = partition.getPartitionSubstitutionModel();

        if (model.getDataType() == Nucleotides.INSTANCE && model.getCodonHeteroPattern() != null) {
            for (int i = 1; i <= model.getCodonPartitionCount(); i++) {
                writeTreeLikelihood(TreeLikelihood.TREE_LIKELIHOOD, i, partition, writer);
            }
        } else {
            writeTreeLikelihood(TreeLikelihood.TREE_LIKELIHOOD, -1, partition, writer);
        }
    }

    /**
     * Write the tree likelihood XML block.
     *
     * @param id     the id of the tree likelihood
     * @param num    the likelihood number
     * @param model  the partition model to write likelihood block for
     * @param writer the writer
     */
    public void writeTreeLikelihood(String id, int num, PartitionData partition, XMLWriter writer) {
    	PartitionSubstitutionModel substModel = partition.getPartitionSubstitutionModel();
    	PartitionTreeModel treeModel = partition.getPartitionTreeModel();
    	PartitionClockModel clockModel = partition.getPartitionClockModel();
    	
    	if (num > 0) {
    		writer.writeOpenTag(
	                TreeLikelihood.TREE_LIKELIHOOD,
	                new Attribute[]{
	                        new Attribute.Default<String>(XMLParser.ID, substModel.getPrefix(num) + partition.getName() + "." + id),
	                        new Attribute.Default<Boolean>(TreeLikelihood.USE_AMBIGUITIES, useAmbiguities(substModel))}
	        );
    	} else {
	        writer.writeOpenTag(
	                TreeLikelihood.TREE_LIKELIHOOD,
	                new Attribute[]{
	                        new Attribute.Default<String>(XMLParser.ID, substModel.getPrefix() + partition.getName() + "." + id),
	                        new Attribute.Default<Boolean>(TreeLikelihood.USE_AMBIGUITIES, useAmbiguities(substModel))}
	        );
    	}
    	
        if (!options.samplePriorOnly) {
        	 if (num > 0) {
	            writer.writeTag(SitePatternsParser.PATTERNS,
	                    new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, substModel.getPrefix(num) + partition.getName() + "." 
	                    		+ SitePatternsParser.PATTERNS)}, true);  
        	 } else {
        		 writer.writeTag(SitePatternsParser.PATTERNS,
                         new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, substModel.getPrefix() + partition.getName() + "." 
                         		 + SitePatternsParser.PATTERNS)}, true);
        	 }
        } else {
            // We just need to use the dummy alignment
            writer.writeTag(SitePatternsParser.PATTERNS,
                    new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, AlignmentParser.ALIGNMENT)}, true);
        }

        writer.writeTag(TreeModel.TREE_MODEL,
                new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, treeModel.getPrefix() + TreeModel.TREE_MODEL)}, true);
        
        if (num > 0) {
	        writer.writeTag(GammaSiteModel.SITE_MODEL,
	                new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, substModel.getPrefix(num) + SiteModel.SITE_MODEL)}, true);
        } else {
        	writer.writeTag(GammaSiteModel.SITE_MODEL,
	                new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, substModel.getPrefix() + SiteModel.SITE_MODEL)}, true);
        }
        
        
        switch (clockModel.getClockType()) {
            case STRICT_CLOCK:
                writer.writeTag(StrictClockBranchRates.STRICT_CLOCK_BRANCH_RATES,
                        new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, clockModel.getPrefix() + BranchRateModel.BRANCH_RATES)}, true);
                break;
            case UNCORRELATED_EXPONENTIAL:
            case UNCORRELATED_LOGNORMAL:
            case RANDOM_LOCAL_CLOCK:
                writer.writeTag(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES,
                        new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, clockModel.getPrefix() + BranchRateModel.BRANCH_RATES)}, true);
                break;

            case AUTOCORRELATED_LOGNORMAL:
                writer.writeTag(ACLikelihood.AC_LIKELIHOOD,
                        new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, clockModel.getPrefix() + BranchRateModel.BRANCH_RATES)}, true);
                break;

            default:
                throw new IllegalArgumentException("Unknown clock model");
        }

        /*if (options.clockType == ClockType.STRICT_CLOCK) {
            writer.writeTag(StrictClockBranchRates.STRICT_CLOCK_BRANCH_RATES,
                    new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
        } else {
            writer.writeTag(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES,
                    new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
        }*/

        generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_TREE_LIKELIHOOD, writer);

        writer.writeCloseTag(TreeLikelihood.TREE_LIKELIHOOD);
    }

    public void writeTreeLikelihoodReferences(XMLWriter writer) {
//        for (PartitionSubstitutionModel model : options.getPartitionSubstitutionModels()) {
    	for (PartitionData partition : options.dataPartitions) { // Each PD has one TreeLikelihood
    		PartitionSubstitutionModel substModel = partition.getPartitionSubstitutionModel();
            if (substModel.getDataType() == Nucleotides.INSTANCE && substModel.getCodonHeteroPattern() != null) {
                for (int i = 1; i <= substModel.getCodonPartitionCount(); i++) {
                    writer.writeIDref(TreeLikelihood.TREE_LIKELIHOOD, substModel.getPrefix(i) + partition.getName() + "." + TreeLikelihood.TREE_LIKELIHOOD);
                }
            } else {
            	writer.writeIDref(TreeLikelihood.TREE_LIKELIHOOD, substModel.getPrefix() + partition.getName() + "." + TreeLikelihood.TREE_LIKELIHOOD);
            }
            
            PartitionClockModel clockModel = partition.getPartitionClockModel();
            if (clockModel.getClockType() == ClockType.AUTOCORRELATED_LOGNORMAL) {
            	writer.writeIDref(ACLikelihood.AC_LIKELIHOOD,  clockModel.getPrefix() + BranchRateModel.BRANCH_RATES);
            }            
        }        
    }

    private boolean useAmbiguities(PartitionSubstitutionModel model) {
        boolean useAmbiguities = false;

        switch (model.getDataType().getType()) {
            case DataType.TWO_STATES:
            case DataType.COVARION:

                switch (model.getBinarySubstitutionModel()) {
                    case ModelOptions.BIN_COVARION:
                        useAmbiguities = true;
                        break;

                    default:
                }
                break;

            default:
                useAmbiguities = false;
        }

        return useAmbiguities;
    }

}
