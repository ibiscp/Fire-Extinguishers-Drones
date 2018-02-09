/**
 * Define all the possible actions of the agents.
 * JOIN an area (if hovering over it)
 * LEAVE an area (if previously joined)
 * MOVE toward an area of interests 
 * EXSTINGUISH the fire in an area (if previously joined and not left)
 * 
 * @author dario albani
 * @mail albani@dis.uniroma1.it
 * @thanks Sean Luke
 */

package sim.app.firecontrol;

public enum AgentAction {
	SELECT_TASK, SELECT_CELL, MOVE, EXTINGUISH
}
