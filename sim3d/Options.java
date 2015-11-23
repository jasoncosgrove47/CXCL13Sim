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
	
	public static int WIDTH = 102;
	public static int HEIGHT = 102;
	public static int DEPTH = 12;
	public static double MAX_DISTANCE = 5;
	public static double GRID_SIZE = 0.00001;
	public static double DIFFUSION_COEFFICIENT = 1.519 * Math.pow(10, -10);
	// from http://physics-server.uoregon.edu/~raghu/TeachingFiles/Winter08Phys352/Notes_Diffusion.pdf
	public static double DIFFUSION_TIMESTEP = Math.pow(GRID_SIZE, 2)/(3.7*DIFFUSION_COEFFICIENT);
	public static int DIFFUSION_STEPS = 0;//(int)(1/DIFFUSION_TIMESTEP);
	
	public static class BC
	{
		public static int COUNT = 100;
		public static double DISCRETISATION = 5;
		public static int MIN_RECEPTORS() { return 1600; }
		public static double TRAVEL_DISTANCE(){ return 1; }
		public static double DIRECTION_ERROR(){ return Math.PI/2; }
		public static double RANDOM_TURN_ANGLE(){ return Math.PI/12; }
		public static double DRAW_SCALE(){ return 0.7; }
		public static Color DRAW_COLOR(){ return new Color(90,90,255); }
		public static Color RANDOM_COLOR(){ return new Color(90,255,255); }
		public static double RECEPTOR_BIND_CHANCE(){ return 0.1; }
		public static double COLLISION_RADIUS = 0.5;
		public static class ODE
		{
			public static double K_a(){ return 0.03; }
			public static double K_r(){ return 0.1; }
			public static double K_i(){ return 0.1; }
			public static double gamma(){ return 0.1; }
			public static double delta(){ return 2; }
		}
	}
	public static class FDC
	{
		public static double DISCRETISATION = 5;
		public static int COUNT = 6000;
		public static double DRAW_SCALE(){ return 0.5; }
		public static Color DRAW_COLOR(){ return new Color(200,130,60); }
		public static int CXCL13_EMITTED(){ return 500; }
		public static double STROMA_NODE_RADIUS = 0.5;
		public static double STROMA_EDGE_RADIUS = 0.1;
	}
	
	public static class FRCGenerator {
		public static int MAX_EDGE_LENGTH(){ return 3; }
	}
}
