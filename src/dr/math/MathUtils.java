/*
 * MathUtils.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
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
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.math;

/**
 * Handy utility functions which have some Mathematical relavance.
 *
 * @author Matthew Goode
 * @author Alexei Drummond
 * @author Gerton Lunter
 *
 * @version $Id: MathUtils.java,v 1.13 2006/08/31 14:57:24 rambaut Exp $
 */
public class MathUtils {

	private MathUtils() {}

	/**
	 * A random number generator that is initialized with the clock when this
	 * class is loaded into the JVM. Use this for all random numbers.
	 * Note: This method or getting random numbers in not thread-safe. Since
	 * MersenneTwisterFast is currently (as of 9/01) not synchronized using
	 * this function may cause concurrency issues. Use the static get methods of the
	 * MersenneTwisterFast class for access to a single instance of the class, that
	 * has synchronization.
	 */
	private static final MersenneTwisterFast random = MersenneTwisterFast.DEFAULT_INSTANCE;

    // Chooses one category if a cumulative probability distribution is given
	public static int randomChoice(double[] cf)
	{

		double U = random.nextDouble();

		int s;
		if (U <= cf[0])
		{
			s = 0;
		}
		else
		{
			for (s = 1; s < cf.length; s++)
			{
				if (U <= cf[s] && U > cf[s-1])
				{
					break;
				}
			}
		}

		return s;
	}

	
	/** 
	 * @return a sample according to an unnormalized probability distribution
     * @param pdf array of unnormalized probabilities
	 */
	public static int randomChoicePDF(double[] pdf) {

		double U = random.nextDouble() * getTotal(pdf);
		for (int i=0; i<pdf.length; i++) {
			
			U -= pdf[i];
			if (U < 0.0) {
				return i;
			}

		}
        for (int i = 0; i < pdf.length; i++) {
            System.out.println(i + "\t" + pdf[i]);
        }
        throw new Error("randomChoiceUnnormalized falls through -- negative components in input distribution?");
	}

	
	/**
	 * @return a new double array where all the values sum to 1.
	 * Relative ratios are preserved.
     * @param array to normalize
	 */
	public static double[] getNormalized(double[] array) {
		double[] newArray = new double[array.length];
		double total = getTotal(array);
		for(int i = 0 ; i < array.length ; i++) {
			newArray[i] = array[i]/total;
		}
		return newArray;
	}
		
	
	/**
     * @param array entries to be summed
     * @param start start position
     * @param end the index of the element after the last one to be included
	 * @return the total of a the values in a range of an array
	 */
	public static double getTotal(double[] array, int start, int end) {
		double total = 0.0;
		for(int i = start ; i < end; i++) {
			total+=array[i];
		}
		return total;
	}

	/**
	 * @return the total of the values in an array
     * @param array to sum over
	 */
	public static double getTotal(double[] array) {
		return getTotal(array,0, array.length);

	}

    // ===================== (Synchronized) Static access methods to the private random instance ===========

    /** Access a default instance of this class, access is synchronized */
    public static long getSeed() {
        synchronized(random) {
            return random.getSeed();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static void setSeed(long seed) {
        synchronized(random) {
            random.setSeed(seed);
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static byte nextByte() {
        synchronized(random) {
            return random.nextByte();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static boolean nextBoolean() {
        synchronized(random) {
            return random.nextBoolean();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static void nextBytes(byte[] bs) {
        synchronized(random) {
            random.nextBytes(bs);
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static char nextChar() {
        synchronized(random) {
            return random.nextChar();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static double nextGaussian() {
        synchronized(random) {
            return random.nextGaussian();
        }
    }

    /** Access a default instance of this class, access is synchronized
     * @return a pseudo random double precision floating point number in [01)
     */
    public static double nextDouble() {
        synchronized(random) {
            return random.nextDouble();
        }
    }

    /**
     * @return log of random variable in [0,1]
     */
    public static double randomLogDouble() {
        return Math.log(nextDouble());
    }

    /** Access a default instance of this class, access is synchronized */
    public static float nextFloat() {
        synchronized(random) {
            return random.nextFloat();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static long nextLong() {
        synchronized(random) {
            return random.nextLong();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static short nextShort() {
        synchronized(random) {
            return random.nextShort();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static int nextInt() {
        synchronized(random) {
            return random.nextInt();
        }
    }
    /** Access a default instance of this class, access is synchronized */
    public static int nextInt(int n) {
        synchronized(random) {
            return random.nextInt(n);
        }
    }

    /**
     * Shuffles an array.
     */
    public static void shuffle(int[] array) {
        synchronized(random) {
            random.shuffle(array);
        }
    }
    /**
     * Shuffles an array. Shuffles numberOfShuffles times
     */
    public static void shuffle(int[] array, int numberOfShuffles) {
        synchronized(random) {
            random.shuffle(array, numberOfShuffles);
        }
    }
    /**
     * Returns an array of shuffled indices of length l.
     * @param l length of the array required.
     */
    public static int[] shuffled(int l) {
        synchronized(random) {
            return random.shuffled(l);
        }
    }
}
