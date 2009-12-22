package com.where.web;

import org.restlet.Restlet;
import org.restlet.routing.Router;

public class GaeWmtRestApplication extends WmtRestApplication{

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public Restlet createInboundRoot() {
       Router router = new Router(getContext());

        // Defines only one route
        router.attach("/", RootResource.class);
        router.attach("/"+LINE_RESOURCE_NAME+"/{"+LINE_URL_PATH_NAME+"}", GaeLinesResource.class);
        router.attach("/"+STATIONS_RESOURCE_NAME+"/{"+LINE_URL_PATH_NAME+"}", StationsResource.class);
        
        return router;
    }
}
