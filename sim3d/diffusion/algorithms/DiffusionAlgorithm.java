package sim3d.diffusion.algorithms;

import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.diffusion.Particle;
import sim3d.diffusion.ParticleMoles;

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
public class DiffusionAlgorithm {
	/**
	 * The relative amount of diffusion to each neighbour in the diffusion
	 * neighbourhood, usually proportional to the distance to the neighbour
	 */
	protected double[][][] m_adDiffusionCoefficients;

	/**
	 * The height, width, and depth of the space being diffused in
	 */
	protected int m_iWidth, m_iHeight, m_iDepth;

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
	protected DiffusionAlgorithm(int iWidth, int iHeight, int iDepth) {
		m_iWidth = iWidth;
		m_iHeight = iHeight;
		m_iDepth = iDepth;
	}

	/**
	 * Visitor design pattern - so diffusion and particle grids are separate but
	 * can work together could have put diffuse in particle class but this keeps
	 * it as a distinct thing without worrying about implementation and
	 * separates diffusion from the particle grid that performs one diffusion
	 * step of the particle to the immediate diffusion neighbourhood using the
	 * coefficients set in m_adDiffusionCoefficients.
	 * 
	 * 
	 * @param pSpace
	 *            The Particle object to visit
	 */
	public void diffuse(Particle pSpace) {
		// A temporary variable containing the previous value of pSpace.field
		int[][][] ia3Concentrations = new int[m_iWidth][m_iHeight][m_iDepth];

		// Copy the contents of field over
		// Note: we can't use clone() or just set the variable because of the
		// fact that Java uses pointers
		// too much so they would both still reference the same variable.
		for (int x = 0; x < m_iWidth; x++) {
			for (int y = 0; y < m_iHeight; y++) {
				for (int z = 0; z < m_iDepth; z++) {
					ia3Concentrations[x][y][z] = pSpace.field[x][y][z];
				}
			}
		}

		// Precalculate these values for the edge checks inside the for loops
		// so we don't have to keep recalculating them

		// you want to iterate through 1 to gridwidth,height depth -1 so we
		// precalcualte for
		// efficiency
		int iWidth = m_iWidth - 1;
		int iHeight = m_iHeight - 1;
		int iDepth = m_iDepth - 1;

		// Loop through the grid spaces ignoring borders for now
		for (int x = 1; x < iWidth; x++) {
			for (int y = 1; y < iHeight; y++) {
				for (int z = 1; z < iDepth; z++) {
					// We now diffuse from this grid space outwards

					int iCount = 0;
					// BECAUSE YOU DIFFUSE FROM -1 TO +1 IN EACH DIRECTION
					for (int r = -1; r < 2; r++) {
						for (int s = -1; s < 2; s++) {
							for (int t = -1; t < 2; t++) {

								// TAKE THE DIFFERENCE IN CONCENTRATIONS BETWEEN
								// THIS SPACE AND THE TARGET SPACE
								// IF THERE IS NO DIFFERENCE IN GRADIENT THEN IT
								// WONT DIFFUSE
								int iDelta = (int) (m_adDiffusionCoefficients[r + 1][s + 1][t + 1] * (ia3Concentrations[x][y][z] - ia3Concentrations[x
										+ r][y + s][z + t]));

								// if negative, then the net diffusion direction
								// is TO this grid space, so we just ignore it
								if (iDelta > 0) {

									// UPDATE THIS GRIDSPACE AND THE TARGET
									// GRIDSPACE
									pSpace.field[x + r][y + s][z + t] += iDelta;
									pSpace.field[x][y][z] -= iDelta;
								}

								// Keep track of how many particles we've
								// diffused
								iCount += (int) (m_adDiffusionCoefficients[r + 1][s + 1][t + 1] * (ia3Concentrations[x][y][z]));
							}
						}
					}

					// Randomly assign locations for the last few
					for (int iRemainder = ia3Concentrations[x][y][z] - iCount; iRemainder > 0; iRemainder--) {
						int iRandom = Settings.RNG.nextInt(27);
						pSpace.field[x + (iRandom / 9 - 1)][y
								+ ((iRandom % 9) / 3 - 1)][z
								+ ((iRandom % 3) - 1)]++;
						pSpace.field[x][y][z]--;
					}
				}
			}
		}

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

	public void diffuse(ParticleMoles pSpace) {
		// A temporary variable containing the previous value of pSpace.field
		double[][][] ia3Concentrations = new double[m_iWidth][m_iHeight][m_iDepth];

		// Copy the contents of field over
		// Note: we can't use clone() or just set the variable because of the
		// fact that Java uses pointers too much so they would both still 
		// reference the same variable.

		for (int x = 0; x < m_iWidth; x++) {
			for (int y = 0; y < m_iHeight; y++) {
				for (int z = 0; z < m_iDepth; z++) {
					ia3Concentrations[x][y][z] = pSpace.field[x][y][z];
				}
			}
		}

		// Precalculate these values for the edge checks inside the for loops
		// so we don't have to keep recalculating them

		// you want to iterate through 1 to gridwidth,height depth -1 so we
		// precalcualte for
		// efficiency
		int iWidth = m_iWidth - 1;
		int iHeight = m_iHeight - 1;
		int iDepth = m_iDepth - 1;

		// Loop through the grid spaces ignoring borders for now
		for (int x = 1; x < iWidth; x++) {
			for (int y = 1; y < iHeight; y++) {
				for (int z = 1; z < iDepth; z++) {
					// We now diffuse from this grid space outwards

					// BECAUSE YOU DIFFUSE FROM -1 TO +1 IN EACH DIRECTION
					for (int r = -1; r < 2; r++) 						
					{
						for (int s = -1; s < 2; s++) {
							for (int t = -1; t < 2; t++) {

								// TAKE THE DIFFERENCE IN CONCENTRATIONS BETWEEN
								// THIS SPACE AND THE TARGET SPACE
								// IF THERE IS NO DIFFERENCE IN GRADIENT THEN IT
								// WONT DIFFUSE!!!
								double iDelta = (m_adDiffusionCoefficients[r + 1][s + 1][t + 1] * (ia3Concentrations[x][y][z] - ia3Concentrations[x
										+ r][y + s][z + t]));

								// if negative, then the net diffusion direction
								// is TO this grid space, so we just ignore it
								if (iDelta > 0) {

									// UPDATE THIS GRIDSPACE AND THE TARGET
									// GRIDSPACE
									pSpace.field[x + r][y + s][z + t] += iDelta;
									pSpace.field[x][y][z] -= iDelta;
								}
							}
						}
					}

				}
			}
		}

		// should really encapsulate so we can implement different boundary
		// conditions
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
}
