package se.ltu.trafikgeneratorserver;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

class TestResource extends ResourceBase  {
	/*
	 * The test resource, corresponding to coap://server:port/test,
	 * needs nothing advanced: only acknowledging CON CoAP messages.
	 */
	public TestResource(String name) {
		super(name);
	}
	public void handlePOST(CoapExchange exchange) {
		//TODO: check if test data comes from the expected IP?
		if (exchange.advanced().getCurrentRequest().isConfirmable())
			exchange.respond(ResponseCode.CONTINUE);
	}
}