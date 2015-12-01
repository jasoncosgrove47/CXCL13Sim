package sim3d;

import java.awt.Color;

import ec.util.MersenneTwisterFast;

/**
 * All the parameters for the simulation.
 * 
 * Note: parameters in functions can be changed while the simulation is running!
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class Options
{
	/**
	 * Allows MASON's random variable to be accessed globally
	 */
	public static MersenneTwisterFast	RNG;
										
	/**
	 * Dimensions of the simulation
	 * 
	 * Note: the simulation has a 1 unit border around the edge hence the + 2
	 */
	public static int					WIDTH					= 100 + 2,
												HEIGHT = 100 + 2, DEPTH = 10 + 2;
												
	/**
	 * Size of one edge of a grid space in meters
	 */
	public static double				GRID_SIZE				= 0.00001;							// 1E-05
																									// =
																									// 10
																									// microns
																
	/**
	 * Speed of diffusion, used by DiffusionAlgorithm
	 */
	public static double				DIFFUSION_COEFFICIENT	= 1.519 * Math.pow( 10, -10 );
																
	/**
	 * How much time a single iteration of the diffusion process will take us
	 * forward
	 * 
	 * @see http://physics-server.uoregon.edu/~raghu/TeachingFiles/
	 *      Winter08Phys352/Notes_Diffusion.pdf
	 */
	public static double				DIFFUSION_TIMESTEP		= Math.pow( GRID_SIZE, 2 )
			/ (3.7 * DIFFUSION_COEFFICIENT);
			
	/**
	 * What the DIFFUSION_TIMESTEP translates to in number of iterations
	 */
	public static int					DIFFUSION_STEPS			= (int) (1 / DIFFUSION_TIMESTEP);
																
	/**
	 * Subclass containing all the BC parameters
	 */
	public static class BC
	{
		/**
		 * Number of BCs to generate
		 */
		public static int		COUNT			= 100;
												
		/**
		 * used by Continuous3D - related to getting neighbours; the size of the
		 * bags
		 * 
		 */
		public static double	DISCRETISATION	= 5;
												
		/**
		 * The number of receptors required for chemotaxis
		 */
		public static int MIN_RECEPTORS = 1600;
		
		/**
		 * The distance a BC will travel
		 */
		public static double TRAVEL_DISTANCE()
		{
			return 1;
		}
		
		/**
		 * Used to add noise to the system. The maximum angle away from
		 * "perfect" chemotaxis
		 */
		public static double DIRECTION_ERROR()
		{
			return Math.PI / 2;
		}
		
		/**
		 * The max angle to turn when moving randomly
		 */
		public static double RANDOM_TURN_ANGLE()
		{
			return Math.PI / 12;
		}
		
		/**
		 * The colour of BCs
		 * 
		 * @return
		 */
		public static Color DRAW_COLOR()
		{
			return new Color( 90, 90, 255 );
		}
		
		/**
		 * The radius of the sphere that will collide with things. Also
		 * determines the display size.
		 */
		public static double COLLISION_RADIUS = 0.5;
		
		/**
		 * Parameterisation of the ODE for the receptors in the BCs
		 */
		public static class ODE
		{
			/**
			 * Rate of binding with ligand
			 */
			public static double K_a()
			{
				return 0.03;
			}
			
			/**
			 * Rate of recycling receptions
			 */
			public static double K_r()
			{
				return 0.1;
			}
			
			/**
			 * Rate of internalisation
			 */
			public static double K_i()
			{
				return 0.1;
			}
			
			/**
			 * yes
			 */
			public static double gamma()
			{
				return 0.1;
			}
			
			/**
			 * true
			 */
			public static double delta()
			{
				return 2;
			}
		}
	}
	
	/**
	 * Subclass containing all the FDC parameters
	 */
	public static class FDC
	{
		/**
		 * Number of FDCs to generate
		 * 
		 * NOTE: this is just a maximum. If there's no room to fit them all, it
		 * won't keep trying
		 */
		public static int		COUNT			= 6000;
												
		/**
		 * used by Continuous3D - related to getting neighbours; the size of the
		 * bags
		 */
		public static double	DISCRETISATION	= 5;
												
		/**
		 * The colour of the FDCs
		 */
		public static Color DRAW_COLOR()
		{
			return new Color( 200, 130, 60 );
		}
		
		/**
		 * THe amount of chemokine secreted at each time step
		 */
		public static int CXCL13_EMITTED()
		{
			return 500;
		}
		
		/**
		 * The radius of the sphere nucleus that will collide with things. Also
		 * determines the display size.
		 */
		public static double	STROMA_NODE_RADIUS	= 0.5;
													
		/**
		 * The radius of the cylinder edge that will collide with things.
		 */
		public static double	STROMA_EDGE_RADIUS	= 0.1;
	}
}
