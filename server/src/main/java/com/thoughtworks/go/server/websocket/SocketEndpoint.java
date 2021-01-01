/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface SocketEndpoint {
    void send(ByteBuffer data) throws IOException;

    void ping() throws IOException;

    boolean isOpen();

    void close();

    void close(int code, String reason);

    /**
     * Returns a unique key for hashing/registration. Implementations should not rely on underlying session
     * as it may not be guaranteed that registration happens after an onConnect event.
     *
     * @return a unique String
     */
    String key();
}
