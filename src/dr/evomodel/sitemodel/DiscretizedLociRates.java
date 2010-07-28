package dr.evomodel.sitemodel;

import dr.inference.model.*;
import dr.inference.distribution.ParametricDistributionModel;

/**
 * @author Chieh-Hsi Wu
 *
 * This class models the relative loci rates using the given discritized parametric distribution. 
 */
public class DiscretizedLociRates extends AbstractModel {

    private CompoundParameter lociRates;
    private Parameter rateCategoryParameter;
    private ParametricDistributionModel distrModel;
    private double normalizeRateTo;
    private double[] rates;
    private boolean normalize;
    private int categoryCount;
    private double scaleFactor;


    public DiscretizedLociRates(
            CompoundParameter lociRates,
            Parameter rateCategoryParameter,
            ParametricDistributionModel model,
            boolean normalize,
            double normalizeLociRateTo,

            int categoryCount) {
        super("DiscretizedLociRatesModel");
        this.lociRates = lociRates;

        this.rateCategoryParameter = rateCategoryParameter;
        //Force the boundaries of rateCategoryParameter to match the category count
        Parameter.DefaultBounds bound = new Parameter.DefaultBounds(categoryCount - 1, 0, rateCategoryParameter.getDimension());
        this.rateCategoryParameter.addBounds(bound);
        
        this.distrModel = model;
        this.normalizeRateTo = normalizeLociRateTo;
        this.normalize = normalize;
        this.categoryCount = categoryCount;
        rates = new double[categoryCount];
        setupRates();


        addModel(distrModel);
        addVariable(this.rateCategoryParameter);

    }

    private void setupRates(){
        double categoryIntervalSize = 1.0/categoryCount;
        for(int i = 0; i < categoryCount; i++){
            rates[i]= distrModel.quantile((i+0.5)*categoryIntervalSize);
        }

        if(normalize){
           computeFactor();
        }
        int lociCount = rateCategoryParameter.getDimension();
        for(int i = 0; i < lociCount; i ++){
            lociRates.setParameterValue(i,rates[(int)rateCategoryParameter.getParameterValue(i)]*scaleFactor);
        }

    }
    public void handleModelChangedEvent(Model model, Object object, int index) {
        if (model == distrModel) {
            setupRates();
            fireModelChanged();
        }
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        setupRates();
    }

    protected void storeState() {
    }
    protected void acceptState() {
    }
    protected void restoreState() {
        setupRates();
    }
    private void computeFactor(){
        double sumRates = 0.0;
        int lociCount = rateCategoryParameter.getDimension();
        for(int i = 0; i < lociCount; i++){
            sumRates += rates[(int)rateCategoryParameter.getParameterValue(i)];
        }
        scaleFactor = normalizeRateTo/(sumRates/lociCount);
    }

}
