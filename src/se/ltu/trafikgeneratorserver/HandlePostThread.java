package se.ltu.trafikgeneratorserver;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import se.ltu.trafikgeneratorcoap.testing.TrafficConfig;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;

public class HandlePostThread extends Thread{

	CoapExchange exchange;
	TrafikgeneratorServer server;
	//PacketDumper pcapLog;
	public HandlePostThread(CoapExchange exchange, TrafikgeneratorServer server){
		this.exchange = exchange;
		this.server = server;
		//this.pcapLog = pcapLog;
	}
	
	public void run() {
		String options = exchange.getRequestText();
		String query = exchange.getRequestOptions().getURIQueryString();
		String time = query.split("=")[1];
		String token = exchange.advanced().getRequest().getTokenString();
		File root = new File(System.getProperty("user.home"));
		File appRoot = new File(root, "trafikgeneratorcoap");
		File subDir = new File(appRoot, "logs");
		File logfile = new File(subDir, time + "-" + token + "-rcvr.pcap");
		logfile.getParentFile().mkdirs();
		try {
			if (!logfile.exists()) { // We do not want to overwrite any existing log files.
				NetworkConfig testConfig = TrafficConfig.stringListToNetworkConfig(options);
				//pcapLog = new PacketDumper(logfile, testConfig.getInt(NetworkConfigDefaults.DEFAULT_COAP_PORT));				
				//new Thread(pcapLog).start();
				if (logfile.exists()) {
					/*TrafikgeneratorServer testserver = new TrafikgeneratorServer(testConfig);
					testserver.setExecutor(Executors.newScheduledThreadPool(4));
					TestResource test = new TestResource("test");
					testserver.token = token;
					testserver.add(test);
					testserver.start();
					server.subservers.add(testserver);*/
					exchange.respond(ResponseCode.CREATED);
					//testserver.stop();
				}
			}
		}
		catch (Exception e) {
			System.out.println("Error in server: " + e.getMessage());
			e.printStackTrace();
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
	}
}
