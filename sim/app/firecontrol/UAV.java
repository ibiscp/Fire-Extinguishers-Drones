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
import java.util.LinkedList;
import java.util.Set;
import java.util.Random;
import java.util.Iterator;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double3D;
import sim.util.Int3D;

// Personal
import sim.field.grid.ObjectGrid2D;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	// Personal
	Random random = new Random();
	public ObjectGrid2D knownForest;
	public int attempt;
	public static int height = 60; //size of the forest
	public static int width = 60; //size of the forest
	public DataPacket data;
	public Map<UAV, Double> proposals;
	private Lock lock = new ReentrantLock();

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

		// Personal
		this.knownForest = new ObjectGrid2D(width, height);
		this.attempt = 0;
		this.data = null;
		this.proposals = new HashMap<UAV, Double>();
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

		assignTasks(ignite);
		//this.action = a;

		switch(a){
		case PROPOSED:
				this.attempt += 1;
				if (this.target == null && this.attempt == 5){
					this.attempt = 0;
					this.target = new Double3D(random.nextInt(height), random.nextInt(width), this.z);
				}
				break;
		case SELECT_TASK:
			// ------------------------------------------------------------------------
			// this is where your task allocation logic has to go.
			// be careful, agents have their own knowledge about already explored cells, take that
			// in consideration if you want to implement an efficient strategy.
			// TODO Implement here your task allocation strategy
			//System.err.println("TODO: and now? Use one of methods for tasks assignment!");

			selectTask(ignite); //<- change the signature if needed

			//this.action = a;
			break;

		case SELECT_CELL:
			// ------------------------------------------------------------------------
			// this is where your random walk or intra-task allocation logic has to go.
			// be careful, agents have their own knowledge about already explored cells, take that
			// in consideration if you want to implement an efficient strategy.
			// TODO Implement here your random walk or intra-task allocation strategy
			//System.err.println("TODO: and now? Use random walk or task assignment!");

			selectCell(ignite); //<- change the signature if needed
			//this.action = a;
			break;

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
				//this.action=null;
			}

			//this.action = a;//AgentAction.SELECT_CELL;
			break;

		default:
			//System.exit(-1);
		}
	}

	/**
	 * What to do next?
	 * TODO Feel free to modify this at your own will in case you have a better
	 * strategy
	 */
	private AgentAction nextAction(Ignite ignite){
		//System.err.println("UAV " + this.id + ":\tTask " + this.myTask + "\tTarget " + this.target + "\tAction " + this.action);
		/*if(this.myTask == null && this.action == AgentAction.PROPOSED){
			//System.err.println("UAV " + this.id + ":\tProposed!");
			return AgentAction.PROPOSED;
		}
		if(this.myTask == null && (this.action == null || this.action == AgentAction.PROPOSED)){
			return AgentAction.PROPOSED;
		}*/
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
		//else if(this.target.equals(ignite.air.discretize(new Double3D(this.x, this.y, this.z)))){
		else if(this.target.equals(new Double3D(x, y, z))){
			//if on fire then extinguish, otherwise move on
			WorldCell cell = (WorldCell)ignite.forest.field[(int) x][(int) y];
			//store the knowledge for efficient selection
			this.knownCells.add(cell);
			this.knownForest.field[cell.getX()][cell.getY()] = cell;

			//TODO maybe, you can share the knowledge about the just extinguished cell here!

			Double3D position = new Double3D(this.x,this.y,this.z);
			DataPacket data = new DataPacket(this.id, position, this.knownCells, this.myTask, this.myTask.manager.id == this.id);
			if(cell.type.equals(CellType.FIRE)){
				sendData(data, ignite, true);			// Only send data if in FIRE cells
				return AgentAction.EXTINGUISH;
			}
			else{
				sendData(data, ignite, false);
				this.attempt += 1;
				return AgentAction.SELECT_CELL;
			}
		}
		else{
			//System.err.println("UAV " + this.id + "\tTarget " + this.target + "\tPosition " + new Double3D(this.x, this.y, this.z));
			return AgentAction.MOVE;
		}
	}

	private void assignTasks(Ignite ignite) {
		try{
			if(this.id == this.myTask.manager.id){
					/*System.err.println("UAV " + this.id + ":" +
										"\tAssigned " + this.myTask.UAVassigned +
										"\tProposals " + this.proposals.size());*/
		}
			if(this.id == this.myTask.manager.id && this.proposals.size() > 0){

				//System.err.println("UAV " + this.id + ": Tasksize " + ignite.tasks.size() +
				//					" Proposals size " + this.proposals.size());

				if(this.myTask.radius == 0){
					ignite.tasks.remove(this.myTask);
				}
				else{
					int totalFire = 0;

					// Get total number of cells with fire
					for(Task task : ignite.tasks){
						if (task.radius != 0)
							totalFire += task.utility;
					}

					// Calculate UAV needed
					int uavNeeded = (int)(ignite.numUAVs * this.myTask.utility / totalFire);															

					// Assign tasks for the drones
					lock.lock();
					try{
						int proposals = this.proposals.size();
						for (int i=0; i<proposals; i++){
							// Exit if more UAV than needed
							if (this.myTask.UAVassigned >= uavNeeded)
								break;

							UAV bestUAV = null;
							double bestOffer = 0;
							for(Map.Entry<UAV, Double> UAVoffer : this.proposals.entrySet()){
								if (UAVoffer.getValue() > bestOffer && UAVoffer.getKey().myTask == null){
									bestOffer = UAVoffer.getValue();
									bestUAV = UAVoffer.getKey();
								}
							}
							this.proposals.remove(bestUAV);
							this.myTask.UAVassigned += 1;
							bestUAV.myTask = this.myTask;
							bestUAV.target = new Double3D(this.myTask.centroid.x, this.myTask.centroid.y, bestUAV.z);
							System.err.println("UAV " + bestUAV.id + ":" + "\tAssigned task " + this.myTask.id + "\t by UAV " +
								this.myTask.manager.id + "\tProposals size " + this.proposals.size());
						}

						/*proposals = this.proposals.size();
						for (int i=0; i<proposals; i++){
							for(Map.Entry<UAV, Double> UAVoffer : this.proposals.entrySet()){
								UAV uav = UAVoffer.getKey();
								uav.action = null;
								this.proposals.remove(uav);
							}
						}*/
					} finally {
						lock.unlock();
					}

					System.err.println("UAV " + this.id + ":" +
										"\tAssigned " + this.myTask.UAVassigned +
										"\tProposals " + this.proposals.size() +
										"\tUAV needed " + uavNeeded);
				}
			}
		}catch(NullPointerException e){}
	}

	 /**
 	 * Take the centroid of the fire and its expected radius and extract the new
 	 * task for the agent.
 	 */
	private void selectTask(Ignite ignite) {
		Task newTask = this.myTask;

		// Check if UAV is the manager
		for(Task task : ignite.tasks){
			if(this.id == task.manager.id){
				requestForBid(task, ignite);
				//System.err.println("UAV " + this.id + ":\tRequest for proposal sent!");

				this.action = AgentAction.PROPOSED;
				this.myTask = task;
				this.target = new Double3D(task.centroid.x, task.centroid.y, this.z);
				break;
			}
		}

		// Check for tasks and propose
		if (newTask == null && ignite.tasks.size()>1){//} && this.action == null){
			LinkedList<DataPacket> dataReceived = receiveData(ignite, false);

			for (DataPacket dp : dataReceived){
				if (dp.header.taskProposal == true)
					propose(dp.payload.task, ignite);
					this.action = AgentAction.PROPOSED;
					System.err.println("UAV " + this.id + ":\tPropose sent to UAV " + dp.payload.task.manager.id
						+ "\tPackage size " + dp.payload.task.manager.proposals.size());
			}
		}
		else{
			this.myTask = ignite.tasks.get(0);
			this.target = new Double3D(myTask.centroid.x, myTask.centroid.y, this.z);
		}

		//System.err.println("UAV " + this.id + ":\tAction " + this.action);
	}

	/**
	 * Take the centroid of the fire and its expected radius and select the next
	 * cell that requires closer inspection or/and foam.
	 */
	private void selectCell(Ignite ignite) {
		//remember to set the new target at the end of the procedure
		Double3D newTarget = new Double3D(this.x, this.y, this.z);

		// If tasks does not exist anymore
		if (this.myTask.radius == 0){
			this.myTask = null;
			this.target = null;
			this.action = null;
			this.attempt=0;
		}

		// Request communication if UAV is lost in NORMAL or BURNED cells
		else if (this.attempt >= 10){
			this.attempt = 0;

			LinkedList<DataPacket> dataReceived = receiveData(ignite, true);
			//LinkedList<DataPacket> totalData = testeAllData(ignite, this.id);

			if (dataReceived.size() == 0){
				//System.err.println("UAV " + this.id + ":\tNo data received");
				//System.err.println("UAV " + this.id + " new task");
				//this.myTask = null;
				this.target = null;
			}
			else{
			// Find the closest one
				//System.err.println("UAV " + this.id + ":\tFind closest");
			Double3D closest = findClosest(dataReceived);

			//if (closest.x != 0 && closest.y != 0){
			newTarget = new Double3D(closest.x, closest.y, this.z);
				/*System.err.println("UAV " + this.id +
												"\tOld position: " + this.x + " " + this.y +
												"\tNew position: " + newTarget.x + " " + newTarget.y +
												"\tNumber of messages: " + dataReceived.size() +
												"\tNumber of total messages: " + totalData.size());*/
			}
		}
		if (this.myTask != null)
			this.target = selectRandomCell(ignite, newTarget);
	}

	private Double3D selectRandomCell(Ignite ignite, Double3D newTarget){
		int randX, randY;
		int trials = 0;
		Double3D newPosition = null;
		double radius = myTask.radius;
		double newRadius;

		do{
			trials += 1;
			randX = random.nextInt(5) - 2;
			randY = random.nextInt(5) - 2;
			newRadius = Math.sqrt(Math.pow(x + randX - myTask.centroid.x,2) + Math.pow(y + randY - myTask.centroid.y,2));
			newPosition = new Double3D(newTarget.x + randX, newTarget.y + randY, newTarget.z);

			if(trials >= 10){
				//System.err.println("UAV " + this.id + ":\tERROR");
				newPosition = newTarget;
				this.attempt += 1;
				break;
			}
		} while((randX == 0 && randY == 0) || !(ignite.isInBounds(newPosition)) || newRadius > radius || newRadius < radius-7);

		return newPosition;
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

		/*System.err.println("UAV " + this.id +
							"\tPosition: " + this.x + " " + this.y +
							"\tTarget: " + this.target);*/


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
	 * @see this.startedToExtiextinguishnguishAt
	 */
	private boolean extinguish(Ignite ignite){
		if(startedToExtinguishAt==-1){
			this.startedToExtinguishAt = (int) ignite.schedule.getSteps();
		}
		//enough time has passed, the fire is gone
		if(ignite.schedule.getSteps() - startedToExtinguishAt >= stepToExtinguish){
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
		Double3D myLoc = new Double3D(this.x,this.y,this.z);
		return myLoc.distance(otherLoc) <= UAV.communicationRange;
	}

	/**
	 * COMMUNICATION
	 * Send a message to the team
	 */
	public void sendData(DataPacket packet, Ignite ignite, boolean add){
		// if the drone is a manager, its packet cannot be deleted
		if(add)
			this.data = packet;
		else if (this.myTask.manager.id != this.id)
			this.data = null;
	}

	/**
	 * COMMUNICATION
	 * Receive a message from the team
	 */
	public LinkedList<DataPacket> receiveData(Ignite ignite, boolean eliminateManager){
		LinkedList<DataPacket> dataReceived = new LinkedList<>();

		for(UAV uav : ignite.UAVs){
			DataPacket dp = uav.data;
			if(dp != null){
				if(dp.header.id != this.id &&
					isInCommunicationRange(dp.payload.position) &&
					(dp.payload.task == this.myTask || this.action == null) &&
					(!eliminateManager || dp.header.id != this.myTask.manager.id))
						dataReceived.add(dp);
			}
		}
		return dataReceived;
	}

	// Check all data received for the given id
	public LinkedList<DataPacket> testeAllData(Ignite ignite, int id){
		LinkedList<DataPacket> dataReceived = new LinkedList<>();

		for(UAV uav : ignite.UAVs){
			DataPacket dp = uav.data;
			if(dp != null){
					dataReceived.add(dp);
			}
		}
		return dataReceived;
	}

	// Messages for the auction
	//#############################################################################
	// Request for bid
	public void requestForBid(Task task, Ignite ignite){
		Double3D position = new Double3D(task.centroid.x,task.centroid.y,this.z);
		DataPacket taskProposal = new DataPacket(this.id, position, null, task, true);
		sendData(taskProposal, ignite, true);
	}

	// Accept task
	public void propose(Task task, Ignite ignite){
		double distance = Math.sqrt(Math.pow(this.x - task.centroid.x,2) + Math.pow(this.y - task.centroid.y,2));
		double offer = task.utility / distance;
		lock.lock();
		try{
			task.manager.proposals.put(this, offer);
		} finally{
			lock.unlock();
		}
		
	}

	// Refuse task
	public void refuse(Task task, Ignite ignite){
		//DataPacket taskRefused = new DataPacket(-1, null, null, task, "taskRefused");
		lock.lock();
		try{
			task.manager.proposals.put(this, 0.0);
		} finally{
			lock.unlock();
		}
	}


	// Given a list of DataPacket return the closest one to the UAV
	public Double3D findClosest(LinkedList<DataPacket> data){
		Double3D newPosition = new Double3D();
		Double3D myLoc = new Double3D(this.x,this.y,this.z);
		double distance = 1000000;

		for(DataPacket dp : data) {
			if(myLoc.distance(dp.payload.position) < distance) {
					distance = myLoc.distance(dp.payload.position);
					newPosition = dp.payload.position;
			 }
		}
		return newPosition;
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
