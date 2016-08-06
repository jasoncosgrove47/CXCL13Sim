package sim3d.migration;

import sim3d.cell.BC;
import sim3d.cell.Lymphocyte;

public interface MigrationAlgorithm {

	public void performMigration(Lymphocyte lymphocyte);
	
}
