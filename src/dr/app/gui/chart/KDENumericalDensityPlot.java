package dr.app.gui.chart;

import dr.inference.trace.TraceDistribution;
import dr.math.distributions.GammaKDEDistribution;
import dr.math.distributions.KernelDensityEstimatorDistribution;
import dr.math.distributions.NormalKDEDistribution;
import dr.stats.Variate;
import dr.util.FrequencyDistribution;

/**
 * @author Marc A. Suchard
 */
public class KDENumericalDensityPlot extends NumericalDensityPlot { //Plot.AbstractPlot {


    public KDENumericalDensityPlot(double[] data, int minimumBinCount, TraceDistribution traceD) {
        super(data, minimumBinCount, traceD); // TODO Remove when all linked together

//        kde = new GammaKDEDistribution(data);
//
//        System.err.println("Making KDE with " + minimumBinCount + " points");
//
//        Variate xData = getXCoordinates(minimumBinCount);
//        Variate yData = getYCoordinates(xData);
//        setData(xData, yData);
    }

    private KernelDensityEstimatorDistribution getKDE(double[] samples) {
//        System.err.println("samples is null? " + (samples == null ? "yes" : "no"));
//        System.err.println("type is null? " + (type == null ? "yes" : "no"));
        type = KernelDensityEstimatorDistribution.Type.GAUSSIAN;
        switch (type) {
            case GAUSSIAN: return new NormalKDEDistribution(samples);
            case GAMMA: return new GammaKDEDistribution(samples);
            default:
                throw new RuntimeException("Unknown type");
        }
    }

    /**
     * Set data
     */
    public void setData(Variate data, int minimumBinCount) {

        setRawData(data);
        double[] samples = new double[data.getCount()];
        for (int i = 0; i < data.getCount(); i++) {
            samples[i] = data.get(i);
        }
        kde = getKDE(samples);

        FrequencyDistribution frequency = getFrequencyDistribution(data, minimumBinCount);

        Variate.Double xData = new Variate.Double();
        Variate.Double yData = new Variate.Double();

//        double x = frequency.getLowerBound() - frequency.getBinSize();
//        double maxDensity = 0.0;
//        // TODO Compute KDE once
////        for (int i = 0; i < frequency.getBinCount(); i++) {
////            double density = frequency.getFrequency(i) / frequency.getBinSize() / data.getCount();
////            if (density > maxDensity) maxDensity = density;
////        }
//
//        xData.add(x + (frequency.getBinSize() / 2.0));
//        yData.add(0.0);
//        x += frequency.getBinSize();
//
//        for (int i = 0; i < frequency.getBinCount(); i++) {
//            double xPoint = x + (frequency.getBinSize() / 2.0);
//            xData.add(xPoint);
////            double density = frequency.getFrequency(i) / frequency.getBinSize() / data.getCount();
//            double density = kde.pdf(xPoint);
//            if (relativeDensity) {
//                yData.add(density / maxDensity);
//            } else {
//                yData.add(density);
//            }
//            x += frequency.getBinSize();
//        }
//
//        xData.add(x + (frequency.getBinSize() / 2.0));
//        yData.add(0.0);
        double x = frequency.getLowerBound() - (frequency.getBinSize() / 2.0);
        int extraEdgeCount = 0;
        while (kde.pdf(x) > minDensity && x > lowerBoundary) {
            x -= frequency.getBinSize();
            extraEdgeCount += 1;
        }
        xData.add(x);
        yData.add(0.0);
        x += frequency.getBinSize();
        int count = 0;
        while (count < (frequency.getBinCount() + extraEdgeCount)) {// ||
//                (kde.pdf(x) > minDensity && x < upperBoundary)) {
            xData.add(x);
            yData.add(kde.pdf(x));
            x += frequency.getBinSize();
            count++;
        }
        System.err.println("kde = " + kde.pdf(x));
        while (kde.pdf(x) > minDensity ) {
            System.err.println("add bit on end!!!");
            xData.add(x);
            yData.add(kde.pdf(x));
            x += frequency.getBinSize();
        }
        xData.add(x);
        yData.add(0.0);



//
//
//        int extraBinsOnEdges = 5;
//        double x = frequency.getLowerBound() - extraBinsOnEdges * frequency.getBinSize();
//        for (int i = 0; i < frequency.getBinCount() + 2 * extraBinsOnEdges; i++) {
//            double xMidPoint = x + (frequency.getBinSize() / 2.0);
//            xData.add(xMidPoint);
//            yData.add(kde.pdf(xMidPoint));
//            x += frequency.getBinSize();
//        }

        setData(xData, yData);
    }

    protected Variate getXCoordinates(int numPoints) {
        double[] points = new double[numPoints];
        for (int i = 0; i < numPoints; i++) {
            points[i] = i;
        }
        return new Variate.Double(points);
    }

    protected Variate getYCoordinates(Variate xData) {
        final int length = xData.getCount();
        double[] points = new double[length];
        for (int i = 0; i < length; i++) {
            points[i] = kde.pdf(xData.get(i));
        }
        return new Variate.Double(points);
    }

    private KernelDensityEstimatorDistribution kde;
    private NumericalDensityPlot densityPlot;

    private KernelDensityEstimatorDistribution.Type type;

    private double lowerBoundary = 0;
    private double upperBoundary = Double.POSITIVE_INFINITY;
    private static final double minDensity = 10E-6;

//    @Override
//
//    protected void paintData(Graphics2D g2, Variate xData, Variate yData) {
//
//        double x = transformX(xData.get(0));
//        double y = transformY(yData.get(0));
//
//        GeneralPath path = new GeneralPath();
//        path.moveTo((float) x, (float) y);
//
//        int n = xData.getCount();
//        boolean failed = false;
//        for (int i = 1; i < n; i++) {
//            x = transformX(xData.get(i));
//            y = transformY(yData.get(i));
//            if (x == Double.NEGATIVE_INFINITY || y == Double.NEGATIVE_INFINITY ||
//                    Double.isNaN(x) || Double.isNaN(y)) {
//                failed = true;
//            } else if (failed) {
//                failed = false;
//                path.moveTo((float) x, (float) y);
//            } else {
//                path.lineTo((float) x, (float) y);
//            }
//        }
//
//        g2.setPaint(linePaint);
//        g2.setStroke(lineStroke);
//
//        g2.draw(path);
//
//    }
}
