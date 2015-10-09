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
	
	public static int WIDTH = 50;
	public static int HEIGHT = 50;
	public static int DEPTH = 5;
	public static double MAX_DISTANCE = 5;
	public static int DIFFUSION_STEPS = 2;
	
	public static class BC
	{
		public static int COUNT = 1;
		public static double DISCRETISATION = 5;
		public static int MIN_RECEPTORS() { return 2800; }
		public static double TRAVEL_DISTANCE(){ return 1; }
		public static double DIRECTION_ERROR(){ return Math.PI/2; }
		public static double RANDOM_TURN_ANGLE(){ return Math.PI/12; }
		public static double DRAW_SCALE(){ return 0.7; }
		public static Color DRAW_COLOR(){ return new Color(90,90,255); }
		public static Color RANDOM_COLOR(){ return new Color(90,255,255); }
		public static double RECEPTOR_BIND_CHANCE(){ return 0.1; }
		public static class ODE
		{
			public static double K_a(){ return 0.1; }
			public static double K_r(){ return 0.1; }
			public static double K_i(){ return 0.2; }
			public static double gamma(){ return 0.2; }
			public static double delta(){ return 0.2; }
		}
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
		public static int CXCL13_EMITTED(){ return 500; }
	}
	
	public static class FRCGenerator {
		public static int MAX_EDGE_LENGTH(){ return 3; }
	}
}
