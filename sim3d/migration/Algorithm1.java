package sim3d.migration;

import java.util.ArrayList;

import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.BC;
import sim3d.util.Vector3DHelper;

public class Algorithm1 {

	
	public Int3D getDiscretizedLocation(Continuous3D grid) {
		Double3D me = grid.getObjectLocation(this);// obtain coordinates of the
													// tcell
		Int3D meDiscrete = grid.discretize(me);
		return meDiscrete;
	}

	/*
	 * moves the BC based on the precomputed trajectory from previous timestep
	 */
	public void performSavedMovements(BC bc) {

		for (Double3D d3Movement : bc.getM_d3aMovements()) {

			bc.x += d3Movement.x;
			bc.y += d3Movement.y;
			bc.z += d3Movement.z;
		}

		// Remember which way we're now facing
		bc.setM_d3Face(bc.getM_d3aMovements().get(bc.getM_d3aMovements().size() - 1).normalize());
		// if space to move then move

		if (determineSpaceToMove(bc.x, bc.y, bc.z)) {
			bc.setObjectLocation(new Double3D(bc.x, bc.y, bc.z));
		}

	}

	/**
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
	 * calculate where to move for the next timestep.
	 */
	public void calculateWhereToMoveNext(BC bc) {
		Double3D vMovement;
		vMovement = getMoveDirection(bc);
		double vectorMagnitude = vMovement.lengthSq();

		if (vMovement.lengthSq() > 0) {
			if (vectorMagnitude >= Settings.BC.SIGNAL_THRESHOLD) {

				// Add some noise to the direction and take the average of our
				// current direction and the new direction
				// the multiply is to scale the new vector, when we multiply by
				// 2 we are favouring the new signal more than the old
				vMovement = bc.getM_d3Face().add(Vector3DHelper
						.getBiasedRandomDirectionInCone(vMovement.normalize(),
								Settings.BC.DIRECTION_ERROR()).multiply(
								Settings.BC.PERSISTENCE));

				// why do we need this line here?
				if (vMovement.lengthSq() > 0) {
					vMovement = vMovement.normalize();
				}
			}

			else {

				// maybe this is where we have to add the persistence
				vMovement = null;
			}
		}

		// we detect no chemokine, or at least difference in chemokine
		else {
			vMovement = null;
		}

		if (vMovement == null || vMovement.lengthSq() == 0) {
			// no data! so do a random turn
			vMovement = Vector3DHelper.getRandomDirectionInCone(bc.getM_d3Face(),
					Settings.BC.RANDOM_TURN_ANGLE());



		}

		// Reset all the movement/collision data
		bc.getM_d3aCollisions().clear();
		bc.setM_d3aMovements(new ArrayList<Double3D>());

		// calculated from maiuri paper in cell 2015
		// Persistence represents the strength of the new vector with
		// respect to where we are now, thus the memory vector is 1/alpha
		// times stronger than the current vector and we use this as a
		// measure of the cell
		double speedScalar = (Math.log(1 / Settings.BC.PERSISTENCE))
				/ Settings.BC.SPEED_SCALAR;

		double travelDistance;
		// lets make travelDistance a gaussian for a better fit
		// and constrain it so it cant give a value less than zero
		do {
			travelDistance = Settings.RNG.nextGaussian()
					* Settings.BC.TRAVEL_DISTANCE_SD
					+ Settings.BC.TRAVEL_DISTANCE();

			// only sample within oneSD
		} while (travelDistance <= 0);

		// if there is some signalling then the cell increases it's
		// instantaneous velocity
		if (vectorMagnitude > Settings.BC.SIGNAL_THRESHOLD) {

			bc.getM_d3aMovements()
					.add(vMovement.multiply(travelDistance + speedScalar));
			// m_d3aMovements.add(vMovement.multiply(travelDistance));
			// this is also the case if receptors are saturated or equally
			// biased
			// in each direction, still signalling going on but must be a
			// minimum threshold

		} else if (vectorMagnitude < Settings.BC.SIGNAL_THRESHOLD
				&& bc.m_iL_r > Settings.BC.SIGNAL_THRESHOLD) {

			// m_d3aMovements.add(vMovement.multiply(travelDistance));
			bc.getM_d3aMovements()
					.add(vMovement.multiply(travelDistance + speedScalar));

		} else {
			// no signalling therefore no increase in instantaneous velocity
			bc.getM_d3aMovements().add(vMovement.multiply(travelDistance));
		}

	}

	
	/**
	 * 
	 * Samples CXCL13 in the vicinity of the cell, and calculates a new movement
	 * direction. Also removes some CXCL13 from the simulation.
	 * 
	 * @return The new direction for the cell to move
	 */
	Double3D getMoveDirection(BC bc) {


		double[] iaBoundReceptors = bc.calculateLigandBindingMolar();

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

	
	
	
	
	
}
