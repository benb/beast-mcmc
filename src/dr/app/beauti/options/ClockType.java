package dr.app.beauti.options;

/**
 * @author Alexei Drummond
 */
public enum ClockType {

    STRICT_CLOCK("Strict Clock"),
    UNCORRELATED_EXPONENTIAL("Relaxed Clock: Uncorrelated Exp"),
    UNCORRELATED_LOGNORMAL("Relaxed Clock: Uncorrelated Lognormal"),
    AUTOCORRELATED_LOGNORMAL("Relaxed Clock: Autocorrelated Lognormal"),
    RANDOM_LOCAL_CLOCK("Random local clock model");


    ClockType(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return displayName;
    }

    private final String displayName;

    final public static String LOCAL_CLOCK = "localClock";
    final public static String UCED_MEAN = "uced.mean";
    final public static String UCLD_MEAN = "ucld.mean";
    final public static String UCLD_STDEV = "ucld.stdev";    
}
