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
	 * @authors: Jason cosgrove, Steph Dyson
	 */

	/**
	 * Saves the FDC network so we can load it into another simulation
	 * 
	 * @param simulation
	 */
	public void writeFDC(SimulationEnvironment simulation) {
		try { // Catch errors in I/O if necessary.
				// Open a file to write to, named SavedObj.sav.
			FileOutputStream saveFile = new FileOutputStream(
					"/Users/jc1571/Desktop/fdcoutput.sav");

			// Create an ObjectOutputStream to put objects into save file.
			ObjectOutputStream save = new ObjectOutputStream(saveFile);

			save.writeObject(simulation.fdcEnvironment);

			// Close the file.
			save.close(); // This also closes saveFile.
		} catch (Exception exc) {
			exc.printStackTrace(); // If there was an error, print the info.

		}
	}

	/**
	 * Saves the B-cell grid so we can load it into another simulation
	 * 
	 * @param simulation
	 */
	public void writeBC(SimulationEnvironment simulation) {
		try { // Catch errors in I/O if necessary.
				// Open a file to write to, named SavedObj.sav.
			FileOutputStream saveFile = new FileOutputStream(
					"/Users/jc1571/Desktop/bcoutput.sav");

			// Create an ObjectOutputStream to put objects into save file.
			ObjectOutputStream save = new ObjectOutputStream(saveFile);
			save.writeObject(BC.bcEnvironment);

			// Close the file.
			save.close(); // This also closes saveFile.
		} catch (Exception exc) {
			exc.printStackTrace(); // If there was an error, print the info.

		}
	}

	/**
	 * Saves the CXCL13 grid so it can be loaded into another simulation.
	 * 
	 * @param simulation
	 */
	public void writeCXCL13(SimulationEnvironment simulation) {
		try { // Catch errors in I/O if necessary.
				// Open a file to write to, named SavedObj.sav.
			FileOutputStream saveFile = new FileOutputStream(
					"/Users/jc1571/Desktop/cxcl13output.sav");

			// Create an ObjectOutputStream to put objects into save file.
			ObjectOutputStream save = new ObjectOutputStream(saveFile);
			save.writeObject(SimulationEnvironment.particlemoles);// TODO this
																	// needs to
																	// be
			// the particle grid
			// Close the file.
			save.close(); // This also closes saveFile.
		} catch (Exception exc) {
			exc.printStackTrace(); // If there was an error, print the info.

		}
	}

}
