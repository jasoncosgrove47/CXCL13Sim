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
import sim3d.SimulationEnvironment;
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
public class ReticularFiber extends StromaEdge  {

	
	
	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D drawEnvironment;
	
	
	private Color m_col;
	
	public ReticularFiber(Double3D d3Point1, Double3D d3Point2) {
		super(d3Point1, d3Point2);
		setAntigenLevel(0);
		
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




			aAppearance.setColoringAttributes(new ColoringAttributes(getM_col()
					.getRed() / 255f, getM_col().getGreen() / 255f,
					getM_col().getBlue() / 255f, ColoringAttributes.FASTEST));
			aAppearance.setTransparencyAttributes(new TransparencyAttributes(
					TransparencyAttributes.FASTEST, 0.4f));

			LineAttributes la = new LineAttributes();
			la.setLineWidth((float) Settings.FDC.STROMA_EDGE_RADIUS * 20);
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


	public Color getM_col() {
		return m_col;
	}


	public void setM_col(Color m_col) {
		this.m_col = m_col;
	}

	
}
