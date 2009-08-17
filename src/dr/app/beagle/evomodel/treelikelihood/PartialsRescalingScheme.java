package dr.app.beagle.evomodel.treelikelihood;

/**
 * @author Marc Suchard
 * @author Andrew Rambaut
 */
public enum PartialsRescalingScheme {
    
    NONE("none"),
    ALWAYS_RESCALE("alwaysRescale"),
    DYNAMIC_RESCALING("dynamicRescaling");

    PartialsRescalingScheme(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    private final String text;
}