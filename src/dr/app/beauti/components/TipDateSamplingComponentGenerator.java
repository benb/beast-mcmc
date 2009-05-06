package dr.app.beauti.components;

import dr.app.beauti.XMLWriter;
import dr.app.beauti.generator.BaseComponentGenerator;
import dr.app.beauti.options.*;
import dr.evolution.util.Taxon;
import dr.evolution.util.TaxonList;
import dr.inference.model.ParameterParser;
import dr.util.Attribute;
import dr.xml.XMLParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class TipDateSamplingComponentGenerator extends BaseComponentGenerator {

    public TipDateSamplingComponentGenerator(final BeautiOptions options) {
        super(options);
    }

    public boolean usesInsertionPoint(final InsertionPoint point) {
        TipDateSamplingComponentOptions comp = (TipDateSamplingComponentOptions)options.getComponentOptions(TipDateSamplingComponentOptions.class);

        if (comp.tipDateSamplingType == TipDateSamplingType.NO_SAMPLING) {
            return false;
        }

        switch (point) {
            case IN_TREE_MODEL:
            case IN_FILE_LOG_PARAMETERS:
                return true;
            case IN_MCMC_PRIOR:
                return comp.tipDateSamplingType == TipDateSamplingType.SAMPLE_JOINT;
            default:
                return false;
        }
    }

    protected void generate(final InsertionPoint point, final Object item, final XMLWriter writer) {
        TipDateSamplingComponentOptions comp = (TipDateSamplingComponentOptions)options.getComponentOptions(TipDateSamplingComponentOptions.class);

        switch (point) {
            case IN_TREE_MODEL: {
                if (comp.tipDateSamplingType == TipDateSamplingType.SAMPLE_INDIVIDUALLY) {
                    TaxonList taxa = comp.getTaxonSet();
                    for (int i = 0; i < taxa.getTaxonCount(); i++) {
                        Taxon taxon = taxa.getTaxon(i);
                        writer.writeOpenTag("leafHeight",
                                new Attribute[]{
                                        new Attribute.Default<String>("taxon", taxon.getId()),
                                }
                        );
                        writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.ID, "age(" + taxon.getId() + ")"), true);
                        writer.writeCloseTag("leafHeight");
                    }
                } else if (comp.tipDateSamplingType == TipDateSamplingType.SAMPLE_JOINT) {
//                    writer.writeOpenTag("nodeHeights",
//                            new Attribute[]{
//                                    new Attribute.Default<Boolean>("external", taxon.getId()),
//                            }
//                    );
//                    writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.ID, "age(" + taxon.getId() + ")"), true);
//                    writer.writeCloseTag("leafHeight");
                }
            } break;
            case IN_MCMC_PRIOR:
                if (comp.tipDateSamplingType == TipDateSamplingType.SAMPLE_INDIVIDUALLY) {
                    // nothing to do - indivual parameter priors are written automatically
                } else if (comp.tipDateSamplingType == TipDateSamplingType.SAMPLE_JOINT) {

                }
                break;
            case IN_FILE_LOG_PARAMETERS:
                if (comp.tipDateSamplingType == TipDateSamplingType.SAMPLE_INDIVIDUALLY) {
                    TaxonList taxa = comp.getTaxonSet();
                    for (int i = 0; i < taxa.getTaxonCount(); i++) {
                        Taxon taxon = taxa.getTaxon(i);
                        writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "age(" + taxon.getId() + ")"), true);
                    }
                } else if (comp.tipDateSamplingType == TipDateSamplingType.SAMPLE_JOINT) {
                    writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "treeModel.tipDates"), true);
                }
                break;
            default:
                throw new IllegalArgumentException("This insertion point is not implemented for " + this.getClass().getName());
        }

    }

    protected String getCommentLabel() {
        return "Tip date sampling";
    }

}