package sim3d.migration;

import java.util.ArrayList;

import sim.util.Double3D;
import sim3d.Settings;
import sim3d.cell.Lymphocyte;
import sim3d.cell.Lymphocyte.Receptor;
import sim3d.diffusion.Chemokine;
import sim3d.util.Vector3DHelper;

public class Algorithm2 extends Algorithm1{
	
	
	

	
	
	//TODO need to comment on how this works and determine what the best model is
	//less than 1 favours CXCL13, greater than one favours EBI2L
	double signallingBias = Settings.SIGNALLING_BIAS;
	
	public static boolean multipleChemokines = true;
	
	
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
			receptorStep2(lymphocyte, chemokine1);
			receptorStep2(lymphocyte, chemokine2);
			lymphocyte.registerCollisions(lymphocyte.m_cgGrid); // Register the new movement with the grid
		
	}
	

	

	/**
	public void updateMigrationData2(Lymphocyte  bc, Double3D vMovement, double vectorMagnitude, double persistence, double scalar){
		
		// Reset all the movement/collision data
		bc.getM_d3aCollisions().clear();
		bc.setM_d3aMovements(new ArrayList<Double3D>());

		// We make speed a function of cell polarity
		// speed scalar will be zero if persistence 
		// is equal to 1. calculated from maiuri paper in cell 2015
		double speedScalar = (Math.log(Settings.BC.RANDOM_POLARITY / persistence))
				/ scalar;

	
		
		double travelDistance;
		

		// lets make travelDistance a gaussian for a better fit
		// and constrain it so it cant give a value less than zero
		do {
			travelDistance = Settings.RNG.nextGaussian()
					* Settings.BC.TRAVEL_DISTANCE_SD
					+ Settings.BC.TRAVEL_DISTANCE();

			// only sample within oneSD
		} while (travelDistance <= 0);//must be greater than zero
		
		
	
		bc.getM_d3aMovements().add(vMovement.multiply(travelDistance + speedScalar));
	
			
	}
	
	*/
	
	
	@Override
	public void updateMigrationData(Lymphocyte  bc, Double3D vMovement, double vectorMagnitude, double persistence){
		
		// Reset all the movement/collision data
		bc.getM_d3aCollisions().clear();
		bc.setM_d3aMovements(new ArrayList<Double3D>());

		// We make speed a function of cell polarity
		// speed scalar will be zero if persistence 
		// is equal to 1. calculated from maiuri paper in cell 2015
		//double speedScalar = (Math.log(Settings.BC.RANDOM_POLARITY / persistence))
		//		/ Settings.BC.SPEED_SCALAR;

		
		
		double CXCR5signalling = bc.getM_receptorMap().get(Receptor.CXCR5).get(0);
		double EBI2signalling = bc.getM_receptorMap().get(Receptor.EBI2).get(0);	
				
		double CXCR5internal = bc.getM_receptorMap().get(Receptor.CXCR5).get(2);
		double EBI2internal = bc.getM_receptorMap().get(Receptor.EBI2).get(2);	
		
		double receptorsInternal = CXCR5internal + EBI2internal;
		double receptorsSignalling = CXCR5signalling + EBI2signalling;
		
		System.out.println("receptorsInternal: " + receptorsInternal);
		System.out.println("receptorsSignalling: " + receptorsSignalling);
		
		//double receptors = calculateReceptorsSignalling(bc)
	
		//careful cos current implementation will affect sensitivity of the parameters
		//needs to be scaled and what have you
		double speedScalar = (((receptorsSignalling / Settings.BC.ODE.Rf))*12.0 * Settings.BC.TRAVEL_DISTANCE());
		
		
		//System.out.println("r_percent: " + (receptorsSignalling / Settings.BC.ODE.Rf));
		
		//System.out.println("speedScalar: " + speedScalar);
		
		double travelDistance;
		

		// lets make travelDistance a gaussian for a better fit
		// and constrain it so it cant give a value less than zero
		do {
			travelDistance = Settings.RNG.nextGaussian()
					* Settings.BC.TRAVEL_DISTANCE_SD
					+ Settings.BC.TRAVEL_DISTANCE();

			// only sample within oneSD
		} while (travelDistance <= 0);//must be greater than zero
		
		
	
		bc.getM_d3aMovements().add(vMovement.multiply(travelDistance + speedScalar));
	
			
	}
	
	
	Double calculateReceptorsSignalling(Lymphocyte lymphocyte,Chemokine.TYPE chemokine1,Chemokine.TYPE chemokine2){
		
		double[] iaBoundReceptors1 = calculateLigandBindingMolar(lymphocyte, chemokine1);
		double[] iaBoundReceptors2 = calculateLigandBindingMolar(lymphocyte, chemokine2);
		
		double signallingReceptors = 0;
		
		//need to calculate the totalnumber of receptors
		//to determine the increase in cell velocity
		for(int i =0; i < iaBoundReceptors1.length;i++){
			signallingReceptors +=  iaBoundReceptors1[i] + iaBoundReceptors2[i];
			
		}
		
		return signallingReceptors;
		
	}
	
	void receptorStep2(Lymphocyte lymphocyte, Chemokine.TYPE chemokine) {
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
		int iR_i, iL_r, iR_d;
		double h = 1; // the parameters are already in seconds so don't need to
						// scale them

		double Ki = Settings.BC.ODE.K_i();// Ka is already in seconds
	
		
		double Kr = Settings.BC.ODE.K_r();
		double Koff = Settings.BC.ODE.Koff;
		
		//rate of receptor desensitisation...
		double Kdes =  0.015;
		
		/**
		 * Solve the ODE using 4th order Runge Kutta timestep j equals 1 second
		 */

		for (int i = 0; i < iTimesteps; i++) {

			iR_i = lymphocyte.getM_Ri(receptor);
			//System.out.println("iR_i: " + iR_i);
			
			iL_r = lymphocyte.getM_LR(receptor);
			//System.out.println("iL_r: " + iL_r);
			
			iR_d = lymphocyte.getM_Rd(receptor);
			
			// receptors desensitised from surface
			double LRdK1 = h * (Kdes * iL_r);
			double LRdK2 = h * ((Kdes * iL_r) + LRdK1 / 2);
			double LRdK3 = h * ((Kdes * iL_r) + LRdK2 / 2);
			double LRdK4 = h * ((Kdes * iL_r) + LRdK3);

			
			// receptors internalised from surface
			double LRK1 = h * (Ki * iR_d);
			double LRK2 = h * ((Ki * iR_d) + LRK1 / 2);
			double LRK3 = h * ((Ki * iR_d) + LRK2 / 2);
			double LRK4 = h * ((Ki * iR_d) + LRK3);
			
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
			
			int d_rd = lymphocyte.getM_Rd(receptor);
			
			
			
			//receptors recycled + ligand dissociation
			d_rf += (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6))
					+ (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6));
			
			//receptors internalised - receptors recycled....
			d_ri += (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6))
					- (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));
			
			// ligand dissociation - receptor desensitisation
			d_lr -=  (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6))
					+ (int) ((LRdK1 / 6) + (LRdK2 / 3) + (LRdK3 / 3) + (LRdK4 / 6));
			
			//receptors desensitised - receptors internalised
			d_rd += (int) ((LRdK1 / 6) + (LRdK2 / 3) + (LRdK3 / 3) + (LRdK4 / 6))
					- (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6));
			
			lymphocyte.setM_Rf(receptor, d_rf);
			lymphocyte.setM_Ri(receptor, d_ri);
			lymphocyte.setM_LR(receptor, d_lr);
			lymphocyte.setM_Rd(receptor, d_rd);
			
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
	
	
	
	
	/**
	 * 
	 * Samples CXCL13 in the vicinity of the cell, and calculates a new movement
	 * direction.
	 * 
	 * @return The new direction for the cell to move
	 * 
	 * TODO need to account for the different KOs...
	 * 
	 * 
	 */
	Double3D getMoveDirection(Lymphocyte lymphocyte,Chemokine.TYPE chemokine1,Chemokine.TYPE chemokine2) {

		
		double[] iaBoundReceptors1 = calculateLigandBindingMolar(lymphocyte, chemokine1);
		double[] iaBoundReceptors2 = calculateLigandBindingMolar(lymphocyte, chemokine2);
		

		
		
		

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
		

		
		//TODO if its a KO then we dont need to scale the responses...
		
		//System.out.println("ebi2Before: " + vMovement2);
		
		
		
		if(multipleChemokines){
		
		
		//lets make cxcl13 more potent that ebi2 for the lawls
			//should only be scaled with respect to CXCL13 so dont see why this is having an effect
		Double3D ebi2Scaled = vMovement2.multiply(signallingBias);
		
		//System.out.println("ebi2Scaled: " + ebi2Scaled);
		//System.out.println("vMoveBefore" + vMovement1);
		
		vMovement1 = vMovement1.add(ebi2Scaled);
		
		}
		
		else{
			
			//if there is just one chemokine then we dont need to worry about scaling. One vector
			//will be zero so its fine to just sum them together. 
			vMovement1 = vMovement1.add(vMovement2);
			
		}
		

		//System.out.println("vMoveAfter" + vMovement1);
		
		//TODO what happens when we have the ebi2KO
		
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
			//persistence = Settings.BC.POLARITY;
			
			//this was the old bit of code to do it
			//vMovement = Vector3DHelper.getRandomDirectionInCone(bc.getM_d3Face(),
			//		Settings.BC.RANDOM_TURN_ANGLE());
			
			//lets try the new way
			Double3D newdirection = Vector3DHelper.getRandomDirectionInCone(lymphocyte.getM_d3Face(),
					Settings.BC.MAX_TURN_ANGLE());
			
			//newdirection = newdirection.multiply(persistence);
			
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
	
	
	
}
