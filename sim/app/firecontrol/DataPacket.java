/*
 * Simple structure for a data packet.
 *
 * @author dario albani
 * @mail dario.albani@istc.cnr.it
 */

package sim.app.firecontrol;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.Date;
import sim.util.Double3D;

public class DataPacket{

	public class Header{
		public String timestamp;
		public int id;
		public Header(int id){
			//TODO
			//System.err.println("TODO: You have to define the header. Maybe a timestamp and an ID?");
			this.timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			this.id = id;
		}
	};

	public class Payload{
		public Double3D position;
		public Set<WorldCell> knownCells;

		public Payload(Double3D position, Set<WorldCell> knownCells){
			//TODO
			//System.err.println("TODO: You have to define the payload. What are you going to share?");
			this.position = position;
			this.knownCells = knownCells;
		}
	};

	public Header header;
	public Payload payload;

	//TODO
	//define the data packet according to your payload and your header.
	//please, note that if you do not define a good header you could have problem
	//with duplicates messages
	public DataPacket(int id, Double3D position, Set<WorldCell> knownCells){
		this.header = new Header(id);
		this.payload = new Payload(position, knownCells);
	}
}
