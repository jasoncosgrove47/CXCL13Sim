package sim3d;

import java.awt.Color;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ec.util.MersenneTwisterFast;

/**
 * All the parameters for the simulation. Note: parameters in functions can be
 * changed while the simulation is running.
 * 
 * @author Jason Cosgrove - {@link jc1571@york.ac.uk}
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */

public class Settings {
	


	
	public static void loadParameters(Document params) {
		// Simulation Tag
		Element paramOElement = (Element) params.getElementsByTagName("O")
				.item(0);

		NodeList widthNL = paramOElement.getElementsByTagName("WIDTH");
		Node widthN = widthNL.item(0);
		WIDTH = Integer.parseInt(widthN.getTextContent());

		NodeList heightNL = paramOElement.getElementsByTagName("HEIGHT");
		Node heightN = heightNL.item(0);
		HEIGHT = Integer.parseInt(heightN.getTextContent());

		NodeList depthNL = paramOElement.getElementsByTagName("DEPTH");
		Node depthN = depthNL.item(0);
		DEPTH = Integer.parseInt(depthN.getTextContent());

		NodeList elNL = paramOElement.getElementsByTagName("EXPERIMENTLENGTH");
		Node elN = elNL.item(0);
		EXPERIMENTLENGTH = Integer.parseInt(elN.getTextContent());
		
		NodeList threadNL = paramOElement.getElementsByTagName("NUM_THREADS");
		Node threadN = threadNL.item(0);
		NUM_THREADS = Integer.parseInt(threadN.getTextContent());

		NodeList gridNL = paramOElement.getElementsByTagName("GRID_SIZE");
		Node gridN = gridNL.item(0);
		GRID_SIZE = Double.parseDouble(gridN.getTextContent());
		
		NodeList sbNL = paramOElement.getElementsByTagName("SIGNALLING_BIAS");
		Node sbN = sbNL.item(0);
		SIGNALLING_BIAS= Double.parseDouble(sbN.getTextContent());

	

		
	}

	/*
	 * due to floating point inaccuracies you cant directly
	 * compare double3Ds so we set a threshold here for use
	 * throughout the simulation
	 */
	public static double DOUBLE3D_PRECISION = 0.01;
	
	
	
	/*
	 * the number of cores to use for diffusion
	 */
	public static int NUM_THREADS;
	
	
	/*
	 * strength of CXCL13 in comparison with EBI2
	 * less than 1 favours CXCL13, greater than one
	 * favours EBI2
	 */
	public static double SIGNALLING_BIAS;
	
	
	/**
	 * Allows MASON's random variable to be accessed globally
	 */
	public static MersenneTwisterFast RNG;

	/**
	 * Dimensions of the simulation Note: the simulation has a 1 unit border
	 * around the edge hence the + 2
	 */
	public static int WIDTH;
	public static int HEIGHT;
	public static int DEPTH;

	/*
	 * the length of an experiment, once steady state is reached
	 */
	public static int EXPERIMENTLENGTH;

	/**
	 * Size of one edge of a grid space in meters
	 */
	public static double GRID_SIZE; // 1E-05 = 10 micron


	/**
	 * subclass containing all of the TC parameters
	 */
	public static class TC{
		/**
		 * This loads the parameters from an XML file for high throughput
		 * analyses
		 * 
		 * @param params
		 */
		public static void loadParameters(Document params) {
			Element paramTCElement = (Element) params
					.getElementsByTagName("TC").item(0);

			NodeList countNL = paramTCElement.getElementsByTagName("COUNT");
			Node countN = countNL.item(0);
			COUNT = Integer.parseInt(countN.getTextContent());
		}
		/**
		 * Number of TCs to generate
		 */
		public static int COUNT;
		
	}
	
	
	/**
	 * Subclass containing all the BC parameters
	 */
	public static class BC {

		/**
		 * This loads the parameters from an XML file for high throughput
		 * analyses
		 * 
		 * @param params
		 */
		public static void loadParameters(Document params) {
			Element paramBCElement = (Element) params
					.getElementsByTagName("BC").item(0);

			NodeList countNL = paramBCElement.getElementsByTagName("COUNT");
			Node countN = countNL.item(0);
			COUNT = Integer.parseInt(countN.getTextContent());

			NodeList ccountNL = paramBCElement
					.getElementsByTagName("COGNATECOUNT");
			Node ccountN = ccountNL.item(0);
			COGNATECOUNT = Integer.parseInt(ccountN.getTextContent());

			NodeList discretisationNL = paramBCElement
					.getElementsByTagName("DISCRETISATION");
			Node discretisationN = discretisationNL.item(0);
			DISCRETISATION = Integer.parseInt(discretisationN.getTextContent());

			NodeList traveldistanceNL = paramBCElement
					.getElementsByTagName("TRAVEL_DISTANCE");
			Node traveldistanceN = traveldistanceNL.item(0);
			TRAVEL_DISTANCE = Double.parseDouble(traveldistanceN
					.getTextContent());

			NodeList traveldistancesdNL = paramBCElement
					.getElementsByTagName("TRAVEL_DISTANCE_SD");
			Node traveldistancesdN = traveldistancesdNL.item(0);
			TRAVEL_DISTANCE_SD = Double.parseDouble(traveldistancesdN
					.getTextContent());

			NodeList stNL = paramBCElement
					.getElementsByTagName("SIGNAL_THRESHOLD");
			Node stN = stNL.item(0);
			SIGNAL_THRESHOLD = Double.parseDouble(stN.getTextContent());

			NodeList rtaNL = paramBCElement
					.getElementsByTagName("MAX_TURN_ANGLE");
			Node rtaN = rtaNL.item(0);
			MAX_TURN_ANGLE_DEGREES = Double.parseDouble(rtaN
					.getTextContent());

			NodeList csNL = paramBCElement
					.getElementsByTagName("DIRECTION_ERROR");
			Node csN = csNL.item(0);
			DIRECTION_ERROR_DEGREES = Double.parseDouble(csN.getTextContent());

			NodeList pNL = paramBCElement.getElementsByTagName("POLARITY");
			Node pN = pNL.item(0);
			POLARITY = Double.parseDouble(pN.getTextContent());

			NodeList rpNL = paramBCElement
					.getElementsByTagName("RANDOM_POLARITY");
			Node rpN = rpNL.item(0);
			RANDOM_POLARITY = Double.parseDouble(rpN.getTextContent());

			NodeList ssNL = paramBCElement.getElementsByTagName("SPEED_SCALAR");
			Node ssN = ssNL.item(0);
			SPEED_SCALAR = Double.parseDouble(ssN.getTextContent());

			convertAnglesToRadians();

		}
		

		public static void convertAnglesToRadians() {
			DIRECTION_ERROR = Math.toRadians(DIRECTION_ERROR_DEGREES);
			MAX_TURN_ANGLE = Math.toRadians(MAX_TURN_ANGLE_DEGREES);
		}

		/**
		 * The standard deviation of cell displacement constant
		 */
		public static double TRAVEL_DISTANCE_SD;

		/**
		 * The bias of the memory vector with respect to the cells orientation
		 * for a directed walk
		 */
		public static double POLARITY;

		/**
		 * The bias of the memory vector with respect to the cells orientation
		 * for a random walk
		 */
		public static double RANDOM_POLARITY;

		/**
		 * Used to relate the speed of the cell to its persistence
		 */
		public static double SPEED_SCALAR;

		/**
		 * Number of BCs to generate
		 */
		public static double SIGNAL_THRESHOLD;

		/**
		 * Number of BCs to generate
		 */
		public static int COUNT;

		/**
		 * Number of antigen specific BCs to generate
		 */
		public static int COGNATECOUNT;

		/**
		 * used by Continuous3D - related to getting neighbours; the size of the
		 * bags A value of 5 means a collision grid neighbourhood of 5*5*5
		 */
		public static double DISCRETISATION;

		/**
		 * The distance a BC will travel
		 */
		static double TRAVEL_DISTANCE;

		public static double TRAVEL_DISTANCE() {
			return TRAVEL_DISTANCE;

		}

		/**
		 * Used to add noise to the system. The maximum angle away from
		 * "perfect" chemotaxis
		 */
		static double DIRECTION_ERROR;
		private static double DIRECTION_ERROR_DEGREES;

		public static double DIRECTION_ERROR() {


			return DIRECTION_ERROR;
		}

		/**
		 * The max angle to turn when moving randomly
		 */
		private static double MAX_TURN_ANGLE_DEGREES;

		static double MAX_TURN_ANGLE;

		public static double MAX_TURN_ANGLE() {
			return MAX_TURN_ANGLE;
		}

		/**
		 * The colour of BCs we use green as that is the convention for MP
		 * experiments
		 * 
		 * @return
		 */
		public static Color DRAW_COLOR() {
			return new Color(0, 248, 0, 150);
		}

		/**
		 * The radius of the sphere that will collide with things. Also
		 * determines the display size.
		 * 
		 * code is highly sensitive to changes in this so be very careful!!
		 */
		public static double COLLISION_RADIUS = 0.35;

		/**
		 * Parameterisation of the ODE for the receptors in the BCs
		 */
		public static class ODE {

			/**
			 * This loads the parameters from an XML file for high throughput
			 * analyses
			 * 
			 * @param params
			 */
			public static void loadParameters(Document params) {
				Element paramODEElement = (Element) params
						.getElementsByTagName("ODE").item(0);

				NodeList LRNL = paramODEElement.getElementsByTagName("LR");
				Node LRN = LRNL.item(0);
				LR = Integer.parseInt(LRN.getTextContent());

				NodeList RfNL = paramODEElement.getElementsByTagName("Rf");
				Node RfN = RfNL.item(0);
				Rf = Integer.parseInt(RfN.getTextContent());

				NodeList RiNL = paramODEElement.getElementsByTagName("Ri");
				Node RiN = RiNL.item(0);
				Ri = Integer.parseInt(RiN.getTextContent());

				NodeList KaNL = paramODEElement.getElementsByTagName("Ka");
				Node KaN = KaNL.item(0);
				Ka_PREFIX = Double.parseDouble(KaN.getTextContent());
				
				Ka = scaleKa();

				
				NodeList KaENL = paramODEElement.getElementsByTagName("Ka_EBI2");
				Node KaEN = KaENL.item(0);
				Ka_EBI2 = Double.parseDouble(KaEN.getTextContent());
				
				
				NodeList KrNL = paramODEElement.getElementsByTagName("Kr");
				Node KrN = KrNL.item(0);
				Kr = Double.parseDouble(KrN.getTextContent());

				NodeList KiNL = paramODEElement.getElementsByTagName("Ki");
				Node KiN = KiNL.item(0);
				Ki = Double.parseDouble(KiN.getTextContent());
				
				NodeList KdesNL = paramODEElement.getElementsByTagName("Kdes");
				Node KdesN = KdesNL.item(0);
				Kdes = Double.parseDouble(KdesN.getTextContent());
				
				NodeList KoffNL = paramODEElement.getElementsByTagName("Koff");
				Node KoffN = KoffNL.item(0);
				Koff_PREFIX = Double.parseDouble(KoffN.getTextContent());
				
				Koff = scaleKoff();
				
			}

			/**
			 * ligand bound receptors on cell surface
			 */
			public static int LR;

			public static int LR() {
				return LR;
			}

			/**
			 * Free surface receptors
			 */
			public static int Rf;

			public static int Rf() {
				return Rf;
			}

			/**
			 * Number of receptors inside the cell
			 */
			public static int Ri;

			public static int Ri() {
				return Ri;
			}

			
			public static double Kdes;
			
			public static double Ka_EBI2;
			
			/**
			 * binding constant for receptor-ligand
			 */
			public static double Ka_PREFIX;

			private static double scaleKa() {
				return (Ka_PREFIX * 1e+5);
			}

			public static double Ka;

			public static double K_a() {
				return Ka;
			}

			
			public static double Koff;
			
			public static double Koff_PREFIX;
			
			private static double scaleKoff() {
				return (Koff_PREFIX * 1);
			}
			
			
			/**
			 * rate of receptor recycling from an internal pool
			 */
			static double Kr;

			public static double K_r() {
				return Kr;
			}

			/**
			 * rate of receptor internalisation
			 */
			static double Ki;

			public static double K_i() {
				return Ki;
			}

		}
	}

	/**
	 * Subclass containing all the FDC parameters
	 */
	public static class FDC {

		public static void loadParameters(Document params) {
			// Simulation Tag

			Element paramFDCElement = (Element) params.getElementsByTagName(
					"FDC").item(0);

			NodeList alNL = paramFDCElement
					.getElementsByTagName("STARTINGANTIGENLEVEL");
			Node alN = alNL.item(0);
			STARTINGANTIGENLEVEL = Integer.parseInt(alN.getTextContent());

			NodeList countNL = paramFDCElement.getElementsByTagName("FDCCOUNT");
			Node countN = countNL.item(0);
			COUNT = Integer.parseInt(countN.getTextContent());

			NodeList discretisationNL = paramFDCElement
					.getElementsByTagName("DISCRETISATION");
			Node discretisationN = discretisationNL.item(0);
			DISCRETISATION = Integer.parseInt(discretisationN.getTextContent());

			NodeList cxcl13NL = paramFDCElement
					.getElementsByTagName("CXCL13_EMITTED_FDC");
			Node cxcl13N = cxcl13NL.item(0);
			emissionrate = Double.parseDouble(cxcl13N.getTextContent());

			NodeList stromanodeNL = paramFDCElement
					.getElementsByTagName("STROMA_NODE_RADIUS");
			Node stromanodeN = stromanodeNL.item(0);
			STROMA_NODE_RADIUS = Double.parseDouble(stromanodeN
					.getTextContent());

			NodeList stromaedgeNL = paramFDCElement
					.getElementsByTagName("STROMA_EDGE_RADIUS");
			Node stromaedgeN = stromaedgeNL.item(0);
			STROMA_EDGE_RADIUS = Double.parseDouble(stromaedgeN
					.getTextContent());

			NodeList branchNL = paramFDCElement
					.getElementsByTagName("BRANCH_RADIUS");
			Node branchN = branchNL.item(0);
			BRANCH_RADIUS = Double.parseDouble(branchN.getTextContent());

			CXCL13_EMITTED = scaleEmissionRate(emissionrate);
		}

		/**
		 * The radius of the cylinder edge that will collide with things.
		 */
		public static double BRANCH_RADIUS;

		/**
		 * Number of antigen per FDC at the start of the simulation
		 */
		public static int STARTINGANTIGENLEVEL;

		/**
		 * Number of FDCs to generate NOTE: this is just a maximum. If there's
		 * no room to fit them all, it won't keep trying
		 */
		public static int COUNT;

		/**
		 * used by Continuous3D - related to getting neighbours; the size of the
		 * bags
		 */
		public static double DISCRETISATION;

		/**
		 * The colour of the FDCs
		 */
		public static Color DRAW_COLOR() {
			return new Color(200, 130, 60);
		}

		/**
		 * The amount of chemokine secreted at each time step
		 */
		private static double emissionrate;

		public static double CXCL13_EMITTED;

		//how much is secreted in nanomoles (NOT MOLAR)
		public static double scaleEmissionRate(double emissionrate) {
			return (emissionrate * 1E-21);
		}

		public static double CXCL13_EMITTED() {
			return CXCL13_EMITTED;
		}

		/**
		 * The radius of the sphere nucleus that will collide with things. Also
		 * determines the display size.
		 */
		public static double STROMA_NODE_RADIUS;

		/**
		 * The radius of the cylinder edge that will collide with things.
		 */
		public static double STROMA_EDGE_RADIUS;
	}


	/**
	 * subclass containing all of the FRC parameters
	 */
	public static class bRC{
		
		
	
		public static void loadParameters(Document params) {
			// Simulation Tag

			Element paramFRCElement = (Element) params.getElementsByTagName(
					"bRC").item(0);

			NodeList countNL = paramFRCElement.getElementsByTagName("RCCOUNT");
			Node countN = countNL.item(0);
			COUNT = Integer.parseInt(countN.getTextContent());
			
			NodeList countSCS = paramFRCElement.getElementsByTagName("SCSDEPTH");
			Node countSC = countSCS.item(0);
			SCSDEPTH = Integer.parseInt(countSC.getTextContent());
			
			NodeList cxcl13NL = paramFRCElement
					.getElementsByTagName("CXCL13_EMITTED_FRC");
			Node cxcl13N = cxcl13NL.item(0);
			emissionrate = Double.parseDouble(cxcl13N.getTextContent());

			CXCL13_EMITTED = FDC.scaleEmissionRate(emissionrate);
		}
		
		public static Color DRAW_COLOR() {
			return new Color(180, 110, 80);
		}
		

		public static double CXCL13_EMITTED() {
			return CXCL13_EMITTED;
		}
		
		private static double emissionrate;

		public static double CXCL13_EMITTED;
		
		public static int COUNT;
		public static int SCSDEPTH;
		
	}
	
	/**
	 * subclass containing all of the MRC parameters
	 */
	public static class MRC{
		
		
		public static void loadParameters(Document params) {
			// Simulation Tag

			Element paramMRCElement = (Element) params.getElementsByTagName(
					"MRC").item(0);

			NodeList ebi2lNL = paramMRCElement
					.getElementsByTagName("EBI2L_EMITTED_MRC");
			Node ebi2lN = ebi2lNL.item(0);
			emissionrate_ebi2l = Double.parseDouble(ebi2lN.getTextContent());

			EBI2L_EMITTED = FDC.scaleEmissionRate(emissionrate_ebi2l);
			
			NodeList countNL = paramMRCElement.getElementsByTagName("MRCCOUNT");
			Node countN = countNL.item(0);
			COUNT = Integer.parseInt(countN.getTextContent());
			
			NodeList cxcl13NL = paramMRCElement
					.getElementsByTagName("CXCL13_EMITTED_MRC");
			Node cxcl13N = cxcl13NL.item(0);
			emissionrate_cxcl13 = Double.parseDouble(cxcl13N.getTextContent());

			CXCL13_EMITTED = FDC.scaleEmissionRate(emissionrate_cxcl13);
			
			
		}
		
		public static Color DRAW_COLOR() {
			return new Color(255, 212, 212);
		}

		public static int COUNT;
		
		private static double emissionrate_ebi2l;
		
		private static double emissionrate_cxcl13;

		public static double EBI2L_EMITTED;
		public static double CXCL13_EMITTED;
		
		
		public static double CXCL13_EMITTED() {
			return CXCL13_EMITTED;
		}
		
		public static double EBI2L_EMITTED() {
			return EBI2L_EMITTED;
		}
	}
	
	/**
	 * subclass containing all of the CXCL13 parameters
	 */
	public static class CXCL13 {

		public static void loadParameters(Document params) {
			// Simulation Tag
			
			Element paramCXCL13Element = (Element) params.getElementsByTagName(
					"CXCL13").item(0);
			
			NodeList diffusionNL = paramCXCL13Element
					.getElementsByTagName("DIFFUSION_COEFFICIENT_CXCL13");
			Node diffusionN = diffusionNL.item(0);
			DIFFUSION_COEFFICIENT_PREFIX = Double.parseDouble(diffusionN
					.getTextContent());

			// this must be computed here otherwise these values get set to zero
			DIFFUSION_COEFFICIENT = scaleDIFFUSION_COEFFICIENT();
			DIFFUSION_TIMESTEP = calculateDIFFUSION_TIMESTEP();
			DIFFUSION_STEPS = calculateDIFFUSION_STEPS();


			NodeList stromaedgeNL = paramCXCL13Element
					.getElementsByTagName("DECAY_CONSTANT_CXCL13");
			Node stromaedgeN = stromaedgeNL.item(0);
			DECAY_CONSTANT = Double.parseDouble(stromaedgeN.getTextContent());
			
			
		}

		/*
		 * Scale the diffusion coefficient
		 */
		static double scaleDIFFUSION_COEFFICIENT() {

			return (DIFFUSION_COEFFICIENT_PREFIX * (1e-12));
		}
		
		/*
		 * Calculates the appropriate timestep for diffusion
		 */
		static double calculateDIFFUSION_TIMESTEP() {
			return (Math.pow(GRID_SIZE, 2) / (10 * DIFFUSION_COEFFICIENT));
		}
		


		/**
		 * multiply by 60 as we want to update diffusion in seconds and not minutes
		 * @return
		 */
		static int calculateDIFFUSION_STEPS() {
			return (int) (60 / DIFFUSION_TIMESTEP);
		}
		
		
		/**
		 * the rate of protein decay per timestep
		 */
		public static double DECAY_CONSTANT;
		
		
		/**
		 * Speed of diffusion, used by DiffusionAlgorithm need to specify the units
		 * so we know what we are dealing with
		 */
		public static double DIFFUSION_COEFFICIENT;
		public static double DIFFUSION_COEFFICIENT_PREFIX;
		
		/**
		 * How much time a single iteration of the diffusion process will take us
		 * forward
		 * 
		 * @see http
		 *      ://physics-server.uoregon.edu/~raghu/TeachingFiles/Winter08Phys352
		 *      /Notes_Diffusion.pdf
		 */
		public static double DIFFUSION_TIMESTEP;
		public static int DIFFUSION_STEPS;
	}
	
	/**
	 * subclass containing all of the CCL19 parameters
	 */
	public static class CCL19 {

		public static void loadParameters(Document params) {
			// Simulation Tag
			Element paramCCL19Element = (Element) params.getElementsByTagName(
					"CCL19").item(0);

			NodeList ccl19NL = paramCCL19Element
					.getElementsByTagName("DECAY_CONSTANT_CCL19");
			Node ccl19N = ccl19NL.item(0);
			DECAY_CONSTANT = Double.parseDouble(ccl19N.getTextContent());
			
			NodeList diffusionNL = paramCCL19Element
					.getElementsByTagName("DIFFUSION_COEFFICIENT_CCL19");
			Node diffusionN = diffusionNL.item(0);
			DIFFUSION_COEFFICIENT_PREFIX = Double.parseDouble(diffusionN
					.getTextContent());

			// this must be computed here otherwise these values get set to zero
			DIFFUSION_COEFFICIENT = scaleDIFFUSION_COEFFICIENT();
			DIFFUSION_TIMESTEP = calculateDIFFUSION_TIMESTEP();
			DIFFUSION_STEPS = calculateDIFFUSION_STEPS();
		}
		/*
		 * Scale the diffusion coefficient
		 */
		static double scaleDIFFUSION_COEFFICIENT() {

			return (DIFFUSION_COEFFICIENT_PREFIX * (1e-12));
		}
		
		
		/*
		 * Calculates the appropriate timestep for diffusion
		 */
		static double calculateDIFFUSION_TIMESTEP() {
			return (Math.pow(GRID_SIZE, 2) / (10 * DIFFUSION_COEFFICIENT));
		}
		

		/**
		 * multiply by 60 as we want to update diffusion in seconds and not minutes
		 * @return
		 */
		static int calculateDIFFUSION_STEPS() {
			return (int) (60 / DIFFUSION_TIMESTEP);
		}
		
		/**
		 * the rate of protein decay per timestep
		 */
		public static double DECAY_CONSTANT;
		
		
		/**
		 * Speed of diffusion, used by DiffusionAlgorithm need to specify the units
		 * so we know what we are dealing with
		 */
		public static double DIFFUSION_COEFFICIENT;
		public static double DIFFUSION_COEFFICIENT_PREFIX;
		
		/**
		 * How much time a single iteration of the diffusion process will take us
		 * forward
		 * 
		 * @see http
		 *      ://physics-server.uoregon.edu/~raghu/TeachingFiles/Winter08Phys352
		 *      /Notes_Diffusion.pdf
		 */
		public static double DIFFUSION_TIMESTEP;
		public static int DIFFUSION_STEPS;
	}
	
	/**
	 * subclass containing all of the EBI2L parameters
	 */
	public static class EBI2L {

		public static void loadParameters(Document params) {
			// Simulation Tag
			Element paramEBI2LElement = (Element) params.getElementsByTagName(
					"EBI2L").item(0);

			NodeList stromaedgeNL = paramEBI2LElement
					.getElementsByTagName("DECAY_CONSTANT_EBI2L");
			Node stromaedgeN = stromaedgeNL.item(0);
			DECAY_CONSTANT = Double.parseDouble(stromaedgeN.getTextContent());
			
			NodeList diffusionNL = paramEBI2LElement
					.getElementsByTagName("DIFFUSION_COEFFICIENT_EBI2L");
			Node diffusionN = diffusionNL.item(0);
			DIFFUSION_COEFFICIENT_PREFIX = Double.parseDouble(diffusionN
					.getTextContent());

			// this must be computed here otherwise these values get set to zero
			DIFFUSION_COEFFICIENT = scaleDIFFUSION_COEFFICIENT();
			DIFFUSION_TIMESTEP = calculateDIFFUSION_TIMESTEP();
			DIFFUSION_STEPS = calculateDIFFUSION_STEPS();
			
		}

		
		/*
		 * Scale the diffusion coefficient
		 */
		static double scaleDIFFUSION_COEFFICIENT() {

			return (DIFFUSION_COEFFICIENT_PREFIX * (1e-12));
		}
		
		
		/*
		 * Calculates the appropriate timestep for diffusion
		 */
		static double calculateDIFFUSION_TIMESTEP() {
			return (Math.pow(GRID_SIZE, 2) / (10 * DIFFUSION_COEFFICIENT));
		}
		

		/**
		 * multiply by 60 as we want to update diffusion in seconds and not minutes
		 * @return
		 */
		static int calculateDIFFUSION_STEPS() {
			return (int) (60 / DIFFUSION_TIMESTEP);
		}
		
		/**
		 * Speed of diffusion, used by DiffusionAlgorithm need to specify the units
		 * so we know what we are dealing with
		 */
		public static double DIFFUSION_COEFFICIENT;
		public static double DIFFUSION_COEFFICIENT_PREFIX;
		
		/**
		 * the rate of protein decay per timestep
		 */
		public static double DECAY_CONSTANT;
		
		/**
		 * How much time a single iteration of the diffusion process will take us
		 * forward
		 * 
		 * @see http
		 *      ://physics-server.uoregon.edu/~raghu/TeachingFiles/Winter08Phys352
		 *      /Notes_Diffusion.pdf
		 */
		public static double DIFFUSION_TIMESTEP;
		public static int DIFFUSION_STEPS;
	}
	
	

}
