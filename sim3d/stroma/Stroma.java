package sim3d.stroma;



import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import sim3d.stroma.Stroma.TYPE;

/**
 * TO do cell profiler analysis of raw image files then neural network classification
 * can then read the files in directly as an adjacency matrix and need to figure out
 * how to do a voxel-based reconstruction....
 * @author jc1571
 *
 */
public class Stroma extends DrawableCell3D implements Steppable, Collidable {

	
	/**
	 * This contains information about all stromal cells
	 */
	private static ArrayList<Stroma> nodeinformation = new ArrayList<Stroma>();
	
	/*
	 * sometimes we have a location and not a node so this is a useful way to 
	 * compare to move between the two. TODO may be very inefficient
	 */

	//static Map<Double3D, Stroma> NodeIndex = new HashMap<Double3D, Stroma>();
	
	
	double FDCsecretionRate_CXCL13 = Settings.FDC.CXCL13_EMITTED();
	double bRCsecretionRate_CXCL13  = Settings.bRC.CXCL13_EMITTED();
	double MRCsecretionRate_CXCL13 = Settings.MRC.CXCL13_EMITTED();
	double MRCsecretionRate_EBI2L  = Settings.MRC.EBI2L_EMITTED();
	

	//public ArrayList<StromaEdge> m_Edges; 
	
	//the other nodes a stromal cell is connected to
	//public ArrayList<Stroma> m_Nodes; 
	
	//dont want duplicate entries
	private Set<Stroma> m_Nodes;
	private Set<StromaEdge> m_Edges ;
	
	private ArrayList<Integer> cellsCollidedWith = new ArrayList<Integer>();
	
	
	/**
	 * Divide each dendrite in two so that a B cell must be at the correct part
	 * of the dendrite to acquire antigen
	 */
	private int m_antigenLevel;
	
	
	private Double3D m_location;
	
	
	//the index used for the adjacency matriz
	private int m_index;
	
	/**
	 * How to remove a BC from the schedule:
	 * 
	 * when you schedule the BC it will return a stoppable object we store this
	 * object so we can access it when we need to stop the object. Then to
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
	
	
	//TODO need to sort out what the draw environment is...
	public Stroma(TYPE type, Double3D loc){
		
		
		this.setM_Edges(new LinkedHashSet<StromaEdge>());
		this.setM_Nodes(new LinkedHashSet<Stroma>());
		
		this.m_location = loc;
		
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
				//Color col = new Color(255, 100, 100, 125);
				Color col = new Color(20, 210, 100, 125);
				//Color col =  new Color(200, 130, 40);
				
				SpherePortrayal3D s = new SpherePortrayal3D(
						col,
						Settings.FDC.STROMA_NODE_RADIUS * 3.5, 6);
				s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
				TransformGroup localTG = s.getModel(obj, null);

				localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
				transf.addChild(localTG);
			}
			break;
	
			
		case bRC:

				if (transf == null) {
					transf = new TransformGroup();

					Color col = new Color(10, 210, 120, 125);
					
					SpherePortrayal3D s = new SpherePortrayal3D(
							col,
							Settings.FDC.STROMA_NODE_RADIUS * 3, 6);
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
							Settings.FDC.STROMA_NODE_RADIUS * 4, 6);
					s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
					TransformGroup localTG = s.getModel(obj, null);

					localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
					transf.addChild(localTG);
				
				
				}
		
			break;
			
		case LEC:
			
			
			if (transf == null) {
			
				transf = new TransformGroup();

				
				//Appearance aAppearance = new Appearance();
				//Color col = Color.white;
				//aAppearance.setColoringAttributes(new ColoringAttributes(col
				//		.getRed() / 255f, col.getGreen() / 255f,
				//		col.getBlue() / 255f, ColoringAttributes.FASTEST));
				Color col =  new Color(200, 130, 40);
				

				CubePortrayal3D s = new CubePortrayal3D(col,1.0);

				//SpherePortrayal3D s = new SpherePortrayal3D(
				//		Settings.FDC.DRAW_COLOR(),
				//		Settings.FDC.STROMA_NODE_RADIUS * 4, 6);
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
		return true;
	}

	@Override
	public void registerCollisions(CollisionGrid cgGrid) {
		cgGrid.addSphereToGrid(this, new Double3D(x, y, z),
				Settings.FDC.STROMA_NODE_RADIUS);
	}

	@Override
	public void step(final SimState state) {

		
		//System.out.println(this.m_dendrites.size());
		
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
			//Chemokine.add(Chemokine.TYPE.CCL19, (int) x, (int) y, (int) z,
			//		FRCsecretionRate_CCL19);
		}
		if(m_EBI2L){
			//Chemokine.add(Chemokine.TYPE.EBI2L, (int) x, (int) y, (int) z,
				//	MRCsecretionRate_EBI2L);
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

	public Double3D getM_location() {
		return m_location;
	}

	public void setM_location(Double3D m_location) {
		this.m_location = m_location;
	}

	public static ArrayList<Stroma> getNodeinformation() {
		return nodeinformation;
	}

	public static void setNodeinformation(ArrayList<Stroma> nodeinformation) {
		Stroma.nodeinformation = nodeinformation;
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

	
	
}
