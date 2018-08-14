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

package com.thoughtworks.go.apiv1.adminsconfig.spring;

import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.adminsconfig.AdminControllerV1Delegate;
import com.thoughtworks.go.server.service.AdminsConfigService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminsControllerV1 implements SparkSpringController {
    private final AdminControllerV1Delegate delegate;

    @Autowired
    public AdminsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, AdminsConfigService service) {
        delegate = new AdminControllerV1Delegate(apiAuthenticationHelper, entityHashingService, service);
    }

    @Override
    public void setupRoutes() {
        delegate.setupRoutes();
    }
}
