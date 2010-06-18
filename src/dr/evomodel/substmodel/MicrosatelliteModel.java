package dr.evomodel.substmodel;


import dr.evolution.datatype.Microsatellite;
import dr.inference.model.Parameter;
import dr.inference.model.Model;


/**
 * @author Chieh-Hsi Wu
 *
 * An abstract class for microsatellite models
 */
public abstract class MicrosatelliteModel extends ComplexSubstitutionModel{
    protected OnePhaseModel subModel = null;
    protected boolean isNested = false;
    protected double[][] infinitesimalRateMatrix = null;
    protected boolean useStationaryFreqs = false;
    protected boolean modelUpdate = false;



    /**
     * Constructor
     * @param name              Model name
     * @param msat              Microsatellite data type
     * @param rootFreqModel     Frequency model
     * @param parameter         Infinitesimal rates
     */
    public MicrosatelliteModel(String name, Microsatellite msat, FrequencyModel rootFreqModel, Parameter parameter) {
        super(name, msat, rootFreqModel, parameter);
        if(parameter == null){
            double[] q = new double[stateCount*(stateCount-1)];
            infinitesimalRates = new Parameter.Default(q);
        }

        infinitesimalRateMatrix = new double[stateCount][stateCount];
    }

    public void setToEqualFrequencies(){
        double[] freqs = new double[stateCount];
            for(int i = 0; i < freqs.length; i++){
                freqs[i] = 1.0/stateCount;
            }
        this.freqModel = new FrequencyModel(dataType, freqs);
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        updateMatrix = true;
    }

    //store the infinitesimal rates in the vector to a matrix called amat
    public void storeIntoAmat(){
        amat = infinitesimalRateMatrix;
    }

    //matrix is already valid
    protected void makeValid(double[][] matrix, int dimension){}

    protected double getRate(int i, int j){
       return infinitesimalRateMatrix[i][j];
    }

    /*
     * Set up stationary frequencies
    */
    public void computeTwoPhaseStationaryDistribution(){
        synchronized (this) {
            if (updateMatrix) {
                setupMatrix();
            }
        }

        if (!wellConditioned) {
           throw new RuntimeException("not well conditioned");
        }
        int eigenValPos = -1;

        for(int i = 0; i < stateCount; i++){
            if(Eval[i] == 0){
                eigenValPos = i;
                break;
            }
        }
        /*for(int i = 0; i < EvalImag.length; i++){
            //System.out.println("imaginery part" + EvalImag[i]);
            if(EvalImag[i] != 0.0){
                throw new RuntimeException("imaginery part" + EvalImag[i]);
            }
        }*/

        double[] empFreq = new double[stateCount];
        //System.out.println("eq dist");
        for(int i = 0; i < stateCount; i++){
            empFreq[i] = Evec[i][eigenValPos]*Ievc[eigenValPos][i];
            //System.out.println(empFreq[i]);

        }
        this.freqModel = new FrequencyModel(dataType, empFreq);
    }


    /*public void computeTwoPhaseStationaryDistribution2(){
        setToEqualFrequencies();
        super.computeStationaryDistribution();
        synchronized (this) {
            if (updateMatrix) {
                setupMatrix();
            }
        }

        if (!wellConditioned) {
           throw new RuntimeException("not well conditioned");
        }

        double[] empFreq = new double[stateCount];

        for(int i = 0; i < stateCount; i++){
            empFreq[i] = getOneTransitionProbabilityEntry(Double.MAX_VALUE, i, i);
            //System.out.println(empFreq[i]);
        }

        this.freqModel = new FrequencyModel(dataType, empFreq);
    }*/


    public double[] getRates(){
        return super.getRates();
    }

    /*public void computeStationaryDistribution(){
       super.computeStationaryDistribution();
    }*/

    public abstract void setupInfinitesimalRates();

    public double[][] getInfinitesimalRates(){
        return infinitesimalRateMatrix;
    }

    public double getLogOneTransitionProbabilityEntry(double distance, int parentState, int childState){
        return Math.log(getOneTransitionProbabilityEntry(distance, parentState, childState));
    }

    public double getOneTransitionProbabilityEntry(double distance, int parentState, int childState){

       double probability = 0.0;
       double temp;

        synchronized (this) {
            if (updateMatrix) {
                setupMatrix();
            }
        }

        if (!wellConditioned) {

            //throw new RuntimeException("not well conditioned");
            return 0.0;
        }

        double [] iexp = new double[stateCount];
        for(int i = 0; i < stateCount; i++){
            if(EvalImag[i] == 0){
                temp = Math.exp(distance*(Eval[i]));
                iexp[i] = temp*Ievc[i][childState];

            }else{
                int i2 = i + 1;
                double b = EvalImag[i];
                double expat = Math.exp(distance * Eval[i]);
                double expatcosbt = expat * Math.cos(distance * b);
                double expatsinbt = expat * Math.sin(distance * b);
                iexp[i] = expatcosbt * Ievc[i][childState] + expatsinbt * Ievc[i2][childState];
                iexp[i2] = expatcosbt * Ievc[i2][childState] - expatsinbt * Ievc[i][childState];
                i ++;

            }
        }
        for(int i = 0; i < stateCount; i++){
            probability += Evec[parentState][i]*iexp[i];
        }

        if(probability <= 0.0){
            probability = minProb;
        }

        return probability;
    }

    public double[] getColTransitionProbabilities(double distance, int childState){

       double[] probability = new double[stateCount];
       double temp;

        synchronized (this) {
            if (updateMatrix) {
                setupMatrix();
            }
        }

        if (!wellConditioned) {

            //throw new RuntimeException("not well conditioned");
            return probability;
        }

        double [] iexp = new double[stateCount];
        for(int i = 0; i < stateCount; i++){
            if(EvalImag[i] == 0){
                temp = Math.exp(distance*(Eval[i]));
                iexp[i] = temp*Ievc[i][childState];

            }else{
                int i2 = i + 1;
                double b = EvalImag[i];
                double expat = Math.exp(distance * Eval[i]);
                double expatcosbt = expat * Math.cos(distance * b);
                double expatsinbt = expat * Math.sin(distance * b);
                iexp[i] = expatcosbt * Ievc[i][childState] + expatsinbt * Ievc[i2][childState];
                iexp[i2] = expatcosbt * Ievc[i2][childState] - expatsinbt * Ievc[i][childState];
                i ++;

            }
        }
        for(int i = 0; i < stateCount; i++){
            for(int j = 0; j < stateCount; j++){
                probability[i] += Evec[i][j]*iexp[j];

            }
            if(probability[i] <= 0.0){
                probability[i] = minProb;
            }
        }

        return probability;
    }

    /*
     * The One Phase Models are special cases of the birth-death chain,
     * and therefore we can use this to calculate the stationay distribution
     * given a infinitesimal rate matrix.
     */
    public void computeOnePhaseStationaryDistribution(){
        double[] pi = new double[stateCount];

        pi[0] = 1.0;
        double piSum = 1.0;
        for(int i = 1; i < stateCount; i++){
            pi[i] = pi[i-1]*infinitesimalRateMatrix[i-1][i]/infinitesimalRateMatrix[i][i-1];
            piSum = piSum+pi[i];
        }

        for(int i = 0; i < stateCount; i++){
            pi[i] = pi[i]/piSum;
            //System.out.println(pi[i]);

        }
        freqModel = new FrequencyModel(dataType,pi);


    }

    public void setupMatrix(){
        setupInfinitesimalRates();
        super.setupMatrix();
    }


    public MicrosatelliteModel getSubmodel(){
        return subModel;
    }

    public boolean isSubmodel(){
        return isNested;
    }

    public boolean hasSubmodel(){
        return subModel != null;
    }


    public boolean isModelUpdated(){
        return true;
    }

}