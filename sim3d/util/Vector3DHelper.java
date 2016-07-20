package sim3d.util;

import sim.util.Double3D;
import sim3d.Settings;

/**
 * A singleton with helper functions for dealing with vectors (Double3Ds)
 * 
 * @author Simon Jarrett (simonjjarrett@gmail.com)
 */
public class Vector3DHelper {

	
	/**
	 * Generates a uniform random point on the unit sphere
	 * 
	 * @return A unit vector for a random direction
	 */
	public static Double3D getRandomDirection() {
		// TODO which is faster? this one has less calcs, but one more random
		// call - I suspect this is slower
		// see http://mathworld.wolfram.com/SpherePointPicking.html at the
		// bottom return new Double3D(Math.abs(Options.RNG.nextGaussian()),
		// Options.RNG.nextGaussian(), Options.RNG.nextGaussian()).normalize();
		// see getRandomDirectionInCone()
		double z = Settings.RNG.nextDouble() * 2 - 1;
		double phi = Settings.RNG.nextDouble() * 2 * Math.PI;

		return new Double3D(Math.sqrt(1 - z * z) * Math.cos(phi), Math.sqrt(1
				- z * z)
				* Math.sin(phi), z);
	}

	/**
	 * Generates a uniform random point on the surface of the unit sphere within
	 * a specified cone
	 * 
	 * @param d3Direction
	 *            The unit vector at the centre of the cone
	 * @param dConeAngle
	 *            Half of the total angle of the cone
	 * @return A unit vector for a random direction in the cone
	 */
	public static Double3D getRandomDirectionInCone(Double3D d3Direction,
			double dConeAngle) {
		// see http://math.stackexchange.com/a/205589

		// Basically the surface area of a sphere has a 1-1 relationship with
		// the curved surface of a
		// cylinder. A vertical section of either with the same height has the
		// same surface area
		// So what we do is pick a random point on a cylinder with height
		// between
		// cos dConeAngle and 1 and convert it to the spherical equivalent

		// Height on surface of cylinder
		double z = (1 - Math.cos(dConeAngle)) * Settings.RNG.nextDouble()
				+ Math.cos(dConeAngle);

		// Angle on surface of cylinder
		double phi = Settings.RNG.nextDouble() * 2 * Math.PI;

		// Change from cylindrical coordinates to spherical
		return rotateUsingVector(
				new Double3D(Math.sqrt(1 - z * z) * Math.cos(phi), Math.sqrt(1
						- z * z)
						* Math.sin(phi), z), d3Direction);
	}

	/**
	 * Generates points approximately equidistant on a sphere using the
	 * fibonacci sequence.
	 * 
	 * @param iNumPoints
	 *            Desired number of points
	 * @return An array of equidistant points on a unit sphere.
	 * @see {@link http://www.openprocessing.org/sketch/41142}
	 */
	public static Double3D[] getEqDistPointsOnSphere(int iNumPoints) {
		Double3D[] ad3Return = new Double3D[iNumPoints];
		double phi = (Math.sqrt(5) + 1) / 2 - 1;
		double ga = phi;// * 2 * Math.PI;
		for (int i = 0; i < iNumPoints; i++) {
			double dLongitude = ga * i;
			double dLatitude = Math.asin(-1 + 2.0 * i / iNumPoints);

			double x = Math.cos(dLatitude) * Math.cos(dLongitude);
			double y = Math.cos(dLatitude) * Math.sin(dLongitude);
			double z = Math.sin(dLatitude);

			ad3Return[i] = new Double3D(x, y, z).normalize();
		}

		return ad3Return;
	}

	/**
	 * Generates a random point on the surface of the unit sphere within a
	 * specified cone biased towards the centre of the cone
	 * 
	 * @param d3Direction
	 *            The unit vector at the centre of the cone
	 * @param dConeAngle
	 *            The total angle of the cone
	 * @return A unit vector for a random direction in the cone
	 */
	public static Double3D getBiasedRandomDirectionInCone(Double3D d3Direction,
			double dConeAngle) {
		// see getRandomDirectionInCone

		// Height on surface of cylinder
		double z = (1 - Math.cos(dConeAngle))
				* Math.cbrt(Settings.RNG.nextDouble()) + Math.cos(dConeAngle);

		// Angle on surface of cylinder
		double phi = Settings.RNG.nextDouble() * 2 * Math.PI;

		// Change from cylindrical coordinates to spherical
		return rotateUsingVector(
				new Double3D(Math.sqrt(1 - z * z) * Math.cos(phi), Math.sqrt(1
						- z * z)
						* Math.sin(phi), z), d3Direction);
	}

	/**
	 * Given a unit directional vector, this generates a rotation such that the
	 * vector (0,0,1) rotates to the aforementioned directional vector. This
	 * rotation is then applied to a given point.
	 * 
	 * @param d3Point
	 *            The vector to apply the rotation to
	 * @param d3Direction
	 *            The end position of the rotation on the vector (0, 0, 1)
	 * @return The rotated vector
	 */
	public static Double3D rotateUsingVector(Double3D d3Point,
			Double3D d3Direction) {
		if (d3Direction.z == 1) {
			// we're in the right direction
			return d3Point;
		} else if (d3Direction.z == -1) {
			// We're facing the wrong way! Just negate the z coordinate
			return new Double3D(d3Point.x, d3Point.y, -d3Point.z);
		}

		// math.stackexchange.com/questions/180418/calculate-rotation-matrix-to-align-vector-b-in-3d/476311#476311

		Double3D d3Cross = crossProduct(new Double3D(0, 0, 1), d3Direction);
		double a = (1 - dotProduct(new Double3D(0, 0, 1), d3Direction))
				/ (d3Cross.length() * d3Cross.length());

		double x = d3Point.x
				* (1 + a * (-d3Cross.z * d3Cross.z - d3Cross.y * d3Cross.y))
				+ d3Point.y * (-d3Cross.z + a * d3Cross.x * d3Cross.y)
				+ d3Point.z * (d3Cross.y + a * d3Cross.x * d3Cross.z);
		double y = d3Point.x * (d3Cross.z + a * d3Cross.x * d3Cross.y)
				+ d3Point.y
				* (1 + a * (-d3Cross.z * d3Cross.z - d3Cross.x * d3Cross.x))
				+ d3Point.z * (-d3Cross.x + a * d3Cross.y * d3Cross.z);
		double z = d3Point.x * (-d3Cross.y + a * d3Cross.x * d3Cross.z)
				+ d3Point.y * (d3Cross.x + a * d3Cross.y * d3Cross.z)
				+ d3Point.z
				* (1 + a * (-d3Cross.x * d3Cross.x - d3Cross.y * d3Cross.y));

		return new Double3D(x, y, z);
	}

	/**
	 * Given 2 unit directional vectors, this generates a rotation that rotates
	 * from one direction to the other. This rotation is then applied to a given
	 * point.
	 * 
	 * @param d3Point
	 *            The vector to apply the rotation to
	 * @param d3Direction
	 *            The start position of the rotation
	 * @param d3NewDirection
	 *            The end position of the rotation
	 * @return The rotated vector TODO can we specify an angle here?
	 */
	public static Double3D rotateVectorToVector(Double3D d3Point,
			Double3D d3OldDirection, Double3D d3NewDirection) {
		if (dotProduct(d3NewDirection, d3OldDirection) == 1) {
			// we're in the right direction
			return d3Point;
		} else if (dotProduct(d3NewDirection, d3OldDirection) == -1) {
			// We're facing the wrong way! Just negate the coordinates
			return new Double3D(-d3Point.x, -d3Point.y, -d3Point.z);
		}

		// math.stackexchange.com/questions/180418/calculate-rotation-matrix-to-align-vector-b-in-3d/476311#476311

		Double3D d3Cross = crossProduct(d3OldDirection, d3NewDirection);
		double a = (1 - dotProduct(d3OldDirection, d3NewDirection))
				/ (d3Cross.length() * d3Cross.length());

		double x = d3Point.x
				* (1 + a * (-d3Cross.z * d3Cross.z - d3Cross.y * d3Cross.y))
				+ d3Point.y * (-d3Cross.z + a * d3Cross.x * d3Cross.y)
				+ d3Point.z * (d3Cross.y + a * d3Cross.x * d3Cross.z);
		double y = d3Point.x * (d3Cross.z + a * d3Cross.x * d3Cross.y)
				+ d3Point.y
				* (1 + a * (-d3Cross.z * d3Cross.z - d3Cross.x * d3Cross.x))
				+ d3Point.z * (-d3Cross.x + a * d3Cross.y * d3Cross.z);
		double z = d3Point.x * (-d3Cross.y + a * d3Cross.x * d3Cross.z)
				+ d3Point.y * (d3Cross.x + a * d3Cross.y * d3Cross.z)
				+ d3Point.z
				* (1 + a * (-d3Cross.x * d3Cross.x - d3Cross.y * d3Cross.y));

		return new Double3D(x, y, z);
	}

	/**
	 * Helper function that will calculate the cross product between two vectors
	 * 
	 * @param v1
	 *            The first vector
	 * @param v2
	 *            The second vector
	 * @return The cross product between the two vectors
	 */
	public static Double3D crossProduct(Double3D v1, Double3D v2) {
		double x, y, z;

		x = v1.y * v2.z - v1.z * v2.y;
		y = v1.z * v2.x - v1.x * v2.z;
		z = v1.x * v2.y - v1.y * v2.x;

		return new Double3D(x, y, z);
	}

	/**
	 * Helper function that will calculate the dot product between two vectors
	 * 
	 * @param v1
	 *            The first vector
	 * @param v2
	 *            The second vector
	 * @return The dot product between the two vectors
	 */
	public static double dotProduct(Double3D v1, Double3D v2) {
		return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
	}

	/**
	 * 
	 * http://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space
	 * 
	 * Rotate a vector about the x axis
	 * 
	 * @param d3Vector
	 *            The vector to rotate
	 * @param dTheta
	 *            The angle to rotate
	 * @return The rotated vector
	 */
	public static Double3D rX(Double3D d3Vector, double dTheta) {
		double dSinTheta = Math.sin(dTheta), dCosTheta = Math.cos(dTheta);

		double x, y, z;

		x = d3Vector.x;
		y = dCosTheta * d3Vector.y - dSinTheta * d3Vector.z;
		z = dSinTheta * d3Vector.y + dCosTheta * d3Vector.z;

		return new Double3D(x, y, z);
	}

	/**
	 * Rotate a vector about the y axis
	 * 
	 * @param d3Vector
	 *            The vector to rotate
	 * @param dTheta
	 *            The angle to rotate
	 * @return The rotated vector
	 */
	public static Double3D rY(Double3D d3Vector, double dTheta) {
		double dSinTheta = Math.sin(dTheta), dCosTheta = Math.cos(dTheta);

		double x, y, z;

		x = dCosTheta * d3Vector.x + dSinTheta * d3Vector.z;
		y = d3Vector.y;
		z = -dSinTheta * d3Vector.x + dCosTheta * d3Vector.z;

		return new Double3D(x, y, z);
	}


	/**
	 * Rotate a vector about the z axis
	 * 
	 * @param d3Vector
	 *            The vector to rotate
	 * @param dTheta
	 *            The angle to rotate
	 * @return The rotated vector
	 */
	public static Double3D rZ(Double3D d3Vector, double dTheta) {
		double dSinTheta = Math.sin(dTheta), dCosTheta = Math.cos(dTheta);

		double x, y, z;

		x = dCosTheta * d3Vector.x - dSinTheta * d3Vector.y;
		y = dSinTheta * d3Vector.x + dCosTheta * d3Vector.y;
		z = d3Vector.z;

		return new Double3D(x, y, z);
	}

}
