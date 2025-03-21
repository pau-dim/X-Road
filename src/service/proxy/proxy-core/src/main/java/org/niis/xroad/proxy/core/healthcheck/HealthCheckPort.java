/*
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.proxy.core.healthcheck;

import ee.ria.xroad.common.SystemProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.eclipse.jetty.http.HttpStatus.SERVICE_UNAVAILABLE_503;


/**
 * Service that listens for health check requests on a specific port and interface
 */
@Slf4j
public class HealthCheckPort implements InitializingBean, DisposableBean {

    public static final String MAINTENANCE_MESSAGE = "Health check interface is in maintenance mode.";

    private static final int SOCKET_MAX_IDLE_MILLIS = 30000;
    private static final int THREAD_POOL_SIZE = 8;
    private static final int ACCEPTOR_THREAD_COUNT = 1;
    private static final int SELECTOR_THREAD_COUNT = 1;

    private final Server server;
    private final StoppableHealthCheckProvider stoppableHealthCheckProvider;
    private final int portNumber;
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    /**
     * Create a new {@link HealthCheckPort}.
     */
    public HealthCheckPort(HealthChecks healthChecks) {
        server = new Server(new QueuedThreadPool(THREAD_POOL_SIZE));
        stoppableHealthCheckProvider = new StoppableCombinationHealthCheckProvider(healthChecks);
        portNumber = SystemProperties.getHealthCheckPort();
        createHealthCheckConnector();
    }

    public HealthCheckPort(StoppableHealthCheckProvider testProvider, int testPort) {
        server = new Server(new QueuedThreadPool(THREAD_POOL_SIZE));
        stoppableHealthCheckProvider = testProvider;
        portNumber = testPort;
        createHealthCheckConnector();
    }

    private void createHealthCheckConnector() {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSendServerVersion(false);
        HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfiguration);
        ServerConnector connector = new ServerConnector(server, ACCEPTOR_THREAD_COUNT, SELECTOR_THREAD_COUNT,
                connectionFactory);
        connector.setName("HealthCheckPort");
        connector.setHost(SystemProperties.getHealthCheckInterface());
        connector.setPort(portNumber);
        connector.setIdleTimeout(SOCKET_MAX_IDLE_MILLIS);
        server.addConnector(connector);

        var handlerCollection = new Handler.Sequence();
        handlerCollection.addHandler(new HealthCheckHandler(stoppableHealthCheckProvider));

        server.setHandler(handlerCollection);
    }

    /**
     * A setter for the HealthCheckPort maintenance mode
     *
     * @param targetState boolean value for the intended new state of maintenance mode
     * @return returns a String representation of the occurred state transition
     */
    public String setMaintenanceMode(boolean targetState) {
        boolean oldValue = maintenanceMode.getAndSet(targetState);
        return "Maintenance mode set: "
                + oldValue
                + " => "
                + targetState;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Started HealthCheckPort on port {}", portNumber);
        server.start();
    }


    @Override
    public void destroy() throws Exception {
        log.info("Stopping HealthCheckPort on port {}", portNumber);
        server.stop();
        stoppableHealthCheckProvider.stop();
    }


    /**
     * A {@link org.eclipse.jetty.server.Handler} for health check requests. It responds to all requests the same way
     * based on the health check results and does not filter out requests at all. Blocking requests is left for the
     * firewall.
     */
    @RequiredArgsConstructor
    public class HealthCheckHandler extends Handler.Abstract {

        private final HealthCheckProvider healthCheckProvider;

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {

            if (isMaintenanceMode()) {
                response.setStatus(SERVICE_UNAVAILABLE_503);
                Content.Sink.write(response, true, MAINTENANCE_MESSAGE, callback);
            } else {
                HealthCheckResult result = healthCheckProvider.get();
                if (result.isOk()) {
                    response.setStatus(OK_200);
                    callback.succeeded();
                } else {
                    response.setStatus(INTERNAL_SERVER_ERROR_500);
                    Content.Sink.write(response, true, result.getErrorMessage().concat(System.lineSeparator()), callback);
                }
            }

            return true;
        }
    }
}
