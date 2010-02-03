package dr.math.distributions;

import dr.stats.DiscreteStatistics;
import dr.math.GammaFunction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;


/**
 * @author Jennifer Tom
 * Based on S. X. Chen. Probability density function estimation using gamma kernels. Annals of the
Institute of Statistical Mathematics, 52(3):471�480, 2000.
 * Use to create KDE for positive valued functions
 * Assumes limits are (0, inf)
 * Must provide with a bandwidth, or defaults to Scott's Rule
 * Univariate distribution only
 */
public class GammaKDEDistribution extends KernelDensityEstimatorDistribution {


     public GammaKDEDistribution(double[] sample, Double bandWidth) {
         super(sample, 0.0, Double.POSITIVE_INFINITY, bandWidth);

     }

     protected void processBounds(Double lowerBound, Double upperBound) {
         if (lowerBound > DiscreteStatistics.min(sample)) {
             throw new RuntimeException("Sample min out of bounds.  Gamma kernel for use with positive data only: "+DiscreteStatistics.min(sample));
         }
         else if (upperBound < DiscreteStatistics.max(sample)) {
             throw new RuntimeException("Sample max out of bounds" +DiscreteStatistics.max(sample));
         }
         this.lowerBound = lowerBound;
         this.upperBound = upperBound;

     }

     protected void setBandWidth(Double bandWidth) {
         if (bandWidth == null) {
       double sigma = DiscreteStatistics.stdev(sample);
       //Scott's rule  (Hardle, 2004, Nonparametric and Semiparameteric Models)
       this.bandWidth =  sigma*Math.pow(N, -0.2);
     }   else
       this.bandWidth = bandWidth;

     }


     protected double evaluateKernel(double x) {

        double shape;
        double scale;

        if (x >= 2*bandWidth) {
            shape = x/bandWidth;
        } else
        {
            shape = .25*Math.pow(x/bandWidth,2) + 1;
        }
        scale = bandWidth;
        double pdf = 0;
        for (int i = 0; i < N; i++) {
             pdf +=
        Math.pow(sample[i],shape-1)*Math.exp(-sample[i]/scale)/(Math.pow(scale,shape)*gamma(shape));
        }
        return pdf/N;
     }

    private double gamma(double value) {
        return cern.jet.stat.Gamma.gamma(value);

    }

    private double sampleMean() {return DiscreteStatistics.mean(sample);}


// public static void main(String[] args) {
//    String fileName = "/Users/jen/School/Programs/BEAST/kdeTest/simulUExp.txt";
//    //String fileName = "/Users/jen/School/Programs/BEAST/kdeTest/simulUNorm.txt";
//    double[] values = null;  //vector of the training set
//    try{
//    BufferedReader br = new BufferedReader(new FileReader(fileName));
//    StringBuilder AllDataSb = new StringBuilder();
//    String s;
//    String myLineSeparator = "\r";
//   while (( s = br.readLine()) != null){
//    AllDataSb.append(s);
//        AllDataSb.append(myLineSeparator);
//    }
//    //convert from stringbuilder to string
//    String AllDataS = AllDataSb.toString();
//    String[] result = AllDataS.split("\t|\r|,");
//
//    //convert string to double
//     values = new double[result.length];
//     for (int i = 0; i < result.length; i++) {values[i] = Double.parseDouble(result[i]);}
//
//    } //close try
//    catch(Exception e){
//        System.out.println(e.getMessage());
//        System.exit(-1);
//    }
//
//
//     GammaKDEDistribution kde;
//
//
//     //expecting 2.02177 -> .073; .4046729 -> 1.0257; 0.1502078 -> 1.4021
//     //normal-reference bandwidth              0.1939
//     kde = new GammaKDEDistribution(values, null);
//     System.err.println("prediction: at 2.02: "+kde.pdf(2.02177)+" at 0.405: "+kde.pdf(0.4046729)+" at 0.15: "+kde.pdf(0.1502078));
//     System.err.println("sm: "+kde.sampleMean());
// }
}
