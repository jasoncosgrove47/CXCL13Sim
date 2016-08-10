package sim3d.stroma;



import java.awt.Color;

import javax.media.j3d.TransformGroup;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.DrawableCell3D;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;


public class Stroma extends DrawableCell3D implements Steppable, Collidable {



	private Color m_col;
	private TYPE stromatype;
	
	boolean m_CXCL13 = false;
	boolean m_CCL19  = false;
	boolean m_EBI2L  = false;
	
	public static enum TYPE {
		FDC, FRC, MRC, LEC
	}
	
	
	//TODO need to sort out what the draw environment is...
	public Stroma(TYPE type){
		
		this.setStromatype(type);
		
		switch (type) {
		case FDC: 
			m_CXCL13 = true;	
			break;
			
		case FRC:
			m_CCL19  = true;	
			break;
			
		case MRC:
			m_CXCL13 = true;	
			m_EBI2L  = true;	
			break;
			
		case LEC:
			break;
		}
	}
	
	
	
	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D drawEnvironment;

	private static final long serialVersionUID = 1L;

	@Override
	public void addCollisionPoint(Int3D i3Point) {
		// We're not interested in collisions as we're static
		return;
	}

	
	@Override
	public CLASS getCollisionClass() {
		return CLASS.STROMA;
	}

	@Override
	public Continuous3D getDrawEnvironment() {
		return drawEnvironment;
	}


	// Create a model to visualising the stroma node in 3D
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		
		
		
		switch (this.getStromatype()) {
		case FDC: 
			if (transf == null) {
				transf = new TransformGroup();

				SpherePortrayal3D s = new SpherePortrayal3D(
						Settings.FDC.DRAW_COLOR(),
						Settings.FDC.STROMA_NODE_RADIUS * 2, 6);
				s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
				TransformGroup localTG = s.getModel(obj, null);

				localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
				transf.addChild(localTG);
			}
	
		case FRC:

				if (transf == null) {
					transf = new TransformGroup();

					SpherePortrayal3D s = new SpherePortrayal3D(
							Settings.FRC.DRAW_COLOR(),
							Settings.FDC.STROMA_NODE_RADIUS * 2, 6);
					s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
					TransformGroup localTG = s.getModel(obj, null);

					localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
					transf.addChild(localTG);
				}
				
		case MRC:
			
				if (transf == null) {
					transf = new TransformGroup();

					SpherePortrayal3D s = new SpherePortrayal3D(
							Settings.FDC.DRAW_COLOR(),
							Settings.FDC.STROMA_NODE_RADIUS * 2, 6);
					s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
					TransformGroup localTG = s.getModel(obj, null);

					localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
					transf.addChild(localTG);
				}
		
			break;
			
		case LEC:
			
			if (transf == null) {
				transf = new TransformGroup();

				//TODO this needs to be a rectangle shape and not a sphere!!
				SpherePortrayal3D s = new SpherePortrayal3D(
						Settings.FDC.DRAW_COLOR(),
						Settings.FDC.STROMA_NODE_RADIUS * 4, 6);
				s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
				TransformGroup localTG = s.getModel(obj, null);

				localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
				transf.addChild(localTG);
			}
			
			break;
		
		}
		
		

		return transf;
	}

	
	@Override
	public void handleCollisions(CollisionGrid cgGrid) {
		return;
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public void registerCollisions(CollisionGrid cgGrid) {
		cgGrid.addSphereToGrid(this, new Double3D(x, y, z),
				Settings.FDC.STROMA_NODE_RADIUS);
	}

	@Override
	public void step(final SimState state) {

		if(m_CXCL13){
			Chemokine.add(Chemokine.TYPE.CXCL13, (int) x, (int) y, (int) z,
					Settings.FDC.CXCL13_EMITTED());
		}

		if(m_CCL19){
			Chemokine.add(Chemokine.TYPE.CCL19, (int) x, (int) y, (int) z,
					Settings.FDC.CXCL13_EMITTED());
		}
		if(m_EBI2L){
			Chemokine.add(Chemokine.TYPE.EBI2L, (int) x, (int) y, (int) z,
					Settings.FDC.CXCL13_EMITTED());
		}

	}


	public TYPE getStromatype() {
		return stromatype;
	}


	public void setStromatype(TYPE stromatype) {
		this.stromatype = stromatype;
	}


	public Color getM_col() {
		return m_col;
	}


	public void setM_col(Color m_col) {
		this.m_col = m_col;
	}
	
	
	
	
}
