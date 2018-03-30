package sim.app.firecontrol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import sim.engine.SimState;
import sim.engine.Steppable;

public class FireController implements Steppable{
	private static final long serialVersionUID = 1L;
	
	/**
	 * This will check for termination conditions and writes out a file on mason root directory.
	 * TODO: fill the file with information about your simulation according to what you would like to show
	 * in your report
	 */
	@Override
	public void step(SimState state) {
		//create a .txt file where we can store simulation informations
		if(Ignite.cellsOnFire == 0){
			String fileName = System.getProperty("user.dir") + "/" + System.currentTimeMillis() + ".txt";
			
			try {
				FileWriter fw = new FileWriter(new File(fileName),true);
				BufferedWriter bwr = new BufferedWriter(fw);
				bwr.append("TODO");
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

}
