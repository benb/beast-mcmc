package dr.evomodel.newtreelikelihood;

import dr.evomodel.sitemodel.SiteModel;
import dr.evomodel.substmodel.SubstitutionModel;

/*
 * NativeLikelihoodCore.java
 *
 * @author Andrew Rambaut
 *
 */

public class NativeLikelihoodCore implements LikelihoodCore {

    public static final String LIBRARY_NAME = "NewNativeLikelihoodCore";

    public NativeLikelihoodCore() {
        // don't need to do any thing;
    }

    public native void initialize(int nodeCount, int patternCount, int matrixCount);

    public void finalize() throws Throwable {
        super.finalize();
        freeNativeMemory();
    }

    private native void freeNativeMemory();

    public native void setTipPartials(int tipIndex, double[] partials);

    public void updateSubstitutionModel(SubstitutionModel substitutionModel) {
        updateRootFrequencies(substitutionModel.getFrequencyModel().getFrequencies());
        updateEigenDecomposition(
                substitutionModel.getCMatrix(),
                substitutionModel.getEigenValues());
    }

    private native void updateRootFrequencies(double[] frequencies);
    private native void updateEigenDecomposition(double[] cMatrix,
                                                 double[] eigenValues);

    public void updateSiteModel(SiteModel siteModel) {
        if (rates == null) {
            rates = new double[siteModel.getCategoryCount()];
        }
        for (int i = 0; i < rates.length; i++) {
            rates[i] = siteModel.getRateForCategory(i);
        }
        updateCategoryRates(rates);
        updateCategoryProportions(siteModel.getCategoryProportions());
    }

    /**
     * A utility array to transfer category rates
     */
    private double[] rates = null;


    private native void updateCategoryRates(double[] rates);
    private native void updateCategoryProportions(double[] proportions);

    public native void updateMatrices(int[] branchUpdateIndices, double[] branchLengths, int branchUpdateCount);

    public native void updatePartials(int[] operations, int[] dependencies, int operationCount);

    public native void calculateLogLikelihoods(int rootNodeIndex, double[] outLogLikelihoods);

    public native void storeState();

    public native void restoreState();

    /* Library loading routines */

    public static class LikelihoodCoreLoader implements LikelihoodCoreFactory.LikelihoodCoreLoader {

        public String getLibraryName() { return LIBRARY_NAME; }

        public LikelihoodCore createLikelihoodCore(int[] configuration, AbstractTreeLikelihood treeLikelihood) {
            int stateCount = configuration[0];
            try {
                System.loadLibrary(getLibraryName()+"-"+stateCount);
            } catch (UnsatisfiedLinkError e) {
                return null;
            }
            // System.out.println("Successfully loaded native library: " + getLibraryName()+"-"+stateCount);
            return new NativeLikelihoodCore();
        }
    }

}