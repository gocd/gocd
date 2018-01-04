/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.common.serverinfo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MessageHandlerForServerInfoRequestProcessor1_0 implements MessageHandlerForServerInfoRequestProcessor {

    @Override
    public String serverInfoToJSON(String serverId, String siteUrl, String secureSiteUrl) {
        Gson gson = new Gson();
        JsonObject object = new JsonObject();
        object.addProperty("server_id", serverId);
        object.addProperty("site_url", siteUrl);
        object.addProperty("secure_site_url", secureSiteUrl);

        return gson.toJson(object);
    }
}
