package se.ltu.trafikgeneratorserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class FileResource extends ResourceBase {
	/*
	 * The file resource is meant to handle requests on
	 * coap://server/file and we expect only one kind of request:
	 * * POST, when a client asks the server to store some kind of log
	 *         file. The server only stores the payload as a file if
	 *         if the query is formulated in the right way. And we
	 *         only accept a Pcap log for which we already have our
	 *         own capture. (I.e. save rcvr only if sndr exists.)
	 *         
	 *         The sending of a Pcap log is assumed to have been
	 *         preceded by the sending (and saving) of a meta log.
	 *         The time disparity denoted by the NTP information in
	 *         the meta file is used to adjust timestamps
	 *         in the Pcap log file.
	 */
	private TrafikgeneratorServer server;
	public FileResource(String name, TrafikgeneratorServer server) {
		super(name);
		this.server = server;
	}
	public void handlePOST(CoapExchange exchange) {
		exchange.accept();
		String query = exchange.getRequestOptions().getURIQueryString();
		if (query.split("&").length == 3 && query.split("=").length == 4) {
			String[] options = query.split("&");
			String token = options[1].split("=")[1];
			String time = options[2].split("=")[1];
			String fileType = options[0].split("=")[1];
			File root = new File(System.getProperty("user.home"));
			File appRoot = new File(root, "trafikgeneratorcoap");
			File subDir = new File(appRoot, "logs");
			subDir.mkdirs();
			File metaFile = new File(subDir, (time + "-" + token + "-meta.txt"));
			File rcvrFile = new File(subDir, (time + "-" + token + "-rcvr.pcap"));
			File sndrFile = new File(subDir, (time + "-" + token + "-sndr.pcap"));
			if (rcvrFile.exists() || sndrFile.exists()) {
				if(fileType.equals("log")){
					File localFile = null, remoteFile = null;
					if (rcvrFile.exists() && !sndrFile.exists()) {
						localFile = rcvrFile;
						remoteFile = sndrFile;
					}
					else if (sndrFile.exists() && !rcvrFile.exists()) {
						localFile = sndrFile;
						remoteFile = rcvrFile;
					}
					try {
						FileOutputStream fos = new FileOutputStream(remoteFile);
						fos.write(exchange.getRequestPayload());
						fos.close();
						exchange.respond(ResponseCode.VALID);
						for (TrafikgeneratorServer server : this.server.subservers) {
							if (server.token.equals(token)) {
								BufferedReader metadataReader = new BufferedReader(new FileReader(metaFile));
								String metadata;
								int millisecondsOffset = 0;
								while(metadataReader.ready()){
									metadata = metadataReader.readLine();
									if(metadata.contains("BEFORE_TEST NTP_ERROR="))
										millisecondsOffset = Integer.valueOf(metadata.split("=")[1]);
										//TODO: average of before/after? Just check that values are sane.
										//There were times when AFTER_TEST NTP_ERROR was wildly inaccurate.
								}
								int seconds = millisecondsOffset / 1000;
								int microseconds = (millisecondsOffset - (seconds*1000))*1000;
								//PacketEditor.modifyTimestamps(remoteFile, seconds, microseconds);
								metadataReader.close();
								//TODO: Implement log merging through JNetPcap.
								//In the meanwhile, use, for example, the merging function in Wireshark.
							}
						}
					} catch (IOException e) {
						exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
					}
				} else if(fileType.equals("meta")) {
					try {
						FileOutputStream metadataWriter = new FileOutputStream(metaFile);
						metadataWriter.write(exchange.getRequestPayload());
						metadataWriter.close();						
						exchange.respond(ResponseCode.VALID);
					} catch (IOException e) {
						exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
					}
				}
			}
			else
				exchange.respond(ResponseCode.NOT_FOUND);
		}
		else
			exchange.respond(ResponseCode.BAD_REQUEST);
	}
}
