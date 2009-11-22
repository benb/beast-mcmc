package dr.app.phylogeography.generator;

import dr.app.phylogeography.structure.*;
import dr.app.phylogeography.structure.Container;
import dr.app.phylogeography.structure.Style;
import dr.app.phylogeography.structure.Polygon;
import org.boehn.kmlframework.kml.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class KMLGenerator implements Generator {

    private final Map<Style, String> styles = new HashMap<Style, String>();

    private TimeLine timeLine;

    public KMLGenerator() {
    }

    public void generate(PrintWriter writer, final TimeLine timeLine, final Collection<Layer> layers) throws IOException {

        this.timeLine = timeLine;

        // We create a new KML Document
        Kml kml = new Kml();

        // We add a document to the kml
        Document document = new Document();
        kml.setFeature(document);

        for (Layer layer : layers) {
            document.addFeature(generateLayer(layer));
        }

        // We generate the kml file
        try {
            kml.createKml(writer);
        } catch (KmlException e) {
            e.printStackTrace();
        }
    }

    private Feature generateLayer(final Layer layer) {
        // We create a Placemark for the Department of Informatics at the university of Oslo
        Placemark placemark = new Placemark(layer.getName());
        placemark.setDescription(layer.getDescription());
//        placemark.setLocation(10.717344, 59.943355);

        Folder folder = new Folder();
        folder.addFeature(generateContent(layer));

        return placemark;
    }

    private Feature generateContent(final Container container) {
        for (Item item : container.getItems()) {
            generateItem(item);
        }
        return null;
    }

    private Feature generateItem(final Item item) {
        if (item instanceof Line) {
            return generateLine((Line)item);
        } else if (item instanceof Polygon) {
            return generatePolygon((Polygon)item);
        } else if (item instanceof Place) {
            return generatePlacemark((Place)item);
        }
        throw new IllegalArgumentException("unknown item type");
    }

    private Feature generatePolygon(final Polygon polygon) {
        Placemark placemark = new Placemark(polygon.getName());
//        placemark.setStyleUrl(getStyleURL(polygon.getStyle()));
//
//        placemark.setTimePrimitive(new TimeSpan(Utils.getKMLDate(date), null));
//
//        double altitude = 0;
//
//        if (altitudeMode != AltitudeModeEnum.clampToGround) {
//            double scaleFactor = plotHeight/tree.getHeight(tree.getRootNode());
//            altitude = (tree.getHeight(node)*scaleFactor);
//        }
//
//        int modality = Utils.getIntegerNodeAttribute(node, latLongName+"_95%HPD_modality");
//
//
//        List<Geometry> geometries = new ArrayList<Geometry>();
//
//        for (int x = 0; x < modality; x++) {
//            Object[] latitudeHPDs = Utils.getArrayNodeAttribute(node, latitudeName+"_95%HPD_"+(x + 1));
//            Object[] longitudeHPDs = Utils.getArrayNodeAttribute(node, longitudeName+"_95%HPD_"+(x + 1));
//
//            geometries.add(generateContourPolygon(latitudeHPDs, longitudeHPDs, altitude, altitudeMode));
//        }
//
////        if (styleName != null) {
////            styles.add(generatePolyStyle(styleName, surfaces.getStartColor()));
////        }
//
//        if (geometries.size() > 1) {
//            placemark.setGeometry(new MultiGeometry(geometries));
//        } else {
//            placemark.setGeometry(geometries.get(0));
//
//        }
        return placemark;
    }

//    private Geometry generateContourPolygon(Object[] latitudeHPDs, Object[] longitudeHPDs, double altitude,
//                                            AltitudeModeEnum altitudeMode) {
//
//        org.boehn.kmlframework.kml.Polygon polygon = new org.boehn.kmlframework.kml.Polygon();
//        polygon.setAltitudeMode(altitudeMode);
//
//        List<org.boehn.kmlframework.kml.Point> coordinates = new ArrayList<org.boehn.kmlframework.kml.Point>();
//        if (altitudeMode == AltitudeModeEnum.clampToGround) {
//            polygon.setTessellate(true);
//            for (int y = 0; y < longitudeHPDs.length; y++) {
//                coordinates.add(new org.boehn.kmlframework.kml.Point(
//                        ((Number)longitudeHPDs[y]).doubleValue(),
//                        ((Number)latitudeHPDs[y]).doubleValue()));
//            }
//        } else {
//            for (int y = 0; y < longitudeHPDs.length; y++) {
//                coordinates.add(new org.boehn.kmlframework.kml.Point(
//                        ((Number)longitudeHPDs[y]).doubleValue(),
//                        ((Number)latitudeHPDs[y]).doubleValue(), altitude));
//            }
//        }
//
//        LinearRing linearRing = new LinearRing();
//        linearRing.setCoordinates(coordinates);
//        polygon.setOuterBoundary(linearRing);
//
//        return polygon;
//    }

//    private Element generateBranch(TreeType treeType, RootedTree tree, Node node,
//                                   String nodeName,
//                                   double startDate, double finishDate,
//                                   double startLatitude, double finishLatitude,
//                                   double startLongitude, double finishLongitude,
//                                   double startAltitude, double finishAltitude,
//                                   double peakPosition, double peakAltitude,
//                                   phylogeochronogrammer.LineStyle startStyle, phylogeochronogrammer.LineStyle finishStyle,
//                                   int divisionCount,
//                                   List<Element> styles) {
//
//        Element element;
//
//        boolean hasDivisionStyles = startStyle != null && finishStyle != null;
//
//        if (divisionCount > 0) {
//            double latDiff = finishLatitude - startLatitude;
//            double latDelta = latDiff / divisionCount;
//
//            double longDiff = finishLongitude - startLongitude;
//            double longDelta = longDiff / divisionCount;
//
//            double altDiff = finishAltitude - startAltitude;
//            double altDelta = altDiff / divisionCount;
//
//            double dateDiff = finishDate - startDate;
//            double dateDelta = dateDiff / divisionCount;
//
//            double lastLatitude = startLatitude;
//            double latitude = startLatitude + latDelta;
//
//            double lastLongitude = startLongitude;
//            double longitude = startLongitude + longDelta;
//
//            // x goes 0 to 1, offset by peak Position
//            double x = -peakPosition;
//            double xDelta = 1.0 / divisionCount;
//            // assume a parabolic curve that peaks at peakAltitude
//            double a = peakAltitude - finishAltitude;
//            if (peakPosition == 0.5) {
//                a *= 4;
//            }
//            double altitude = peakAltitude - (a * (x * x));
//            double lastAltitude = altitude;
//            x += xDelta;
//
//            double date = startDate;
//
//            String styleName = null;
//            if (!hasDivisionStyles && startStyle != null) {
//                styleName = nodeName + "_style";
//
//                styles.add(generateLineStyle(styleName, startStyle.getWidth(), startStyle.getColor()));
//            }
//
//            element = generateContainer("Folder", nodeName, null, (styleName != null ? "#" + styleName : null));
//
//            for (int division = 0; division < divisionCount; division++) {
//                if (ageCutOff == 0.0 || date > ageCutOff) {
//                    String partName = nodeName + "_part" + (division + 1);
//                    styleName = null;
//                    if (hasDivisionStyles) {
//                        styleName = partName + "_style";
//                    }
//                    Element placeMark = generateContainer("Placemark", partName, null, (styleName != null ? "#" + styleName : null));
//
//                    if (hasDivisionStyles) {
//                        // Create a style for this branch segment
//                        double width = startStyle.getWidth();
//                        Color color = getBlendedColor(((float)division) / (divisionCount - 1), startStyle.getColor(), finishStyle.getColor());
//                        styles.add(generateLineStyle(styleName, width, color));
//                    }
//
//                    if (dateDiff > 0.0) {
//                        Element timeSpan = new Element("TimeSpan");
//
//                        //convert height of the branch segment to a real date (based on th date for the most recent sample)
//                        timeSpan.addContent(generateElement("begin", getKMLDate(date)));
//                        placeMark.addContent(timeSpan);
//
//                    }
//
//                    Element lineString = new Element("LineString");
//                    Element coordinates = new Element("coordinates");
//                    if (altDiff > 0.0 || peakAltitude > 0.0) {
//                        lineString.addContent(generateElement("altitudeMode", altitudeMode));
//
//                        altitude = peakAltitude - (a * (x * x));
//
//                        coordinates.addContent("" +lastLongitude + "," +lastLatitude + "," + lastAltitude + "\r");
//                        coordinates.addContent("" +longitude + "," +latitude + "," + altitude + "\r");
//                    } else {
//                        lineString.addContent(generateElement("altitudeMode", "clampToGround"));
//                        lineString.addContent(generateElement("tessellate", true));
//
//                        coordinates.addContent("" +lastLongitude + "," +lastLatitude + "\r");
//                        coordinates.addContent("" +longitude + "," +latitude + "\r");
//                    }
//
//                    lastLatitude = latitude;
//                    latitude += latDelta;
//
//                    lastLongitude = longitude;
//                    longitude += longDelta;
//
//                    lastAltitude = altitude;
//                    x += xDelta;
//
//                    date += dateDelta;
//
//                    lineString.addContent(coordinates);
//
//                    placeMark.addContent(lineString);
//
//                    element.addContent(placeMark);
//                }
//
//            }
//
//        } else {
//            String styleName = null;
//            if (startStyle != null) {
//                styleName = nodeName + "_style";
//
//                double width = startStyle.getWidth();
//                styles.add(generateLineStyle(styleName, width, startStyle.getColor()));
//            }
//
//            element = generateContainer("Placemark", nodeName, null, (styleName != null ? "#" + styleName : null));
//
//            Element lineString = new Element("LineString");
//            lineString.addContent(generateElement("altitudeMode", altitudeMode));
//
//            Element coordinates = new Element("coordinates");
//            if (treeType == TreeType.RECTANGLE_TREE) {
//                coordinates.addContent(""+finishLongitude+","+finishLatitude+","+finishAltitude+"\r");
//                coordinates.addContent(""+finishLongitude+","+finishLatitude+","+startAltitude+"\r");
//                coordinates.addContent(""+startLongitude+","+startLatitude+","+startAltitude+"\r");
//            } else { // TRIANGLE_TREE
//                coordinates.addContent(""+finishLongitude+","+finishLatitude+","+finishAltitude+"\r");
//                coordinates.addContent(""+startLongitude+","+startLatitude+","+startAltitude+"\r");
//
//            }
//            lineString.addContent(coordinates);
//
//            element.addContent(lineString);
//
//        }
//
//        return element;
//    }

    private Feature generateLine(final Line line) {
        Feature feature;

        if (line.getEndStyle() == null || line.getStartStyle().equals(line.getEndStyle())) {
            double startTime = line.getStartTime();
            double endTime = line.getEndTime();
            double duration = line.getDuration();

            double timeRange = endTime - startTime;
            if (timeLine.isInstantaneous() || timeRange == 0.0) {

                feature = generateLineSegment(line.getStartLocation(), line.getEndLocation(), startTime, duration, line.getStartStyle());
            } else {
                Folder folder = new Folder();
//            for (int i = 0; i < divisionCount; i++) {
//            Placemark lineSegment = generateLineSegment(loc1, loc2, time, duration, style);
//            folder.addFeature(lineSegment);
//            }
                feature = folder;
            }

        } else {
            Folder folder = new Folder();
//            for (int i = 0; i < divisionCount; i++) {
//            Placemark lineSegment = generateLineSegment(loc1, loc2, time, duration, style);
//            folder.addFeature(lineSegment);
//            }
            feature = folder;
        }

        feature.setName(line.getName());
        return feature;
    }

    private Placemark generateLineSegment(Coordinates startCoordinates,
                                        Coordinates endCoordinates,
                                        double startTime,
                                        double duration,
                                        Style style) {

        LineString lineString = new LineString();

        List<Point> points = new ArrayList<Point>();
        points.add(generatePoint(startCoordinates));
        points.add(generatePoint(endCoordinates));

        lineString.setCoordinates(points);

        Placemark placemark = new Placemark();
        TimePrimitive timePrimitive = new TimeSpan(
                timeLine.getDate(startTime).toString(),
                (duration > 0.0 ? timeLine.getDate(startTime + duration).toString() : ""));

        placemark.setTimePrimitive(timePrimitive);
        placemark.setStyleUrl(getStyleURL(style));
        placemark.setGeometry(lineString);

        return placemark;

    }

    private String getStyleURL(Style style) {
        String URL = styles.get(style);
        if (URL == null) {
            URL = "style_" + (styles.keySet().size() + 1);
            styles.put(style, URL);
        }
        return URL;
    }

    private Placemark generatePlacemark(final Place place) {
        Placemark placemark = new Placemark();
        placemark.setGeometry(generatePoint(place.getCoordinates()));
        return placemark;
    }

    private Point generatePoint(final Coordinates coordinates) {
        Point point = new Point();
        point.setAltitude(coordinates.getAltitude());
        point.setLongitude(coordinates.getLongitude());
        point.setLatitude(coordinates.getLatitude());
        return point;
    }

    public String toString() {
        return "KML";
    }
}
