package sim.app.firecontrol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import sim.engine.SimState;
import sim.engine.Steppable;

public class FireController implements Steppable{
	private static final long serialVersionUID = 1L;
	public static int height = 60; //size of the forest
	public static int width = 60; //size of the forest
	/**
	 * This will check for termination conditions and writes out a file on mason root directory.
	 * TODO: fill the file with information about your simulation according to what you would like to show
	 * in your report
	 */
	@Override
	public void step(SimState state) {
		Ignite ignite = (Ignite)state;
		//create a .txt file where we can store simulation informations
		if(Ignite.cellsOnFire == 0){
			String fileName = System.getProperty("user.dir") + "/" + System.currentTimeMillis() + ".txt";
			
			try {
				FileWriter fw = new FileWriter(new File(fileName),true);
				BufferedWriter bwr = new BufferedWriter(fw);
				bwr.append("Total cells: " + height * width);
				bwr.append("\nCells recovered: " + cellsRecovered(ignite, CellType.EXTINGUISHED));
				bwr.append("\nCells burned: " + cellsRecovered(ignite, CellType.BURNED));
				bwr.append("\nCells water: " + cellsRecovered(ignite, CellType.WATER));
				bwr.append("\nCells not touched: " + cellsRecovered(ignite, CellType.NORMAL));
				bwr.flush();
				bwr.close();
			} catch (IOException e) {
				System.err.println("Exception in FireControll.step() " + e.toString());
				e.printStackTrace();
			}
			
			//kill the current job of the simulation
			state.kill();
		}
	}

	public int cellsRecovered(Ignite ignite, CellType celltype){
		int cells = 0;

		for(int w=0; w<width; w++){
			for(int h=0; h<height; h++){
				WorldCell cell = (WorldCell)ignite.forest.field[w][h];

				if(cell.type.equals(celltype))
					cells += 1;
			}
		}
		return cells;
	}

}
