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

package com.thoughtworks.go.apiv1.internalmaterials;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.internalmaterials.representers.MaterialWithModificationsRepresenter;
import com.thoughtworks.go.apiv1.internalmaterials.representers.UsagesRepresenter;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.service.MaterialConfigService;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

@Component
public class InternalMaterialsControllerV1 extends ApiController implements SparkSpringController {
    public static final String FINGERPRINT = "fingerprint";
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final MaterialConfigService materialConfigService;
    private final MaterialService materialService;

    @Autowired
    public InternalMaterialsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, MaterialConfigService materialConfigService, MaterialService materialService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialConfigService = materialConfigService;
        this.materialService = materialService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MaterialConfig.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get(Routes.MaterialConfig.USAGES, mimeType, this::usages);
            get("", mimeType, this::index);
        });
    }

    private String index(Request request, Response response) throws Exception {
        MaterialConfigs materialConfigs = materialConfigService.getMaterialConfigs(currentUsernameString());
        Map<String, Modifications> modifications = materialService.getModificationWithMaterial();
        Map<MaterialConfig, Modifications> mergedMap = createMergedMap(materialConfigs, modifications);
        return writerForTopLevelObject(request, response, writer -> MaterialWithModificationsRepresenter.toJSON(writer, mergedMap));
    }

    public String usages(Request request, Response response) throws Exception {
        String fingerprint = request.params(FINGERPRINT);
        Map<String, List<String>> usagesForMaterial = materialConfigService.getUsagesForMaterial(currentUsernameString(), fingerprint);
        return writerForTopLevelObject(request, response, writer -> UsagesRepresenter.toJSON(writer, fingerprint, usagesForMaterial));
    }

    private Map<MaterialConfig, Modifications> createMergedMap(MaterialConfigs materialConfigs, Map<String, Modifications> modificationsMap) {
        HashMap<MaterialConfig, Modifications> map = new HashMap<>();
        if (materialConfigs.isEmpty()) {
            return map;
        }
        for (MaterialConfig materialConfig : materialConfigs) {
            if (!materialConfig.getType().equals(DependencyMaterialConfig.TYPE)) {
                Modifications mod = modificationsMap.getOrDefault(materialConfig.getFingerprint(), new Modifications());
                map.put(materialConfig, mod);
            }
        }
        return map;
    }
}
