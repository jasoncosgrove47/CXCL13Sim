package dataLogger;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import sim.field.continuous.Continuous3D;

public class ReadObjects {

	public static Object restoreFDC() {

		Continuous3D fdcEnvironment = null;

		try {
			// Open file to read from, named SavedObj.sav.
			//need to do this once for each class
			FileInputStream saveFile = new FileInputStream(
					"/Users/jc1571/Desktop/fdcoutput.sav");

			// Create an ObjectInputStream to get objects from save file.
			ObjectInputStream save = new ObjectInputStream(saveFile);

			// Now we do the restore.
			// readObject() returns a generic Object, we cast those back
			// into their original class type.
			// For primitive types, use the corresponding reference class.
			fdcEnvironment = (Continuous3D) save.readObject();
			// Close the file.
			save.close(); // This also closes saveFile.
		} catch (Exception exc) {
			exc.printStackTrace(); // If there was an error, print the info.
		}

		return fdcEnvironment;
	}

	/*
	 * 3) In the class that you want to write out, use this method (and change
	 * step== to whenever you want to write it):
	 * 
	 * if(step ==1440*20 && !IBDSim.restore){ 
	 * 	WriteObjects wo= new WriteObjects(); 
	 *  wo.writeColon(simulation); }
	 * 
	 * 4) To restore a grid you just need to use this in your main class:
	 * colon=(Colon) ReadObjects.restoreColon();
	 */

}
