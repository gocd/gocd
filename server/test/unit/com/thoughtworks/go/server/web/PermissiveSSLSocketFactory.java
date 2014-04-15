/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * From https://github.com/test-load-balancer/tlb (pre http-components) e19d4911b089eeaf1a2c
 */
public class PermissiveSSLSocketFactory implements SecureProtocolSocketFactory {

    private SSLContext sslcontext = null;

    private SSLContext getSSLContext() {
        if (this.sslcontext == null) {
            SSLContext context;
            try {
                context = SSLContext.getInstance("SSL");
                context.init(null, new TrustManager[]{new PermissiveX509TrustManager(null)}, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.sslcontext = context;
        }
        return this.sslcontext;
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException {
        return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params) throws IOException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        return timeout == 0 ? createSocket(host, port, localAddress, localPort) :
                ControllerThreadSocketFactory.createSocket(this, host, port, localAddress, localPort, timeout);
    }

    public Socket createSocket(String host, int port) throws IOException {
        return getSSLContext().getSocketFactory().createSocket(host, port);
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(PermissiveSSLSocketFactory.class));
    }

    public int hashCode() {
        return PermissiveSSLSocketFactory.class.hashCode();
    }

}