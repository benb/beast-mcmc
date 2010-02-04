/*
 * VisualizeKMLBrownianBridge.java
 *
 * Copyright (C) 2002-2010 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.geo;

import dr.math.distributions.MultivariateNormalDistribution;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alexei Drummond
 */
public class VisualizeKMLBrownianBridge extends VisualizeBrownianBridge2D {

    List<Polygon2D> polygons;
    List<Reject> rejects = new ArrayList<Reject>();
    int MAX_DEPTH = 10;
    int MAX_TRIES = 20;
    int TRIALS = 3;

    Point2D brussels = new Point2D.Double(4.35, 50.85);
    Point2D amsterdam = new Point2D.Double(4.89, 52.37);
    Point2D berlin = new Point2D.Double(13.41, 52.52);
    Point2D rome = new Point2D.Double(12.48, 41.9);
    Point2D athens = new Point2D.Double(23.72, 37.98);
    Point2D paris = new Point2D.Double(2.35, 48.86);
    Point2D montepelier = new Point2D.Double(3.88, 43.61);
    Point2D munich = new Point2D.Double(11.58, 48.14);


    Color[] depthColor = new Color[]{Color.red, Color.orange, Color.yellow, Color.green, Color.cyan, Color.blue, Color.magenta};

    public VisualizeKMLBrownianBridge(String kmlFileName) {

        polygons = Polygon2D.readKMLFile(kmlFileName);

        System.out.println("Read " + polygons.size() + " polygons");

        start = new SpaceTime[]{
                new SpaceTime(0, amsterdam),
                new SpaceTime(0, amsterdam),
                new SpaceTime(1, munich),
                new SpaceTime(1, munich),

        };
        end = new SpaceTime[]{
                new SpaceTime(2, montepelier),
                new SpaceTime(1, munich),
                new SpaceTime(2, rome),
                new SpaceTime(2, athens)
        };

        topLeft = new Point2D.Double(-5, 28);
        bottomRight = new Point2D.Double(25, 57);

        System.out.println("Converting polygons to shapes");
        shapes = new ArrayList<Shape>();
        for (Polygon2D p : polygons) {
            shapes.add(getShape(p));
            System.out.print(".");
            System.out.flush();
        }
        System.out.println();

        rejector = new SpaceTimeRejector() {

//            private boolean stop = false;

            public boolean reject(SpaceTime point, int attribute) {
                Point2D p = new Point2D.Double(point.getX(0), point.getX(1));
                for (Shape s : shapes) {
                    if (s.contains(p)) {
                        rejects.add(new Reject(attribute, point));
                        return true;
                    }
                }
                return false;
            }

            // removes all rejects
            public void reset() {
                rejects.clear();
            }

            public List<Reject> getRejects() {
                return rejects;
            }

//            public boolean getStop() {
//                return stop;
//            }
//
//            public void setStop(boolean stop) {
//                this.stop = stop;
//            }

        };

        mnd = new MultivariateNormalDistribution(new double[]{0.0}, new double[][]{{0.15, 0}, {0, 0.15}});

        shapeColor = Color.BLACK;

        for (int i = 0; i < start.length; i++) {
            if (rejector.reject(start[i], 0) || rejector.reject(end[i], 0)) {
                throw new RuntimeException("Start or end in water");
            }
        }

        for (int i = 0; i < depthColor.length; i++) {
            depthColor[i] = new Color(depthColor[i].getRed(), depthColor[i].getGreen(), depthColor[i].getBlue(), 128);
        }
    }

    public int getMaxDepth() {
        return MAX_DEPTH;
    }

    public int getMaxTries() {
        return MAX_TRIES;
    }


    public int getTrials() {
        return TRIALS;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        AffineTransform transform = getFullTransform();
        for (Reject r : rejects) {
            g.setColor(depthColor[(r.getDepth() - 1) % depthColor.length]);
            SpaceTime.paintDot(r.getSpaceTime(), 2, transform, (Graphics2D) g);
        }
        rejector.reset();
    }

    Shape getShape(Polygon2D poly) {
        GeneralPath path = new GeneralPath();

        LinkedList<Point2D> points = poly.point2Ds;
        path.moveTo((float) points.get(0).getX(), (float) points.get(0).getY());

        System.out.println("x=" + points.get(0).getX() + ", y=" + points.get(0).getY());

        for (int i = 1; i < points.size(); i++) {
            path.lineTo((float) points.get(i).getX(), (float) points.get(i).getY());
        }
        path.closePath();
        return path;
    }

    AffineTransform getTranslate() {
        return AffineTransform.getTranslateInstance(-topLeft.getX(), -bottomRight.getY());
    }

    AffineTransform getScale() {
        return AffineTransform.getScaleInstance(scaleX, -scaleY);
    }


    public static void main(String[] args) {

        JFrame frame = new JFrame("Europe");
        frame.getContentPane().add(BorderLayout.CENTER, new VisualizeKMLBrownianBridge(args[0]));
        frame.setSize(900, 900);
        frame.setVisible(true);
    }

}
