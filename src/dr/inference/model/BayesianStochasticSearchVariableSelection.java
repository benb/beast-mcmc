package dr.inference.model;

import cern.colt.bitvector.BitVector;
import dr.math.MathUtils;

/**
 * @author Marc Suchard
 */

public interface BayesianStochasticSearchVariableSelection {

    public Parameter getIndicators();

    public boolean validState();

    public class Utils {

        private static double defaultExpectedMutations = 1.0;

        public static boolean connectedAndWellConditioned(double[] probability,
                                                          dr.app.beagle.evomodel.substmodel.SubstitutionModel substModel) {
            if (probability == null) {
                int stateCount = substModel.getDataType().getStateCount();
                probability = new double[stateCount*stateCount];
            }
            try {
                substModel.getTransitionProbabilities(defaultExpectedMutations,probability);
                return connectedAndWellConditioned(probability);
            } catch (Exception e) { // Any numerical error is bad news
                return false;
            }
        }

        public static boolean connectedAndWellConditioned(double[] probability,
                                                          dr.evomodel.substmodel.SubstitutionModel substModel) {
            if (probability == null) {
                int stateCount = substModel.getDataType().getStateCount();
                probability = new double[stateCount*stateCount];
            }
            try {
                substModel.getTransitionProbabilities(defaultExpectedMutations,probability);
                return connectedAndWellConditioned(probability);
            } catch (Exception e) { // Any numerical error is bad news
                return false;
            }
        }
                
        public static boolean connectedAndWellConditioned(double[] probability) {
            for(double prob : probability) {
                if(prob <= 0 || prob > 1)
                    return false;
            }
            return true;
        }

        public static void randomize(Parameter indicators,int dim, boolean reversible) {
            do {
                for (int i = 0; i < indicators.getDimension(); i++)
                    indicators.setParameterValue(i,
                            (MathUtils.nextDouble() < 0.5) ? 0.0 : 1.0);
            } while (!(isStronglyConnected(indicators.getParameterValues(),
                    dim, reversible)));
        }

        /* Determines if the graph is strongly connected, such that there exists
        * a directed path from any vertex to any other vertex
        *
        */
        public static boolean isStronglyConnected(double[] indicatorValues, int dim, boolean reversible) {
            BitVector visited = new BitVector(dim);
            boolean connected = true;
            for (int i = 0; i < dim && connected; i++) {
                visited.clear();
                depthFirstSearch(i, visited, indicatorValues, dim, reversible);
                connected = visited.cardinality() == dim;
            }
            return connected;
        }

        private static boolean hasEdge(int i, int j, double[] indicatorValues,
                                      int dim, boolean reversible) {
            return i != j && indicatorValues[getEntry(i, j, dim, reversible)] == 1;
        }

        private static int getEntry(int i, int j, int dim, boolean reversible) {
            if (reversible && j > i)
                return getEntry(j,i,dim,false);            

            int entry = i * (dim - 1) + j;
            if (j > i)
                entry--;
            return entry;
        }

        private static void depthFirstSearch(int node, BitVector visited, double[] indicatorValues,
                                            int dim, boolean reversible) {
            visited.set(node);
            for (int v = 0; v < dim; v++) {
                if (hasEdge(node, v, indicatorValues, dim, reversible) && !visited.get(v))
                    depthFirstSearch(v, visited, indicatorValues, dim, reversible);
            }
        }
    }
}
