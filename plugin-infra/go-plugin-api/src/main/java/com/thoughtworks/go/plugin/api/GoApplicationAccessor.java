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
package com.thoughtworks.go.plugin.api;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;

/**
 * GoApplicationAccessor will provide an abstraction over communication from plugin to Go. An instance GoApplicationAccessor
 * will be provided to plugin via GoPlugin abstraction
 */
public abstract class GoApplicationAccessor {
    /**
     * Submits an instance of GoApiRequest to Go and returns an instance of GoApiResponse
     *
     * @param request An instance of GoApiRequest
     * @return an instance of GoApiResponse
     */
    public abstract GoApiResponse submit(GoApiRequest request);
}

