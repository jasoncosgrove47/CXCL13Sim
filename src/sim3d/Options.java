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
	public static int DEPTH = 15;
	public static double MAX_DISTANCE = 5;
	public static int DIFFUSION_STEPS = 1;
	
	public static class BC
	{
		public static int COUNT = 300;
		public static double DISCRETISATION = 5;
		public static double CONC_CARRYING(){ return 0.014; }
		public static double VECTOR_MIN() { return 0.01; }
		public static double VECTOR_NOISE(){ return 0; }
		public static double TRAVEL_DISTANCE(){ return 0.1; }
		public static double DIRECTION_ERROR(){ return Math.PI/2; }
		public static double RANDOM_TURN_ANGLE(){ return Math.PI/12; }
		public static double DRAW_SCALE(){ return 0.7; }
		public static Color DRAW_COLOR(){ return new Color(90,90,255); }
		public static Color RANDOM_COLOR(){ return new Color(90,255,255); }
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
		public static int COUNT = 15000;
		public static double DRAW_SCALE(){ return 0.5; }
		public static Color DRAW_COLOR(){ return new Color(200,130,60); }
		public static double CXCL13_EMITTED(){ return 1; }
	}
	
	public static class FRCGenerator {
		public static int MAX_EDGE_LENGTH = 4;
	}
}
