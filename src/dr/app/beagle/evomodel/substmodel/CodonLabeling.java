package dr.app.beagle.evomodel.substmodel;

import dr.evolution.datatype.Codons;

/**
 * @author Marc A. Suchard
 * @author Vladimir Minin
 *         <p/>
 *         An enum for different types of codon labelings in BEAST using BEAGLE
 *         This work is supported by NSF grant 0856099
 *         <p/>
 *         O'Brien JD, Minin VN and Suchard MA (2009) Learning to count: robust estimates for labeled distances between
 *         molecular sequences. Molecular Biology and Evolution, 26, 801-814
 */

public enum CodonLabeling {
    SYN("S"), // synonymous mutations
    NON_SYN("N"); // non-synonymous mutations

    CodonLabeling(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static double[] getRegisterMatrix(CodonLabeling labeling, Codons codonDataType) {
        return getRegisterMatrix(labeling, codonDataType, false);
    }

    public static double[] getRegisterMatrix(CodonLabeling labeling, Codons codonDataType, boolean base64) {
        final int stateCount = codonDataType.getStateCount();
        final int rateCount = ((stateCount - 1) * stateCount) / 2;

        byte[] rateMap = Codons.constructRateMap(
                rateCount,
                stateCount,
                codonDataType,
                codonDataType.getGeneticCode());

        double[] registerMatrix = new double[stateCount * stateCount];

        int index = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = i + 1; j < stateCount; j++) {
                byte b = rateMap[index];
                if (
                        (labeling == SYN && (b == 1 || b == 2)) ||
                                (labeling == NON_SYN && (b == 3 || b == 4))
                        ) {
                    registerMatrix[j * stateCount + i] = registerMatrix[i * stateCount + j] = 1.0;
                }
                index++;
            }
        }

        if (base64) { // Expand matrix back out to the 4 x 4 x 4 stateSpace for a product chain
            double[] oldRegisterMatrix = registerMatrix;
            registerMatrix = new double[64 * 64];
            for (int i = 0; i < stateCount; i++) {
                for (int j = 0; j < stateCount; j++) {
                    if (oldRegisterMatrix[i * stateCount + j] == 1) {
                        registerMatrix[codonDataType.getCanonicalState(i) * 64
                                + codonDataType.getCanonicalState(j)] = 1;
                    }
                }
            }
        }
        return registerMatrix;
    }

    private final String text;

    public static CodonLabeling parseFromString(String text) {
        for (CodonLabeling scheme : CodonLabeling.values()) {
            if (scheme.getText().compareToIgnoreCase(text) == 0)
                return scheme;
        }
        return null;
    }
}
