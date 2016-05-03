package sim3d.cell;

import java.awt.Color;
import java.util.Random;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3d;

import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;

public class branch extends StromaEdge {

	/**
	 * A branch is a smaller edge and used to account for the web-like
	 * morphology of the FDC network, a branch can connect to the midpoint of
	 * two edges, or the midpoints of two branches
	 * 
	 * @author: Jason Cosgrove
	 */
	private static final long serialVersionUID = 1L;

	public double STROMA_EDGE_RADIUS;

	/**
	 * Constructor for the branch class
	 * 
	 * @param d3Point1
	 *            the start of the branch
	 * @param d3Point2
	 *            the end of the branch
	 */
	public branch(Double3D d3Point1, Double3D d3Point2) {

		super(d3Point1, d3Point2, true);

		// divide antigen amount by 2 to make sure a BC has to interact with the
		// correct portion of the edge to acquire antigen. Otherwise a BC could
		// interact with one end of the edge but take antigen from the other end
		this.setAntigenLevelLowerHalf((Settings.FDC.STARTINGANTIGENLEVEL / 4));
		this.setAntigenLevelUpperEdge((Settings.FDC.STARTINGANTIGENLEVEL / 4));

		this.STROMA_EDGE_RADIUS = Settings.FDC.BRANCH_RADIUS;

	}

	@Override
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		if (transf == null)// add || true to update the stroma visualisation

		{

			StromaEdge fdc = (StromaEdge) obj;

			transf = new TransformGroup();

			LineArray lineArr = new LineArray(2, LineArray.COORDINATES);
			lineArr.setCoordinate(0, new Point3d(0, 0, 0));
			lineArr.setCoordinate(1, new Point3d(m_d3Edge.x, m_d3Edge.y,
					m_d3Edge.z));

			Appearance aAppearance = new Appearance();

			Color col = Settings.FDC.DRAW_COLOR();

			aAppearance.setColoringAttributes(new ColoringAttributes(col
					.getRed() / 255f, col.getGreen() / 255f,
					col.getBlue() / 255f, ColoringAttributes.FASTEST));
			aAppearance.setTransparencyAttributes(new TransparencyAttributes(
					TransparencyAttributes.FASTEST, 0.4f));

			LineAttributes la = new LineAttributes();
			la.setLineWidth((float) this.STROMA_EDGE_RADIUS * 20);
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

	@Override
	public void registerCollisions(CollisionGrid cgGrid) {
		cgGrid.addLineToGrid(this, new Double3D(x, y, z), new Double3D(x
				+ m_d3Edge.x, y + m_d3Edge.y, z + m_d3Edge.z),
				this.STROMA_EDGE_RADIUS);
	}

	@Override
	public CLASS getCollisionClass() {
		return CLASS.BRANCH;
	}

}
