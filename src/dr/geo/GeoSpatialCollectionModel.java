package dr.geo;

import dr.inference.model.AbstractModelLikelihood;
import dr.inference.model.Model;
import dr.inference.model.Parameter;

import java.util.List;
import java.awt.geom.Point2D;

/**
 * @author Marc A. Suchard
 *
 * Provides a GeoSpatialDistribution over multiple points in multiple polygon.
 * Uses AbstractModelLikelihood to cache 'contains' to reduce recalculations
 * when only a single point is updated
 */

public class GeoSpatialCollectionModel extends AbstractModelLikelihood {

    public GeoSpatialCollectionModel(String name, Parameter points, List<GeoSpatialDistribution> geoSpatialDistributions) {
        
        super(name);
        this.points = points;
        this.geoSpatialDistributions = geoSpatialDistributions;

        dim = points.getDimension() / GeoSpatialDistribution.dimPoint;
        cachedPointLogLikelihood = new double[dim];
        storedCachedPointLogLikelihood = new double[dim];
        validPointLogLikelihood = new boolean[dim];
        storedValidPointLogLikelihood = new boolean[dim];
        likelihoodKnown = false;

        addParameter(points);
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        // No submodels; do nothing
    }

    protected void handleParameterChangedEvent(Parameter parameter, int index, Parameter.ChangeType type) {
        // Mark appropriate dim as invalid
        validPointLogLikelihood[index / GeoSpatialDistribution.dimPoint] = false;
        likelihoodKnown = false;
    }

    protected void storeState() {

        System.arraycopy(cachedPointLogLikelihood,0,storedCachedPointLogLikelihood,0,dim);
        System.arraycopy(validPointLogLikelihood,0,storedValidPointLogLikelihood,0,dim);

        storedLikelihoodKnown = likelihoodKnown;
        storedLogLikelihood = logLikelihood;
    }

    protected void restoreState() {

        double[] tmp1 = cachedPointLogLikelihood;
        cachedPointLogLikelihood = storedCachedPointLogLikelihood;
        storedCachedPointLogLikelihood = tmp1;

        boolean[] tmp2 = validPointLogLikelihood;
        validPointLogLikelihood = storedValidPointLogLikelihood;
        storedValidPointLogLikelihood = tmp2;

        likelihoodKnown = storedLikelihoodKnown;
        logLikelihood = storedLogLikelihood;
    }

    protected void acceptState() {

    }

    public Model getModel() {
        return this;
    }

    public double getLogLikelihood() {

        if (likelihoodKnown)
            return logLikelihood;

        logLikelihood = 0.0;
        final double[] point = new double[GeoSpatialDistribution.dimPoint];

        for(int i=0; i<dim; i++) {
            if (!validPointLogLikelihood[i]) {
                final int offset = i*GeoSpatialDistribution.dimPoint;
                for(int j=0; j<GeoSpatialDistribution.dimPoint; j++)
                    point[j] = points.getParameterValue(offset+j);

                double pointLogLikelihood = 0;
                for(GeoSpatialDistribution distribution : geoSpatialDistributions) {
                    pointLogLikelihood += distribution.logPdf(point);
                    if (pointLogLikelihood == Double.NEGATIVE_INFINITY)
                        break; // No need to finish
                }
                cachedPointLogLikelihood[i] = pointLogLikelihood;
                validPointLogLikelihood[i] = true;
            }
            logLikelihood += cachedPointLogLikelihood[i];
            if (logLikelihood == Double.NEGATIVE_INFINITY)
                break; // No need to finish
        }
        likelihoodKnown = true;
        return logLikelihood;        
    }

    public void makeDirty() {
        likelihoodKnown = false;
        for(int i=0; i<dim; i++)
            validPointLogLikelihood[i] = false;
    }

    private Parameter points;
    private List<GeoSpatialDistribution> geoSpatialDistributions;
    private int dim;

    private double[] cachedPointLogLikelihood;
    private double[] storedCachedPointLogLikelihood;

    private boolean likelihoodKnown;
    private boolean storedLikelihoodKnown;

    private double logLikelihood;
    private double storedLogLikelihood;

    private boolean[] validPointLogLikelihood;
    private boolean[] storedValidPointLogLikelihood;  
}
