package dr.evomodel.newtreelikelihood;

import java.util.logging.Logger;

/**
 * @author Marc Suchard
 * @author Andrew Rambaut
 * @author Dat Hunyh
 *
 */

public class GPULikelihoodCore extends NativeLikelihoodCore {

	/**
	 * Constructor
	 *
	 * @param treeLikelihood reference back to ATL, should be able to remove at some point
	 * @param gpuInfo number of states
	 */
    public GPULikelihoodCore(AbstractTreeLikelihood treeLikelihood, GPUInfo gpuInfo) {
//		super();  // super() only prints out information
		StringBuffer sb = new StringBuffer();
		sb.append("Constructing GPU likelihood core:\n");
		sb.append("\tGPU Name: "+gpuInfo.name+"\n");
		sb.append("\tOther info here: "+gpuInfo.memorySize+"\n");
		sb.append("If you publish results using this core, please reference Hunyh, Rambaut and Suchard (in preparation)\n");
		Logger.getLogger("dr.evomodel.treelikelihood").info(sb.toString());
		this.treeLikelihood = treeLikelihood;
	}

    private AbstractTreeLikelihood treeLikelihood;   // TODO Not needed if everything runs in one thread or I get GPU contexts working

	/** GPU-specific loading **/

	protected static String GPU_LIBRARY_NAME = "GPULikelihoodCore";

	protected static boolean isCompatible(GPUInfo gpuInfo, int[] configuration) {
		return true;
	}


	/** GPU-specific native interface **/

	private static native GPUInfo getGPUInfo();

	/** Native interface overriding NativeLikelihoodCore **/

	protected native void updateRootFrequencies(double[] frequencies);

	protected native void updateEigenDecomposition(double[][] eigenVectors, double[][] inverseEigenValues, double[] eigenValues);

	protected native void updateCategoryRates(double[] rates);

    protected native void updateCategoryProportions(double[] proportions);

    public native void updateMatrices(int[] branchUpdateIndices, double[] branchLengths, int branchUpdateCount);

    public native void updatePartials(int[] operations, int[] dependencies, int operationCount);

    public native void calculateLogLikelihoods(int rootNodeIndex, double[] outLogLikelihoods);

    public native void storeState();

    public native void restoreState();

	public static class LikelihoodCoreLoader implements LikelihoodCoreFactory.LikelihoodCoreLoader {

		public String getLibraryName() { return GPU_LIBRARY_NAME; }

		public LikelihoodCore createLikelihoodCore(int[] configuration, AbstractTreeLikelihood treeLikelihood) {
			int stateCount = configuration[0];
			GPUInfo gpuInfo;
			try {
				System.loadLibrary(getLibraryName()+"-"+stateCount);
				gpuInfo = GPULikelihoodCore.getGPUInfo();
				if (gpuInfo == null) // No GPU is present
					return null;
				if (!GPULikelihoodCore.isCompatible(gpuInfo, configuration)) // GPU is not compatible
					return null;
			} catch (UnsatisfiedLinkError e) {
				return null;
			}
			return new GPULikelihoodCore(treeLikelihood, GPULikelihoodCore.getGPUInfo());
		}
	}
}