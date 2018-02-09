/*
 * Simple structure for a data packet.
 * 
 * @author dario albani
 * @mail dario.albani@istc.cnr.it
 */

package sim.app.firecontrol;

public class DataPacket{

	public class Header{
		public Header(){
			//TODO
			System.err.println("TODO: You have to define the header. Maybe a timestamp and an ID?");
		}
	};

	public class Payload{
		public Payload(){
			//TODO
			System.err.println("TODO: You have to define the payload. What are you going to share?");
		}
	};

	public Header header;
	public Payload payload;

	//TODO
	//define the data packet according to your payload and your header.
	//please, note that if you do not define a good header you could have problem 
	//with duplicates messages
	public DataPacket(){
		this.header = new Header();
		this.payload = new Payload();
	}
}
