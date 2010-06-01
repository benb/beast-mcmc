package dr.app.tools;

import dr.app.beast.BeastVersion;
import dr.app.util.Arguments;
import dr.evolution.io.Importer;
import dr.evolution.io.NewickImporter;
import dr.evolution.io.NexusImporter;
import dr.evolution.io.TreeImporter;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.geo.KMLCoordinates;
import dr.geo.KernelDensityEstimator2D;
import dr.geo.Polygon2D;
import dr.geo.contouring.*;
import dr.geo.math.SphericalPolarCoordinates;
import dr.inference.trace.Trace;
import dr.inference.trace.TraceDistribution;
import dr.math.distributions.MultivariateNormalDistribution;
import dr.util.HeapSort;
import dr.util.Version;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.awt.geom.Point2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Marc A. Suchard
 * @author Philippe Lemey
 *         <p/>
 *         example for location slices through time:
 *         -burnin1000 -trait location -sliceFileHeights sliceTimes -summary true -mrsd 2007.63 -format KML -progress true -impute true -noise true -HPD 80 WNV_relaxed_geo_1.trees outpu.txt
 *         <p/>
 *         example for Markov reward o backbone summary (note we cannot imput here):
 *         -burnin1 -trait africa_R,canada_R,europe_R,hawai_R,indochina_R,new_zealand_R,russia_R,east_china_R,mideast_japan_R,midwest_china_R,midwest_japan_R,north_china_R,northeast_australia_R,northeast_japan_R,northeast_usa_R,northwest_australia_R,northwest_usa_R,south_america_R,south_asia_R,south_australia_R,south_china_R,south_korea_R,southeast_asia_R,southeast_usa_R,southwest_japan_R,southwest_usa_R -sliceTimes2003,2004,2005,2006 -summary true -mrsd 2007 -branchNorm true -format TAB -progress true -backbonetaxa taxaBackbone -branchset backbone H3N2_rev.trees output.txt
 */

public class TimeSlicer {

    public static final String sep = "\t";
    public static final String PRECISION_STRING = "precision";
    public static final String RATE_STRING = "rate"; // TODO the rate used is supposed to be the relaxed diffusion rate, but relaxed clock models will also result in a "rate attribute", we somehow need to distinguis between them!

    public static final String SLICE_ELEMENT = "slice";
    public static final String REGIONS_ELEMENT = "hpdRegion";
    public static final String TRAIT = "trait";
    public static final String NAME = "name";
    public static final String DENSITY_VALUE = "density";
    public static final String SLICE_VALUE = "time";

    public static final String STYLE = "Style";
    public static final String ID = "id";
    public static final String WIDTH = "0.5";
    public static final String startHPDColor = "FFFFFF"; //blue=B36600
    public static final String endHPDColor = "0000FF"; //red=0000FF
    public static final String opacity = "6f";

    public static final String BURNIN = "burnin";
    public static final String SKIP = "skip";
    public static final String SLICE_TIMES = "sliceTimes";
    public static final String SLICE_HEIGHTS = "sliceHeights";
    public static final String SLICE_COUNT = "sliceCount";
    public static final String START_TIME = "startTime";
    public static final String SLICE_FILE_HEIGHTS = "sliceFileHeights";
    public static final String SLICE_FILE_TIMES = "sliceFileTimes";
    public static final String MRSD = "mrsd";
    public static final String HELP = "help";
    public static final String NOISE = "noise";
    public static final String IMPUTE = "impute";
    public static final String SUMMARY = "summary";
    public static final String FORMAT = "format";
    public static final String MODE = "mode";
    public static final String NORMALIZATION = "normalization";
    public static final String HPD = "hpd";
    public static final String SDR = "sdr";
    public static final String PROGRESS = "progress";
    public static final double treeLengthPercentage = 0.00;
    public static final String BRANCH_NORMALIZE = "branchnorm";
    public static final String BRANCHSET = "branchset";
    public static final String BACKBONETAXA = "backbonetaxa";

    public static final String[] falseTrue = new String[]{"false", "true"};

    private final static Calendar calendar = GregorianCalendar.getInstance();
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public TimeSlicer(String treeFileName, int burnin, int skipEvery, String[] traits, double[] sliceHeights, boolean impute,
                      boolean trueNoise, double mrsd, ContourMode contourMode,
                      Normalization normalize, boolean getSRD, String progress, boolean branchNormalization, BranchSet branchset, Set backboneTaxa) {

        this.traits = traits;
        traitCount = traits.length;

        sliceCount = 1;
        doSlices = false;
        mostRecentSamplingDate = mrsd;
        this.contourMode = contourMode;
        sdr = getSRD;

        if (progress != null) {
            if (progress.equalsIgnoreCase("true")) {
                sliceProgressReport = true;
            } else if (progress.equalsIgnoreCase("check")) {
                sliceProgressReport = true;
                checkSliceContours = true;
            }
        }

        if (sliceHeights != null) {
            sliceCount = sliceHeights.length;
            doSlices = true;

            this.sliceHeights = sliceHeights;
        }

        values = new ArrayList<List<List<Trait>>>(sliceCount);
        for (int i = 0; i < sliceCount; i++) {
            List<List<Trait>> thisSlice = new ArrayList<List<Trait>>(traitCount);
            values.add(thisSlice);
            for (int j = 0; j < traitCount; j++) {
                List<Trait> thisTraitSlice = new ArrayList<Trait>();
                thisSlice.add(thisTraitSlice);
            }
        }

        try {
            readAndAnalyzeTrees(treeFileName, burnin, skipEvery, traits, sliceHeights, impute, trueNoise, normalize, branchNormalization, branchset, backboneTaxa);
        } catch (IOException e) {
            System.err.println("Error reading file: " + treeFileName);
            System.exit(-1);
        } catch (Importer.ImportException e) {
            System.err.println("Error parsing trees in file: " + treeFileName);
            System.exit(-1);
        }

        if (values.get(0).get(0).size() == 0) {
            System.err.println("Trait(s) values missing from trees.");
            System.exit(-1);
        }

        progressStream.println(treesRead + " trees read.");
        progressStream.println(treesAnalyzed + " trees analyzed.");

    }

    public void output(String outFileName, boolean summaryOnly) {
        output(outFileName, summaryOnly, OutputFormat.XML, 0.80, null);
    }

    private Element rootElement;
    private Element documentElement;
    private Element folderElement;
    private StringBuffer tabOutput = new StringBuffer();

    public void output(String outFileName, boolean summaryOnly, OutputFormat outputFormat, double hpdValue, String sdrFile) {

        resultsStream = System.out;

        if (outFileName != null) {
            try {
                resultsStream = new PrintStream(new File(outFileName));
            } catch (IOException e) {
                System.err.println("Error opening file: " + outFileName);
                System.exit(-1);
            }
        }

        if (!summaryOnly) {
            outputHeader(traits);

            if (sliceHeights == null || sliceHeights.length == 0) {
                outputSlice(0, Double.NaN);
            } else {
                for (int i = 0; i < sliceHeights.length; i++) {
                    outputSlice(i, sliceHeights[i]);
                }

            }
        } else { // Output summaries

            if (outputFormat == OutputFormat.XML) {
                rootElement = new Element("xml");

            } else if (outputFormat == OutputFormat.KML) {
                Element folderNameElement = new Element("name");
                folderNameElement.addContent("surface HPD regions");

                folderElement = new Element("Folder");
                //rootElement.setAttribute("xmlns","http://earth.google.com/kml/2.2");
                folderElement.addContent(folderNameElement);

                Element documentNameElement = new Element("name");
                String documentName = outFileName;
                if (documentName == null)
                    documentName = "default";
                if (documentName.endsWith(".kml"))
                    documentName = documentName.replace(".kml", "");
                documentNameElement.addContent(documentName);

                documentElement = new Element("Document");
                documentElement.addContent(documentNameElement);
                documentElement.addContent(folderElement);

                rootElement = new Element("kml");
                rootElement.addContent(documentElement);
            }

            if (sliceHeights == null)
                summarizeSlice(0, Double.NaN, outputFormat, hpdValue);
            else {
                if (outputFormat == OutputFormat.TAB) {
                    if (mostRecentSamplingDate > 0) {
                        tabOutput.append("trait\t" + "sliceTime\t" + "mean\t" + "stdev\t" + "HPDlow\t" + "HPDup");
                    } else {
                        tabOutput.append("trait\t" + "sliceHeight\t" + "mean\t" + "stdev\t" + "HPDlow\t" + "HPDup");
                    }
                }
                for (int i = 0; i < sliceHeights.length; i++)
                    summarizeSlice(i, sliceHeights[i], outputFormat, hpdValue);
            }

            if (outputFormat == OutputFormat.TAB) {
                resultsStream.println(tabOutput);
            } else {
                XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat().setTextMode(Format.TextMode.PRESERVE));
                try {
                    xmlOutputter.output(rootElement, resultsStream);
                } catch (IOException e) {
                    System.err.println("IO Exception encountered: " + e.getMessage());
                    System.exit(-1);
                }
            }

        }
// writes out dispersal rate summaries for the whole tree, there is a beast xml statistic that can do this now
//        if (containsLocation) {
//            try{
//                PrintWriter dispersalRateFile = new PrintWriter(new FileWriter("dispersalRates.log"), true);
//                dispersalRateFile.print("state"+"\t"+"dispersalRate(native units)"+"\t"+"dispersalRate(great circle distance, km)\r");
//                for(int x = 0; x < dispersalrates.size(); x++ ) {
//                    dispersalRateFile.print(x+"\t"+dispersalrates.get(x)+"\r");
//                }
//
//            } catch (IOException e) {
//                System.err.println("IO Exception encountered: "+e.getMessage());
//                System.exit(-1);
//            }
//        }


        if (sdr) {
            double[][] sliceTreeWeightedAverageRates = new double[sliceTreeDistanceArrays.size()][sliceCount];
            double[][] sliceTreeMaxRates = new double[sliceTreeDistanceArrays.size()][sliceCount];

            double[][] sliceTreeDistances = new double[sliceCount][sliceTreeDistanceArrays.size()];
            double[][] sliceTreeTimes = new double[sliceCount][sliceTreeDistanceArrays.size()];
            //double[][] sliceTreeMaxDistances = new double[sliceCount][sliceTreeMaxDistanceArrays.size()];
            double[][] sliceTreeMaxDistances = new double[sliceTreeMaxDistanceArrays.size()][sliceCount];
            double[][] sliceTreeTimesFromRoot = new double[sliceCount][sliceTreeTimeFromRootArrays.size()];
            double[][] sliceTreeDiffusionCoefficients = new double[sliceTreeDiffusionCoefficientArrays.size()][sliceCount];
            //double[][] sliceTreeWeightedAverageDiffusionCoefficients = new double[sliceTreeDistanceArrays.size()][sliceCount];
            for (int q = 0; q < sliceTreeDistanceArrays.size(); q++) {
                double[] distanceArray = (double[]) sliceTreeDistanceArrays.get(q);
                double[] timeArray = (double[]) sliceTreeTimeArrays.get(q);
                double[] maxDistanceArray = (double[]) sliceTreeMaxDistanceArrays.get(q);
                double[] timeFromRootArray = (double[]) sliceTreeTimeFromRootArrays.get(q);
                double[] diffusionCoefficientArray = (double[]) sliceTreeDiffusionCoefficientArrays.get(q);
                for (int r = 0; r < distanceArray.length; r++) {
                    sliceTreeDistances[r][q] = distanceArray[r];
                    sliceTreeTimes[r][q] = timeArray[r];
                    sliceTreeMaxDistances[q][r] = maxDistanceArray[r];
                    sliceTreeTimesFromRoot[r][q] = timeFromRootArray[r];
                    sliceTreeDiffusionCoefficients[q][r] = diffusionCoefficientArray[r];
                }
            }

            //print2DArray(sliceTreeDistances,"sliceTreeDistances.txt");
            //print2DArray(sliceTreeTimes,"sliceTreeTimes.txt");
            //print2DTransposedArray(sliceTreeDiffusionCoefficients,"sliceTreeDiffusionCoefficients.txt");
            //print2DArray(sliceTreeTimesFromRoot,"sliceTreeTimesFromRoot.txt");

            if (sliceCount > 1) {
                for (int s = 0; s < sliceTreeDistanceArrays.size(); s++) {
                    double[] distanceArray = (double[]) sliceTreeDistanceArrays.get(s);
                    double[] timeArray = (double[]) sliceTreeTimeArrays.get(s);
                    double[] maxDistanceArray = (double[]) sliceTreeMaxDistanceArrays.get(s);
                    double[] timeFromRoot = (double[]) sliceTreeTimeFromRootArrays.get(s);

                    for (int t = 0; t < (sliceCount - 1); t++) {
                        sliceTreeMaxRates[s][t] = (maxDistanceArray[t]) / (timeFromRoot[t]);
                        if ((timeArray[t] - timeArray[t + 1]) > ((Double) treeLengths.get(s) * treeLengthPercentage)) {
                            sliceTreeWeightedAverageRates[s][t] = (distanceArray[t] - distanceArray[t + 1]) / (timeArray[t] - timeArray[t + 1]);
                            //sliceTreeWeightedAverageRates[s][t] = (sliceTreeDistances[t][s] - sliceTreeDistances[t+1][s])/(sliceTreeTimes[t][s] - sliceTreeTimes[t+1][s]);
                            //sliceTreeWeightedAverageDiffusionCoefficients[s][t] = Math.pow((distanceArray[t] - distanceArray[t+1]),2.0)/(4.0*(timeArray[t] - timeArray[t+1]));
                        } else {
                            if (timeArray[t] > 0) {
                                if (t == 0) {
                                    throw new RuntimeException("Philippe to fix: the time slices are expected in ascending height order");
                                }
                                sliceTreeWeightedAverageRates[s][t] = sliceTreeWeightedAverageRates[s][t - 1];
                                //sliceTreeWeightedAverageDiffusionCoefficients[s][t] = sliceTreeWeightedAverageDiffusionCoefficients[s][t-1];
                            } else {
                                //set it to NaN, we ignore NaNs when getting summary stats
                                sliceTreeWeightedAverageRates[s][t] = Double.NaN;
                                //sliceTreeWeightedAverageDiffusionCoefficients[s][t] = Double.NaN;
                            }
                        }
                    }

                    if ((timeArray[sliceCount - 1]) > ((Double) treeLengths.get(s) * treeLengthPercentage)) {
                        sliceTreeWeightedAverageRates[s][sliceCount - 1] = (distanceArray[sliceCount - 1]) / (timeArray[sliceCount - 1]);
                        //sliceTreeWeightedAverageDiffusionCoefficients[s][sliceCount-1] = Math.pow(distanceArray[sliceCount-1],2.0)/(4.0*timeArray[sliceCount-1]);
                    } else {
                        if ((timeArray[sliceCount - 1]) > 0) {
                            sliceTreeWeightedAverageRates[s][sliceCount - 1] = sliceTreeWeightedAverageRates[s][sliceCount - 2];
                            //sliceTreeWeightedAverageDiffusionCoefficients[s][sliceCount-1] = sliceTreeWeightedAverageDiffusionCoefficients[s][sliceCount-2];
                        } else {
                            sliceTreeWeightedAverageRates[s][sliceCount - 1] = (distanceArray[sliceCount - 1]) / (timeArray[sliceCount - 1]);
                            //sliceTreeWeightedAverageDiffusionCoefficients[s][sliceCount-1] = Math.pow(distanceArray[sliceCount-1],2.0)/(4.0*timeArray[sliceCount-1]);
                        }
                    }
                    sliceTreeMaxRates[s][sliceCount - 1] = maxDistanceArray[sliceCount - 1] / timeFromRoot[sliceCount - 1];
                }
            } else {
                for (int s = 0; s < sliceTreeDistanceArrays.size(); s++) {
                    double[] distanceArray = (double[]) sliceTreeDistanceArrays.get(s);
                    double[] timeArray = (double[]) sliceTreeTimeArrays.get(s);
                    double[] maxDistanceArray = (double[]) sliceTreeMaxDistanceArrays.get(s);
                    double[] timeFromRoot = (double[]) sliceTreeTimeFromRootArrays.get(s);
                    sliceTreeWeightedAverageRates[s][0] = distanceArray[0] / timeArray[0];
                    //sliceTreeWeightedAverageDiffusionCoefficients[s][0] = Math.pow(distanceArray[0],2.0)/(4.0*timeArray[0]);
                    sliceTreeMaxRates[s][0] = maxDistanceArray[0] / timeFromRoot[0];
                }
            }

            //print2DArray(sliceTreeWeightedAverageRates,"sliceTreeWeightedAverageRates.txt");


            try {
                PrintWriter sliceDispersalRateFile = new PrintWriter(new FileWriter(sdrFile), true);
                sliceDispersalRateFile.print("sliceTime" + "\t");
                if (mostRecentSamplingDate > 0) {
                    sliceDispersalRateFile.print("realTime" + "\t");
                }
                sliceDispersalRateFile.print("mean dispersalRate" + "\t" + "hpd low" + "\t" + "hpd up" + "\t" + "mean MaxDispersalRate" + "\t" + "hpd low" + "\t" + "hpd up" + "\t" + "mean MaxDispersalDistance" + "\t" + "hpd low" + "\t" + "hpd up" + "\t" + "mean cumulative DiffusionCoefficient" + "\t" + "hpd low" + "\t" + "hpd up" + "\r");
                double[] meanWeightedAverageDispersalRates = meanColNoNaN(sliceTreeWeightedAverageRates);
                double[][] hpdWeightedAverageDispersalRates = getArrayHPDintervals(sliceTreeWeightedAverageRates);
                double[] meanMaxDispersalDistances = meanColNoNaN(sliceTreeMaxDistances);
                double[][] hpdMaxDispersalDistances = getArrayHPDintervals(sliceTreeMaxDistances);
                double[] meanMaxDispersalRates = meanColNoNaN(sliceTreeMaxRates);
                double[][] hpdMaxDispersalRates = getArrayHPDintervals(sliceTreeMaxRates);
                double[] meanDiffusionCoefficients = meanColNoNaN(sliceTreeDiffusionCoefficients);
                double[][] hpdDiffusionCoefficients = getArrayHPDintervals(sliceTreeDiffusionCoefficients);
                //double[] meanWeightedAverageDiffusionCoefficients = meanColNoNaN(sliceTreeWeightedAverageDiffusionCoefficients);
                //double[][] hpdWeightedAverageDiffusionCoefficients = getArrayHPDintervals(sliceTreeWeightedAverageDiffusionCoefficients);
                for (int u = 0; u < sliceCount; u++) {
                    sliceDispersalRateFile.print(sliceHeights[u] + "\t");
                    if (mostRecentSamplingDate > 0) {
                        sliceDispersalRateFile.print((mostRecentSamplingDate - sliceHeights[u]) + "\t");
                    }
                    sliceDispersalRateFile.print(meanWeightedAverageDispersalRates[u] + "\t" + hpdWeightedAverageDispersalRates[u][0] + "\t" + hpdWeightedAverageDispersalRates[u][1] + "\t" + meanMaxDispersalRates[u] + "\t" + hpdMaxDispersalRates[u][0] + "\t" + hpdMaxDispersalRates[u][1] + "\t" + meanMaxDispersalDistances[u] + "\t" + hpdMaxDispersalDistances[u][0] + "\t" + hpdMaxDispersalDistances[u][1] + "\t" + meanDiffusionCoefficients[u] + "\t" + hpdDiffusionCoefficients[u][0] + "\t" + hpdDiffusionCoefficients[u][1] + "\r");
                }
                sliceDispersalRateFile.close();
            } catch (IOException e) {
                System.err.println("IO Exception encountered: " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    enum Normalization {
        LENGTH,
        HEIGHT,
        NONE
    }

    enum OutputFormat {
        TAB,
        KML,
        XML
    }

    enum BranchSet {
        ALL,
        INT,
        EXT,
        BACKBONE
    }

    public static <T extends Enum<T>> String[] enumNamesToStringArray(T[] values) {
        int i = 0;
        String[] result = new String[values.length];
        for (T value : values) {
            result[i++] = value.name();
        }
        return result;
    }


    private void addDimInfo(Element element, int j, int dim) {
        if (dim > 1)
            element.setAttribute("dim", Integer.toString(j + 1));
    }

    private void summarizeSliceTrait(Element sliceElement, int slice, List<Trait> thisTrait, int traitIndex, double sliceValue,
                                     OutputFormat outputFormat,
                                     double hpdValue) {

        if (thisTrait.size() == 0) {
            return;
        }

        boolean isNumber = thisTrait.get(0).isNumber();
        boolean isMultivariate = thisTrait.get(0).isMultivariate();
        int dim = thisTrait.get(0).getDim();
        boolean isBivariate = isMultivariate && dim == 2;
        if (sliceProgressReport) {
            progressStream.print("slice " + sliceValue + "\t");
            if (mostRecentSamplingDate > 0) {
                progressStream.print("time=" + (mostRecentSamplingDate - sliceValue) + "\t");
            }
            progressStream.print("trait=" + traits[traitIndex] + "\t");
        }
        if (isNumber) {

            Element traitElement = null;
            if (outputFormat == OutputFormat.XML || outputFormat == OutputFormat.TAB) {
                traitElement = new Element(TRAIT);
                traitElement.setAttribute(NAME, traits[traitIndex]);
            }

            if (outputFormat == OutputFormat.KML) {
                Element styleElement = new Element(STYLE);
                constructPolygonStyleElement(styleElement, sliceValue);
                documentElement.addContent(styleElement);
            }

            int count = thisTrait.size();
            Double[][] x = new Double[dim][count];
            for (int i = 0; i < count; i++) {
                Trait trait = thisTrait.get(i);
                double[] value = trait.getValue();
                for (int j = 0; j < dim; j++)
                    x[j][i] = value[j];
            }

            if (outputFormat == OutputFormat.XML || outputFormat == OutputFormat.TAB) {
                // Compute marginal means and standard deviations
                for (int j = 0; j < dim; j++) {
                    TraceDistribution trace = new TraceDistribution(x[j]);
                    Element statsElement = new Element("stats");
                    addDimInfo(statsElement, j, dim);
                    StringBuffer sb = new StringBuffer();
                    sb.append(KMLCoordinates.NEWLINE);
                    tabOutput.append(KMLCoordinates.NEWLINE);
                    tabOutput.append(traits[traitIndex] + "\t");
                    if (mostRecentSamplingDate > 0) {
                        tabOutput.append((mostRecentSamplingDate - sliceValue) + "\t");
                    } else {
                        tabOutput.append(sliceValue + "\t");
                    }
                    sb.append(String.format(KMLCoordinates.FORMAT,
                            trace.getMean())).append(KMLCoordinates.SEPARATOR);
                    tabOutput.append(String.format(KMLCoordinates.FORMAT,
                            trace.getMean())).append("\t");
                    sb.append(String.format(KMLCoordinates.FORMAT,
                            trace.getStdError())).append(KMLCoordinates.SEPARATOR);
                    tabOutput.append(String.format(KMLCoordinates.FORMAT,
                            trace.getStdError())).append("\t");
                    sb.append(String.format(KMLCoordinates.FORMAT,
                            trace.getLowerHPD())).append(KMLCoordinates.SEPARATOR);
                    tabOutput.append(String.format(KMLCoordinates.FORMAT,
                            trace.getLowerHPD())).append("\t");
                    sb.append(String.format(KMLCoordinates.FORMAT,
                            trace.getUpperHPD())).append(KMLCoordinates.NEWLINE);
                    tabOutput.append(String.format(KMLCoordinates.FORMAT,
                            trace.getUpperHPD())).append("\t");
                    statsElement.addContent(sb.toString());
                    traitElement.addContent(statsElement);
                }
            }

            if (isBivariate) {

                //for testing how much points are within the polygons
                double numberOfPointsInPolygons = 0;
                double totalArea = 0;

                ContourMaker contourMaker;
                if (contourMode == ContourMode.JAVA)
                    contourMaker = new KernelDensityEstimator2D(Trace.arrayConvert(x[0]), Trace.arrayConvert(x[1]));
                else if (contourMode == ContourMode.R)
                    contourMaker = new ContourWithR(Trace.arrayConvert(x[0]), Trace.arrayConvert(x[1]));
                else if (contourMode == ContourMode.SNYDER)
                    contourMaker = new ContourWithSynder(Trace.arrayConvert(x[0]), Trace.arrayConvert(x[1]));
                else
                    throw new RuntimeException("Unimplemented ContourModel!");

                ContourPath[] paths = contourMaker.getContourPaths(hpdValue);
                for (ContourPath path : paths) {

                    KMLCoordinates coords = new KMLCoordinates(path.getAllX(), path.getAllY());
                    if (outputFormat == OutputFormat.XML) {
                        Element regionElement = new Element(REGIONS_ELEMENT);
                        regionElement.setAttribute(DENSITY_VALUE, Double.toString(hpdValue));
                        regionElement.addContent(coords.toXML());
                        traitElement.addContent(regionElement);
                    }

                    // only if the trait is location we will write KML
                    if (outputFormat == OutputFormat.KML) {
                        //because KML polygons require long,lat,alt we need to switch lat and long first
                        coords.switchXY();
                        Element placemarkElement = generatePlacemarkElementWithPolygon(sliceValue, coords, slice);
                        //testing how many points are within the polygon
                        if (checkSliceContours) {
                            Element testElement = new Element("test");
                            testElement.addContent(coords.toXML());
                            Polygon2D testPolygon = new Polygon2D(testElement);
                            totalArea += testPolygon.calculateArea();

                            double[][] dest = new double[x.length][x[0].length];
                            for (int i = 0; i < x.length; i++) {
                                for (int j = 0; j < x[0].length; j++) {
                                    dest[i][j] = x[i][j].doubleValue();
                                }
                            }
                            numberOfPointsInPolygons += getNumberOfPointsInPolygon(dest, testPolygon);
                        }

                        folderElement.addContent(placemarkElement);
                    }
                }
                if (checkSliceContours) {
                    progressStream.print("numberOfContours=" + paths.length + "\tfreqOfPointsInContour=" + numberOfPointsInPolygons / count + "\ttotalArea = " + totalArea);
                }

            }
            if (outputFormat == OutputFormat.XML)
                sliceElement.addContent(traitElement);
        } // else skip
        if (sliceProgressReport) {
            progressStream.print("\r");
        }
    }

    public static int getNumberOfPointsInPolygon(double[][] pointsArray, Polygon2D testPolygon) {
        int numberOfPointsInPolygon = 0;
        for (int x = 0; x < pointsArray[0].length; x++) {
            if (testPolygon.containsPoint2D(new Point2D.Double(pointsArray[1][x], pointsArray[0][x]))) {
                numberOfPointsInPolygon++;
            }
        }
        return numberOfPointsInPolygon;
    }

    private void summarizeSlice(int slice, double sliceValue, OutputFormat outputFormat, double hpdValue) {

        //if (outputFormat == OutputFormat.TAB)
        //    throw new RuntimeException("Only XML/KML output is implemented");

        Element sliceElement = null;

        if (outputFormat == OutputFormat.XML) {
            sliceElement = new Element(SLICE_ELEMENT);
            sliceElement.setAttribute(SLICE_VALUE, Double.toString(sliceValue));
        }

        List<List<Trait>> thisSlice = values.get(slice);
        int traitCount = thisSlice.size();

        for (int traitIndex = 0; traitIndex < traitCount; traitIndex++) {

            summarizeSliceTrait(sliceElement, slice, thisSlice.get(traitIndex), traitIndex, sliceValue,
                    outputFormat,
                    hpdValue);
        }

        if (outputFormat == OutputFormat.XML) {
            rootElement.addContent(sliceElement);
        }
    }

    private void constructPolygonStyleElement(Element styleElement, double sliceValue) {
        double date = mostRecentSamplingDate - sliceValue;
        styleElement.setAttribute(ID, REGIONS_ELEMENT + date + "_style");
        Element lineStyle = new Element("LineStyle");
        Element width = new Element("width");
        width.addContent(WIDTH);
        lineStyle.addContent(width);
        Element polyStyle = new Element("PolyStyle");
        Element color = new Element("color");
        double[] minMax = new double[2];
        minMax[0] = sliceHeights[0];
        minMax[1] = sliceHeights[(sliceHeights.length - 1)];
        String colorString = getKMLColor(sliceValue, minMax, endHPDColor, startHPDColor);
        color.addContent(opacity + colorString);
        Element outline = new Element("outline");
        outline.addContent("0");
        polyStyle.addContent(color);
        polyStyle.addContent(outline);
        styleElement.addContent(lineStyle);
        styleElement.addContent(polyStyle);
    }

    private Element generatePlacemarkElementWithPolygon(double sliceValue, KMLCoordinates coords, int sliceInteger) {
        double date = mostRecentSamplingDate - sliceValue;
        Element placemarkElement = new Element("Placemark");

        Element visibility = new Element("visibility");
        visibility.addContent("1");
        placemarkElement.addContent(visibility);

        if (sliceCount > 1) {
            Element timeSpan = new Element("TimeSpan");
            Element begin = new Element("begin");

            calendar.set(Calendar.YEAR, (int) Math.floor(date));
            calendar.set(Calendar.DAY_OF_YEAR, (int) (365 * (date - Math.floor(date))));

            begin.addContent(dateFormat.format(calendar.getTime()));
            timeSpan.addContent(begin);
//            if (sliceInteger > 1) {
//                Element end = new Element("end");
//                end.addContent(Double.toString(mostRecentSamplingDate- sliceHeights[(sliceInteger-1)]));
//                timeSpan.addContent(end);
//            }
            placemarkElement.addContent(timeSpan);
        }

        Element style = new Element("styleUrl");
        style.addContent("#" + REGIONS_ELEMENT + date + "_style");
        placemarkElement.addContent(style);

        Element polygonElement = new Element("Polygon");
        Element altitudeMode = new Element("altitudeMode");
        altitudeMode.addContent("clampToGround");
        polygonElement.addContent(altitudeMode);
        Element tessellate = new Element("tessellate");
        tessellate.addContent("1");
        polygonElement.addContent(tessellate);
        Element outerBoundaryIs = new Element("outerBoundaryIs");
        Element LinearRing = new Element("LinearRing");
        LinearRing.addContent(coords.toXML());
        outerBoundaryIs.addContent(LinearRing);
        polygonElement.addContent(outerBoundaryIs);
        placemarkElement.addContent(polygonElement);

        return placemarkElement;
    }

    private void outputHeader(String[] traits) {
        StringBuffer sb = new StringBuffer("slice");
        for (int i = 0; i < traits.length; i++) {
            // Load first value to check dimensionality
            Trait trait = values.get(0).get(i).get(0);
            if (trait.isMultivariate()) {
                int dim = trait.getDim();
                for (int j = 1; j <= dim; j++)
                    sb.append(sep).append(traits[i]).append(j);
            } else
                sb.append(sep).append(traits[i]);
        }
        sb.append("\n");
        resultsStream.print(sb);
    }

//    private List<Tree> importTrees(String treeFileName, int burnin) throws IOException, Importer.ImportException {
//
//        int totalTrees = 10000;
//
//        progressStream.println("Reading trees (bar assumes 10,000 trees)...");
//        progressStream.println("0              25             50             75            100");
//        progressStream.println("|--------------|--------------|--------------|--------------|");
//
//        int stepSize = totalTrees / 60;
//        if (stepSize < 1) stepSize = 1;
//
//        List<Tree> treeList = new ArrayList<Tree>();
//        BufferedReader reader1 = new BufferedReader(new FileReader(treeFileName));
//
//        String line1 = reader1.readLine();
//        TreeImporter importer1;
//        if (line1.toUpperCase().startsWith("#NEXUS")) {
//            importer1 = new NexusImporter(new FileReader(treeFileName));
//        } else {
//            importer1 = new NewickImporter(new FileReader(treeFileName));
//        }
//        totalTrees = 0;
//        while (importer1.hasTree()) {
//            Tree treeTime = importer1.importNextTree();
//
//            if (totalTrees > burnin)
//                treeList.add(treeTime);
//
//            if (totalTrees > 0 && totalTrees % stepSize == 0) {
//                progressStream.print("*");
//                progressStream.flush();
//            }
//            totalTrees++;
//        }
//        return treeList;
//    }

    private void readAndAnalyzeTrees(String treeFileName, int burnin, int skipEvery,
                                     String[] traits, double[] slices,
                                     boolean impute, boolean trueNoise, Normalization normalize,
                                     boolean divideByBranchLength, BranchSet branchset, Set backboneTaxa)
            throws IOException, Importer.ImportException {

        int totalTrees = 10000;
        int totalStars = 0;

        progressStream.println("Reading and analyzing trees (bar assumes 10,000 trees)...");
        progressStream.println("0              25             50             75            100");
        progressStream.println("|--------------|--------------|--------------|--------------|");

        int stepSize = totalTrees / 60;
        if (stepSize < 1) stepSize = 1;

        BufferedReader reader1 = new BufferedReader(new FileReader(treeFileName));

        String line1 = reader1.readLine();
        TreeImporter importer1;
        if (line1.toUpperCase().startsWith("#NEXUS")) {
            importer1 = new NexusImporter(new FileReader(treeFileName));
        } else {
            importer1 = new NewickImporter(new FileReader(treeFileName));
        }
        totalTrees = 0;

        while (importer1.hasTree()) {
            Tree treeTime = importer1.importNextTree();
            if (totalTrees % skipEvery == 0) {
                treesRead++;
                if (totalTrees >= burnin) {
                    analyzeTree(treeTime, traits, slices, impute, trueNoise, normalize, divideByBranchLength, branchset, backboneTaxa);
                }
            }
            if (totalTrees > 0 && totalTrees % stepSize == 0) {
                progressStream.print("*");
                totalStars++;
                if (totalStars % 61 == 0)
                    progressStream.print("\n");
                progressStream.flush();
            }
            totalTrees++;
        }
        progressStream.print("\n");
    }

    class Trait {

        Trait(Object obj) {
            this.obj = obj;
            if (obj instanceof Object[]) {
                isMultivariate = true;
                array = (Object[]) obj;
            }
        }

        public boolean isMultivariate() {
            return isMultivariate;
        }

        public boolean isNumber() {
            if (!isMultivariate)
                return (obj instanceof Double);
            return (array[0] instanceof Double);
        }

        public int getDim() {
            if (isMultivariate) {
                return array.length;
            }
            return 1;
        }

        public double[] getValue() {
            int dim = getDim();
            double[] result = new double[dim];
            if (!isMultivariate) {
                result[0] = (Double) obj;
            } else {
                for (int i = 0; i < dim; i++)
                    result[i] = (Double) array[i];
            }
            return result;
        }

        public void multiplyBy(double factor) {
            if (!isMultivariate) {
                obj = ((Double) obj * factor);
            } else {
                for (int i = 0; i < array.length; i++) {
                    array[i] = ((Double) array[i] * factor);
                }
            }
        }

        private Object obj;
        private Object[] array;
        private boolean isMultivariate = false;

        public String toString() {
            if (!isMultivariate)
                return obj.toString();
            StringBuffer sb = new StringBuffer(array[0].toString());
            for (int i = 1; i < array.length; i++)
                sb.append(sep).append(array[i]);
            return sb.toString();
        }
    }

    private List<List<List<Trait>>> values;

    private void outputSlice(int slice, double sliceValue) {

        List<List<Trait>> thisSlice = values.get(slice);
        int traitCount = thisSlice.size();
        int valueCount = thisSlice.get(0).size();

        StringBuffer sb = new StringBuffer();

        for (int v = 0; v < valueCount; v++) {
            if (Double.isNaN(sliceValue))
                sb.append("All");
            else
                sb.append(sliceValue);
            for (int t = 0; t < traitCount; t++) {
                sb.append(sep);
                sb.append(thisSlice.get(t).get(v));
            }
            sb.append("\n");
        }

        resultsStream.print(sb);
    }

    private static boolean onBackbone(Tree tree, NodeRef node, Set targetSet) {

        if (tree.isExternal(node)) return false;

        Set leafSet = Tree.Utils.getDescendantLeaves(tree, node);
        int size = leafSet.size();

        leafSet.retainAll(targetSet);

        if (leafSet.size() > 0) {

            // if all leaves below are in target then check just above.
            if (leafSet.size() == size) {

                Set superLeafSet = Tree.Utils.getDescendantLeaves(tree, tree.getParent(node));
                superLeafSet.removeAll(targetSet);

                // the branch is on ancestral path if the super tree has some non-targets in it
                return (superLeafSet.size() > 0);

            } else return true;

        } else return false;
    }

    private void analyzeTree(Tree treeTime, String[] traits, double[] slices, boolean impute,
                             boolean trueNoise, Normalization normalize, boolean divideByBranchlength, BranchSet branchset, Set backboneTaxa) {

        double[][] precision = null;

        if (impute) {

            Object o = treeTime.getAttribute(PRECISION_STRING);
            double treeNormalization = 1; // None
            if (normalize == Normalization.LENGTH) {
                treeNormalization = Tree.Utils.getTreeLength(treeTime, treeTime.getRoot());
            } else if (normalize == Normalization.HEIGHT) {
                treeNormalization = treeTime.getNodeHeight(treeTime.getRoot());
            }

            if (o != null) {
                Object[] array = (Object[]) o;
                int dim = (int) Math.sqrt(1 + 8 * array.length) / 2;
                precision = new double[dim][dim];
                int c = 0;
                for (int i = 0; i < dim; i++) {
                    for (int j = i; j < dim; j++) {
                        precision[j][i] = precision[i][j] = ((Double) array[c++]) * treeNormalization;
                    }
                }
            }
        }


//  employed to get dispersal rates across the whole tree
//        double treeNativeDistance = 0;
//        double treeKilometerGreatCircleDistance = 0;

        double[] treeSliceTime = new double[sliceCount];
        double[] treeSliceDistance = new double[sliceCount];
        double[] treeSliceMaxDistance = new double[sliceCount];
        double[] treeTimeFromRoot = new double[sliceCount];
        double[] maxDistanceFromRoot = new double[sliceCount];
        //this one will used for weighted average (WA)
        //double[] treeSliceDiffusionCoefficientWA = new double[sliceCount];
        //this one is used for simple average
        double[] treeSliceDiffusionCoefficientA = new double[sliceCount];
        double[] treeSliceBranchCount = new double[sliceCount];

        treeLengths.add(Tree.Utils.getTreeLength(treeTime, treeTime.getRoot()));

        for (int x = 0; x < treeTime.getNodeCount(); x++) {

            NodeRef node = treeTime.getNode(x);

            if (!(treeTime.isRoot(node))) {

                double nodeHeight = treeTime.getNodeHeight(node);
                double parentHeight = treeTime.getNodeHeight(treeTime.getParent(node));

                double oneOverBranchLength = 1.0;
                if (divideByBranchlength) {
                    oneOverBranchLength = 1.0 / treeTime.getBranchLength(node);
                }

                boolean proceed = false;
                if (branchset == BranchSet.ALL) {
                    proceed = true;
                } else if (branchset == BranchSet.EXT && treeTime.isExternal(node)) {
                    proceed = true;
                } else if (branchset == BranchSet.INT && !treeTime.isExternal(node)) {
                    proceed = true;
                } else if (branchset == BranchSet.BACKBONE && onBackbone(treeTime, node, backboneTaxa)) {
                    proceed = true;
                }
//  employed to get dispersal rates across the whole tree
//                if (containsLocation){
//
//                    Trait nodeLocationTrait = new Trait (treeTime.getNodeAttribute(node, LOCATIONTRAIT));
//                    Trait parentNodeLocationTrait = new Trait (treeTime.getNodeAttribute(treeTime.getParent(node), LOCATIONTRAIT));
//                    treeNativeDistance += getNativeDistance(nodeLocationTrait.getValue(),parentNodeLocationTrait.getValue());
//                    treeKilometerGreatCircleDistance += getKilometerGreatCircleDistance(nodeLocationTrait.getValue(),parentNodeLocationTrait.getValue());
//
//                }

                for (int i = 0; i < sliceCount; i++) {
                    //System.out.println(slices[i]);
                    if (sdr) {
                        if (!doSlices ||
                                (slices[i] < nodeHeight)
                                ) {
                            if (proceed) {

                                treeSliceTime[i] += (parentHeight - nodeHeight);
                                //TreeModel model = new TreeModel(treeTime, true);
                                //treeSliceDistance[i] += getKilometerGreatCircleDistance(model.getMultivariateNodeTrait(node, LOCATIONTRAIT),model.getMultivariateNodeTrait(model.getParent(node), LOCATIONTRAIT));
                                Trait nodeLocationTrait = new Trait(treeTime.getNodeAttribute(node, traits[0]));
                                Trait parentNodeLocationTrait = new Trait(treeTime.getNodeAttribute(treeTime.getParent(node), traits[0]));
                                treeSliceDistance[i] += getGeographicalDistance(nodeLocationTrait.getValue(), parentNodeLocationTrait.getValue());
                                //treeSliceDiffusionCoefficientWA[i] += (Math.pow((getGeographicalDistance(nodeLocationTrait.getValue(),parentNodeLocationTrait.getValue())),2.0)/(4.0*(parentHeight-nodeHeight)))*(parentHeight-nodeHeight);
                                treeSliceDiffusionCoefficientA[i] += (Math.pow((getGeographicalDistance(nodeLocationTrait.getValue(), parentNodeLocationTrait.getValue())), 2.0) / (4.0 * (parentHeight - nodeHeight)));
                                treeSliceBranchCount[i]++;
                            }
                        }
                    }

                    if (!doSlices ||
                            (slices[i] >= nodeHeight && slices[i] < parentHeight)
                            ) {

                        if (proceed) {

                            List<List<Trait>> thisSlice = values.get(i);
                            for (int j = 0; j < traitCount; j++) {

                                List<Trait> thisTraitSlice = thisSlice.get(j);
                                Object tmpTrait = treeTime.getNodeAttribute(node, traits[j]);
                                if (tmpTrait == null) {
                                    System.err.println("Trait '" + traits[j] + "' not found on branch.");
                                    System.exit(-1);
                                }
                                Trait trait = new Trait(tmpTrait);
                                //System.out.println("trees "+treesAnalyzed+"\tslice "+slices[i]+"\t"+trait.toString());
                                if (divideByBranchlength) {
                                    trait.multiplyBy(oneOverBranchLength);
                                }
                                if (impute) {
                                    Double rateAttribute = (Double) treeTime.getNodeAttribute(node, RATE_STRING);
                                    double rate = 1.0;
                                    if (rateAttribute != null) {
                                        rate = rateAttribute;
                                        if (outputRateWarning) {
                                            progressStream.println("Warning: using a rate attribute during imputation!");
                                            outputRateWarning = false;
                                        }
                                    }
                                    if (trueNoise && precision == null) {
                                        progressStream.println("Error: not precision available for imputation with correct noise!");
                                        System.exit(-1);
                                    }
//                                    if (slices[i] > nodeHeight) {
                                    trait = imputeValue(trait, new Trait(treeTime.getNodeAttribute(treeTime.getParent(node), traits[j])),
                                            slices[i], nodeHeight, parentHeight, precision, rate, trueNoise);
//                                    }
                                    // QUESTION to PL: MAS does not see how slices[i] is ever less than nodeHeight
                                }
                                thisTraitSlice.add(trait);
                                //System.out.println("trees "+treesAnalyzed+"\tslice "+slices[i]+"\t"+trait.toString());

                                //if trait is location
                                if (sdr) {
                                    treeSliceTime[i] += (parentHeight - slices[i]);
                                    Trait parentTrait = new Trait(treeTime.getNodeAttribute(treeTime.getParent(node), traits[j]));
                                    treeSliceDistance[i] += getGeographicalDistance(trait.getValue(), parentTrait.getValue());
                                    treeSliceDiffusionCoefficientA[i] += (Math.pow((getGeographicalDistance(trait.getValue(), parentTrait.getValue())), 2.0) / (4.0 * (parentHeight - slices[i])));
                                    treeSliceBranchCount[i]++;
                                    double tempDistanceFromRoot = getDistanceFromRoot(treeTime, traits[j], trait.getValue());
                                    if (maxDistanceFromRoot[i] < tempDistanceFromRoot) {
                                        maxDistanceFromRoot[i] = tempDistanceFromRoot;
                                        treeSliceMaxDistance[i] = getPathDistance(treeTime, node, traits[j], trait.getValue());
                                        //putting this below here ensures that treeTimeFromRoot is never < 0
                                        treeTimeFromRoot[i] = treeTime.getNodeHeight(treeTime.getRoot()) - slices[i];
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //System.out.println(Tree.Utils.getTreeLength(treeTime, treeTime.getRoot())+"\t"+test);

        if (sdr) {
            sliceTreeDistanceArrays.add(treeSliceDistance);
            sliceTreeTimeArrays.add(treeSliceTime);
            sliceTreeMaxDistanceArrays.add(treeSliceMaxDistance);
            sliceTreeTimeFromRootArrays.add(treeTimeFromRoot);
            for (int i = 0; i < treeSliceDiffusionCoefficientA.length; i++) {
                //treeSliceDiffusionCoefficientWA[i] = treeSliceDiffusionCoefficientWA[i]/treeSliceTime[i];
                treeSliceDiffusionCoefficientA[i] = treeSliceDiffusionCoefficientA[i] / treeSliceBranchCount[i];
                //System.out.println(treeSliceTime[i]+"\t"+treeLengths.get(i));
            }
            sliceTreeDiffusionCoefficientArrays.add(treeSliceDiffusionCoefficientA);
        }


//  employed to get dispersal rates across the whole tree
//        if (containsLocation) {
//           double treelength = Tree.Utils.getTreeLength(treeTime, treeTime.getRoot());
//            double dispersalNativeRate = treeNativeDistance/treelength;
//            double dispersalKilometerRate = treeKilometerGreatCircleDistance/treelength;
        //System.out.println(dispersalNativeRate+"\t"+dispersalKilometerRate);
//            dispersalrates.add(dispersalNativeRate+"\t"+dispersalKilometerRate);
//        }

        treesAnalyzed++;

    }
//  employed to get dispersal rates across the whole tree
//    private static double getNativeDistance(double[] location1, double[] location2) {

    //        return Math.sqrt(Math.pow((location2[0]-location1[0]),2.0)+Math.pow((location2[1]-location1[1]),2.0));
    //    }
    //

    private static double getGeographicalDistance(double[] location1, double[] location2) {
        if (location1.length == 1) {
            // assume we only have latitude so put them on the prime meridian
            return getKilometerGreatCircleDistance(new double[] { location1[0], 0.0}, new double[] { location2[0], 0.0});
        } else if (location1.length == 2) {
            return getKilometerGreatCircleDistance(location1, location2);
        }
        throw new RuntimeException("Distances can only be calculated for longitude and latitude (or just latitude)");
    }

    private static double getKilometerGreatCircleDistance(double[] location1, double[] location2) {
        SphericalPolarCoordinates coord1 = new SphericalPolarCoordinates(location1[0], location1[1]);
        SphericalPolarCoordinates coord2 = new SphericalPolarCoordinates(location2[0], location2[1]);
        return (coord1.distance(coord2));
    }

    private double getPathDistance(Tree tree, NodeRef node, String locationTrait, double[] sliceTrait) {

        double pathDistance = 0;
        NodeRef parentNode = tree.getParent(node);
        Trait parentTrait = new Trait(tree.getNodeAttribute(parentNode, locationTrait));
        pathDistance += getGeographicalDistance(sliceTrait, parentTrait.getValue());

        while (parentNode != tree.getRoot()) {
            node = tree.getParent(node);
            parentNode = tree.getParent(parentNode);
            Trait nodeTrait = new Trait(tree.getNodeAttribute(node, locationTrait));
            parentTrait = new Trait(tree.getNodeAttribute(parentNode, locationTrait));
            pathDistance += getGeographicalDistance(nodeTrait.getValue(), parentTrait.getValue());
        }
        return pathDistance;
    }

    private double getDistanceFromRoot(Tree tree, String locationTrait, double[] sliceTrait) {
        NodeRef rootNode = tree.getRoot();
        Trait rootTrait = new Trait(tree.getNodeAttribute(rootNode, locationTrait));
        return getGeographicalDistance(sliceTrait, rootTrait.getValue());
    }

    private int traitCount;
    private int sliceCount;
    private String[] traits;
    private double[] sliceHeights;
    private boolean sliceProgressReport;
    private boolean checkSliceContours;
    private boolean doSlices;
    private int treesRead = 0;
    private int treesAnalyzed = 0;
    private double mostRecentSamplingDate;
    private ContourMode contourMode;

//  employed to get dispersal rates across the whole tree
//    private static boolean containsLocation = false;
//    private static ArrayList dispersalrates = new ArrayList();

//    private void run(List<Tree> trees, String[] traits, double[] slices, boolean impute, boolean trueNoise) {
//
//        for (Tree treeTime : trees) {
//            analyzeTree(treeTime, traits, slices, impute, trueNoise);
//        }
//    }

    private ArrayList sliceTreeDistanceArrays = new ArrayList();
    private ArrayList sliceTreeTimeArrays = new ArrayList();
    private ArrayList sliceTreeMaxDistanceArrays = new ArrayList();
    private ArrayList sliceTreeTimeFromRootArrays = new ArrayList();
    private ArrayList sliceTreeDiffusionCoefficientArrays = new ArrayList();
    private boolean sdr;
    private ArrayList treeLengths = new ArrayList();

    private boolean outputRateWarning = true;


    private Trait imputeValue(Trait nodeTrait, Trait parentTrait, double time, double nodeHeight, double parentHeight, double[][] precision, double rate, boolean trueNoise) {
        if (!nodeTrait.isNumber()) {
            System.err.println("Can only impute numbers!");
            System.exit(-1);
        }

        int dim = nodeTrait.getDim();
        double[] nodeValue = nodeTrait.getValue();
        double[] parentValue = parentTrait.getValue();

        final double scaledTimeChild = (time - nodeHeight) * rate;
        final double scaledTimeParent = (parentHeight - time) * rate;
        final double scaledWeightTotal = 1.0 / scaledTimeChild + 1.0 / scaledTimeParent;

        if (scaledTimeChild == 0)
            return nodeTrait;

        if (scaledTimeParent == 0)
            return parentTrait;

        // Find mean value, weighted average
        double[] mean = new double[dim];
        double[][] scaledPrecision = new double[dim][dim];

        for (int i = 0; i < dim; i++) {
            mean[i] = (nodeValue[i] / scaledTimeChild + parentValue[i] / scaledTimeParent) / scaledWeightTotal;
            if (trueNoise) {
                for (int j = i; j < dim; j++)
                    scaledPrecision[j][i] = scaledPrecision[i][j] = precision[i][j] * scaledWeightTotal;
            }
        }

        if (trueNoise) {
            mean = MultivariateNormalDistribution.nextMultivariateNormalPrecision(mean, scaledPrecision);
        }
        Object[] result = new Object[dim];
        for (int i = 0; i < dim; i++)
            result[i] = mean[i];
        return new Trait(result);
    }

    // Messages to stderr, output to stdout
    private static PrintStream progressStream = System.err;
    private PrintStream resultsStream;

    private final static Version version = new BeastVersion();

    private static final String commandName = "timeslicer";


    public static void printUsage(Arguments arguments) {

        arguments.printUsage(commandName, "<input-file-name> [<output-file-name>]");
        progressStream.println();
        progressStream.println("  Example: " + commandName + " test.trees out.kml");
        progressStream.println();
    }

    public static void centreLine(String line, int pageWidth) {
        int n = pageWidth - line.length();
        int n1 = n / 2;
        for (int i = 0; i < n1; i++) {
            progressStream.print(" ");
        }
        progressStream.println(line);
    }

    public static void printTitle() {
        progressStream.println();
        centreLine("TimeSlicer " + version.getVersionString() + ", " + version.getDateString(), 60);
        centreLine("MCMC Output analysis", 60);
        centreLine("by", 60);
        centreLine("Marc A. Suchard, Philippe Lemey,", 60);
        centreLine("Alexei J. Drummond and Andrew Rambaut", 60);
        progressStream.println();
        centreLine("Department of Biomathematics", 60);
        centreLine("University of California, Los Angeles", 60);
        centreLine("msuchard@ucla.edu", 60);
        progressStream.println();
        centreLine("Rega Institute for Medical Research", 60);
        centreLine("Katholieke Universiteit Leuven", 60);
        centreLine("philippe.lemey@gmail.com", 60);
        progressStream.println();
        centreLine("Department of Computer Science", 60);
        centreLine("University of Auckland", 60);
        centreLine("alexei@cs.auckland.ac.nz", 60);
        progressStream.println();
        centreLine("Institute of Evolutionary Biology", 60);
        centreLine("University of Edinburgh", 60);
        centreLine("a.rambaut@ed.ac.uk", 60);
        progressStream.println();
        progressStream.println();
    }

    private static double[] parseVariableLengthDoubleArray(String inString) throws Arguments.ArgumentException {

        List<Double> returnList = new ArrayList<Double>();
        StringTokenizer st = new StringTokenizer(inString, ",");
        while (st.hasMoreTokens()) {
            try {
                returnList.add(Double.parseDouble(st.nextToken()));
            } catch (NumberFormatException e) {
                throw new Arguments.ArgumentException();
            }

        }

        if (returnList.size() > 0) {
            double[] doubleArray = new double[returnList.size()];
            for (int i = 0; i < doubleArray.length; i++)
                doubleArray[i] = returnList.get(i);

            return doubleArray;
        }
        return null;
    }

    private static String[] parseVariableLengthStringArray(String inString) {

        List<String> returnList = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(inString, ",");
        while (st.hasMoreTokens()) {
            returnList.add(st.nextToken());
        }

        if (returnList.size() > 0) {
            String[] stringArray = new String[returnList.size()];
            stringArray = returnList.toArray(stringArray);
            return stringArray;
        }
        return null;
    }

    private static double[] parseFileWithArray(String file) {
        List<Double> returnList = new ArrayList<Double>();
        try {
            BufferedReader readerTimes = new BufferedReader(new FileReader(file));
            String line = readerTimes.readLine();
            while (line != null && !line.equals("")) {
                returnList.add(Double.valueOf(line));
                line = readerTimes.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error reading " + file);
            System.exit(1);
        }

        if (returnList.size() > 0) {
            double[] doubleArray = new double[returnList.size()];
            for (int i = 0; i < doubleArray.length; i++)
                doubleArray[i] = returnList.get(i);

            //System.out.println(doubleArray.length);
            return doubleArray;
        }
        return null;
    }

    public static String getKMLColor(double value, double[] minMaxMedian, String startColor, String endColor) {

        startColor = startColor.toLowerCase();
        String startBlue = startColor.substring(0, 2);
        String startGreen = startColor.substring(2, 4);
        String startRed = startColor.substring(4, 6);

        endColor = endColor.toLowerCase();
        String endBlue = endColor.substring(0, 2);
        String endGreen = endColor.substring(2, 4);
        String endRed = endColor.substring(4, 6);

        double proportion = (value - minMaxMedian[0]) / (minMaxMedian[1] - minMaxMedian[0]);

        // generate an array with hexadecimal code for each RGB entry number
        String[] colorTable = new String[256];

        int colorTableCounter = 0;

        for (int a = 0; a < 10; a++) {

            for (int b = 0; b < 10; b++) {

                colorTable[colorTableCounter] = a + "" + b;
                colorTableCounter++;
            }

            for (int c = (int) ('a'); c < 6 + (int) ('a'); c++) {
                colorTable[colorTableCounter] = a + "" + (char) c;
                colorTableCounter++;
            }

        }
        for (int d = (int) ('a'); d < 6 + (int) ('a'); d++) {

            for (int e = 0; e < 10; e++) {

                colorTable[colorTableCounter] = (char) d + "" + e;
                colorTableCounter++;
            }

            for (int f = (int) ('a'); f < 6 + (int) ('a'); f++) {
                colorTable[colorTableCounter] = (char) d + "" + (char) f;
                colorTableCounter++;
            }

        }


        int startBlueInt = 0;
        int startGreenInt = 0;
        int startRedInt = 0;

        int endBlueInt = 0;
        int endGreenInt = 0;
        int endRedInt = 0;

        for (int i = 0; i < colorTable.length; i++) {

            if (colorTable[i].equals(startBlue)) {
                startBlueInt = i;
            }
            if (colorTable[i].equals(startGreen)) {
                startGreenInt = i;
            }
            if (colorTable[i].equals(startRed)) {
                startRedInt = i;
            }
            if (colorTable[i].equals(endBlue)) {
                endBlueInt = i;
            }
            if (colorTable[i].equals(endGreen)) {
                endGreenInt = i;
            }
            if (colorTable[i].equals(endRed)) {
                endRedInt = i;
            }

        }

        int blueInt = startBlueInt + (int) Math.round((endBlueInt - startBlueInt) * proportion);
        int greenInt = startGreenInt + (int) Math.round((endGreenInt - startGreenInt) * proportion);
        int redInt = startRedInt + (int) Math.round((endRedInt - startRedInt) * proportion);

        String blue = null;
        String green = null;
        String red = null;

        for (int j = 0; j < colorTable.length; j++) {

            if (j == blueInt) {
                blue = colorTable[j];
            }
            if (j == greenInt) {
                green = colorTable[j];
            }
            if (j == redInt) {
                red = colorTable[j];
            }

        }

        return blue + green + red;
    }

    private static double[] getHPDInterval(double proportion, double[] array, int[] indices) {

        double returnArray[] = new double[2];
        double minRange = Double.MAX_VALUE;
        int hpdIndex = 0;

        int diff = (int) Math.round(proportion * (double) array.length);
        for (int i = 0; i <= (array.length - diff); i++) {
            double minValue = array[indices[i]];
            double maxValue = array[indices[i + diff - 1]];
            double range = Math.abs(maxValue - minValue);
            if (range < minRange) {
                minRange = range;
                hpdIndex = i;
            }
        }
        returnArray[0] = array[indices[hpdIndex]];
        returnArray[1] = array[indices[hpdIndex + diff - 1]];
        return returnArray;
    }

    private static void print2DArray(double[][] array, String name) {
        try {
            PrintWriter outFile = new PrintWriter(new FileWriter(name), true);

            for (double[] anArray : array) {
                for (int j = 0; j < array[0].length; j++) {
                    outFile.print(anArray[j] + "\t");
                }
                outFile.println("");
            }
            outFile.close();

        } catch (IOException io) {
            System.err.print("Error writing to file: " + name);
        }
    }

    private static void print2DTransposedArray(double[][] array, String name) {
        try {
            PrintWriter outFile = new PrintWriter(new FileWriter(name), true);

            for (int i = 0; i < array[0].length; i++) {
                for (double[] anArray : array) {
                    outFile.print(anArray[i] + "\t");
                }
                outFile.println("");
            }
            outFile.close();

        } catch (IOException io) {
            System.err.print("Error writing to file: " + name);
        }
    }

    private static double[] meanColNoNaN(double[][] x) {
        double[] returnArray = new double[x[0].length];

        for (int i = 0; i < x[0].length; i++) {

            double m = 0.0;
            int lenNoZero = 0;


            for (int j = 0; j < x.length; j++) {

                if (!(((Double) x[j][i]).isNaN())) {
                    m += x[j][i];
                    lenNoZero += 1;
                }
            }
            returnArray[i] = m / (double) lenNoZero;

        }
        return returnArray;
    }

    private static double[][] getArrayHPDintervals(double[][] array) {

        double[][] returnArray = new double[array[0].length][2];

        for (int col = 0; col < array[0].length; col++) {

            int counter = 0;

            for (int row = 0; row < array.length; row++) {

                if (!(((Double) array[row][col]).isNaN())) {
                    counter += 1;
                }
            }

            if (counter > 0) {
                double[] columnNoNaNArray = new double[counter];

                int index = 0;
                for (int row = 0; row < array.length; row++) {

                    if (!(((Double) array[row][col]).isNaN())) {
                        columnNoNaNArray[index] = array[row][col];
                        index += 1;
                    }
                }
                int[] indices = new int[counter];
                HeapSort.sort(columnNoNaNArray, indices);
                double hpdBinInterval[] = getHPDInterval(0.95, columnNoNaNArray, indices);

                returnArray[col][0] = hpdBinInterval[0];
                returnArray[col][1] = hpdBinInterval[1];
            } else {
                returnArray[col][0] = Double.NaN;
                returnArray[col][1] = Double.NaN;
            }

        }

        return returnArray;
    }

    private static Set getTargetSet(String x) {
        Set targetSet = null;
        targetSet = new HashSet();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(x));
            try {
                String line = reader.readLine().trim();
                while (line != null && !line.equals("")) {
                    targetSet.add(line);
                    line = reader.readLine();
                    if (line != null) line = line.trim();
                }
            }
            catch (IOException io) {
                System.err.println("Error reading " + x);
            }
        }
        catch (FileNotFoundException a) {
            System.err.println("Error finding " + x);
        }
        return targetSet;
    }

    public static void main(String[] args) throws IOException {

        String inputFileName = null;
        String outputFileName = null;
        String[] traitNames = null;
        double[] sliceHeights = null;
        OutputFormat outputFormat = OutputFormat.KML;
        boolean impute = false;
        boolean trueNoise = true;
        boolean summaryOnly = false;
        ContourMode contourMode = ContourMode.SNYDER;
        Normalization normalize = Normalization.LENGTH;
        int burnin = -1;
        int skipEvery = 1;
        double mrsd = 0;
        double hpdValue = 0.80;
        String outputFileSDR = null;
        boolean getSDR = false;
        String progress = null;
        boolean branchNormalization = false;
        BranchSet set = BranchSet.ALL;
        Set backboneTaxa = null;


//        if (args.length == 0) {
//          // TODO Make flash GUI
//        }

        printTitle();

        Arguments arguments = new Arguments(
                new Arguments.Option[]{
                        new Arguments.IntegerOption(BURNIN, "the number of states to be considered as 'burn-in' [default = 0]"),
                        new Arguments.IntegerOption(SKIP, "skip every i'th tree [default = 0]"),
                        new Arguments.StringOption(TRAIT, "trait_name", "specifies an attribute-list to use to create a density map [default = location.rate]"),
                        new Arguments.StringOption(SLICE_TIMES, "slice_times", "specifies a slice time-list [default=none]"),
                        new Arguments.StringOption(SLICE_HEIGHTS, "slice_heights", "specifies a slice height-list [default=none]"),
                        new Arguments.StringOption(SLICE_FILE_HEIGHTS, "heights_file", "specifies a file with a slice heights-list, is overwritten by command-line specification of slice heights [default=none]"),
                        new Arguments.StringOption(SLICE_FILE_TIMES, "Times_file", "specifies a file with a slice Times-list, is overwritten by command-line specification of slice times [default=none]"),
                        new Arguments.IntegerOption(SLICE_COUNT, "the number of time slices to use [default=0]"),
                        new Arguments.RealOption(START_TIME, "the time of the earliest slice [default=0]"),
                        new Arguments.RealOption(MRSD, "specifies the most recent sampling data in fractional years to rescale time [default=0]"),
                        new Arguments.Option(HELP, "option to print this message"),
                        new Arguments.StringOption(NOISE, falseTrue, false,
                                "add true noise [default = true])"),
                        new Arguments.StringOption(IMPUTE, falseTrue, false,
                                "impute trait at time-slice [default = false]"),
                        new Arguments.StringOption(SUMMARY, falseTrue, false,
                                "compute summary statistics [default = true]"),
                        new Arguments.StringOption(FORMAT, enumNamesToStringArray(OutputFormat.values()), false,
                                "summary output format [default = KML]"),
                        new Arguments.IntegerOption(HPD, "mass (1 - 99%) to include in HPD regions [default = 80]"),
                        new Arguments.StringOption(MODE, enumNamesToStringArray(ContourMode.values()), false,
                                "contouring model [default = snyder]"),
                        new Arguments.StringOption(NORMALIZATION, enumNamesToStringArray(Normalization.values()), false,
                                "tree normalization [default = length"),
                        new Arguments.StringOption(SDR, "sliceDispersalRate", "specifies output file name for dispersal rates for each slice (from previous sliceTime[or root of the trees] up to current sliceTime"),
                        new Arguments.StringOption(PROGRESS, "progress report", "reports slice progress and checks the bivariate contour HPD regions by calculating what fraction of points the polygons for a given slice contain  [default = false]"),
                        new Arguments.StringOption(BRANCH_NORMALIZE, falseTrue, false,
                                "devide a branch trait by branch length (can be useful for 'rewards' [default = false]"),
                        new Arguments.StringOption(BRANCHSET, TimeSlicer.enumNamesToStringArray(BranchSet.values()), false,
                                "branch set [default = all]"),
                        new Arguments.StringOption(BACKBONETAXA, "Backbone taxa file", "specifies a file with taxa that define the backbone"),

                });

        try {
            arguments.parseArguments(args);
        } catch (Arguments.ArgumentException ae) {
            progressStream.println(ae);
            printUsage(arguments);
            System.exit(1);
        }

        if (arguments.hasOption(HELP)) {
            printUsage(arguments);
            System.exit(0);
        }

        try { // Make sense of arguments

            String sliceHeightsFileString = arguments.getStringOption(SLICE_FILE_HEIGHTS);
            if (sliceHeightsFileString != null) {
                sliceHeights = parseFileWithArray(sliceHeightsFileString);
            }

            if (arguments.hasOption(MRSD)) {
                mrsd = arguments.getRealOption(MRSD);
            }

            String sliceTimesFileString = arguments.getStringOption(SLICE_FILE_TIMES);
            if (sliceTimesFileString != null) {
                //System.out.println(sliceTimesFileString);
                double[] sliceTimes = parseFileWithArray(sliceTimesFileString);
                sliceHeights =  new double[sliceTimes.length];
                for (int i = 0; i < sliceTimes.length; i++) {
                    if (mrsd == 0) {
                        sliceHeights[i] = sliceTimes[i];
                    } else {
                        //System.out.println((mrsd - sliceTimes[i]));
                        sliceHeights[i] = mrsd - sliceTimes[i];

                    }
                }
            }

            if (arguments.hasOption(BURNIN)) {
                burnin = arguments.getIntegerOption(BURNIN);
                System.err.println("Ignoring a burnin of " + burnin + " trees.");
            }

            if (arguments.hasOption(SKIP)) {
                skipEvery = arguments.getIntegerOption(SKIP);
                System.err.println("Skipping every " + skipEvery + " trees.");
            }
            if (skipEvery < 1) {
                skipEvery = 1;
            }

            if (arguments.hasOption(HPD)) {
                int intValue = arguments.getIntegerOption(HPD);
                if (intValue < 1 || intValue > 99) {
                    progressStream.println("HPD Region mass falls outside of 1 - 99% range.");
                    System.exit(-1);
                }
                hpdValue = intValue / 100.0;
            }

            String traitString = arguments.getStringOption(TRAIT);
            if (traitString != null) {
                traitNames = parseVariableLengthStringArray(traitString);
//employed to get dispersal rates across the whole tree
//                for (int y = 0; y < traitNames.length; y++) {
//                    if (traitNames[y].equals(LOCATIONTRAIT)){
//                       containsLocation =  true;
//                    }
//                }
            }

            if (traitNames == null) {
                traitNames = new String[1];
                traitNames[0] = "location.rate";
            }

            String sliceTimeString = arguments.getStringOption(SLICE_TIMES);
            if (sliceTimeString != null) {
                double[] sliceTimes = parseVariableLengthDoubleArray(sliceTimeString);
                sliceHeights = new double[sliceTimes.length];
                for (int i = 0; i < sliceTimes.length; i++) {
                    if (mrsd == 0) {
                        sliceHeights[i] = sliceTimes[i];
                    } else {
                        sliceHeights[i] = mrsd - sliceTimes[i];
                    }
                }
            }

            String sliceHeightString = arguments.getStringOption(SLICE_HEIGHTS);
            if (sliceHeightString != null) {
                if (sliceTimeString != null) {
                    progressStream.println("Either sliceTimes, sliceHeights, timesFile or sliceCount" +
                            "nt.");
                    System.exit(-1);
                }
                sliceHeights = parseVariableLengthDoubleArray(sliceHeightString);
            }

            if (arguments.hasOption(SLICE_COUNT)) {
                int sliceCount = arguments.getIntegerOption(SLICE_COUNT);
                double startTime = arguments.getRealOption(START_TIME);
                double delta;
                if (mrsd != 0) {
                    delta = (mrsd - startTime) / (sliceCount - 1);
                } else {
                    delta = startTime / (sliceCount - 1);
                }
                sliceHeights = new double[sliceCount];
//                double height = mrsd - startTime;
//                for (int i = 0; i < sliceCount; i++) {
//                   sliceHeights[i] = height;
//                   height -= delta;
//                }
                double height = 0;
                for (int i = 0; i < sliceCount; i++) {
                    sliceHeights[i] = height;
                    height += delta;
                }
            }

            String imputeString = arguments.getStringOption(IMPUTE);
            if (imputeString != null && imputeString.compareToIgnoreCase("true") == 0)
                impute = true;

            String branchNormString = arguments.getStringOption(BRANCH_NORMALIZE);
            if (branchNormString != null && branchNormString.compareToIgnoreCase("true") == 0)
                branchNormalization = true;

            String noiseString = arguments.getStringOption(NOISE);
            if (noiseString != null && noiseString.compareToIgnoreCase("false") == 0)
                trueNoise = false;

            String summaryString = arguments.getStringOption(SUMMARY);
            if (summaryString != null && summaryString.compareToIgnoreCase("true") == 0)
                summaryOnly = true;

            String modeString = arguments.getStringOption(MODE);
            if (modeString != null) {
                contourMode = ContourMode.valueOf(modeString.toUpperCase());
                if (contourMode == ContourMode.R && !ContourWithR.processWithR)
                    contourMode = ContourMode.SNYDER;
            }

            String normalizeString = arguments.getStringOption(NORMALIZATION);
            if (normalizeString != null) {
                normalize = Normalization.valueOf(normalizeString.toUpperCase());
            }

            String summaryFormat = arguments.getStringOption(FORMAT);
            if (summaryFormat != null) {
                outputFormat = OutputFormat.valueOf(summaryFormat.toUpperCase());
            }

            String sdrString = arguments.getStringOption(SDR);
            if (sdrString != null) {
                outputFileSDR = sdrString;
                getSDR = true;
            }

            String progressString = arguments.getStringOption(PROGRESS);
            if (progressString != null) {
                progress = progressString;
            }

            String branch = arguments.getStringOption(BRANCHSET);
            if (branch != null) {
                set = BranchSet.valueOf(branch.toUpperCase());
                System.out.println("Using the branch set: " + set.name());
            }
            if (set == set.BACKBONE) {
                if (arguments.hasOption(BACKBONETAXA)) {
                    backboneTaxa = getTargetSet(arguments.getStringOption(BACKBONETAXA));
                } else {
                    System.err.println("you want to summarize the backbone, but have no taxa to define it??");
                }
            }


        } catch (Arguments.ArgumentException e) {
            progressStream.println(e);
            printUsage(arguments);
            System.exit(-1);
        }


        final String[] args2 = arguments.getLeftoverArguments();

        switch (args2.length) {
            case 0:
                printUsage(arguments);
                System.exit(1);
            case 2:
                outputFileName = args2[1];
                // fall to
            case 1:
                inputFileName = args2[0];
                break;
            default: {
                System.err.println("Unknown option: " + args2[2]);
                System.err.println();
                printUsage(arguments);
                System.exit(1);
            }
        }

        TimeSlicer timeSlicer = new TimeSlicer(inputFileName, burnin, skipEvery, traitNames, sliceHeights, impute,
                trueNoise, mrsd, contourMode, normalize, getSDR, progress, branchNormalization, set, backboneTaxa);
        timeSlicer.output(outputFileName, summaryOnly, outputFormat, hpdValue, outputFileSDR);

        System.exit(0);
    }
}
