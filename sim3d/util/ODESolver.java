package sim3d.util;


import sim3d.cell.Lymphocyte;
import sim3d.diffusion.Chemokine;


/**
 * 4th order Runge Kutta Solver as described from:
 * http://mathworld.wolfram.com/Runge-KuttaMethod.html 
 * @author Jason Cosgrove
 * 
 */
public class ODESolver {


	
	//TODO encapsulate so its not as messy
	
	/**
	 * This portion of the ODE does not interface directly with the ABM so we
	 * calculate this separately 4th order Runge Kutta
	 */
	public static void solveODE(double Ka, double Kr, double Ki,double Koff,double Kdes,
			Chemokine.TYPE chemokine,Lymphocyte lymphocyte) {
		

		//figure out which receptor you need
		Lymphocyte.Receptor receptor = null;
		
		switch (chemokine) {
		case CXCL13: 
			receptor = Lymphocyte.Receptor.CXCR5;
			break;
		case CCL19:
			receptor = Lymphocyte.Receptor.CCR7;	
			break;
			
		case EBI2L:
			receptor = Lymphocyte.Receptor.EBI2;	
			break;
		default:
			break;
		}

	
		double h = 1;
		int iR_i, iL_r, iR_d, iR_f;

		//every second do the following
		//TODO this should be 60
		for (int i = 0; i < 1; i++) {
			
			
			//calculate the total amount of ligand available

			double[][][] ia3Concs = Chemokine.get(chemokine, (int) lymphocyte.x,
					(int) lymphocyte.y, (int) lymphocyte.z);

			
			
			double vol = 1e-12;// volume of one gridspace in litres
			
			// get CXCL13 concentrations at each psuedopod
			// {x+, x-, y+, y-, z+, z-}
			double[] iaConcs = { ia3Concs[2][1][1]/ vol, ia3Concs[0][1][1]/ vol,
					ia3Concs[1][2][1]/ vol, ia3Concs[1][0][1]/ vol, ia3Concs[1][1][2]/ vol,
					ia3Concs[1][1][0] / vol};
			
			
			double totalLigand = 0;
			
			for (int j = 0; j < 6; j++) // for each pseudopod
			{
				totalLigand += iaConcs[j];
			}
			
			
			
			iR_i = lymphocyte.getM_Ri(receptor);	
			iL_r = lymphocyte.getM_LR(receptor);	
			iR_d = lymphocyte.getM_Rd(receptor);
			iR_f = lymphocyte.getM_Rf(receptor);

			


			
			double ligandBinding1 = h * (Ka * totalLigand);
			double ligandBinding2 = h * ((Ka * totalLigand) + ligandBinding1 / 2);
			double ligandBinding3 = h * ((Ka * totalLigand) + ligandBinding2 / 2);
			double ligandBinding4 = h * ((Ka * totalLigand) + ligandBinding3);
			

			// receptors desensitised from surface
			double LRdK1 = h * (Kdes * iL_r);
			double LRdK2 = h * ((Kdes * iL_r) + LRdK1 / 2);
			double LRdK3 = h * ((Kdes * iL_r) + LRdK2 / 2);
			double LRdK4 = h * ((Kdes * iL_r) + LRdK3);
			
			// receptors internalised from surface
			double LRK1 = h * (Ki * iR_d);
			double LRK2 = h * ((Ki * iR_d) + LRK1 / 2);
			double LRK3 = h * ((Ki * iR_d) + LRK2 / 2);
			double LRK4 = h * ((Ki * iR_d) + LRK3);
			
			// receptors that are recycled from internal pool
			double RfK1 = h * (Kr * iR_i);
			double RfK2 = h * ((Kr * iR_i) + RfK1 / 2);
			double RfK3 = h * ((Kr * iR_i) + RfK2 / 2);
			double RfK4 = h * ((Kr * iR_i) + RfK3);

			// ligand dissociation from receptor
			double RdisK1 = h * (Koff * iL_r);
			double RdisK2 = h * ((Koff * iL_r) + RdisK1 / 2);
			double RdisK3 = h * ((Koff * iL_r) + RdisK2 / 2);
			double RdisK4 = h * ((Koff * iL_r) + RdisK3);
			
			
			// the total change in bound receptor for this time increment is
			// given by this equation
			double proportionToBind = ((ligandBinding1 / 6) + (ligandBinding2 / 3)
									+ (ligandBinding3 / 3) + (ligandBinding4 / 6));
					
			
			// cap the amount of receptors that can be bound
			if (proportionToBind > 1) {proportionToBind = 1;}
			else if (proportionToBind < 0) {proportionToBind = 0;}
	

			
			
			//determine the number of receptors that have bound
			double receptorsBound = (int) (proportionToBind * iR_f);

			
			//remove any chemokine that has bound from the grid
			consumeLigand(lymphocyte,chemokine, receptorsBound);
			
			int receptorsRecycled = (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));
			int receptorsInternalised = (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6));
			int ligandDissociation = (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6));
			
			
			
			
			//if(receptorsRecycled > iR_i){receptorsRecycled = iR_i;}
			//if(receptorsRecycled < 0){receptorsRecycled = 0;}
			
			//if(receptorsInternalised > iR_d){receptorsInternalised  = iR_d;}
			//if(receptorsInternalised  < 0){receptorsInternalised  = 0;}
			
			//if(ligandDissociation > iL_r){ligandDissociation = iL_r;}
			//if(ligandDissociation  < 0){ligandDissociation = 0;}
			
			//add chemokine that dissociates to grid
			addChemokine(lymphocyte,chemokine, ligandDissociation);
			
			
			
			

			double receptorsDesensitised = (int) ((LRdK1 / 6) + (LRdK2 / 3) + (LRdK3 / 3) + (LRdK4 / 6));
			
			
			//Desensitisation and ligand dissociation are both a function of LR
			// need to make sure that both values dont surpass the value of LR,
			// if this happns then favour desensitisation
			if(ligandDissociation + receptorsDesensitised > iL_r){
				
				ligandDissociation = (int) (iL_r - receptorsDesensitised);
				
			}
			
			
			//if(receptorsDesensitised > iL_r){receptorsDesensitised= iL_r;}
			//if(receptorsDesensitised < 0){receptorsDesensitised = 0;}
						
			int d_rf = lymphocyte.getM_Rf(receptor);
			int d_ri = lymphocyte.getM_Ri(receptor);

			
			int d_lr = lymphocyte.getM_LR(receptor);
			
			int d_rd = lymphocyte.getM_Rd(receptor);

			
			d_rf = (int) (d_rf - receptorsBound + ligandDissociation + receptorsRecycled);
			
			d_lr = (int) (d_lr + receptorsBound - ligandDissociation - receptorsDesensitised);
			
			d_rd = (int) (d_rd + receptorsDesensitised - receptorsInternalised);
			
			d_ri = (int) (d_ri + receptorsInternalised - receptorsRecycled);
			
		

			
			lymphocyte.setM_Rf(receptor, d_rf);
			lymphocyte.setM_Ri(receptor, d_ri);
			lymphocyte.setM_LR(receptor, d_lr);
			lymphocyte.setM_Rd(receptor, d_rd);	
			
			
		
		}
		
	}
	
	
	private static void consumeLigand(Lymphocyte lymphocyte, Chemokine.TYPE chemokine, double deltaRb) {
		
		double x = lymphocyte.x;
		double y = lymphocyte.y;
		double z = lymphocyte.z;
		
		double[] iaBoundReceptors = determineReceptorDistribution(lymphocyte, chemokine,deltaRb);

		// avogadors number - number of molecules in 1 mole
		double avogadro = 6.0221409e+23;

		// this is in moles, not receptors so need to scale it before i remove,
		// eg if i took away 10,000 that would be 10,000 moles which is not what
		// we want!!!

		Chemokine.add(chemokine, (int) x + 1, (int) y, (int) z,
				-(iaBoundReceptors[0] / avogadro));
		Chemokine.add(chemokine, (int) x - 1, (int) y, (int) z,
				-(iaBoundReceptors[1] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y + 1, (int) z,
				-(iaBoundReceptors[2] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y - 1, (int) z,
				-(iaBoundReceptors[3] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y, (int) z + 1,
				-(iaBoundReceptors[4] / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y, (int) z - 1,
				-(iaBoundReceptors[5] / avogadro));

	}
	
	
	
	/**
	 * TODO need to think more about how much ligand gets given to each gridspace
	 * there is definitely a better way to do this but for now this is fine
	 * @param lymphocyte
	 * @param chemokine
	 * @param ligandDis
	 */
	static void addChemokine(Lymphocyte lymphocyte, Chemokine.TYPE chemokine,double ligandDis){
		double x = lymphocyte.x;
		double y = lymphocyte.y;
		double z = lymphocyte.z;
		

		//assume that the ligand dissociates evenly around surrounding grids
		double amountToAdd = ligandDis/6;

		// avogadors number - number of molecules in 1 mole
		double avogadro = 6.0221409e+23;
		
		Chemokine.add(chemokine, (int) x + 1, (int) y, (int) z,
				+(amountToAdd / avogadro));
		Chemokine.add(chemokine, (int) x - 1, (int) y, (int) z,
				+(amountToAdd / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y + 1, (int) z,
				+(amountToAdd / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y - 1, (int) z,
				+(amountToAdd / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y, (int) z + 1,
				+(amountToAdd / avogadro));
		Chemokine.add(chemokine, (int) x, (int) y, (int) z - 1,
				+(amountToAdd / avogadro));
		
	}
	
	private static double[] determineReceptorDistribution(Lymphocyte lymphocyte, Chemokine.TYPE chemokine,double deltaReceptorsBound){
		
		
	
		double[][][] ia3Concs = Chemokine.get(chemokine, (int) lymphocyte.x,
				(int) lymphocyte.y, (int) lymphocyte.z);
		
		
		// get CXCL13 concentrations at each psuedopod
		// {x+, x-, y+, y-, z+, z-}
		double[] iaConcs = { ia3Concs[2][1][1], ia3Concs[0][1][1],
				ia3Concs[1][2][1], ia3Concs[1][0][1], ia3Concs[1][1][2],
				ia3Concs[1][1][0] };

		double totalLigand = 0;
		
		//determine the total amount of ligand
		for (int j = 0; j < 6; j++) // for each pseudopod
		{
			totalLigand += iaConcs[j];
		}
		
		
		//need to figure out how many receptors
		
		
		// store how many receptors are bound at each
		// of the 6 pseudopods and store values in a double array
		double[] iaBoundReceptors = new double[6];
		
		
		for(int k = 0;k<6;k++){
			double proportionToBind = 0;
					
			proportionToBind = iaConcs[k]/totalLigand;
			iaBoundReceptors[k] = (int) (proportionToBind * deltaReceptorsBound);
			
		}
		
		
		return iaBoundReceptors;
			
	}
	

}
