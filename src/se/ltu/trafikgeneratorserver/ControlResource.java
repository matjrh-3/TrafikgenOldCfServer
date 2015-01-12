package se.ltu.trafikgeneratorserver;

import se.ltu.trafikgeneratorcoap.testing.SendTest;
import se.ltu.trafikgeneratorcoap.testing.Settings;
import se.ltu.trafikgeneratorcoap.testing.TrafficConfig;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ControlResource extends ResourceBase  {
	/*
	 * The control resource is meant to handle requests on
	 * coap://server/control and we expect three kinds of requests:
	 * * GET, for a client to ask a server to send data,
	 * * POST, for a client to ask a server to prepare
	 *         a test server on which it can receive data, and
	 * * DELETE, for a client to tell a server that it can delete
	 *           its test server (identified with a token).
	 */
	private TrafikgeneratorServer server;
	//private PacketDumper pcapLog;
	private TrafficConfig sendConfig = null;
	private String sendToken = null, sendTime = null;
	public ControlResource(String name, TrafikgeneratorServer server) {
		super(name);
		this.server = server;
	}
	public void handleGET(CoapExchange exchange) {
		/*
		 * A client that sends a GET request wants to receive data.
		 * If the request has a payload, it's in the configuration phase.
		 * If the request has no payload, the client is signalling that
		 * it is ready to receive.
		 * 
		 * TODO: generalize so that a control resource can handle several GET requests
		 * from different clients. 
		 */
		if (exchange.getRequestPayload().length == 0 && sendConfig != null) {
			try {
				/*
				 * If a config is set, we may assume that a config has been sent;
				 * so we accept the request (send an empty ACK), send the test/dummy data,
				 * stop packet logging and tell the client to delete its own server.
				 */
				exchange.accept();
				SendTest.run(sendConfig);
				//pcapLog.stop();
				String testURI = String.format(Locale.ROOT, "coap://%1$s:%2$d/test", sendConfig.getStringSetting(Settings.TEST_SERVER), sendConfig.getIntegerSetting(Settings.TEST_TESTPORT));
				Request.newDelete().setURI(testURI).send();
				sendConfig = null;
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
		else {
			/*
			 * If we get a GET request with a payload, we assume that the payload
			 * is a test run configuration. We need to do a slight modification
			 * of the config: set the test data recipient as the originating client.
			 * We also start a packet capture thread through JNetPcap.
			 */
			sendToken = exchange.advanced().getCurrentRequest().getTokenString();
			sendConfig = new TrafficConfig(exchange.getRequestText());
			sendConfig.setStringSetting(Settings.TEST_SERVER, exchange.getSourceAddress().getHostAddress());
			String query = exchange.getRequestOptions().getURIQueryString();
			sendTime = query.split("=")[1];
			File root = new File(System.getProperty("user.home"));
			File appRoot = new File(root, "trafikgeneratorcoap");
			File subDir = new File(appRoot, "logs");
			File logfile = new File(subDir, sendTime + "-" + sendToken + "-sndr.pcap");
			logfile.getParentFile().mkdirs();
/*
			if (!logfile.exists()) { // We do not want to overwrite any existing log files.
				try {
					//pcapLog = new PacketDumper(logfile, sendConfig.getIntegerSetting(Settings.TEST_TESTPORT));
					//new Thread(pcapLog).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			*/
			exchange.respond(ResponseCode.CONTINUE);
		}
	}
	public void handlePOST(CoapExchange exchange) {
		/*
		 * A POST request, that is, a request to ready a server that can receive data,
		 * is assumed to carry necessary information both in the query string and in
		 * the payload. The plain text payload is interpreted as a list of settings
		 * which the server must use. We also ready a packet capture.
		 */
		exchange.accept();
		//new HandlePostThread(exchange, server, pcapLog).start();
		/*
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
				pcapLog = new PacketDumper(logfile, testConfig.getInt(NetworkConfigDefaults.DEFAULT_COAP_PORT));
				new Thread(pcapLog).start();
				if (logfile.exists()) {
					TrafikgeneratorServer testserver = new TrafikgeneratorServer(testConfig);
					testserver.setExecutor(Executors.newScheduledThreadPool(4));
					TestResource test = new TestResource("test");
					testserver.token = token;
					testserver.add(test);
					testserver.start();
					server.subservers.add(testserver);
					exchange.respond(ResponseCode.CREATED);
					testserver.stop();
				}
			}
		} catch (IOException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}*/
	}
	public void handleDELETE(CoapExchange exchange) {
		/*
		 * A DELETE request tells the server to close some test server.
		 * Therefore, we check to see if the token given in the request line
		 * is associated with any server, and shut it down if applicable.
		 */
		exchange.accept();
		String query = exchange.getRequestOptions().getURIQueryString();
		if (query.split("&").length == 1 && query.split(",").length == 1 && query.split("=")[0].equals("token")) {
			String payload = exchange.getRequestText();
			Long clientTimeAfterTest = 0l;
			if (payload.split("=").length == 2 && payload.split("=")[0].equals("NTP_OFFSET"))
				clientTimeAfterTest = Long.valueOf(payload.split("=")[1]);
			String token = query.split("=")[1];
			for (TrafikgeneratorServer server : this.server.subservers) {
				if (server.token.equals(token)) {
					//pcapLog.stop();
					server.clientTimeAfterTest = clientTimeAfterTest;
					server.stop();
					exchange.respond(ResponseCode.DELETED);
				}
			}
		}
		else
			exchange.respond(ResponseCode.BAD_REQUEST);
	}
}
