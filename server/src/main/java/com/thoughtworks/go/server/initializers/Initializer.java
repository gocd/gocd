/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.initializers;

public interface Initializer {
    /**
     * Register listeners, etc. Do not start background processing threads if any. If registered callbacks do any
     * processing, stop them from doing so till startDaemon() is called.
     */
    void initialize();

    /**
     * Start any background processing threads if necessary. Allow registered callbacks to process events. This is
     * not called automatically in case of integration tests.
     */
    void startDaemon();
}
