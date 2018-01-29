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

package com.thoughtworks.go.apiv1.admin.artifactstore.spring;

import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.admin.artifactstore.ArtifactStoresControllerV1Delegate;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.ArtifactStoreService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArtifactStoreControllerV1 implements SparkSpringController {
    private final ArtifactStoresControllerV1Delegate delegate;

    @Autowired
    public ArtifactStoreControllerV1(ArtifactStoreService artifactStoreService, EntityHashingService entityHashingService, ApiAuthenticationHelper authenticationHelper, Localizer localizer) {
        delegate = new ArtifactStoresControllerV1Delegate(artifactStoreService, entityHashingService, authenticationHelper, localizer);
    }


    @Override
    public void setupRoutes() {
        delegate.setupRoutes();
    }
}
