package se.ltu.trafikgeneratorserver;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class TrafikgeneratorServer extends Server {
	/* The main server application keeps a list of sub-servers; that is,
	 * temporary test servers that run on different ports for different clients.
	 * Each server is identified with the CoAP token with which it was started.
	 */
	ArrayList<TrafikgeneratorServer> subservers = new ArrayList<TrafikgeneratorServer>();
	String token = "";
	//private static PacketDumper pcapLog;
	long clientTimeBeforeTest, clientTimeAfterTest;
	
	public TrafikgeneratorServer(NetworkConfig networkConfig) {
		super(networkConfig);
	}
	
	public static void main(String[] args) {
		/* A Californium resource is in essence which URI paths the server
		 * offers interactions on. We tell the server to keep a "control resource"
		 * on coap://server/control and a "file resource" on coap://server/file .
		 * We also start an NTP server, but this can be supplanted by any other
		 * standard NTP server.
		 */
		
		try {
			TrafikgeneratorServer server = new TrafikgeneratorServer(NetworkConfig.createStandardWithoutFile());
			server.setExecutor(Executors.newScheduledThreadPool(4));
			server.add(new ResourceBase("testing") {
			  	   public void handlePOST(CoapExchange exchange) {
			  		   try {
			  			//   System.out.println("Responding to: " + exchange.getSourcePort());
			  			   exchange.respond(ResponseCode.CREATED, "testing");
			  		   } catch(Exception e) {
			  			   System.out.println("Error: " + e.getMessage());
			  			   exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, "testing");
			  		   }
			  	   }
			    });
				server.start();
			
			//ControlResource control = new ControlResource("testing", server);
			//server.add(control);
			//FileResource fileResource = new FileResource("file", server);
			//server.add(fileResource);
			/*File root = new File(System.getProperty("user.home"));
			File appRoot = new File(root, "trafikgeneratorcoap");
			File subDir = new File(appRoot, "logs");
			
			java.util.Date date= new java.util.Date();
			String timestamp = (new SimpleDateFormat("yyyyMMddHHSS",Locale.getDefault())).format(new Date());
			 
			File logfile = new File(subDir, timestamp + "-COAP-LOG" + "-rcvr.pcap");
			logfile.getParentFile().mkdirs();
			if (!logfile.exists()) { // We do not want to overwrite any existing log files.
				pcapLog = new PacketDumper(logfile, 5683);
				new Thread(pcapLog).start();
				if (logfile.exists()) {
					
				}
			}*/
		} catch(Exception e) {
			System.out.println("Server error: " + e.getMessage());
		}
		//NTPServer.main(null);
	}
}
