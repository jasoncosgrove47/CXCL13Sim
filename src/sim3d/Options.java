package sim3d;
import java.awt.Color;

import ec.util.MersenneTwisterFast;

/*
 * All the variables of the simulation in one place
 * Those represented as functions can be hot-swapped!
 */

public class Options
{
	public static MersenneTwisterFast RNG;
	
	public static int WIDTH = 100;
	public static int HEIGHT = 100;
	public static int DEPTH = 10;
	public static double MAX_DISTANCE = 5;
	
	public static class BC
	{
		public static int COUNT = 200;
		public static double DISCRETISATION = 5;
		public static double VECTOR_MIN() { return 0; }
		public static double RECEPTOR_NOISE() { return 0; }
		public static double RECEPTOR_MAX() { return 999; }
		public static double TRAVEL_DISTANCE(){ return 0.1; }
		public static double DRAW_SCALE(){ return 0.7; }
		public static Color DRAW_COLOR(){ return new Color(90,90,255); }
	}
	public static class OVA
	{
		public static int COUNT = 200;
		public static double DISCRETISATION = 5;
		public static double TRAVEL_DISTANCE(){ return 0.2; }
		public static double DRAW_SCALE(){ return 0.7; }
		public static Color DRAW_COLOR(){ return new Color(150,60,60); }
	}
	public static class FDC
	{
		public static double DISCRETISATION = 5;
		public static int COUNT = 60;
		public static double DRAW_SCALE(){ return 0.5; }
		public static Color DRAW_COLOR(){ return new Color(200,130,60); }
		public static double CXCL13_EMITTED(){ return 3; }
	}
}
