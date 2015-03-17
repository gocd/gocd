/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import com.thoughtworks.go.util.SystemUtil;

public class StopJettyFromLocalhostServlet extends HttpServlet {
    final Logger logger = Logger.getLogger(StopJettyFromLocalhostServlet.class);
    private AppServer jettyServer;

    public StopJettyFromLocalhostServlet(AppServer jettyServer) {
        this.jettyServer = jettyServer;
    }

    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        logger.info("Received request to stop jetty.  Request is from:" + request.getRemoteHost());
        if (!SystemUtil.isLocalIpAddress(request.getRemoteAddr())) {
            logger.info("Rejecting request to shutdown Jetty from " + request.getRemoteHost());
            return;
        }
        try {
            Thread thread = new Thread() {
                public void run() {
                    waitOneSecondForJettyToReturnAProperReturnCodeToHttpclient();
                    try {
                        logger.info("stopping Jetty...");
                        jettyServer.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.info("cannot stop Jetty", e);
                    }
                }
            };
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("error servicing request", e);
        }
    }

    private void waitOneSecondForJettyToReturnAProperReturnCodeToHttpclient() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
