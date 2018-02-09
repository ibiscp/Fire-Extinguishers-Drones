/**
 * This is the entry point of the simulation. 
 * It represents the world over which the simulation is running, it extends the class SimState 
 * and offers all the functions that you are allowed to use in order to manage the state 
 * of the simulation.
 * 
 * Please refer to Mason Documentation for further details about the SimState and the Mason Loop.
 * 
 * @see SimState
 * 
 * @author dario albani
 * @mail albani@dis.uniroma1.it
 * @thanks Sean Luke
 */

package sim.app.firecontrol;

import java.util.ArrayList;
import java.util.LinkedList;

import sim.engine.SimState;
import sim.field.continuous.Continuous3D;
import sim.field.grid.ObjectGrid2D;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.Int2D;

public class Ignite extends SimState{
	private static final long serialVersionUID = 1;

	/* where the agents are moving */
	public Continuous3D air;	
	/* Forest discretization
	 * There are 4(5) different kinds of forest cell:
	 * - normal (or foamed), trees are in good health (or saved)
	 * - fire, there are some fires in the area and the cell requires attention
	 * - burned, there is nothing left to save 
	 * - water, the cell is part of a lake or a river
	 */
	public ObjectGrid2D forest;

	/* simulation params */
	public int numUAVs = 2; //number of mavs involved in the simulation
	public Bag UAVs; // all the agents in the simulation. Bag size is numMavs    

	public static int height = 60; //size of the forest
	public static int width = 60; //size of the forest 
	public static int depth = 50; //max altitude 

	public static int cellsOnFire = 0;
	public static int cellsBurned = 0;
	public static int cellsOnWater = 0;

	public LinkedList<Task> tasks;
	
	/**
	 * Constructor
	 */
	public Ignite(long seed){
		super(seed);
	}	

	/**
	 * Check if the given position associated to an UAV is in forest bounds.
	 * @param Double3D pos, the position to check 
	 * @return true, if the UAV is in bound
	 */
	public static boolean isInBounds(Double3D pos){
		return pos.x >= 0 
				&& pos.y >= 0 
				&& pos.z >= 1
				&& pos.x < width 
				&& pos.y < height
				&& pos.z < depth;
	}

	/**
	 * Start a simulation run
	 */
	public void start(){
		super.start();
		//reset variables for job>1
		cellsOnFire = 0;
		cellsBurned = 0;
		WorldCell.selfIgniteMax = 0;
		
		air = new Continuous3D(1, width, height, depth);
		forest = new ObjectGrid2D(width, height);

		//generate the world
		//fill the world with trees
		for(int w=0; w<width; w++){
			for(int h=0; h<height; h++){
				WorldCell cell = new WorldCell(w,h, CellType.NORMAL);
				forest.field[w][h] = cell;
			}
		}

		//generate lakes
		int lakes = 2;
		for(int l=0; l<lakes; l++){
			Int2D lakeCenter;
			Int2D nextLocation;
			WorldCell cell;

			//try to find a center for the lake
			int maxTries = width*height;
			do{
				lakeCenter = new Int2D(random.nextInt(width), random.nextInt(height));
				cell = ((WorldCell)forest.field[lakeCenter.x][lakeCenter.y]);
				maxTries--;
			}while(cell.type.equals(CellType.WATER) && maxTries>=0);

			//break if not able to find a center after some time
			if(maxTries < 0){
				break;
			}

			//place the center of the lake
			cell = new WorldCell(lakeCenter.x, lakeCenter.y, CellType.WATER);
			forest.field[lakeCenter.x][lakeCenter.y] = cell;

			int radius = 1;
			//start creating the lake
			while(radius<=random.nextInt(width)+2 && radius<=random.nextInt(height)+2){
				for(int i=-radius; i<=radius; i++){
					for(int j=-radius; j<=radius; j++){
						nextLocation = new Int2D(lakeCenter.x+i, lakeCenter.y+j); 
						//place if not already present and if in bounds
						if(nextLocation.x>=0 && nextLocation.y>=0 && 
								nextLocation.x<width && nextLocation.y<height){
							double p = gaussianPDF(Math.sqrt(i*i+j*j),(random.nextInt(3)-1)*random.nextDouble(), 3);

							// if p is too low then there is no water
							if(p > 0.25){
								//place water
								cell = new WorldCell(nextLocation.x, nextLocation.y, CellType.WATER);
								forest.field[nextLocation.x][nextLocation.y] = cell;
							}
						}
					}
				}
				radius++;
			}
		}

		//set the world on fire
		//start with 3 fires and store their centroid in the tasks list
		tasks = new LinkedList<>();
		
		//generate fires
		int fires = 3;
		for(int l=0; l<fires; l++){
			Int2D fireCenter;
			Int2D nextLocation;
			WorldCell cell;

			//try to find a center for the lake
			int maxTries = width*height;
			do{
				fireCenter = new Int2D(random.nextInt(width), random.nextInt(height));
				cell = ((WorldCell)forest.field[fireCenter.x][fireCenter.y]);
				maxTries--;
			}while((cell.type.equals(CellType.FIRE) || cell.type.equals(CellType.WATER)) && maxTries>=0);

			//break if not able to find a center after some time
			if(maxTries < 0){
				break;
			}

			//place the center of the fire
			cell = new WorldCell(fireCenter.x, fireCenter.y, CellType.FIRE);
			forest.field[fireCenter.x][fireCenter.y] = cell;
			cellsOnFire++;
			//generate the task for global knowledge
			Task t = new Task(new Int2D(fireCenter.x, fireCenter.y), 0);
			t.addCell(cell);
			tasks.add(t);
			
			int radius = 1;

			//start creating the fire
			LinkedList<Int2D> extractedLocation = new LinkedList<>();
			extractedLocation.add(fireCenter);
			while(radius<=random.nextInt(width)+2 && radius<=random.nextInt(height)+2){
				for(int i=-radius; i<=radius; i++){
					for(int j=-radius; j<=radius; j++){
						nextLocation = new Int2D(fireCenter.x+i, fireCenter.y+j); 
						//place if not already present and if in bounds
						if(nextLocation.x>=0 && 
								nextLocation.y>=0 && 
								nextLocation.x<width && 
								nextLocation.y<height &&
								!extractedLocation.contains(nextLocation)){
							double p = gaussianPDF(Math.sqrt(i*i+j*j),(random.nextInt(3)-1)*random.nextDouble(), 3);

							// if p is too low then there is no fire
							if(p > 0.85){
								//place fire
								cell = new WorldCell(nextLocation.x, nextLocation.y, CellType.FIRE);
								forest.field[nextLocation.x][nextLocation.y] = cell;
								cellsOnFire++;
								//update the task
								t.notifyNewFire(cell);
								//avoid updating more than once
								extractedLocation.add(nextLocation);
							}
						}
					}
				}
				radius++;
			}
		}

		//schedule all the cells
		for(int w=0; w<width; w++){
			for(int h=0; h<height; h++){
				schedule.scheduleRepeating((WorldCell)forest.field[w][h], 2, 1);
			}
		}
		
		//random placement of agents 
		ArrayList<Double3D> extracted = new ArrayList<Double3D>();
		Double3D location;
		for(int i = 0 ; i < numUAVs; i++){
			do{
				location = new Double3D(random.nextInt(width), random.nextInt(height), random.nextInt(depth));
			} while(extracted.contains(location));
			//store extracted location to avoid duplicates
			extracted.add(location);
			//generate a new UAV
			UAV uav = new UAV(i, location);
			//add the UAV to air at the location extracted
			air.setObjectLocation(uav, location);
			//schedule the agent
			schedule.scheduleRepeating(uav, 1, 1);
		}

		//schedule the fireContrller, used to check the end of the simulation
		FireController fireController = new FireController();
		schedule.scheduleRepeating(fireController,3,1);
	}


	/**
	 * PDF
	 * Compute the value of the gaussian PDF at a given x, with a given mean (location parameter) 
	 * and gaussian variance
	 * @see: https://en.wikipedia.org/wiki/Normal_distribution
	 */
	public double gaussianPDF(double x, double mean, double variance){
		return Math.exp(-((x - mean)*(x - mean))/(2 * (variance*variance)));
	}

	/**
	 * PDF
	 * Compute the value of the gaussian PDF at a given x, with a given mean (location parameter),
	 * gaussian variance and max peak.
	 * @see: https://en.wikipedia.org/wiki/Normal_distribution
	 */
	public double gaussianPDF(double x, double mean, double variance, double peak){
		return peak * Math.exp(-(9*(x - mean)*(x - mean))/(2*(variance*variance)));
	}

	/**
	 * PDF
	 * Compute the value of the cauchy PDF at a given x, with a given mean (location parameter) and cauchy variance
	 * for 0 <= x <= 2*pi, 0 < c < 1.
	 * @see: https://en.wikipedia.org/wiki/Cauchy_distribution for more details
	 */
	public double cauchyPDF(double x, double c){
		return (1-Math.pow(c,2)) / (2*Math.PI*(1+Math.pow(c,2)-2*c*Math.cos(x)));
	}

	public static void main(String[] args){
		doLoop(Ignite.class, args);
		System.exit(0);
	}
}
