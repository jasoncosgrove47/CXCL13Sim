package dataLogger;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import sim3d.SimulationEnvironment;
import sim3d.cell.BC;
import sim3d.diffusion.ParticleMoles;

public class WriteObjects {
	/**
	 * Write a simulation out to a file so we can ready in steady states at a
	 * later point
	 * 
	 * @param simulation
	 *            the controller for the simulation - Simulation Environment
	 * 
	 *            Authors: Jason cosgrove, Steph Dyson
	 */

	public void writeFDC(SimulationEnvironment simulation) {
		try { // Catch errors in I/O if necessary.
				// Open a file to write to, named SavedObj.sav.
			FileOutputStream saveFile = new FileOutputStream(
					"/Users/jc1571/Desktop/fdcoutput.sav");

			// Create an ObjectOutputStream to put objects into save file.
			ObjectOutputStream save = new ObjectOutputStream(saveFile);

			save.writeObject(simulation.fdcEnvironment);// TODO this needs to be
														// the particle grid

			// Close the file.
			save.close(); // This also closes saveFile.
		} catch (Exception exc) {
			exc.printStackTrace(); // If there was an error, print the info.

		}
	}
	
	
	
	public void writeBC(SimulationEnvironment simulation) {
		try { // Catch errors in I/O if necessary.
				// Open a file to write to, named SavedObj.sav.
			FileOutputStream saveFile = new FileOutputStream(
					"/Users/jc1571/Desktop/bcoutput.sav");

			// Create an ObjectOutputStream to put objects into save file.
			ObjectOutputStream save = new ObjectOutputStream(saveFile);
			save.writeObject(BC.bcEnvironment);// TODO this needs to be
														// the particle grid
			// Close the file.
			save.close(); // This also closes saveFile.
		} catch (Exception exc) {
			exc.printStackTrace(); // If there was an error, print the info.

		}
	}

	
	
	public void writeCXCL13(SimulationEnvironment simulation) {
		try { // Catch errors in I/O if necessary.
				// Open a file to write to, named SavedObj.sav.
			FileOutputStream saveFile = new FileOutputStream(
					"/Users/jc1571/Desktop/cxcl13output.sav");

			
	
			
			// Create an ObjectOutputStream to put objects into save file.
			ObjectOutputStream save = new ObjectOutputStream(saveFile);
			save.writeObject(simulation.particlemoles);// TODO this needs to be
														// the particle grid
			// Close the file.
			save.close(); // This also closes saveFile.
		} catch (Exception exc) {
			exc.printStackTrace(); // If there was an error, print the info.

		}
	}


	/*
	 * 3) In the class that you want to write out, use this method (and change
	 * step== to whenever you want to write it):
	 * 
	 * if(step ==1440*20 && !IBDSim.restore){ WriteObjects wo= new
	 * WriteObjects(); wo.writeColon(simulation); }
	 * 
	 * 4) To restore a grid you just need to use this in your main class:
	 * colon=(Colon) ReadObjects.restoreColon();
	 */
}
