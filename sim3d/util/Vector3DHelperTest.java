package sim3d.util;

import static org.junit.Assert.*;

import java.awt.Color;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import sim.util.Double3D;
import sim3d.Settings;

public class Vector3DHelperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Settings.RNG = new MersenneTwisterFast();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	// This function should return a random point on the surface of a r=1
	// sphere. The distribution of
	// points across the surface of the sphere should be even. In particular,
	// there should be no
	// grouping in the corners or at the poles - which is common with incorrect
	// algorithms.
	// plots 50000 points returned from the function, and displays the result.
	// The resulting graph can be seen in ./Test01.png. As can be seen, the
	// point on
	// the surface of the r=1 sphere are distributed evenly without grouping.
	@Test
	public void testGetRandomDirection() {
		int size = 10000;
		Double3D[] points = new Double3D[size];
		Color[] colors = new Color[size];

		for (int i = 0; i < size; i++) {
			Double3D rand = Vector3DHelper.getRandomDirectionInCone(
					new Double3D(0, 0, 1), Math.PI / 3);
			points[i] = new Double3D(rand.x, rand.y, rand.z);
			colors[i] = new Color(0, 0, 0, 100);
		}

	}

	@Test
	public void testRandomDirectionInCone() {
		int size = 20000;
		Double3D[] points = new Double3D[4 * size + 400];
		Color[] colors = new Color[4 * size + 400];

		Double3D vec = Vector3DHelper.getRandomDirection();
		for (int i = 0; i < size; i++) {
			Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec,
					Math.PI / 6);
			points[i] = new Double3D(rand.x, rand.y, rand.z);
			colors[i] = new Color(200, 0, 0, 100);
		}
		for (int i = 0; i < 100; i++) {
			Double3D rand = vec.multiply(((double) i) / 100.0);
			points[4 * size + i] = new Double3D(rand.x, rand.y, rand.z);
			colors[4 * size + i] = new Color(255, 0, 0, 255);
		}

		vec = Vector3DHelper.getRandomDirection();
		for (int i = 0; i < size; i++) {
			Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec,
					Math.PI / 6);
			points[size + i] = new Double3D(rand.x, rand.y, rand.z);
			colors[size + i] = new Color(0, 200, 0, 100);
		}
		for (int i = 0; i < 100; i++) {
			Double3D rand = vec.multiply(((double) i) / 100.0);
			points[4 * size + i + 100] = new Double3D(rand.x, rand.y, rand.z);
			colors[4 * size + i + 100] = new Color(0, 255, 0, 255);
		}

		vec = Vector3DHelper.getRandomDirection();
		for (int i = 0; i < size; i++) {
			Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec,
					Math.PI / 6);
			points[2 * size + i] = new Double3D(rand.x, rand.y, rand.z);
			colors[2 * size + i] = new Color(0, 0, 200, 100);
		}/**/
		for (int i = 0; i < 100; i++) {
			Double3D rand = vec.multiply(((double) i) / 100.0);
			points[4 * size + i + 200] = new Double3D(rand.x, rand.y, rand.z);
			colors[4 * size + i + 200] = new Color(0, 0, 255, 255);
		}

		vec = Vector3DHelper.getRandomDirection();
		for (int i = 0; i < size; i++) {
			Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec,
					Math.PI / 6);
			points[3 * size + i] = new Double3D(rand.x, rand.y, rand.z);
			colors[3 * size + i] = new Color(0, 0, 0, 100);
		}/**/
		for (int i = 0; i < 100; i++) {
			Double3D rand = vec.multiply(((double) i) / 100.0);
			points[4 * size + i + 300] = new Double3D(rand.x, rand.y, rand.z);
			colors[4 * size + i + 300] = new Color(0, 0, 0, 255);
		}

	}

	@Test
	public void testVector3DHelperEqDistPointsOnSphere() {
		int size = 100;
		Double3D[] points = new Double3D[size];
		Color[] colors = new Color[size];

		Double3D[] ad3Points = Vector3DHelper.getEqDistPointsOnSphere(size);

		for (int i = 0; i < size; i++) {
			points[i] = new Double3D(ad3Points[i].x, ad3Points[i].y,
					ad3Points[i].z);
			colors[i] = new Color(0, 0, 0, 100);
		}

	}

	/**
	 * Results precomputed using:
	 * 
	 * http://www.nh.cas.cz/people/lazar/celler/online_tools.php
	 * 
	 * assert that our results give the same answers
	 * 
	 * Remember that input is in radians!!
	 */
	@Test
	public void testRotateAboutAnAxis() {

		Double3D input = new Double3D(5, 5, 5);

		Double3D outputX = Vector3DHelper.rX(input, Math.toRadians(90));
		Double3D answerX = new Double3D(5, -5, 5);
		assertEquals(answerX, outputX);

		Double3D outputY = Vector3DHelper.rY(input, Math.toRadians(90));
		Double3D answerY = new Double3D(5, 5, -5);
		assertEquals(answerY, outputY);

		Double3D outputZ = Vector3DHelper.rZ(input, Math.toRadians(90));
		Double3D answerZ = new Double3D(-5, 5, 5);
		assertEquals(answerZ, outputZ);
	}

}
