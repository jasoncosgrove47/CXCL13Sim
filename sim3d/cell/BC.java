package sim3d.cell;




import sim.engine.*;
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
public class BC extends Lymphocyte{
	
	/**
	 * Need a serial ID or you get a warning
	 */
	private static final long serialVersionUID = 1L;

	
	/*
	 * This is the algorithm which controls BC migration
	 */
	private Algorithm1 a1 = new Algorithm1();


	
	/**
	 * Controls what a B cell agent does for each time step Each Bcell registers
	 * its intended path on the collision grid, once all B cells register the
	 * collision grid handles the movement at the next iteration the B cells are
	 * moved. B cells only collide with stroma
	 */
	@Override
	public void step(final SimState state)// why is this final here
	{		
		migrate(a1);	
	}
	
}
