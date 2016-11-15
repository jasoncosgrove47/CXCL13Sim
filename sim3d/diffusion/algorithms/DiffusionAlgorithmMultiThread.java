package sim3d.diffusion.algorithms;

import sim3d.diffusion.Chemokine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



/**
 * Generic diffusion algorithm class. Only handles diffusion, not particle
 * decay. It is up to the implementing class to set the diffusion coefficients
 * in m_adDiffusionCoefficients depending on the algorithm used. These
 * coefficients are used with the difference in concentration to determine the
 * amount diffused. Currently implemented as a continuous process in a discrete
 * space - that is the amount of particle in a particular grid space is
 * represented as a double.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class DiffusionAlgorithmMultiThread
{
	/**
	 * The relative amount of diffusion to each neighbour in the diffusion
	 * neighbourhood, usually proportional to the distance to the neighbour
	 */
	protected double[][][]	m_adDiffusionCoefficients;
							
	/**
	 * The height, width, and depth of the space being diffused in
	 */
	protected int			m_iWidth, m_iHeight, m_iDepth;
	
	
	/**
	 * number of threads to use
	 */
	private int m_iThreads;
	
	
	/**
	 * store the threads so we don't have to keep updating them
	 */
	private DiffuseThread m_atThreads[];
	
	/**
	 * Constructor. Sets member variables
	 * 
	 * @param iWidth
	 *            Width of space
	 * @param iHeight
	 *            Height of space
	 * @param iDepth
	 *            Depth of space
	 */
	protected DiffusionAlgorithmMultiThread( int iWidth, int iHeight, int iDepth , int numThreads)
	{
		m_iWidth = iWidth;
		m_iHeight = iHeight;
		m_iDepth = iDepth;
		
		m_iThreads =numThreads;
		
		if ( m_iThreads > 1 )
		{
			m_atThreads = new DiffuseThread[m_iThreads];
			for ( int i = 0; i < m_iThreads; i++ )
			{
				int endX = 1+(i+1)*((iWidth-1)/m_iThreads);
				if ( i == m_iThreads-1 )
				{
					endX = iWidth-1;
				}
				
				m_atThreads[i] = new DiffuseThread(1+i*((iWidth-1)/m_iThreads),1,1, endX, iHeight-1, iDepth-1);
			}
		}
	}
	
	/**
	 * Visitor design pattern that performs one diffusion step of the particle
	 * to the immediate diffusion neighbourhood using the coefficients set in
	 * m_adDiffusionCoefficients.
	 * 
	 * @param pSpace
	 *            The Particle object to visit
	 */
	public void diffuse( Chemokine pSpace )
	{
		// A temporary variable containing the previous value of pSpace.field
		double[][][] ia3Concentrations = new double[m_iWidth][m_iHeight][m_iDepth];
		
		// Copy the contents of field over
		// Note: we can't use clone() or just set the variable because of the
		// fact that Java uses pointers
		// too much so they would both still reference the same variable.
		for ( int x = 0; x < m_iWidth; x++ )
		{
			for ( int y = 0; y < m_iHeight; y++ )
			{
				for ( int z = 0; z < m_iDepth; z++ )
				{
					ia3Concentrations[x][y][z] = pSpace.field[x][y][z];
				}
			}
		}
		
		// Precalculate these values for the edge checks inside the for loops
		// so we don't have to keep recalculating them
		
		if ( m_iThreads > 1 )
		{
			ExecutorService es = Executors.newCachedThreadPool();
			for ( int i = 0; i < m_iThreads; i++ )
			{
				m_atThreads[i].update(pSpace, ia3Concentrations, m_adDiffusionCoefficients);

				es.execute( m_atThreads[i] );
			}
			es.shutdown();
			try {
				es.awaitTermination(1, TimeUnit.MINUTES);
			}
			catch(InterruptedException e)
			{
			}
		}
		else
		{
			// Loop through the grid spaces ignoring borders for now
			diffuseLoop(pSpace, ia3Concentrations, 1,1,1, m_iWidth-1, m_iHeight-1, m_iDepth-1);
		}
		
		
			enforceBoundaryCondition(pSpace);
		
	}
	
	/**
	 * Enforce the boundary condition so all of the chemokine will bounce back in...
	 * @param pSpace
	 */
	private void enforceBoundaryCondition(Chemokine pSpace){
		
		// you want to iterate through 1 to gridwidth,height depth -1 so we
		// precalculate for efficiency
		int iWidth = m_iWidth - 1;
		int iHeight = m_iHeight - 1;
		int iDepth = m_iDepth - 1;

		// Now enforce the boundary condition
		// Code is a little inefficient, but it's clean at least
		int xEdge = -1, yEdge = -1, zEdge = -1;

		for (int x = 0; x < m_iWidth; x++) {
			// Check if this is on the x edge
			// So this will be calculated m_iWidth times in total
			if (x == 0) {
				xEdge = 1;
			} else if (x == iWidth) {
				xEdge = m_iWidth - 2;
			}

			for (int y = 0; y < m_iHeight; y++) {
				// Check if this is on the y edge
				// So this will be calculated m_iWidth*m_iHeight times in total
				if (y == 0) {
					yEdge = 1;
				} else if (y == iHeight) {
					yEdge = m_iHeight - 2;
				}
				for (int z = 0; z < m_iDepth; z++) {
					// Check if this is on the z edge
					// and this will be calculated m_iWidth*m_iHeight*m_iDepth
					// times in total
					if (z == 0) {
						zEdge = 1;
					} else if (z == iDepth) {
						zEdge = m_iDepth - 2;
					}

					if (xEdge != -1 || yEdge != -1 || zEdge != -1) {
						// just bounce it all back in

						// this is a tertiary operator, bit like quick if
						// statement

						// chooses the x coordinate, then the y and so on
						// if the edge is -1 return x, else return xEdge
						// all of this should be on one line really
						pSpace.field[(xEdge == -1) ? x : xEdge] // if it's -1,
																// set it to the
																// current x val
						[(yEdge == -1) ? y : yEdge] // etc.
						[(zEdge == -1) ? z : zEdge] // i.e. this one isn't on
													// the edge
								
					
						+= pSpace.field[x][y][z];
						
						// the actual boundary is zero but we are only
						// interested in boundary -1 in each axis
						// gives you a buffer zone
						pSpace.field[x][y][z] = 0;

					} else if (xEdge == -1 && yEdge == -1) {
						// this will be z = 0 and we skip to the end to save
						// time
						z = m_iDepth - 2;
					}

					zEdge = -1;
				}

				yEdge = -1;
			}

			xEdge = -1;
		}
		
		
	}
	
	class DiffuseThread extends Thread {
		
		private int m_iStartX, m_iStartY, m_iStartZ, m_iWidth, m_iHeight, m_iDepth;
		private double[][][] m_ia3Concentrations;
		private Chemokine m_pSpace;
		private double[][][]	m_adDiffusionCoefficients;
		
		public DiffuseThread(int iStartX, int iStartY, int iStartZ, int iWidth, int iHeight, int iDepth)
		{
			m_iStartX = iStartX;
			m_iStartY = iStartY;
			m_iStartZ = iStartZ;
			m_iWidth = iWidth;
			m_iHeight = iHeight;
			m_iDepth = iDepth;
		}
		
		public void update(Chemokine pSpace, double[][][] ia3Concentrations, double[][][] adDiffusionCoefficients)
		{
			m_pSpace = pSpace;
			m_ia3Concentrations = ia3Concentrations;
			m_adDiffusionCoefficients = adDiffusionCoefficients;
		}
		
		public void run()
		{
			for ( int x = m_iStartX; x < m_iWidth; x++ )
			{
				for ( int y = m_iStartY ; y < m_iHeight; y++ )
				{
					for ( int z = m_iStartZ; z < m_iDepth; z++ )
					{
						for ( int r = -1; r < 2; r++ )
						{
							for ( int s = -1; s < 2; s++ )
							{
								for ( int t = -1; t < 2; t++ )
								{
									double iDelta = (double) (m_adDiffusionCoefficients[r + 1][s + 1][t + 1]
											* (m_ia3Concentrations[x][y][z] - m_ia3Concentrations[x + r][y + s][z + t]));
											
									// if negative, then the net diffusion direction
									// is TO this grid space, so we just ignore it
									if ( iDelta > 0 )
									{
										m_pSpace.field[x + r][y + s][z + t] += iDelta;
										m_pSpace.field[x][y][z] -= iDelta;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	
	
	
	public void diffuseLoop(Chemokine pSpace, double[][][] ia3Concentrations, int iStartX, int iStartY, int iStartZ, int iWidth, int iHeight, int iDepth)
	{
		for ( int x = iStartX; x < iWidth; x++ )
		{
			for ( int y = iStartY ; y < iHeight; y++ )
			{
				for ( int z = iStartZ; z < iDepth; z++ )
				{
					// We now diffuse from this grid space outwards
					
					int iCount = 0;
					for ( int r = -1; r < 2; r++ )
					{
						for ( int s = -1; s < 2; s++ )
						{
							for ( int t = -1; t < 2; t++ )
							{
								double iDelta = (double) (m_adDiffusionCoefficients[r + 1][s + 1][t + 1]
										* (ia3Concentrations[x][y][z] - ia3Concentrations[x + r][y + s][z + t]));
										
								// if negative, then the net diffusion direction
								// is TO this grid space, so we just ignore it
								if ( iDelta > 0 )
								{
									pSpace.field[x + r][y + s][z + t] += iDelta;
									pSpace.field[x][y][z] -= iDelta;
								}
								
								// Keep track of how many particles we've
								// diffused
								iCount += (int) (m_adDiffusionCoefficients[r + 1][s + 1][t + 1]
										* (ia3Concentrations[x][y][z]));
							}
						}
					}


				}
			}
		}
	}
		
	
}



