package dr.geo;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * @author Alexei Drummond
 */
public class InhomogeneousRandomWalk extends JComponent {

    Lattice lattice;
    double[][] rates;
    int i, j;
    Random random;
    double time;
    int[][] sample;
    double timeInIsland;

    public InhomogeneousRandomWalk(
            Lattice lattice, Location loc, Random random, double[][] rates) {

        this.lattice = lattice;
        this.rates = rates;
        this.i = loc.i;
        this.j = loc.j;
        this.random = random;

        sample = new int[lattice.latticeWidth()][lattice.latticeHeight()];

    }

    public Dimension getPreferredSize() {
        return new Dimension(lattice.latticeWidth(), lattice.latticeHeight());
    }

    public void paintComponent(Graphics g) {

        lattice.paintComponent(g);

        g.setColor(Color.black);

        for (int i = 0; i < lattice.latticeWidth(); i++) {
            for (int j = 0; j < lattice.latticeHeight(); j++) {
                if (sample[i][j] > 0) {
                    if (lattice.getState(i, j) == 0) {
                        g.setColor(Color.black);
                    } else {
                        g.setColor(Color.red);
                    }
                    g.drawRect(i, j, 1, 1);
                }
            }
        }
    }


    public double computeP(Location from, Location to, double t, int n) {


        int count = 0;
        for (int rep = 0; rep < n; rep++) {
            simulate(from, t);
            if (i == to.i && j == to.j) {
                count += 1;
            }
        }
        return (double)count/(double)n;
    }


    public void simulate(Location from, double t) {
        this.i = from.i;
        this.j = from.j;
        time = 0;
        timeInIsland = 0;
        while (time < t) {
            step(t);
        }
    }

    public void simulate(int steps) {
        for (int i = 0; i < steps; i++) {
            step(Double.MAX_VALUE);
        }
    }

    public void step(double toTime) {

        int fromState = lattice.getState(i, j);

        double moveUp = 0.0;
        double moveLeft = 0.0;
        double moveRight = 0.0;
        double moveDown = 0.0;

        if (i > 0) {
            moveLeft = rates[fromState][lattice.getState(i - 1, j)];
        }

        if (i < lattice.latticeWidth() - 1) {
            moveRight = rates[fromState][lattice.getState(i + 1, j)];
        }

        if (j > 0) {
            moveUp = rates[fromState][lattice.getState(i, j - 1)];
        }

        if (j < lattice.latticeHeight() - 1) {
            moveDown = rates[fromState][lattice.getState(i, j + 1)];
        }

        double totalRate =
                moveUp + moveDown + moveLeft + moveRight;


        double V = random.nextDouble();
        double dt = -Math.log(V) / totalRate;
        if (time + dt > toTime) {
            time = toTime;
            return;
        }

        double U = random.nextDouble() * totalRate;

        if (U < moveUp) {
            // move up
            j -= 1;
        } else {
            U -= moveUp;
            if (U < moveRight) {
                // move right
                i += 1;
            } else {
                U -= moveRight;
                if (U < moveDown) {
                    // move down
                    j += 1;
                } else {
                    // move left
                    i -= 1;
                }
            }
        }
        if (fromState == 1) timeInIsland += dt;

        time += dt;
        sample[i][j] += 1;
    }

    public static void main(String[] args) {

        Random random = new Random();

        final int width = 100;
        final int height = 100;

        Lattice lattice = new Lattice(width, height);
        java.util.List<Location> islands = lattice.addIslands(20, random);
        System.out.println("Created " + islands.size() + " islands");

        int growth = 380;

        int attempts = lattice.growIslands(growth, random);
        System.out.println("Took " + attempts + " attempts to grow islands by " + growth);


        lattice.smooth(2);
        System.out.println("Homogeneous P_island=" + (double)lattice.sum() / (double)(width*height));

        double[][] rates = new double[2][2];
        rates[0][0] = 0.25;
        rates[0][1] = 0.25;
        rates[1][0] = 0.25;
        rates[1][1] = 0.25;

        InhomogeneousRandomWalk walk = new InhomogeneousRandomWalk(lattice, new Location(0,0), random, rates);

        for (double t = 5; t < 1000; t *= 2) {

            double p = walk.computeP(new Location(50,50), new Location(40,40), t, 500000);

            System.out.println(t + "\t" + p);

        }


//        for (double a = 1.0; a >= 0.01; a /= 2.0) {
//
//            for (int rep = 0; rep < 3; rep += 1) {
//                Location loc = new Location(random.nextInt(width), random.nextInt(height));
//
//                rates[0][1] = 1/a;
//
//                walk.simulate(100000000);
//
//                //JFrame frame = new JFrame();
//                //frame.getContentPane().add(walk, BorderLayout.CENTER);
//                //frame.pack();
//                //frame.setVisible(true);
//
//                double P_island = walk.timeInIsland / walk.time;
//
//                System.out.println("P_island(" + a + ", " + rep + ")= " + P_island);
//            }
//        }


    }


}
