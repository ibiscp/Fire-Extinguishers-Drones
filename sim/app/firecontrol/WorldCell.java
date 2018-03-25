package sim.app.firecontrol;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double3D;
import sim.util.Int2D;

/**
 * Abstract class that implements a generic cell
 * There are 4 different kinds of forest cell:
 * - normal, trees are still in good health
 * - fire, there are some fires in the area and the cell requires attention
 * - burned, there is nothing left to save
 * - water, the cell is part of a lake or a river
 *
 * @author dario albani
 * @mail albani@dis.uniroma1.it
 */
public class WorldCell implements Steppable{
	private static final long serialVersionUID = 1L;
	public int x; //cell x location
	public int y; //cell y location
	public CellType type; //type of the cell

	//params
	public static double statusThreshold = 10E-3;
	public static double normalStatusMultiplier = 10E-3;
	public static double fireStatusMultiplier = 5*10E-4;
	public static double selfIgniteThreshold = 1-10E-5;
	public static int selfIgniteMax;

	//if this reach 0 and
	// - the type is normal, the type becomes fire
	// - the type is fire, the type becomes burned
	private double status = 1;
	//avoid to enhance the fire status of a cell more than once per step
	private boolean enhanced = false;

	/* Constructor*/
	public WorldCell(int x, int y, CellType type){
		this.x = x;
		this.y = y;
		this.type = type;
	}

	//GETTERS
	public int getX(){
		return this.x;
	}

	public int getY(){
		return this.y;
	}

	public CellType getType(){
		return this.type;
	}

	public void setType(CellType type){
		this.type = type;
	}

	/**
	 * Use this function to extinguish a fire over a cell.
	 * Call it whenever a UAV is over this.
	 */
	public void extinguish(Ignite ignite){
		if(this.type.equals(CellType.FIRE)){
			this.type = CellType.EXTINGUISHED;
			Ignite.cellsOnFire--;
			this.status = 1;
			//notify the tasks to let them compute the update
			for(Task t : ignite.tasks){
				if(t.notifyExtinguishedFire(this)){
					//remove only from the (there must be only one) task
					//that contain this
					break;
				}
			}
		}
	}

	public boolean isNeighborOf(WorldCell wc) {
		Int2D myPos = new Int2D(this.x, this.y);
		Int2D otherPos = new Int2D(wc.x, wc.y);
		return myPos.distance(otherPos)<2;
	}

	@Override
	public void step(SimState state) {
		Ignite ignite = (Ignite) state;

		enhanced = false;

		if(this.type.equals(CellType.WATER) || this.type.equals(CellType.BURNED)){
			return;
		} else if(this.type.equals(CellType.FIRE)){
			//update the status
			status -= fireStatusMultiplier*status*ignite.gaussianPDF(0, ignite.random.nextDouble(), 0.2);

			//propagate fire to neighbors
			for(int i=-1; i<=1; i++){
				for(int j=-1; j<=1; j++){
					if(Ignite.isInBounds(new Double3D(this.x+i, this.y+j, 1))){
						WorldCell neighbor = ((WorldCell)ignite.forest.field[this.x+i][this.y+j]);
						if(neighbor.type.equals(CellType.NORMAL) && !neighbor.enhanced){
							neighbor.status -= normalStatusMultiplier*ignite.gaussianPDF(0, 2*ignite.random.nextDouble(), 0.2);
							neighbor.enhanced = true;
						}
					}
				}
			}
			//update own status
			if(status<statusThreshold){
				this.type = CellType.BURNED;
				Ignite.cellsOnFire--;
				Ignite.cellsBurned++;
			}
		} else if(this.type.equals(CellType.NORMAL)){
			//check status
			//and there is a random probably that the cell with take fire by itself
			if(this.status < statusThreshold){
				this.type = CellType.FIRE;
				this.status = 1;
				Ignite.cellsOnFire++;
				//notify the tasks to let them compute the update
				for(Task t : ignite.tasks){
					if(t.notifyNewFire(this)){
						//add only to one task
						break;
					}
				}
			}else if(selfIgniteMax > 0){
				if(ignite.schedule.getSteps()!=0 &&
						ignite.schedule.getSteps()%500==0 &&
						ignite.random.nextDouble()>selfIgniteThreshold){
					this.type = CellType.FIRE;
					this.status = 1;
					Ignite.cellsOnFire++;
					selfIgniteMax--;
					//generate a new task
					Task t = new Task(ignite.tasks.size()+1, new Int2D(this.x, this.y), 0);
					t.addCell(this);
					ignite.tasks.add(t);
					t.selectManager(ignite);
					//t.manager.myTask = t;
				}
			}
		}
	}

	@Override
	public boolean equals(Object obj){
		WorldCell cell = (WorldCell) obj;
		return cell.x == this.x && cell.y == this.y;
	}
}
