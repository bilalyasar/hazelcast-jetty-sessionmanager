package com.hazelcast.session;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

public class JettyConfigurator extends WebContainerConfigurator<Server> {

    Server server;

    private String clientServerConfigLocation;
    private String p2pConfigLocation;

    public JettyConfigurator(String p2pConfigLocation, String clientServerConfigLocation) {
        super();
        this.p2pConfigLocation = p2pConfigLocation;
        this.clientServerConfigLocation = clientServerConfigLocation;
    }

    public JettyConfigurator() {
    }

    @Override
    public Server configure() throws Exception {
        Server server = new Server(port);

        final URL root = new URL(JettyConfigurator.class.getResource("/"), "../test-classes");
        final String cleanedRoot = URLDecoder.decode(root.getFile(), "UTF-8");

        final String fileSeparator = File.separator.equals("\\") ? "\\\\" : File.separator;
        final String sourceDir = cleanedRoot + File.separator + JettyConfigurator.class.getPackage().getName().replaceAll("\\.", fileSeparator) + File.separator + "webapp" + File.separator;

        WebAppContext context = new WebAppContext();
        context.setResourceBase(sourceDir);
        context.setDescriptor(sourceDir + "/WEB-INF/web.xml");
        context.setLogUrlOnStart(true);
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        HazelcastSessionIdManager idManager;

        if (!clientOnly) {
            idManager = new HazelcastSessionIdManager(server, clientOnly, p2pConfigLocation);
        } else {
            idManager = new HazelcastSessionIdManager(server, clientOnly, clientServerConfigLocation);
        }

        idManager.setWorkerName("worker-" + port);
        server.setSessionIdManager(idManager);

        HazelcastSessionManager sessionManager = new HazelcastSessionManager();
        sessionManager.setSessionIdManager(idManager);

        SessionHandler handler = new SessionHandler(sessionManager);
        context.setSessionHandler(handler);

        server.setHandler(context);
        server.setStopTimeout(0);

        return server;
    }

    @Override
    public void start() throws Exception {
        server = configure();
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void reload() {
        try {
            server.stop();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
