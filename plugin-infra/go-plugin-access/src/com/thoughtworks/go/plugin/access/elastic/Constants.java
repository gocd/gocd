/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

public class Constants {
    public final static String EXTENSION_NAME = "elastic-agent";

    public static final String REQUEST_PREFIX = "go.cd.elastic-agent";

    public static final String REQUEST_CREATE_AGENT = REQUEST_PREFIX + ".create-agent";
    public static final String REQUEST_CAN_PLUGIN_HANDLE = REQUEST_PREFIX + ".can-plugin-handle";
    public static final String REQUEST_SERVER_PING = REQUEST_PREFIX + ".server-ping";
    public static final String REQUEST_SHOULD_ASSIGN_WORK = REQUEST_PREFIX + ".should-assign-work";
    public static final String REQUEST_NOTIFY_AGENT_BUSY = REQUEST_PREFIX + ".notify-agent-busy";
}
