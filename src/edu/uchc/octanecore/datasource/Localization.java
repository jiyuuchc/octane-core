//FILE:          SmNode.java
//PROJECT:       Octane
//-----------------------------------------------------------------------------
//
// AUTHOR:       Ji Yu, jyu@uchc.edu, 2/16/08
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES./**
//

package edu.uchc.octanecore.datasource;

import org.apache.commons.math3.util.FastMath;

/**
 * A class contains information of a detected molecule.
 */
public class Localization implements Cloneable {

	/** x position */
	public double x;
	
	/** y position */
	public double y;
	
	/* z position */
	public double z;

	/** The frame number */
	public int frame;  // 1-based
	
	/** The peak value */
	public double intensity;

	/**
	 * Instantiates a new node.
	 *
	 * @param x the x
	 * @param y the y
	 * @param f the frame number
	 */
	public Localization(double x, double y, int f) {
		this(x, y, 0, f, 0.0);
	}

	/**
	 * Instantiates a new node.
	 *
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @param f the frame number
	 */
	public Localization(double x, double y, double z, int f) {
		this(x, y, z, f, 0.0);
	}

	/**
	 * Instantiates a new node.
	 *
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @param f the frame number
	 * @param h the intensity
	 * @param q the residue
	 */
	public Localization(double x, double y, double z, int f, double h) {
		this.x = x;
		this.y = y;
		this.z = z;
		frame = f;
		intensity = h;
	}

	/**
	 * Instantiates a new node.
	 *
	 * @param line Comma separated text data.
	 */
	public Localization(String line) {
		String[] items = line.split(",");
		frame = Integer.parseInt(items[0].trim());
		x = Double.parseDouble(items[1]);
		y = Double.parseDouble(items[2]);

		if (items.length > 3) {
			z = Double.parseDouble(items[3]);
		}

		if (items.length > 4) {
			intensity = (int) Double.parseDouble(items[4]);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Localization clone() {
		try {
			return (Localization) super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Cloning not allowed.");
			return null;
		}
	}
	
	/** 
	 * Convert to comma separated text data.
	 * @return String
	 */
	public String toString() {
		return (frame + ", " + x + ", " + y + ", " + z + ", " + intensity);
	}

	/**
	 * Calculate square distance between two nodes
	 *
	 * @param n another node
	 * @return distance^2
	 */
	public double distance2(Localization n) {
		return (x - n.x)*(x - n.x) + (y - n.y)*(y - n.y) + (z - n.z)*( z - n.z);
	}

	/**
	 * Calculate distance between two nodes.
	 *
	 * @param n another node
	 * @return the distance
	 */
	public double distance(Localization n) {
		return FastMath.sqrt(distance2(n));
	}
}
