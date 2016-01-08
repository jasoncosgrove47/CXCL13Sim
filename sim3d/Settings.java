package sim3d;

import java.awt.Color;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ec.util.MersenneTwisterFast;

/**
 * All the parameters for the simulation.
 * Note: parameters in functions can be changed while the simulation is running!
 * 
 * @author Jason Cosgrove - {@link jc1571@york.ac.uk}
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 * 
 */
public class Settings
{
	
		 public static void loadParameters(Document params)
		 {
			 	// Simulation Tag
			 	Element paramOElement = (Element) params.getElementsByTagName("O").item(0);	
			 
				NodeList widthNL = paramOElement.getElementsByTagName("WIDTH");
				Node widthN = widthNL.item(0);
				WIDTH = Integer.parseInt(widthN.getTextContent());
				
				NodeList heightNL = paramOElement.getElementsByTagName("HEIGHT");
				Node heightN = heightNL.item(0);
				HEIGHT = Integer.parseInt(heightN.getTextContent());
				
				NodeList depthNL = paramOElement.getElementsByTagName("DEPTH");
				Node depthN = depthNL.item(0);
				DEPTH = Integer.parseInt(depthN.getTextContent());
				
				NodeList gridNL = paramOElement.getElementsByTagName("GRID_SIZE");
				Node gridN = gridNL.item(0);
				GRID_SIZE= Double.parseDouble(gridN.getTextContent());
				
				NodeList diffusionNL = paramOElement.getElementsByTagName("DIFFUSION_COEFFICIENT");
				Node diffusionN = diffusionNL.item(0);
				DIFFUSION_COEFFICIENT = Double.parseDouble(diffusionN.getTextContent());
		
				//this must be computed here otherwise these values get set to zero
				DIFFUSION_TIMESTEP = calculateDIFFUSION_TIMESTEP();
				DIFFUSION_STEPS = calculateDIFFUSION_STEPS();
		   }
	
		///////////////////////////  CORE SIMULATION PARAMETERS  /////////////////////////////////
		/**
		 * Allows MASON's random variable to be accessed globally
		 */
		public static MersenneTwisterFast	RNG;
											
		/**
		 * Dimensions of the simulation
		 * Note: the simulation has a 1 unit border around the edge hence the + 2
		 */
		 public static int		WIDTH;
		 public static int      HEIGHT;
		 public static int      DEPTH;
		
		/**
		 * Size of one edge of a grid space in meters
		 */
		public static double				GRID_SIZE;			//1E-05 = 10 micron
																																	
		/**
		 * Speed of diffusion, used by DiffusionAlgorithm
		 */
		public static double DIFFUSION_COEFFICIENT;
		public static double DIFFUSION_TIMESTEP;
		public static int DIFFUSION_STEPS;
		
		
		/**
		 * How much time a single iteration of the diffusion process will take us
		 * forward
		 * 
		 * @see http://physics-server.uoregon.edu/~raghu/TeachingFiles/Winter08Phys352/Notes_Diffusion.pdf
		 *      
		 * TODO: Get Simon to explain this again to me     
		 */
		static double calculateDIFFUSION_TIMESTEP()
		{
			return Math.pow( GRID_SIZE, 2 )	/ (43.95 * DIFFUSION_COEFFICIENT);
		}
			
		static int calculateDIFFUSION_STEPS()
		{
			return (int) (1 / DIFFUSION_TIMESTEP);
		}
		
	////////////////////////// BC PARAMETERS  ////////////////////////////////
	
	/**
	 * Subclass containing all the BC parameters
	 */
	public static class BC
	{
		/**
		 * This loads the parameters from an XML file for high throughput analyses
		 * @param params
		 */
		 public static void loadParameters(Document params)
		 {
			 	// Simulation Tag

			    Element paramBCElement = (Element) params.getElementsByTagName("BC").item(0);			      
			    
				NodeList countNL = paramBCElement.getElementsByTagName("COUNT");
				Node countN = countNL.item(0);
				COUNT = Integer.parseInt(countN.getTextContent());
				
				NodeList ccountNL = paramBCElement.getElementsByTagName("COGNATECOUNT");
				Node ccountN = ccountNL.item(0);
				COGNATECOUNT = Integer.parseInt(ccountN.getTextContent());
				
				NodeList discretisationNL = paramBCElement.getElementsByTagName("DISCRETISATION");
				Node discretisationN = discretisationNL.item(0);
				DISCRETISATION = Integer.parseInt(discretisationN.getTextContent());
				
				NodeList minreceptorsNL = paramBCElement.getElementsByTagName("MIN_RECEPTORS");
				Node minreceptorsN = minreceptorsNL.item(0);
				MIN_RECEPTORS = Integer.parseInt(minreceptorsN.getTextContent());
				
				NodeList traveldistanceNL = paramBCElement.getElementsByTagName("TRAVEL_DISTANCE");
				Node traveldistanceN = traveldistanceNL.item(0);
				TRAVEL_DISTANCE = Double.parseDouble(traveldistanceN.getTextContent());
	
		   }
		
		
		/**
		 * Number of BCs to generate
		 */
		public static int		COUNT;
				
		/**
		 * Number of antigen specificBCs to generate
		 */
		public static int		COGNATECOUNT;
		
		/**
		 * used by Continuous3D - related to getting neighbours; the size of the bags
		 * A value of 5 means a collision grid neighbourhood of 5*5*5
		 */
		public static double	DISCRETISATION;
												
		/**
		 * The number of receptors required for chemotaxis
		 */
		public static int MIN_RECEPTORS;
		
		/**
		 * The distance a BC will travel
		 */
		static double TRAVEL_DISTANCE;
		public static double TRAVEL_DISTANCE()
		{
			return TRAVEL_DISTANCE; //thus 10 microns per minute? should be 10 microns per hour
		}
		
		/**
		 * Used to add noise to the system. The maximum angle away from
		 * "perfect" chemotaxis
		 */
		static double DIRECTION_ERROR;
		public static double DIRECTION_ERROR()
		{
			return Math.PI / 2;
		}
		
		/**
		 * The max angle to turn when moving randomly
		 * TODO why is this constraint necessary
		 * and how was it derived
		 */
		public static double RANDOM_TURN_ANGLE()
		{
			return Math.PI / 12;
		}
		
		/**
		 * The colour of BCs
		 * we use green as that is the convention for MP experiments
		 * @return
		 */
		public static Color DRAW_COLOR()
		{
			return new Color(0, 248, 0, 150);
		}
		
		/**
		 * The radius of the sphere that will collide with things. Also
		 * determines the display size.
		 */
		public static double COLLISION_RADIUS = 0.5; // TODO why is this 5 microns its to match the diameter of the cell??
		
		/**
		 * Parameterisation of the ODE for the receptors in the BCs
		 */
		public static class ODE
		{
			
			/**
			 * This loads the parameters from an XML file for high throughput analyses
			 * @param params
			 */
			 public static void loadParameters(Document params)
			 {
				 	// Simulation Tag

				    Element paramODEElement = (Element) params.getElementsByTagName("ODE").item(0);			      
			        
					NodeList LRNL = paramODEElement.getElementsByTagName("LR");
					Node LRN = LRNL.item(0);
					LR = Integer.parseInt(LRN.getTextContent());
					
					NodeList RdNL = paramODEElement.getElementsByTagName("Rd");
					Node RdN = RdNL.item(0);
					Rd = Integer.parseInt(RdN.getTextContent());
					
					NodeList RfNL = paramODEElement.getElementsByTagName("Rf");
					Node RfN = RfNL.item(0);
					Rf = Integer.parseInt(RfN.getTextContent());
					
					NodeList RiNL = paramODEElement.getElementsByTagName("Ri");
					Node RiN = RiNL.item(0);
					Ri = Integer.parseInt(RiN.getTextContent());
					
					NodeList KaNL = paramODEElement.getElementsByTagName("Ka");
					Node KaN = KaNL.item(0);
					Ka = Double.parseDouble(KaN.getTextContent());
					
					NodeList KrNL = paramODEElement.getElementsByTagName("Kr");
					Node KrN = KrNL.item(0);
					Kr = Double.parseDouble(KrN.getTextContent());
					
					NodeList KiNL = paramODEElement.getElementsByTagName("Ki");
					Node KiN = KiNL.item(0);
					Ki = Double.parseDouble(KiN.getTextContent());
					
					NodeList gammaNL = paramODEElement.getElementsByTagName("gamma");
					Node gammaN = gammaNL.item(0);
					gamma = Double.parseDouble(gammaN.getTextContent());
					
					NodeList deltaNL = paramODEElement.getElementsByTagName("delta");
					Node deltaN = deltaNL.item(0);
					delta = Double.parseDouble(deltaN.getTextContent());
			   }
			
			
			static int LR;
			public static int LR(){ return LR; }
			
			static int Rd;
			public static int Rd() {return Rd;}
		
			public static int Rf;
			public static int Rf(){ return Rf; }
																
			static int Ri;
			public static int Ri() { return Ri;}
		
			static double Ka;
			public static double K_a(){return Ka;}
			
			static double Kr;
			public static double K_r(){return Kr;}
			
			static double Ki;
			public static double K_i(){return Ki;}
			
			static double gamma;
			public static double gamma(){return gamma;}
			
			static double delta;
			public static double delta(){return delta;}
		}
	}
	
	
	////////////////////////////////////////// FDC  /////////////////////////////////////////
	
	/**
	 * Subclass containing all the FDC parameters
	 */
	public static class FDC
	{
		
		 public static void loadParameters(Document params)
		 {
			 	// Simulation Tag

			    Element paramFDCElement = (Element) params.getElementsByTagName("FDC").item(0);			      
		        
			    NodeList alNL = paramFDCElement.getElementsByTagName("STARTINGANTIGENLEVEL");
				Node alN = alNL.item(0);
				STARTINGANTIGENLEVEL = Integer.parseInt(alN.getTextContent());
			    
			    
				NodeList countNL = paramFDCElement.getElementsByTagName("COUNT");
				Node countN = countNL.item(0);
				COUNT = Integer.parseInt(countN.getTextContent());
				
				NodeList discretisationNL = paramFDCElement.getElementsByTagName("DISCRETISATION");
				Node discretisationN = discretisationNL.item(0);
				DISCRETISATION = Integer.parseInt(discretisationN.getTextContent());
				
				NodeList cxcl13NL = paramFDCElement.getElementsByTagName("CXCL13_EMITTED");
				Node cxcl13N = cxcl13NL.item(0);
				CXCL13_EMITTED = Integer.parseInt(cxcl13N.getTextContent());
				
				NodeList stromanodeNL = paramFDCElement.getElementsByTagName("STROMA_NODE_RADIUS");
				Node stromanodeN = stromanodeNL.item(0);
				STROMA_NODE_RADIUS = Double.parseDouble(stromanodeN.getTextContent());
				
				NodeList stromaedgeNL = paramFDCElement.getElementsByTagName("STROMA_EDGE_RADIUS");
				Node stromaedgeN = stromaedgeNL.item(0);
				STROMA_EDGE_RADIUS = Double.parseDouble(stromaedgeN.getTextContent());
			
		   }
		 
		 /**
		  * Number of antigen per FDC at the start of the simulation
		  */
		 
		 public static int		STARTINGANTIGENLEVEL;
		/**
		 * Number of FDCs to generate
		 * 
		 * NOTE: this is just a maximum. If there's no room to fit them all, it
		 * won't keep trying
		 */
		public static int		COUNT;
												
		/**
		 * used by Continuous3D - related to getting neighbours; the size of the bags
		 */
		public static double	DISCRETISATION;
												
		/**
		 * The colour of the FDCs
		 */
		public static Color DRAW_COLOR()
		{
			return new Color( 200, 130, 60 );
		}
		
		/**
		 * The amount of chemokine secreted at each time step
		 * could do a tissue ELISA and titrate against fluorescently labelled chemokine as
		 * we know roughly how many molecules there are in this???
		 */
		static int CXCL13_EMITTED;
		public static int CXCL13_EMITTED(){return CXCL13_EMITTED;}
		
		/**
		 * The radius of the sphere nucleus that will collide with things. Also
		 * determines the display size.
		 */
		public static double	STROMA_NODE_RADIUS;
													
		/**
		 * The radius of the cylinder edge that will collide with things.
		 */
		public static double	STROMA_EDGE_RADIUS;
	}
	
	
	
	public static class CXCL13
	{
		 public static void loadParameters(Document params)
		 {
			 	// Simulation Tag
			    Element paramCXCL13Element = (Element) params.getElementsByTagName("CXCL13").item(0);			      
		        
				NodeList stromaedgeNL = paramCXCL13Element.getElementsByTagName("DECAY_CONSTANT");
				Node stromaedgeN = stromaedgeNL.item(0);
				DECAY_CONSTANT = Double.parseDouble(stromaedgeN.getTextContent());
		   }
		 
		   /**
			 * the rate of protein decay per timestep (do we also need to account for each gridspace?)
			 */
			public static double	DECAY_CONSTANT;
		 
		
	}
	
}
