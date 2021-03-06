package sim3d.stroma;



import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.media.j3d.TransformGroup;


import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.CubePortrayal3D;

import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.cell.DrawableCell3D;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;


/**
 * This class handles functionality for stromal cell nodes
 * 
 * @author jc1571
 *
 */
public class Stroma extends DrawableCell3D implements Steppable, Collidable {
	
	/**
	 * This contains information about all stromal cells
	 */
	private static ArrayList<Stroma> nodes = new ArrayList<Stroma>();
	
	
	//the collision grid doesnt deal with
	//no static objects so just set to static to
	//false to remove from the collisionGrid
	private boolean m_isStatic = true;
	
	
	double FDCsecretionRate_CXCL13 = Settings.FDC.CXCL13_EMITTED();
	double bRCsecretionRate_CXCL13  = Settings.bRC.CXCL13_EMITTED();
	double MRCsecretionRate_CXCL13 = Settings.MRC.CXCL13_EMITTED();

	
	
	//dont want duplicate entries
	private Set<Stroma> m_Nodes;
	private Set<StromaEdge> m_Edges ;
	
	private ArrayList<Integer> cellsCollidedWith = new ArrayList<Integer>();
	
	
	/**
	 * Divide each dendrite in two so that a B cell must be at the correct part
	 * of the dendrite to acquire antigen
	 */
	private int m_antigenLevel;
	
	
	//the index used for the adjacency matrix
	private int m_index;
	
	/**
	 * How to remove a BC from the schedule:
	 * 
	 * when you schedule the BC it will return a stoppable object we store this
	 * object so we can access it when we need to remove the BC from the schedule. Then to
	 * remove the object we simply call stop() on the stopper object the BC can
	 * then be removed by garbage collection
	 */
	public void removeDeadCell(Continuous3D randomSpace) {
		this.stop();
		randomSpace.remove(this);
	}

	/**
	 * Flag to show if this class has been stopped (when no longer needed)
	 */
	private Stoppable stopper = null;

	/**
	 * Method to change the value of the stopper this is the stoppabkle object
	 * so we can access its stop method stoppable can acess BC but not the other
	 * way round
	 * 
	 * @param stopper
	 *            Whether the class should be stopped or not
	 */
	public void setStopper(Stoppable stopper) {
		this.stopper = stopper;
	}

	/**
	 * Method to stop the class where necessary
	 */
	public void stop() {
		stopper.stop();
	}
	


	private Color m_col;
	
	
	private TYPE m_type;
	
	boolean m_CXCL13 = false;
	boolean m_CCL19  = false;
	boolean m_EBI2L  = false;
	
	public static enum TYPE {
		FDC, bRC, MRC, LEC
	}
	
	

	/**
	 * Initialiser for Stroma
	 * @param type
	 * @param loc
	 */
	public Stroma(TYPE type, Double3D loc){
		
		
		this.setM_Edges(new LinkedHashSet<StromaEdge>());
		this.setM_Nodes(new LinkedHashSet<Stroma>());
		
		this.setM_Location(loc);
		
		this.setStromatype(type);
		
		switch (type) {
		case FDC: 
			m_CXCL13 = true;	
			this.m_drawEnvironment = SimulationEnvironment.fdcEnvironment;
			break;
			
		case bRC:
			m_CXCL13  = true;
			this.m_drawEnvironment = SimulationEnvironment.brcEnvironment;
			break;
			
		case MRC:
			
			//now need to deal with the interactions with BCs....
			this.set_antigenLevel(Settings.FDC.STARTINGANTIGENLEVEL);
			m_CXCL13 = true;	
			this.m_drawEnvironment = SimulationEnvironment.mrcEnvironment;
			break;
			
		case LEC:
			
			this.set_antigenLevel(Settings.FDC.STARTINGANTIGENLEVEL);
			this.m_drawEnvironment = SimulationEnvironment.mrcEnvironment;
			break;
			
		}
		if(this.getStromatype() != Stroma.TYPE.LEC){
			getNodes().add(this);
		}
	}
	
	
	
	
	
	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public Continuous3D m_drawEnvironment;

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
		return m_drawEnvironment;
	}


	// Create a model to visualising the stroma node in 3D
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		

		switch (this.getStromatype()) {
		case FDC: 
			
			
			if (transf == null) {
				transf = new TransformGroup();

				//Color col = new Color(10, 210, 120, 125);
				Color col = new Color(255, 0, 0, 200);
				
				SpherePortrayal3D s = new SpherePortrayal3D(
						col,
						Settings.FDC.STROMA_NODE_RADIUS , 6);
				s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
				TransformGroup localTG = s.getModel(obj, null);

				localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
				transf.addChild(localTG);
			}
			break;
	
			
		case bRC:

				if (transf == null) {
					transf = new TransformGroup();

					//Color col = new Color(10, 210, 120, 125);
					Color col = new Color(255, 0, 0, 200);
					SpherePortrayal3D s = new SpherePortrayal3D(
							col,
							Settings.FDC.STROMA_NODE_RADIUS, 6);
					s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
					TransformGroup localTG = s.getModel(obj, null);

					localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
					transf.addChild(localTG);
				}
				break;
				
		case MRC:
			
				if (transf == null) {

					transf = new TransformGroup();
					Color col = new Color(255, 0, 0, 200);
					
					SpherePortrayal3D s = new SpherePortrayal3D(
							col,
							Settings.FDC.STROMA_NODE_RADIUS, 6);
					s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
					TransformGroup localTG = s.getModel(obj, null);

					localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
					transf.addChild(localTG);
				
				
				}
		
			break;
			
		case LEC:
			
			if (transf == null) {
			
				transf = new TransformGroup();
				Color col =  new Color(200, 130, 40);
				CubePortrayal3D s = new CubePortrayal3D(col,1.0);
				s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
				TransformGroup localTG = s.getModel(obj, null);
				localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
				transf.addChild(localTG);
			}
			
			break;
		}
		
		return transf;
	}

	
	
	public static boolean AreStromaNodesConnected(Stroma n1, Stroma n2){
		//if there is an edge between them then they are connected
		for(StromaEdge se : n1.getM_Edges()){
			if(n2.getM_Edges().contains(se)) return true;
		}
		//no connection so return false
		return false;
	}
	

	
	
	
	@Override
	public void handleCollisions(CollisionGrid cgGrid) {
		return;
	}

	@Override
	public boolean isStatic() {
		return isM_isStatic();
	}

	@Override
	public void registerCollisions(CollisionGrid cgGrid) {
		cgGrid.addSphereToGrid(this, new Double3D(x, y, z),
				Settings.FDC.STROMA_NODE_RADIUS);
	}

	@Override
	public void step(final SimState state) {

		
		if(m_CXCL13){
			
			if(this.m_type == Stroma.TYPE.FDC){
				Chemokine.add(Chemokine.TYPE.CXCL13, (int) x, (int) y, (int) z,
						FDCsecretionRate_CXCL13);
				
		
			}
			else if(this.m_type == Stroma.TYPE.bRC){
				Chemokine.add(Chemokine.TYPE.CXCL13, (int) x, (int) y, (int) z,
						bRCsecretionRate_CXCL13);
			}
			else{
				Chemokine.add(Chemokine.TYPE.CXCL13, (int) x, (int) y, (int) z,
						MRCsecretionRate_CXCL13);
			}
		}

		//for now we dont care about CCL19
		if(m_CCL19){

		}
		if(m_EBI2L){

		}

	}


	public TYPE getStromatype() {
		return m_type;
	}


	public void setStromatype(TYPE stromatype) {
		this.m_type = stromatype;
	}


	public Color getM_col() {
		return m_col;
	}


	public void setM_col(Color m_col) {
		this.m_col = m_col;
	}

	public int get_antigenLevel() {
		return m_antigenLevel;
	}

	public void set_antigenLevel(int m_antigenLevel) {
		this.m_antigenLevel = m_antigenLevel;
	}

	public ArrayList<Integer> getCellsCollidedWith() {
		return cellsCollidedWith;
	}

	public void setCellsCollidedWith(ArrayList<Integer> cellsCollidedWith) {
		this.cellsCollidedWith = cellsCollidedWith;
	}

	public int getM_index() {
		return m_index;
	}

	public void setM_index(int m_index) {
		this.m_index = m_index;
	}

	public Set<Stroma> getM_Nodes() {
		return m_Nodes;
	}

	public void setM_Nodes(Set<Stroma> m_Nodes) {
		this.m_Nodes = m_Nodes;
	}

	public Set<StromaEdge> getM_Edges() {
		return m_Edges;
	}

	public void setM_Edges(Set<StromaEdge> m_Edges) {
		this.m_Edges = m_Edges;
	}

	public static ArrayList<Stroma> getNodes() {
		return nodes;
	}

	public static void setNodes(ArrayList<Stroma> nodes) {
		Stroma.nodes = nodes;
	}

	public boolean isM_isStatic() {
		return m_isStatic;
	}

	public void setM_isStatic(boolean m_isStatic) {
		this.m_isStatic = m_isStatic;
	}

	
}
