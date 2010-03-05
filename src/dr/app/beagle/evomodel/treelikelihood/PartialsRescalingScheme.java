package dr.app.beagle.evomodel.treelikelihood;

/**
 * @author Marc Suchard
 * @author Andrew Rambaut
 */
public enum PartialsRescalingScheme {

    DEFAULT("default"),
    NONE("none"),
    DYNAMIC("dynamic"),
    ALWAYS("always");

    PartialsRescalingScheme(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    private final String text;

    public static PartialsRescalingScheme parseFromString(String text) {
        for(PartialsRescalingScheme scheme : PartialsRescalingScheme.values()) {
            if (scheme.getText().compareToIgnoreCase(text) == 0)
                return scheme;
        }
        return DEFAULT;
    }

}
