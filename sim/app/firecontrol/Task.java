package sim.app.firecontrol;

import java.util.LinkedList;

import sim.util.Int2D;

/**
 * This class is used to represent a complex task in the world.
 * Practically speaking, a task represents a fire, not a single cell but a
 * group of cells on fire.
 *
 * @author Albani Dario
 * @email albani@dis.uniroma1.it
 *
 */
public class Task{
	public int id;
	public Int2D centroid;
	public double radius; //the utility?
	public int utility;
	public int UAVassigned;
	public LinkedList<WorldCell> cells;
	public UAV manager;

	public static int height = 60; //size of the forest
	public static int width = 60; //size of the forest

	public Task(int id, Int2D centroid, int initialRadius){
		this.id = id;
		this.centroid = centroid;
		this.radius = initialRadius;
		this.cells = new LinkedList<>();
		this.UAVassigned = 0;
	}

	public void addCell(WorldCell cell){
		this.cells.add(cell);
	}

	/*
	 * Used to keep the information about the task up to date.
	 * When a new fire is created, the cell calls  this function to let the
	 * task recompute its radius.
	 *
	 * @return true, if the update succeeds and the cell is added
	 */
	public boolean notifyNewFire(WorldCell cell){
		for(WorldCell wc : this.cells){
			if(cell.isNeighborOf(wc)){
				this.cells.add(cell);
				//now update the radius
				Int2D cellPos = new Int2D(cell.x, cell.y);
				this.radius = Math.max(cellPos.distance(this.centroid), this.radius);
				this.utility = this.cells.size();
				//System.err.println("Task " + this.id + "\t utility " + this.utility +
				//									" " + this.cells.size() + " " + height*width);
				return true;
			}
		}
		return false;
	}

	/*
	 * Used to keep the information about the task up to date.
	 * When a new fire is created, the cell calls  this function to let the
	 * task recompute its radius.
	 *
	 * @return true, if the update succeeds and the cell is added
	 */
	public boolean notifyExtinguishedFire(WorldCell cell) {
		if(this.cells.remove(cell)){
			this.utility = this.cells.size();
			Int2D pos = new Int2D(cell.x, cell.y);
			//if it was a border cell
			if(radius == pos.distance(centroid)){
				//update the radius
				radius = 0;
				for(WorldCell wc : this.cells){
					pos = new Int2D(wc.x, wc.y);
					this.radius = Math.max(pos.distance(centroid), radius);
				}
			}
			return true;
		}
		return false;
	}

	public void selectManager(Ignite ignite){
		double bestDistance = 1000000; // Big number as inicial

		//TODO avoud two tasks with the same manager

		for(Object obj : ignite.UAVs){
			UAV uav = (UAV) obj;
			double uavDistance = Math.sqrt(Math.pow(uav.x - this.centroid.x, 2) + Math.pow(uav.y - this.centroid.y, 2));
			if(uavDistance < bestDistance){
				bestDistance = uavDistance;
				this.manager = uav;
			}
		}
	}

	@Override
	public boolean equals(Object obj){
		Task task = (Task) obj;
		return task.centroid == this.centroid;
	}
}
