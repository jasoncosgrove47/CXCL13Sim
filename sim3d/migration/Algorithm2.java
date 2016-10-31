package sim3d.migration;

import java.util.ArrayList;

import sim.util.Bag;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.cell.Lymphocyte;
import sim3d.cell.Lymphocyte.Receptor;
import sim3d.diffusion.Chemokine;
import sim3d.util.ODESolver;
import sim3d.util.Vector3DHelper;

public class Algorithm2 extends Algorithm1{
	
	//less than 1 favours CXCL13, greater than one favours EBI2L
	double signallingBias = Settings.SIGNALLING_BIAS;
	
	public static boolean multipleChemokines = true;
	
	
	
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
	
	
	@Override
	public void performMigration(Lymphocyte lymphocyte) {
		

			Chemokine.TYPE chemokine1 = Chemokine.TYPE.CXCL13;
			Chemokine.TYPE chemokine2 = Chemokine.TYPE.EBI2L;
			lymphocyte.setCollisionCounter(0); // reset the collision counter for this timestep
			lymphocyte.getM_i3lCollisionPoints().clear();

			// if we have a stored move then execute it
			if (lymphocyte.getM_d3aMovements() != null && lymphocyte.getM_d3aMovements().size() > 0) {
				performSavedMovements(lymphocyte);
			}

			calculateWhereToMoveNext(lymphocyte, chemokine1,chemokine2);
			lymphocyte.handleBounce(); // Check for bounces
			
			
			
			ODESolver.solveODE(Settings.BC.ODE.K_a(),Settings.BC.ODE.K_r(),Settings.BC.ODE.K_i(),
					Settings.BC.ODE.Koff,Settings.BC.ODE.Kdes,chemokine1, lymphocyte);
			
			ODESolver.solveODE(Settings.BC.ODE.K_a(),Settings.BC.ODE.K_r(),Settings.BC.ODE.K_i(),
					Settings.BC.ODE.Koff,Settings.BC.ODE.Kdes,chemokine2, lymphocyte);
			
			lymphocyte.registerCollisions(lymphocyte.m_cgGrid); // Register the new movement with the grid
		
	}
	

	
	
	

	public void updateMigrationData(Lymphocyte  bc, Double3D vMovement, double vectorMagnitude, double persistence){
		
		// Reset all the movement/collision data
		bc.getM_d3aCollisions().clear();
		bc.setM_d3aMovements(new ArrayList<Double3D>());

		
		double CXCR5signalling = bc.getM_receptorMap().get(Receptor.CXCR5).get(0);
		double EBI2signalling = bc.getM_receptorMap().get(Receptor.EBI2).get(0);	
			
		double receptorsSignalling = CXCR5signalling + EBI2signalling;
		
		double speedScalar = (((receptorsSignalling / Settings.BC.ODE.Rf))*Settings.BC.SPEED_SCALAR * Settings.BC.TRAVEL_DISTANCE());	
		double travelDistance;
		
		// lets make travelDistance a gaussian for a better fit
		// and constrain it so it cant give a value less than zero
		do {
			travelDistance = Settings.RNG.nextGaussian()
					* Settings.BC.TRAVEL_DISTANCE_SD
					+ Settings.BC.TRAVEL_DISTANCE();

			// only sample within oneSD
		} while (travelDistance <= 0);//must be greater than zero
		
		
		//bc.getM_d3aMovements().add(vMovement.multiply(travelDistance + speedScalar));
		bc.getM_d3aMovements().add(vMovement.multiply(travelDistance));
			
	}
	
	
	

	
	

	
	
	private static Lymphocyte.Receptor setReceptor(Chemokine.TYPE chemokine){
		
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
		
		return receptor;
		
	}
	
	public static double[] calculateLigandBound(Lymphocyte lymphocyte, Chemokine.TYPE chemokine){
		// need to figure out what is sensible to secrete per timestep, might as
				// well do that in moles. Get the surrounding values for moles

		
				Lymphocyte.Receptor receptor = setReceptor(chemokine);
				
		

				double[][][] ia3Concs = Chemokine.get(chemokine, (int) lymphocyte.x,
						(int) lymphocyte.y, (int) lymphocyte.z);

				// Assume the receptors are spread evenly around the cell
				//int iReceptors = lymphocyte.m_iR_free / 6;
				int iReceptors = lymphocyte.getM_LR(receptor)/6;
				
				// get CXCL13 concentrations at each psuedopod
				// {x+, x-, y+, y-, z+, z-}
				double[] iaConcs = { ia3Concs[2][1][1], ia3Concs[0][1][1],
						ia3Concs[1][2][1], ia3Concs[1][0][1], ia3Concs[1][1][2],
						ia3Concs[1][1][0] };

				double totalLigand = 0;
				
				//determine the total amount of ligand
				for (int j = 0; j < 6; j++) // for each pseudopod
				{
					totalLigand += iaConcs[j];
				}
				
				// store how many receptors are bound at each
				// of the 6 pseudopods and store values in a double array
				double[] iaBoundReceptors = new double[6];
				
				
				for(int k = 0;k<6;k++){
					double proportionToBind = 0;	
					proportionToBind = iaConcs[k]/totalLigand;
					iaBoundReceptors[k] = (int) (proportionToBind * iReceptors);	
				}
				
				return iaBoundReceptors;
	}
	
	
	
	
	
	/**
	 * Samples CXCL13 in the vicinity of the cell, and calculates a new movement
	 * direction.
	 * 
	 * @return The new direction for the cell to move
	 * TODO need to account for the different KOs...
	 */
	Double3D getMoveDirection(Lymphocyte lymphocyte,Chemokine.TYPE chemokine1,Chemokine.TYPE chemokine2) {

	

		double[] iaBoundReceptors1 = calculateLigandBound(lymphocyte, chemokine1);
		double[] iaBoundReceptors2 = calculateLigandBound(lymphocyte, chemokine2);
		

		// the new direction for the cell to move
		Double3D vMovement1 = new Double3D();

		// X
		vMovement1 = vMovement1.add(new Double3D(1, 0, 0)
				.multiply(iaBoundReceptors1[0] - iaBoundReceptors1[1]));
		// Y
		vMovement1 = vMovement1.add(new Double3D(0, 1, 0)
				.multiply(iaBoundReceptors1[2] - iaBoundReceptors1[3]));
		// Z
		vMovement1 = vMovement1.add(new Double3D(0, 0, 1)
				.multiply(iaBoundReceptors1[4] - iaBoundReceptors1[5]));
		
		
		// the new direction for the cell to move
		Double3D vMovement2 = new Double3D();

		// X
		vMovement2 = vMovement2.add(new Double3D(1, 0, 0)
				.multiply(iaBoundReceptors2[0] - iaBoundReceptors2[1]));
		// Y
		vMovement2 = vMovement2.add(new Double3D(0, 1, 0)
				.multiply(iaBoundReceptors2[2] - iaBoundReceptors2[3]));
		// Z
		vMovement2 = vMovement2.add(new Double3D(0, 0, 1)
				.multiply(iaBoundReceptors2[4] - iaBoundReceptors2[5]));
		

		
		if(multipleChemokines){
		
			//lets make cxcl13 more potent that ebi2 for the lawls
			//should only be scaled with respect to CXCL13 so dont see why this is having an effect
			Double3D ebi2Scaled = vMovement2.multiply(signallingBias);

			vMovement1 = vMovement1.add(ebi2Scaled);
		
		}
		
		else{
			
			//if there is just one chemokine then we dont need to 
			//worry about scaling. One vector
			//will be zero so its fine to just sum them together. 
			vMovement1 = vMovement1.add(vMovement2);
			
		}
		
		
		return vMovement1;
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
	 * 
	 * 
	 */
	public void calculateWhereToMoveNext(Lymphocyte lymphocyte,Chemokine.TYPE chemokine1,Chemokine.TYPE chemokine2 ) {
		
		
		Double3D vMovement = getMoveDirection(lymphocyte, chemokine1,chemokine2);

		
		double vectorMagnitude = vMovement.lengthSq();
		
		//let's fix persistence to 0.5
		double persistence = 0;
		
		if (vMovement.lengthSq() > 0) {
			if (vectorMagnitude >= Settings.BC.SIGNAL_THRESHOLD) {
				
				
				//System.out.println("cell is chemotactic");
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
			
			//was just used to set speed so need to redefine this function...
			//speaking of which this should be in the model documentation
			persistence = Settings.BC.RANDOM_POLARITY;
			
			//lets try the new way
			Double3D newdirection = Vector3DHelper.getRandomDirectionInCone(lymphocyte.getM_d3Face(),
					Settings.BC.MAX_TURN_ANGLE());
			
			
			// we need to scale this new direction or it will assume the old one is equal
			newdirection = newdirection.multiply(persistence);
			
			//update the direction that the cell is facing 
			// TODO put this back as it was
			//this assumes that the two signals are equal and this is not the case so need to scale it
			vMovement = lymphocyte.getM_d3Face().add(newdirection);
			
			//normalise the vector
			if (vMovement.lengthSq() > 0) {
				vMovement = vMovement.normalize();
			}	
		}
		//update the migration data
		updateMigrationData(lymphocyte, vMovement,vectorMagnitude, persistence);
	}
	
	
	
}
