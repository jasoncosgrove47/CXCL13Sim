package sim3d.cell;


import sim.engine.*;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.util.Map;
import javax.media.j3d.BranchGroup;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3d;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.objectfile.ObjectFile;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim3d.Settings;
import sim3d.migration.Algorithm1;


/**
 * A B-cell agent. Performs chemotaxis/random movement based on the presence of
 * surrounding chemokine and the amount of receptors the cell is expressing. The
 * receptors are controlled by an ODE. The calculated movement is checked to see
 * whether it collides with the edges or other elements before being realised.
 * 
 * @author Jason Cosgrove - {@link jc1571@york.ac.uk}
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class TC extends Lymphocyte{
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Algorithm1 a2 = new Algorithm1();
	
	/**
	 * Controls what a B cell agent does for each time step Each Bcell registers
	 * its intended path on the collision grid, once all B cells register the
	 * collision grid handles the movement at the next iteration the B cells are
	 * moved. B cells only collide with stroma
	 */
	@Override
	public void step(final SimState state)// why is this final here
	{

		migrate(a2);
	}


	
	
	
	

	
	
	
	
	/*
	 * This is the 3D model of the B cell. Overrides JAVA 3D so we never
	 * actually call it anywhere in the simulation ourselves (non-Javadoc)
	 * 
	 * @see sim.portrayal3d.SimplePortrayal3D#getModel(java.lang.Object,
	 * javax.media.j3d.TransformGroup)
	 */
	

	
	
	
	@Override
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		// We choose to always recalculate this model because the movement
		// changes in each time step.
		// Removing the movement indicators and removing this true will make the
		// 3d display a lot faster
		
		boolean highres = true;
		
		if (transf == null || true) {
			if(highres ==true){
			
				//ObjectFile f = new ObjectFile();
				ObjectFile f = new ObjectFile(ObjectFile.RESIZE,ObjectFile.STRIPIFY);
				
				
				
				Scene scene = null;
				try {
					scene = f.load("/Users/jc1571/Desktop/Lymphocyte.obj");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IncorrectFormatException e) {
						e.printStackTrace();
					} catch (ParsingErrorException e) {
						e.printStackTrace();
					}
			
				
				
			    Map<String, Shape3D> nameMap = scene.getNamedObjects(); 

			   // for (String name : nameMap.keySet()) {
			        //System.out.printf("Name: %s\n", name); 
			        
			    //}
			    // Obtains a reference to a specific component in the scene 
				Shape3D lc = nameMap.get("lymphocyte"); 

				//The graph that still contains a reference to "eyes" 
				BranchGroup root = scene.getSceneGroup();
				//root.removeAllChildren();
				// Removes "eyes" from this graph //
				root.removeChild(lc);

					
					
				Shape3DPortrayal3D s = new Shape3DPortrayal3D(lc,
							lc.getAppearance());
				s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
				TransformGroup localTG = s.getModel(obj, null);
				localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
					

				Transform3D t3d = new Transform3D();
				Vector3d scaleVector = new Vector3d(0.4D, 0.4D, 0.4D);
				//Vector3d scaleVector = new Vector3d(2D, 2D, 2D);
				t3d.setScale(scaleVector);
				// Apply all transformations 
				localTG.setTransform(t3d);
					
					

				transf = new TransformGroup();
				transf.addChild(localTG);
					
		}
		
		else{
			
			transf = new TransformGroup();

			
			java.awt.Color red = new Color(255, 0, 0, 200);
			
			// Draw the BC itself
			SpherePortrayal3D s = new SpherePortrayal3D(
					red, Settings.BC.COLLISION_RADIUS * 2,
					6);
			s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
			TransformGroup localTG = s.getModel(obj, null);

			localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			transf.addChild(localTG);

			
			// if we have had any collisions, draw them as red circles
			// modelCollisions(m_d3aCollisions,obj, transf);

			// If we have any movement, then draw it as white lines telling us
			// where the cell is orientated
			// modelMovements(m_d3aMovements,obj, transf);
			
			}
		}
		return transf;
	}
	
	
	
}
