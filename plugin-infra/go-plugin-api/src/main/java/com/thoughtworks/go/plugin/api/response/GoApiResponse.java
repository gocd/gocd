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
package com.thoughtworks.go.plugin.api.response;

import java.util.Map;

/**
 * Provides an abstraction for api response sent from Go to plugin
 */
public abstract class GoApiResponse {

    /**
     * Provides response code for the request sent
     *
     * @return
     */
    public abstract int responseCode();

    /**
     * Provides response headers as key value pair for the response. Response headers can be used to send any meta information related to response
     *
     * @return request headers as a Map
     */
    public abstract Map<String, String> responseHeaders();

    /**
     * Provides json formatted response body
     *
     * @return response body
     */
    public abstract String responseBody();

}
