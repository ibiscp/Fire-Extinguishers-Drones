/*
 * The GUI for the simulation
 *
 * @author dario albani
 * @mail dario.albani@istc.cnr.it
 * @thanks Sean Luke
 */

package sim.app.firecontrol;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.continuous.Continuous3DPortrayal2D;
import sim.portrayal.grid.FastObjectGridPortrayal2D;
import sim.portrayal.simple.CircledPortrayal2D;
import sim.portrayal.simple.LabelledPortrayal2D;
import sim.portrayal.simple.OrientedPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.gui.SimpleColorMap;

public class IgniteWithUI extends GUIState
{
	public Display2D display;
	public JFrame displayFrame;

	public static void main(String[] args){
		new IgniteWithUI().createController();  // randomizes by currentTimeMillis
	}

	Continuous3DPortrayal2D airPortrayal = new Continuous3DPortrayal2D();
	FastObjectGridPortrayal2D cellPortrayal = new FastObjectGridPortrayal2D(){
		public double doubleValue(Object obj){
			WorldCell cell = (WorldCell) obj;
			if(cell.type == CellType.NORMAL || cell.type == CellType.EXTINGUISHED){
				return 0;
			}
			if(cell.type == CellType.FIRE){
				return 1;
			}
			if(cell.type == CellType.BURNED){
				return 2;
			}
			if(cell.type == CellType.WATER){
				return 3;
			}
			return 0;
		}
	};

	public IgniteWithUI(){
		super(new Ignite(System.currentTimeMillis()));
	}

	public IgniteWithUI(SimState state){
		super(state);
	}

	public static String getName(){
		return "Home Work AI - IGNITE";
	}

	public void start(){
		super.start();
		setupPortrayals();
	}

	public void load(SimState state){
		super.load(state);
		setupPortrayals();
	}

	public void setupPortrayals(){
		Ignite ignite= (Ignite)state;

		// set up the cellPortrayal
		this.cellPortrayal.setField(ignite.forest);
		Color colors[] = new Color[]{new Color(50,180,20,255), new Color(180,50,20,255), Color.gray, Color.blue};
		final SimpleColorMap map = new sim.util.gui.SimpleColorMap(colors);
		cellPortrayal.setMap(map);
		cellPortrayal.setGridLines(true);
		cellPortrayal.setGridModulus(1);
		cellPortrayal.setGridLineFraction(0.025);
		cellPortrayal.setGridColor(Color.BLACK);

		// set up the airPortrayal
		airPortrayal.setField(ignite.air);
		OrientedPortrayal2D op = new OrientedPortrayal2D(new OvalPortrayal2D(Color.white, 0.85){
			private static final long serialVersionUID = 1L;

			public void draw(Object object, Graphics2D graphics, DrawInfo2D info){
				paint = Color.white;
				super.draw(object, graphics, info);
			}
		}, 0, 1, new Color(50,50,50), OrientedPortrayal2D.SHAPE_COMPASS);
		op.setDrawFilled(true);
		airPortrayal.setPortrayalForAll(new CircledPortrayal2D(new LabelledPortrayal2D(op, 1.5, null, Color.white, true), 0, 2.5, Color.red, true));

		// reschedule the displayer
		display.reset();
		display.setBackdrop(new Color(255,242,223,255));

		// redraw the displays
		display.repaint();

	}

	public void init(Controller c)
	{
		super.init(c);

		// make the displayer
		int dispWidth = 500;
		int dispHeight = 500;
		display = new Display2D(dispWidth, dispHeight, this);
		displayFrame = display.createFrame();
		displayFrame.setTitle("Forest");
		c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);
		display.attach( cellPortrayal, "Forest" );
		display.attach( airPortrayal, "UAVs" , dispWidth/(Ignite.width*2), dispHeight/(Ignite.height*2), true);
	}

	public void quit()
	{
		super.quit();

		if (displayFrame!=null) displayFrame.dispose();
		displayFrame = null;
		display = null;
	}

}
