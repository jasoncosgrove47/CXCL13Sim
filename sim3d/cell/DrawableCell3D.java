package sim3d.cell;


import sim.field.continuous.Continuous3D;
import sim.portrayal3d.SimplePortrayal3D;
import sim.util.Double3D;

/**
 * A parent class for drawable agents containing common code
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public abstract class DrawableCell3D extends SimplePortrayal3D {
	
	private Double3D m_Location;
	/**
	 * Position of the cell
	 */
	public double x, y, z;

	/**
	 * Accessor for the draw environment this cell is contained in
	 */
	public abstract Continuous3D getDrawEnvironment();

	/**
	 * Set object location and register change with the drawing environment
	 * 
	 * @param d3Location
	 *            The new location of the cell
	 */
	public void setObjectLocation(Double3D d3Location) {
		
		this.setM_Location(d3Location);
		
		x = d3Location.x;
		y = d3Location.y;
		z = d3Location.z;

		getDrawEnvironment().setObjectLocation(this, new Double3D(x, y, z));
	}

	public Double3D getM_Location() {
		return m_Location;
	}

	public void setM_Location(Double3D m_Location) {
		this.m_Location = m_Location;
	}
	
	
	

	
}
