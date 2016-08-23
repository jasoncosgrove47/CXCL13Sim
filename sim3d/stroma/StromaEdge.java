package sim3d.stroma;

import java.awt.Color;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3d;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.DrawableCell3D;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;

/**
 * An agent representing the edge of stroma. Used to represent the dendrites of
 * the FDCs for the purposes of display and collision.
 * 
 * 
 * Should be a subclass of an FDC probably...
 * 
 * @author Jason Cosgrove, Simon Jarrett
 */
public class StromaEdge extends DrawableCell3D implements java.io.Serializable,
		Collidable {

	private TYPE stromaedgetype;
	
	
	private Color m_col;
	
	public static enum TYPE {
		FDC_edge,FDC_branch,RC_edge
	}
	
	
	private static final long serialVersionUID = 1L;

	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D drawEnvironment;

	/**
	 * Records which cells ahve collided with this edge as we want to know the
	 * number of unique edges and branches visited by a cognate BC
	 */

	private ArrayList<Integer> cellsCollidedWith = new ArrayList<Integer>();

	/**
	 * Define colours so that we can add an antigen heatmap later if required.
	 * TODO there might be a cool figure we could get out of this!
	 */
	public java.awt.Color blue0 = new Color(30, 40, 190, 180);
	public java.awt.Color blueLow = new Color(30, 40, 190, 0);
	public java.awt.Color blue1 = new Color(30, 40, 210, 200);
	public java.awt.Color blue2 = new Color(200, 40, 230, 220);
	public java.awt.Color blue3 = new Color(30, 200, 255, 255);

	/**
	 * A vector representing the movement from point 1 to point 2
	 */
	public Double3D m_d3Edge;

	/**
	 * The midpoint of the edge, used for creating branches
	 */
	public Double3D midpoint;

	/**
	 * Divide each dendrite in two so that a B cell must be at the correct part
	 * of the dendrite to acquire antigen
	 */
	private int antigenLevel;
	

	/**
	 * Constructor for the stromal edge
	 * 
	 * @param d3Point1
	 * @param d3Point2
	 * @param branch
	 */
	
	/**
	public StromaEdge(Double3D d3Point1, Double3D d3Point2, boolean branch) {

		// makes sure that first point is always lower on the z axis
		// Make sure it's ordered on the z index
		if (d3Point1.z > d3Point2.z) {
			Double3D temp = d3Point1;
			d3Point1 = d3Point2;
			d3Point2 = temp;
		}

		// location of stroma is static so easiest to specify it's location in
		// the constructor
		x = d3Point1.x;
		y = d3Point1.y;
		z = d3Point1.z;

		// vector representing the stromal edge
		m_d3Edge = d3Point2.subtract(d3Point1);

		midpoint = new Double3D((d3Point1.x + d3Point2.x) / 2,
				(d3Point1.y + d3Point2.y) / 2, (d3Point1.z + d3Point2.z) / 2);

	}

*/

	/**
	 * @param d3Point1
	 * @param d3Point2
	 */
	public StromaEdge(Double3D d3Point1, Double3D d3Point2, TYPE type) {

		this.setStromaedgetype(type);
		
		// makes sure that first point is always lower on the z axis
		// Make sure it's ordered on the z index
		if (d3Point1.z > d3Point2.z) {
			Double3D temp = d3Point1;
			d3Point1 = d3Point2;
			d3Point2 = temp;
		}

		// location of stroma is static so easiest to specify it's location in
		// the constructor
		x = d3Point1.x;
		y = d3Point1.y;
		z = d3Point1.z;

		// vector representing the stromal edge
		m_d3Edge = d3Point2.subtract(d3Point1);

		// the midpoint of the vector
		midpoint = new Double3D((d3Point1.x + d3Point2.x) / 2,
				(d3Point1.y + d3Point2.y) / 2, (d3Point1.z + d3Point2.z) / 2);
		
		switch (type) {
		case FDC_edge: 
			setAntigenLevel(Settings.FDC.STARTINGANTIGENLEVEL);
			setM_col(Settings.FDC.DRAW_COLOR());
			break;
		case FDC_branch:
			setAntigenLevel(Settings.FDC.STARTINGANTIGENLEVEL);	
			setM_col(Settings.FDC.DRAW_COLOR());
			break;
			
		case RC_edge:
			setM_col(Settings.FDC.DRAW_COLOR());
			setAntigenLevel(0);	
			break;
			
		}
		
	
	}

	@Override
	public void addCollisionPoint(Int3D i3Point) {
		return; // We're not interested in collisions as we're static
	}

	@Override
	public CLASS getCollisionClass() {
		return CLASS.STROMA_EDGE;
	}

	@Override
	public Continuous3D getDrawEnvironment() {
		return drawEnvironment;
	}

	/*
	 * This method creates a 3d model of stromal edge for visualisation
	 * (non-Javadoc)
	 * 
	 * @see sim.portrayal3d.SimplePortrayal3D#getModel(java.lang.Object,
	 * javax.media.j3d.TransformGroup)
	 */
	@Override
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		if (transf == null)// add || true to update the stroma visualisation

		{
			
			transf = new TransformGroup();

			LineArray lineArr = new LineArray(2, LineArray.COORDINATES);
			lineArr.setCoordinate(0, new Point3d(0, 0, 0));
			lineArr.setCoordinate(1, new Point3d(m_d3Edge.x, m_d3Edge.y,
					m_d3Edge.z));

			Appearance aAppearance = new Appearance();
			
			LineAttributes la = new LineAttributes();
			
			//Color fdccol = Settings.FDC.DRAW_COLOR();
			
			//Color frccol = Settings.FRC.DRAW_COLOR();

			
			double fdcEdgeRadius = Settings.FDC.STROMA_EDGE_RADIUS * 20;
			double fdcBranchRadius = Settings.FDC.BRANCH_RADIUS * 20;
			double rcEdgeRadius = Settings.FDC.STROMA_EDGE_RADIUS * 20;
			
			switch (this.getStromaedgetype()) {
			case FDC_edge: 
			
		
				// uncomment this code for an antigen heatmap
				/*
				 * if (fdc.getAntigen() < 90) { col = blue3; } if (fdc.getAntigen()
				 * < 85) { col = blueLow; } if (fdc.getAntigen() < 75) { col =
				 * blue0; }
				 */

				aAppearance.setColoringAttributes(new ColoringAttributes(getM_col()
						.getRed() / 255f, getM_col().getGreen() / 255f,
						getM_col().getBlue() / 255f, ColoringAttributes.FASTEST));
				aAppearance.setTransparencyAttributes(new TransparencyAttributes(
						TransparencyAttributes.FASTEST, 0.4f));

				la.setLineWidth((float) fdcEdgeRadius);
			
				break;
			
			case FDC_branch:
			
				aAppearance.setColoringAttributes(new ColoringAttributes(getM_col()
						.getRed() / 255f, getM_col().getGreen() / 255f,
						getM_col().getBlue() / 255f, ColoringAttributes.FASTEST));
				aAppearance.setTransparencyAttributes(new TransparencyAttributes(
						TransparencyAttributes.FASTEST, 0.4f));


				la.setLineWidth((float) fdcBranchRadius);
			

				break;
				
			case RC_edge:

				aAppearance.setColoringAttributes(new ColoringAttributes(getM_col()
						.getRed() / 255f, getM_col().getGreen() / 255f,
						getM_col().getBlue() / 255f, ColoringAttributes.FASTEST));
				aAppearance.setTransparencyAttributes(new TransparencyAttributes(
						TransparencyAttributes.FASTEST, 0.4f));


				la.setLineWidth((float) rcEdgeRadius);
			
				break;

			}
			
			
	
			aAppearance.setLineAttributes(la);

			Shape3D s3Shape = new Shape3D(lineArr, aAppearance);

			Shape3DPortrayal3D s = new Shape3DPortrayal3D(s3Shape, aAppearance);
			s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
			TransformGroup localTG = s.getModel(obj, null);

			localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			transf.addChild(localTG);


		}
		return transf;
	}

	/**
	 * Accessor for point 1
	 */
	public Double3D getPoint1() {
		return new Double3D(x, y, z);
	}

	/**
	 * Accessor for point 2
	 */
	public Double3D getPoint2() {
		return new Double3D(x + m_d3Edge.x, y + m_d3Edge.y, z + m_d3Edge.z);
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
		cgGrid.addLineToGrid(this, new Double3D(x, y, z), new Double3D(x
				+ m_d3Edge.x, y + m_d3Edge.y, z + m_d3Edge.z),
				Settings.FDC.STROMA_EDGE_RADIUS);
	}

	/*
	 * Getters and Setters for the class 
	 */
	public int getAntigenLevel() {
		return antigenLevel;
	}

	public void setAntigenLevel(int antigenLevel) {
		this.antigenLevel = antigenLevel;
	}

	public ArrayList<Integer> getCellsCollidedWith() {
		return cellsCollidedWith;
	}

	public void setCellsCollidedWith(ArrayList<Integer> cellsCollidedWith) {
		this.cellsCollidedWith = cellsCollidedWith;
	}

	public Color getM_col() {
		return m_col;
	}

	public void setM_col(Color m_col) {
		this.m_col = m_col;
	}

	public TYPE getStromaedgetype() {
		return stromaedgetype;
	}

	public void setStromaedgetype(TYPE stromaedgetype) {
		this.stromaedgetype = stromaedgetype;
	}

}
