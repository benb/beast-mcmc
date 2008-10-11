/**
 *
 */
package dr.evomodel.newtreelikelihood;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marc Suchard
 *
 */
public class LikelihoodCoreFactory {

	public static LikelihoodCore loadLikelihoodCore(int[] configuration, AbstractTreeLikelihood treeLikelihood) {

		if (coreRegistry == null) {  // Lazy loading
			coreRegistry = new ArrayList<LikelihoodCoreLoader>();  // List libraries in order of load-priority
			coreRegistry.add(new GPUMemoryLikelihoodCore.LikelihoodCoreLoader());
			coreRegistry.add(new NativeMemoryLikelihoodCore.LikelihoodCoreLoader());
		}

		for(LikelihoodCoreLoader loader: coreRegistry) {
			LikelihoodCore core = loader.createLikelihoodCore(configuration, treeLikelihood);
			if (core != null)
				return core;
		}

		// No libraries/processes available

		int stateCount = configuration[0];
		return new GeneralLikelihoodCore(stateCount);
	}

	private static List<LikelihoodCoreLoader> coreRegistry;

	protected interface LikelihoodCoreLoader {
		public String getLibraryName();

		/**
		 * Actual factory
		 * @param configuration
		 * @param treeLikelihood  Should remove this after migration bug is solved for GPU
		 * @return
		 */
		public LikelihoodCore createLikelihoodCore(int[] configuration, AbstractTreeLikelihood treeLikelihood);
	}

}
