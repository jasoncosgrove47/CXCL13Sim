package sim3d.migration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.cell.Lymphocyte;
import sim3d.cell.TC;
import sim3d.cell.cognateBC;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;
import sim3d.util.Vector3DHelper;

public class Algorithm1 implements MigrationAlgorithm{

	
	/**
	 * The algorithm which dictates cell migration
	 * An adaptation of the scheme developed by Lin et al
	 * 
	 * Implemented using the visitor design pattern so 
	 * we can use different migration algorithms if required
	 * 
	 * 
	 * @author Jason Cosgrove
	 * 
	 * TODO conclusion will need to be updated
	 */
	@Override
	public void performMigration(Lymphocyte lymphocyte) {
		


			Chemokine.TYPE chemokine = Chemokine.TYPE.CXCL13;
			
			if(lymphocyte instanceof TC){

				chemokine = Chemokine.TYPE.CCL19;				
			}

			
			lymphocyte.setCollisionCounter(0); // reset the collision counter for this timestep
			lymphocyte.getM_i3lCollisionPoints().clear();

			// if we have a stored move then execute it
			if (lymphocyte.getM_d3aMovements() != null && lymphocyte.getM_d3aMovements().size() > 0) {
				performSavedMovements(lymphocyte);
			}

			calculateWhereToMoveNext(lymphocyte, chemokine);
			lymphocyte.handleBounce(); // Check for bounces
			receptorStep(lymphocyte, chemokine);
			lymphocyte.registerCollisions(lymphocyte.m_cgGrid); // Register the new movement with the grid
			
			
		

	}
	
	
	/*
	 * moves the BC based on the precomputed trajectory from previous timestep
	 * 
	 * TODO shouldnt need to be updated
	 */
	public void performSavedMovements(Lymphocyte lymphocyte) {

		for (Double3D d3Movement : lymphocyte.getM_d3aMovements()) {

			lymphocyte.x += d3Movement.x;
			lymphocyte.y += d3Movement.y;
			lymphocyte.z += d3Movement.z;
		}

		// Remember which way we're now facing
		lymphocyte.setM_d3Face(lymphocyte.getM_d3aMovements().get(
				lymphocyte.getM_d3aMovements().size() - 1).normalize());
		
		
		// if space to move then move
		if (determineSpaceToMove(lymphocyte.x, lymphocyte.y, lymphocyte.z)) {
			lymphocyte.setObjectLocation(new Double3D(lymphocyte.x, 
					lymphocyte.y, lymphocyte.z));
		}

	}

	/**
	 * Determines if there is space to move to a target destination
	 * at coordinates x,y,z.
	 * 
	 * The decision to move is probabilistic with the threshold
	 * determined by e^-y where y is the number of cells in the
	 * target gridspace.
	 * 
	 * TODO might need to update
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean determineSpaceToMove(double x, double y, double z) {
		Double3D putativeLocation = new Double3D(x, y, z);

		// see if there are any cells at the putative location
		// 0.7 represents the size of a cell
		Bag cells = BC.bcEnvironment.getNeighborsExactlyWithinDistance(
				putativeLocation, 0.7);

		// need to do cells minus one as it includes this cell
		int otherCells = cells.numObjs - 1;

		// need to account for in
		double pmove = Math.exp(-otherCells);

		double random = Settings.RNG.nextDouble();

		if (random < pmove) {
			return true;
		}

		else
			return false;
	}

	
	
	/**
	 * 
	 * TODO this will need to be updated substantially
	 * 
	 * calculate where to move for the next timestep.
	 * The algorithm determines the chemotactic vector
	 * of the cell using the approach developed by
	 * Lin et al. If the magnitude of the vector
	 * (which represents the difference in number of 
	 * signalling receptors exceeds the signal threshold
	 * the cell will move chemotactically with respect to
	 * a persistence vector representing the direction of the
	 * cell from the previous timestep.
	 * 
	 * If the magnitude does not exceed the vector then it undergoes
	 * a random walk with respect to a persistence vector, which is
	 * not as strong as when the cell undergoes chemotaxis due to more
	 * localised actin localisation.
	 * 
	 * Once all these calculations have been performed the
	 * cell updates its movements array (M_d3aMovements) so the movements
	 * can be performed at the next timestep
	 */
	public void calculateWhereToMoveNext(Lymphocyte lymphocyte,Chemokine.TYPE chemokine) {
		Double3D vMovement = getMoveDirection(lymphocyte, chemokine);
		double vectorMagnitude = vMovement.lengthSq();
		
		//let's fix persistence to 0.5
		double persistence = 0;
		
		if (vMovement.lengthSq() > 0) {
			if (vectorMagnitude >= Settings.BC.SIGNAL_THRESHOLD) {
				
				//if there's sufficient directional bias
				//can affect cell polarity
				persistence = Settings.BC.POLARITY;
				
				// Add some noise to the signal
				Double3D newdirection = Vector3DHelper
						.getRandomDirectionInCone(vMovement.normalize(),
								Math.toRadians(2));
					
				//  scale the new vector with respect to the old vector,
				// values less than 1 favour the old vector, values greater than 1 favour the new vector
				// this is constrained between 0 and 2
				newdirection = newdirection.multiply(persistence);
				
				//update the direction that the cell is facing
				vMovement = lymphocyte.getM_d3Face().add(newdirection);
				
				//remember that this is half of the amount of noise that you actually want!
				//TODO - a try catch that the input is less than 90 degrees!! error handling
				vMovement = Vector3DHelper
					.getRandomDirectionInCone(vMovement.normalize(),
							Settings.BC.DIRECTION_ERROR());

				//normalise the vector
				if (vMovement.lengthSq() > 0) {
					vMovement = vMovement.normalize();
				}
			}
			else {
				vMovement = null;
			}
		}
		// we detect no chemokine, or at least difference in chemokine, TODO if no difference in 
		// chemokine you still speed up! needs thinking
		else {
			vMovement = null;
		}

		if (vMovement == null || vMovement.lengthSq() == 0) {
			// no data! so do a random turn
			
			//TODO from what i remember this was between 0.5-2
			//was just used to set speed so need to redefine this function...
			//speaking of which this should be in the model documentation
			persistence = Settings.BC.RANDOM_POLARITY;
			
			//this was the old bit of code to do it
			//vMovement = Vector3DHelper.getRandomDirectionInCone(bc.getM_d3Face(),
			//		Settings.BC.RANDOM_TURN_ANGLE());
			
			//lets try the new way
			Double3D newdirection = Vector3DHelper.getRandomDirectionInCone(lymphocyte.getM_d3Face(),
					Settings.BC.MAX_TURN_ANGLE());
			
			newdirection = newdirection.multiply(persistence);
			
			//update the direction that the cell is facing
			vMovement = lymphocyte.getM_d3Face().add(newdirection);
			
			//TODO review this code, we may not need it...
			//now add noise to this
			//vMovement = Vector3DHelper
			//		.getRandomDirectionInCone(vMovement.normalize(),
			//				Settings.BC.DIRECTION_ERROR());
			//normalise the vector
			if (vMovement.lengthSq() > 0) {
				vMovement = vMovement.normalize();
			}	
		}
		
		
		//update the migration data
		updateMigrationData(lymphocyte, vMovement,vectorMagnitude, persistence);
	}
	
	
	
	
	public void updateMigrationData(Lymphocyte  bc, Double3D vMovement, double vectorMagnitude, double persistence){
		
		// Reset all the movement/collision data
		bc.getM_d3aCollisions().clear();
		bc.setM_d3aMovements(new ArrayList<Double3D>());

		// We make speed a function of cell polarity
		// speed scalar will be zero if persistence 
		// is equal to 1. calculated from maiuri paper in cell 2015
		// TODO make this a parameter called polarityscalar
	
		

	
		double speedScalar = (Math.log(Settings.BC.RANDOM_POLARITY / persistence))
				/ Settings.BC.SPEED_SCALAR;

		double travelDistance;
		
		
		
		//TODO this is quite an ugly bit of code, needs some rethinking...
		
		// lets make travelDistance a gaussian for a better fit
		// and constrain it so it cant give a value less than zero
		do {
			travelDistance = Settings.RNG.nextGaussian()
					* Settings.BC.TRAVEL_DISTANCE_SD
					+ Settings.BC.TRAVEL_DISTANCE();

			// only sample within oneSD
		} while (travelDistance <= 0);//must be greater than zero
		
		
		//TODO may need to put this back if cant calibrate without the speedscalar

		bc.getM_d3aMovements().add(vMovement.multiply(travelDistance + speedScalar));
	
			
	}
	
	

	
	/**
	 * Perform a step for the receptor
	 * 
	 * TODO lots of repetition in this class, need a set receptor method
	 * 
	 * 
	 */
	void receptorStep(Lymphocyte lymphocyte, Chemokine.TYPE chemokine) {
		double[] iaBoundReceptors = calculateLigandBindingMolar(lymphocyte,chemokine);

		
		Lymphocyte.Receptor receptor = null;
		
		switch (chemokine) {
		case CXCL13: 
			receptor = Lymphocyte.Receptor.CXCR5;
			break;
		case CCL19:
			receptor = Lymphocyte.Receptor.CCR7;	
			break;
			
		case EBI2L:
			receptor = Lymphocyte.Receptor.EBI2;	
			break;
		default:
			break;
	
		}
		
		// update the amount of free and bound receptors
		for (int i = 0; i < 6; i++) {
			
			lymphocyte.setM_Rf(receptor,lymphocyte.getM_Rf(receptor) - (int) iaBoundReceptors[i] );
			lymphocyte.setM_LR(receptor,lymphocyte.getM_LR(receptor) + (int)iaBoundReceptors[i] );
			
		}

		//sim timestep increments in 1 min intervals so divide by 60 to get it in seconds.
		int iTimesteps = 60; 
		int iR_i, iL_r;
		double h = 1; // the parameters are already in seconds so don't need to
						// scale them

		double Ki = Settings.BC.ODE.K_i();// Ka is already in seconds
	
		
		double Kr = Settings.BC.ODE.K_r();
		double Koff = Settings.BC.ODE.Koff;
		/**
		 * Solve the ODE using 4th order Runge Kutta timestep j equals 1 second
		 */

		for (int i = 0; i < iTimesteps; i++) {

			iR_i = lymphocyte.getM_Ri(receptor);
			//System.out.println("iR_i: " + iR_i);
			
			iL_r = lymphocyte.getM_LR(receptor);
			//System.out.println("iL_r: " + iL_r);
			// receptors internalised from surface
			double LRK1 = h * (Ki * iL_r);
			double LRK2 = h * ((Ki * iL_r) + LRK1 / 2);
			double LRK3 = h * ((Ki * iL_r) + LRK2 / 2);
			double LRK4 = h * ((Ki * iL_r) + LRK3);

			// receptors that are recycled from internal pool
			double RfK1 = h * (Kr * iR_i);
			double RfK2 = h * ((Kr * iR_i) + RfK1 / 2);
			double RfK3 = h * ((Kr * iR_i) + RfK2 / 2);
			double RfK4 = h * ((Kr * iR_i) + RfK3);

			// ligand dissociation from receptor
			// receptors that are recycled from internal pool
			double RdisK1 = h * (Koff * iL_r);
			double RdisK2 = h * ((Koff * iL_r) + RdisK1 / 2);
			double RdisK3 = h * ((Koff * iL_r) + RdisK2 / 2);
			double RdisK4 = h * ((Koff * iL_r) + RdisK3);

			
		
			//System.out.println("Rf: " + lymphocyte.getM_Rf(receptor));
			//System.out.println("LR: " + lymphocyte.getM_LR(receptor));
			//System.out.println("Ri: " + lymphocyte.getM_Ri(receptor));
			
			
			int d_rf = lymphocyte.getM_Rf(receptor);
			int d_ri = lymphocyte.getM_Ri(receptor);
			//System.out.println("d_ri: " + d_ri);
			
			int d_lr = lymphocyte.getM_LR(receptor);
			
			
			d_rf += (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6))
					+ (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6));
			
			d_ri += (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6))
					- (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));
			
			d_lr -= (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6))
					+ (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6));
			
			
			
			lymphocyte.setM_Rf(receptor, d_rf);
			lymphocyte.setM_Ri(receptor, d_ri);
			lymphocyte.setM_LR(receptor, d_lr);
			
			
			/*
			lymphocyte.m_iR_free += (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6))
					+ (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6));
					
					
			lymphocyte.m_iR_i += (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6))
					- (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));
					
			lymphocyte.m_iL_r -= (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6))
					+ (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6));
			*/																		

	
					
			
		}

	}

	public void consumeLigand(Lymphocyte lymphocyte, Chemokine.TYPE chemokine) {
		
		double x = lymphocyte.x;
		double y = lymphocyte.y;
		double z = lymphocyte.z;
		
		double[] iaBoundReceptors = calculateLigandBindingMoles(lymphocyte, chemokine);

		// avogadors number - number of molecules in 1 mole
		double avogadro = 6.0221409e+23;

		// this is in moles, not receptors so need to scale it before i remove,
		// eg if i took away 10,000 that would be 10,000 moles which is not what
		// we want!!!

		Chemokine.add(chemokine, (int) x + 1, (int) y, (int) z,
				-(iaBoundReceptors[0] / avogadro));
		Chemokine.add(chemokine, (int) x - 1, (int) y, (int) z,
				-(iaBoundReceptors[1] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y + 1, (int) z,
				-(iaBoundReceptors[2] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y - 1, (int) z,
				-(iaBoundReceptors[3] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y, (int) z + 1,
				-(iaBoundReceptors[4] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y, (int) z - 1,
				-(iaBoundReceptors[5] / avogadro));

	}

	/**
	 * 
	 * Samples CXCL13 in the vicinity of the cell, and calculates a new movement
	 * direction.
	 * 
	 * @return The new direction for the cell to move
	 * 
	 */
	Double3D getMoveDirection(Lymphocyte lymphocyte,Chemokine.TYPE chemokine) {


		double[] iaBoundReceptors = calculateLigandBindingMolar(lymphocyte, chemokine);

		// the new direction for the cell to move
		Double3D vMovement = new Double3D();

		// X
		vMovement = vMovement.add(new Double3D(1, 0, 0)
				.multiply(iaBoundReceptors[0] - iaBoundReceptors[1]));
		// Y
		vMovement = vMovement.add(new Double3D(0, 1, 0)
				.multiply(iaBoundReceptors[2] - iaBoundReceptors[3]));
		// Z
		vMovement = vMovement.add(new Double3D(0, 0, 1)
				.multiply(iaBoundReceptors[4] - iaBoundReceptors[5]));

		return vMovement;
	}
	

	
	

	
	/**
	 * Helper method to calculate the amount of ligand bound in moles to 
	 * receptor. Need this because parameter Ka is moles/litre/sec 
	 * @return an int array with the number of bound receptors at each psuedopod
	 */
	public double[] calculateLigandBindingMolar(Lymphocyte lymphocyte, Chemokine.TYPE chemokine) {

		Lymphocyte.Receptor receptor = null;
		
		switch (chemokine) {
		case CXCL13: 
			receptor = Lymphocyte.Receptor.CXCR5;
			break;
		case CCL19:
			receptor = Lymphocyte.Receptor.CCR7;	
			break;
			
		case EBI2L:
			receptor = Lymphocyte.Receptor.EBI2;	
			break;
		default:
			break;
	
		}
		
		
		double[][][] ia3Concs = Chemokine.get(chemokine, (int) lymphocyte.x,
				(int) lymphocyte.y, (int) lymphocyte.z);

		// Assume the receptors are spread evenly around the cell
		//int iReceptors = lymphocyte.m_iR_free / 6;

		int iReceptors = lymphocyte.getM_Rf(receptor)/6;
		
		
		// would need to divide by 1e-12 L (vol of one grid space to get molar
		// conc)
		double vol = 1e-12;// volume of one gridspace in litres

		// get CXCL13 concentrations at each psuedopod
		// {x+, x-, y+, y-, z+, z-}

		double[] iaConcs = { ia3Concs[2][1][1] / vol, ia3Concs[0][1][1] / vol,
				ia3Concs[1][2][1] / vol, ia3Concs[1][0][1] / vol,
				ia3Concs[1][1][2] / vol, ia3Concs[1][1][0] / vol };

		// store how many receptors are bound at each
		// of the 6 pseudopods
		double[] iaBoundReceptors = new double[6];

		for (int i = 0; i < 6; i++) // for each pseudopod
		{

			double proportionToBind = 0;

			for (int j = 0; j < 60; j++) {

				double h = 1; // want to update the equation every second so use
								// 1 / 60

				// Ka = /moles/litre/second
				double Ka = Settings.BC.ODE.K_a();

				double RfK1 = h * (Ka * iaConcs[i]);
				double RfK2 = h * ((Ka * iaConcs[i]) + RfK1 / 2);
				double RfK3 = h * ((Ka * iaConcs[i]) + RfK2 / 2);
				double RfK4 = h * ((Ka * iaConcs[i]) + RfK3);

				// the total change in bound receptor for this time increment is
				// given b this equation
				proportionToBind += ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));

			}

			// cap the amount of receptors that can be bound
			if (proportionToBind > 1) {
				proportionToBind = 1;
			}
			if (proportionToBind < 0) {
				proportionToBind = 0;
			}

			// not sure about this casting, need to make sure that it is ok
			iaBoundReceptors[i] = (int) (proportionToBind * iReceptors);

		}

		consumeLigand(lymphocyte, chemokine);

		return iaBoundReceptors;
	}

	/**
	 * Helper method to calculate the amount of ligand bound to receptor returns
	 * an int array with the number of bound receptors at each psuedopod
	 * 
	 * Updated version for the rungekutta method
	 * 
	 * @return
	 */
	public double[] calculateLigandBindingMoles(Lymphocyte lymphocyte, Chemokine.TYPE chemokine) {

		// need to figure out what is sensible to secrete per timestep, might as
		// well do that in moles. Get the surrounding values for moles

		Lymphocyte.Receptor receptor = null;
		
		switch (chemokine) {
		case CXCL13: 
			receptor = Lymphocyte.Receptor.CXCR5;
			break;
		case CCL19:
			receptor = Lymphocyte.Receptor.CCR7;	
			break;
			
		case EBI2L:
			receptor = Lymphocyte.Receptor.EBI2;	
			break;
		default:
			break;
		}

		double[][][] ia3Concs = Chemokine.get(chemokine, (int) lymphocyte.x,
				(int) lymphocyte.y, (int) lymphocyte.z);

		// Assume the receptors are spread evenly around the cell
		//int iReceptors = lymphocyte.m_iR_free / 6;

		int iReceptors = lymphocyte.getM_Rf(receptor)/6;
		
		
		// get CXCL13 concentrations at each psuedopod
		// {x+, x-, y+, y-, z+, z-}
		double[] iaConcs = { ia3Concs[2][1][1], ia3Concs[0][1][1],
				ia3Concs[1][2][1], ia3Concs[1][0][1], ia3Concs[1][1][2],
				ia3Concs[1][1][0] };

		// store how many receptors are bound at each
		// of the 6 pseudopods
		double[] iaBoundReceptors = new double[6];

		for (int i = 0; i < 6; i++) // for each pseudopod
		{

			double proportionToBind = 0;

			for (int j = 0; j < 60; j++) {

				
				//want to update the equation every second so use
				// 1/60
				double h = 1; 

				// Ka = /moles/litre/second
				double Ka = Settings.BC.ODE.K_a();

				double RfK1 = h * (Ka * iaConcs[i]);
				double RfK2 = h * ((Ka * iaConcs[i]) + RfK1 / 2);
				double RfK3 = h * ((Ka * iaConcs[i]) + RfK2 / 2);
				double RfK4 = h * ((Ka * iaConcs[i]) + RfK3);

				// the total change in bound receptor for this time increment is
				// given b this equation
				proportionToBind += ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));

			}

			// cap the amount of receptors that can be bound
			if (proportionToBind > 1) {
				proportionToBind = 1;
			}
			if (proportionToBind < 0) {
				proportionToBind = 0;
			}

			// not sure about this casting, need to make sure that it is ok
			iaBoundReceptors[i] = (int) (proportionToBind * iReceptors);

		}
		return iaBoundReceptors;
	}
}
