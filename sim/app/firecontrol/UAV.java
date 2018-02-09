/**
 * A single UAV running over the simulation. 
 * This class implements the class Steppable, the latter requires the implementation 
 * of one crucial method: step(SimState).
 * Please refer to Mason documentation for further details about the step method and how the simulation
 * loop is working.
 * 
 * @author dario albani
 * @mail albani@dis.uniroma1.it
 * @thanks Sean Luke
 */
package sim.app.firecontrol;

import java.util.LinkedHashSet;
import java.util.Set;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double3D;
import sim.util.Int3D;

public class UAV implements Steppable{
	private static final long serialVersionUID = 1L;

	// Agent's variable
	public int id; //unique ID
	public double x; //x position in the world
	public double y; //y position in the world
	public double z; //z position in the world
	public Double3D target; //UAV target
	public AgentAction action; //last action executed by the UAV
	public static double communicationRange = 30; //communication range for the UAVs

	// Agent's local knowledge 
	public Set<WorldCell> knownCells; 
	public Task myTask;
	
	// Agent's settings - static because they have to be the same for all the 
	// UAV in the simulation. If you change it once, you change it for all the UAV.
	public static double linearvelocity = 0.02;

	//used to count the steps needed to extinguish a fire in a location
	public static int stepToExtinguish = 10;
	//used to remember when first started to extinguish at current location
	private int startedToExtinguishAt = -1;

	public UAV(int id, Double3D myPosition){
		//set agent's id
		this.id = id;
		//set agent's position
		this.x = myPosition.x;
		this.y = myPosition.y;
		this.z = myPosition.z;
		//at the beginning agents have no action
		this.action = null;
		//at the beginning agents have no known cells 
		this.knownCells = new LinkedHashSet<>();
	}

	// DO NOT REMOVE
	// Getters and setters are used to display information in the inspectors
	public int getId(){
		return this.id;
	}

	public void setId(int id){
		this.id = id;
	}

	public double getX(){
		return this.x;
	}

	public void setX(double x){
		this.x = x;
	}

	public double getY(){
		return this.y;
	}

	public void setY(double y){
		this.y = y;
	}

	public double getZ(){
		return this.z;
	}

	public void setZ(double z){
		this.z = z;
	}

	/**
	 *  Do one step.
	 *  Core of the simulation.   
	 */
	public void step(SimState state){
		Ignite ignite = (Ignite)state;

		//select the next action for the agent
		AgentAction a = nextAction(ignite);
		
		switch(a){	
		case SELECT_TASK:
			// ------------------------------------------------------------------------
			// this is where your task allocation logic has to go. 
			// be careful, agents have their own knowledge about already explored cells, take that 
			// in consideration if you want to implement an efficient strategy.
			// TODO Implement here your task allocation strategy
			System.err.println("TODO: and now? Use one of methods for tasks assignment!");

			selectTask(); //<- change the signature if needed

			this.action = a;
			break;

		case SELECT_CELL:
			// ------------------------------------------------------------------------
			// this is where your random walk or intra-task allocation logic has to go. 
			// be careful, agents have their own knowledge about already explored cells, take that 
			// in consideration if you want to implement an efficient strategy.
			// TODO Implement here your random walk or intra-task allocation strategy
			System.err.println("TODO: and now? Use random walk or task assignment!");

			selectCell(); //<- change the signature if needed
			
		case MOVE:
			move(state);
			break;

		case EXTINGUISH:
			//if true set the cell to be normal and foamed
			if(extinguish(ignite)){
				//retrieve discrete location of this
				Int3D dLoc = ignite.air.discretize(new Double3D(this.x, this.y, this.z));
				//extinguish the fire
				((WorldCell)ignite.forest.field[dLoc.x][dLoc.y]).extinguish(ignite);
				this.target=null;
			}

			this.action = a;
			break;

		default:	
			System.exit(-1);
		}
	}

	/**
	 * What to do next?
	 * TODO Feel free to modify this at your own will in case you have a better 
	 * strategy
	 */ 
	private AgentAction nextAction(Ignite ignite){
		//if I do not have a task I need to take one
		if(this.myTask == null){
			return AgentAction.SELECT_TASK;
		}
		//else, if I have a task but I do not have target I need to take one
		else if(this.target == null){
			return AgentAction.SELECT_CELL;
		}
		//else, if I have a target and task I need to move toward the target
		//check if I am over the target and in that case execute the right action;
		//if not, continue to move toward the target
		else if(this.target.equals(new Double3D(x, y, z))){
			//if on fire then extinguish, otherwise move on
			WorldCell cell = (WorldCell)ignite.forest.field[(int) x][(int) y];
			//store the knowledge for efficient selection
			this.knownCells.add(cell);

			//TODO maybe, you can share the knowledge about the just extinguished cell here!

			if(cell.type.equals(CellType.FIRE))
				return AgentAction.EXTINGUISH;
			else
				return AgentAction.SELECT_CELL;

		}else{
			return AgentAction.MOVE;
		}		
	}

	/**
	 * Take the centroid of the fire and its expected radius and extract the new
	 * task for the agent.
	 */
	private void selectTask() {
		//remember to set the new task at the end of the procedure
		Task newTask = null;
		
		// TODO
		System.err.println("TODO: implement here your strategy for selection/auction");
		
		try{
			this.myTask = newTask;
			this.target = new Double3D(newTask.centroid.x, newTask.centroid.y, z);
		}catch(NullPointerException e){
			System.err.println("Something is null, have you forgetten to implement some part?");
		}
	}

	/**
	 * Take the centroid of the fire and its expected radius and select the next 
	 * cell that requires closer inspection or/and foam. 
	 */
	private void selectCell() {
		//remember to set the new target at the end of the procedure
		Double3D newTarget = null;
		
		// TODO		
		//the cell selection should be inside myTask area.
		System.err.println("TODO: implement here your strategy for exploration");
		
		this.target = newTarget;
	}
	
	/**
	 * Move the agent toward the target position
	 * The agent moves at a fixed given velocity
	 * @see this.linearvelocity
	 */
	public void move(SimState state){
		Ignite ignite = (Ignite) state;

		// retrieve the location of this 
		Double3D location = ignite.air.getObjectLocationAsDouble3D(this);
		double myx = location.x;
		double myy = location.y;
		double myz = location.z;

		// compute the distance w.r.t. the target
		// the z axis is only used when entering or leaving an area
		double xdistance = this.target.x - myx;
		double ydistance = this.target.y - myy;

		if(xdistance < 0)
			myx -= Math.min(Math.abs(xdistance), linearvelocity);
		else
			myx += Math.min(xdistance, linearvelocity);

		if(ydistance < 0){ 
			myy -= Math.min(Math.abs(ydistance), linearvelocity); 
		}
		else{	
			myy += Math.min(ydistance, linearvelocity); 
		}

		// update position in the simulation
		ignite.air.setObjectLocation(this, new Double3D(myx, myy, myz));
		// update position local position
		this.x = myx;
		this.y = myy;
		this.z = myz;
	}

	/**
	 * Start to extinguish the fire at current location.
	 * @return true if enough time has passed and the fire is gone, false otherwise
	 * @see this.stepToExtinguish
	 * @see this.startedToExtinguishAt
	 */
	private boolean extinguish(Ignite ignite){
		if(startedToExtinguishAt==-1){
			this.startedToExtinguishAt = (int) ignite.schedule.getSteps();
		}
		//enough time has passed, the fire is gone
		if(ignite.schedule.getSteps() - startedToExtinguishAt == stepToExtinguish){
			startedToExtinguishAt = -1;
			return true;
		}		
		return false;
	}

	/**
	 * COMMUNICATION
	 * Check if the input location is within communication range
	 */
	public boolean isInCommunicationRange(Double3D otherLoc){
		Double3D myLoc = new Double3D(x,y,z);
		return myLoc.distance(otherLoc) <= UAV.communicationRange;
	}

	/**
	 * COMMUNICATION
	 * Send a message to the team
	 */
	public void sendData(DataPacket packet){
		//TODO
	}

	/**
	 * COMMUNICATION
	 * Receive a message from the team
	 */
	public void receiveData(DataPacket packet){
		//TODO

		//hint for a possible flooding strategy: 
		//if: (neverReceived(packet) && packet.origin != this) -> sendData(packet)
	}

	/**
	 * COMMUNICATION
	 * Retrieve the status of all the agents in the communication range.
	 * @return an array of size Ignite.tasks().size+1 where at position i you have 
	 * the number of agents enrolled in task i (i.e. Ignite.tasks().get(i)). 
	 * 
	 * HINT: you can easily assume that the number of uncommitted agents is equal to:
	 * Ignite.numUAVs - sum of all i in the returned array
	 */
	public int[] retrieveAgents(Ignite ignite){
		int[] status = new int[ignite.tasks.size()];
		
		for(Object obj : ignite.UAVs){ //count also this uav
			UAV other = (UAV) obj;
			if(isInCommunicationRange(new Double3D(other.x, other.y, other.z))){
				Task task = other.myTask;
				if(task != null)
					status[ignite.tasks.indexOf(task)]++;
			}
		}
		
		return status;
	}
	
	@Override
	public boolean equals(Object obj){
		UAV uav = (UAV) obj;
		return uav.id == this.id;
	}
	
	@Override
	public String toString(){ 
		return id+"UAV-"+x+","+y+","+z+"-"+action;
	} 	
}


