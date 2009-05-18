package dr.geo;

import dr.inference.distribution.MultivariateDistributionLikelihood;
import dr.inference.model.Likelihood;
import dr.inference.model.Parameter;
import dr.math.distributions.MultivariateDistribution;
import dr.xml.*;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.awt.geom.Point2D;

/**
 * @author Marc A. Suchard
 * @author Philippe Lemey
 * @author Alexei J. Drummond
 */

public class GeoSpatialDistribution implements MultivariateDistribution {

    public static final String FLAT_SPATIAL_DISTRIBUTION = "flatGeoSpatialPrior";
    public static final String DATA = "data";
    public static final String TYPE = "geoSpatial";
    public static final String NODE_LABEL = "taxon";
    public static final String KML_FILE = "kmlFileName";
    public static final String INSIDE = "inside";

    public static final int dimPoint = 2; // Assumes 2D points only

    public GeoSpatialDistribution(Polygon2D region) {
        this.region = region;
    }

    public GeoSpatialDistribution(String label, Polygon2D region, boolean inside) {
        this.label = label;
        this.region = region;
        this.inside = !inside;
    }

    public double logPdf(double[] x) {
        final boolean contains = region.containsPoint2D(new Point2D.Double(x[0],x[1]));
        if (inside ^ contains)
            return 0;
        return Double.NEGATIVE_INFINITY;
    }

    public double[][] getScaleMatrix() {
        return null;
    }

    public double[] getMean() {
        return null;
    }

    public String getType() {
        return TYPE;
    }

    public String getLabel() { return label; }

    public Polygon2D getRegion() { return region; }

    protected Polygon2D region;
    protected String label = null;
    private boolean inside = true;


    public static XMLObjectParser FLAT_GEOSPATIAL_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return FLAT_SPATIAL_DISTRIBUTION;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            String label = xo.getAttribute(NODE_LABEL,"");

            boolean inside = xo.getAttribute(INSIDE,true);
            boolean readFromFile = false;

            List<GeoSpatialDistribution> geoSpatialDistributions = new ArrayList<GeoSpatialDistribution>();

            if (xo.hasAttribute(KML_FILE)) {
                // read file
                String kmlFileName = xo.getStringAttribute(KML_FILE);
                List<Polygon2D> polygons = Polygon2D.readKMLFile(kmlFileName);
                for(Polygon2D region : polygons)
                    geoSpatialDistributions.add(new GeoSpatialDistribution(label,region,inside));
                readFromFile = true;
            } else {

              for(int i=0; i<xo.getChildCount(); i++) {
                    if (xo.getChild(i) instanceof Polygon2D) {
                        Polygon2D region = (Polygon2D) xo.getChild(i);
                        geoSpatialDistributions.add(
                                new GeoSpatialDistribution(label,region,inside)
                        );
                    }
                }
            }

            List<Parameter> parameters = new ArrayList<Parameter>();
            XMLObject cxo = (XMLObject) xo.getChild(DATA);
            for (int j = 0; j < cxo.getChildCount(); j++) {
                Parameter spatialParameter = (Parameter) cxo.getChild(j);
                parameters.add(spatialParameter);
            }

            if (geoSpatialDistributions.size() == 1 && !readFromFile) {
                MultivariateDistributionLikelihood likelihood = new MultivariateDistributionLikelihood(geoSpatialDistributions.get(0));
                for(Parameter spatialParameter : parameters) {
                    if (spatialParameter.getDimension() != dimPoint)
                        throw new XMLParseException("Spatial priors currently only work in "+dimPoint+"D");
                    likelihood.addData(spatialParameter);
                }
                return likelihood;
            }

            if (parameters.size() == 1) {
                Parameter parameter = parameters.get(0);
                if (parameter.getDimension() % dimPoint != 0)
                    throw new XMLParseException("Spatial priors currently only work in "+dimPoint+"D");

                Logger.getLogger("dr.geo").info(
                        "\nConstructing a GeoSpatialCollectionModel:\n"+
                        "\tParameter: "+parameter.getId()+"\n"+
                        "\tNumber of regions: "+geoSpatialDistributions.size()+"\n\n");
                
                return new GeoSpatialCollectionModel(xo.getId(),parameter,geoSpatialDistributions);
            }

            throw new XMLParseException("Multiple separate parameters and multiple regions not yet implemented");

        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newStringRule(NODE_LABEL,true),
                AttributeRule.newBooleanRule(INSIDE,true),
                new XORRule(
                    AttributeRule.newStringRule(KML_FILE),
                    new ElementRule(Polygon2D.class,1,Integer.MAX_VALUE)
                ),
                new ElementRule(DATA,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class, 1, Integer.MAX_VALUE)}
                )
        };

        public String getParserDescription() {
            return "Calculates the likelihood of some data under a 2D geospatial distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

}
