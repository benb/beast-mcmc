package dr.evomodel.continuous;

import dr.evolution.continuous.SphericalPolarCoordinates;
import dr.inference.model.Parameter;
import dr.xml.*;
import dr.geo.cartogram.CartogramMapping;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * @author Marc A. Suchard
 */

public class CartogramDiffusionModel extends MultivariateDiffusionModel {

    public static final String DIFFUSION_PROCESS = "carogramDiffusionModel";
    public static final String FILENAME = "cartogramFileName";

    public static final String BOUNDING_BOX = "boundingBox";
    public static final String MIN_X = "minX";
    public static final String MAX_X = "maxX";
    public static final String MIN_Y = "minY";
    public static final String MAX_Y = "maxY";

    public static final String XSIZE = "xGridSize";
    public static final String YSIZE = "yGridSize";

    public static final double LOG2PI = Math.log(2*Math.PI);

    public CartogramDiffusionModel(CartogramMapping mapping, Parameter precision) {
        super();
        this.mapping = mapping;
        this.precision = precision;
        addParameter(precision);
    }

    protected double calculateLogDensity(double[] start, double[] stop, double time) {

        Point2D realStart = new Point2D.Double(start[0],start[1]);
        Point2D realStop  = new Point2D.Double(stop[0], stop[1]);

        Point2D mappedStart = mapping.map(realStart);
        Point2D mappedStop  = mapping.map(realStop);

        if (mappedStart == null || mappedStop == null) // points outside of cartogram bounding box
            return Double.NEGATIVE_INFINITY;

        double distance = mappedStop.distance(mappedStart); // Euclidean distance in mapped space
        double inverseVariance = precision.getParameterValue(0) / time;
        return 0.5 * (Math.log(inverseVariance) - LOG2PI - distance * distance * inverseVariance);
    }

    protected void calculatePrecisionInfo() {
    }


    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

         public String getParserName() {
             return DIFFUSION_PROCESS;
         }

         public Object parseXMLObject(XMLObject xo) throws XMLParseException {

             // Parse bounding box
             XMLObject cxo = (XMLObject) xo.getChild(BOUNDING_BOX);
             double minX = cxo.getAttribute(MIN_X,0.0);
             double maxX = cxo.getAttribute(MAX_X,0.0);
             double minY = cxo.getAttribute(MIN_Y,0.0);
             double maxY = cxo.getAttribute(MAX_Y,0.0);

             if (maxX - minX <= 0 || maxY - minY <= 0)
                throw new XMLParseException("Bounding box must contain volume");

             Rectangle2D boundingBox = new Rectangle2D.Double(minX,minY,maxX-minX,maxY-minY);

             int xGridSize = xo.getAttribute(XSIZE,0);
             int yGridSize = xo.getAttribute(YSIZE,0);

             if (xGridSize <= 1 || yGridSize <= 1)
                throw new XMLParseException("Strictly positive grid sizes required");

             CartogramMapping mapping = new CartogramMapping(xGridSize, yGridSize, boundingBox);
             try {
                 mapping.readCartogramOutput(FILENAME);
             } catch (IOException e) {
                 throw new XMLParseException(e.getMessage());
             }

             Parameter diffusionParam = (Parameter) xo.getChild(Parameter.class);

             return new CartogramDiffusionModel(mapping, diffusionParam);
         }

         //************************************************************************
         // AbstractXMLObjectParser implementation
         //************************************************************************

         public String getParserDescription() {
             return "Describes a bivariate diffusion process using cartogram distances.";
         }

         public XMLSyntaxRule[] getSyntaxRules() {
             return rules;
         }

         private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                 new ElementRule(Parameter.class),
                 AttributeRule.newStringRule(FILENAME),
                 AttributeRule.newIntegerRule(XSIZE),
                 AttributeRule.newIntegerRule(YSIZE),
                 new ElementRule(BOUNDING_BOX, new XMLSyntaxRule[]{
                    AttributeRule.newDoubleRule(MIN_X),
                    AttributeRule.newDoubleRule(MAX_X),
                    AttributeRule.newDoubleRule(MIN_Y),
                    AttributeRule.newDoubleRule(MAX_Y),
                 })
         };

         public Class getReturnType() {
             return MultivariateDiffusionModel.class;
         }
     };

    private Parameter precision;
    private CartogramMapping mapping;

}