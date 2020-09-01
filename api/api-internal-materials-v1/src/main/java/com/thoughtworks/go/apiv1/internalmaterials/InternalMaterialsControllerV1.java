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
import com.thoughtworks.go.apiv1.internalmaterials.models.MaterialInfo;
import com.thoughtworks.go.apiv1.internalmaterials.representers.MaterialWithModificationsRepresenter;
import com.thoughtworks.go.apiv1.internalmaterials.representers.UsagesRepresenter;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.server.service.MaterialConfigService;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@Component
public class InternalMaterialsControllerV1 extends ApiController implements SparkSpringController {
    public static final String FINGERPRINT = "fingerprint";
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final MaterialConfigService materialConfigService;
    private final MaterialService materialService;
    private final MaintenanceModeService maintenanceModeService;

    @Autowired
    public InternalMaterialsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, MaterialConfigService materialConfigService, MaterialService materialService, MaintenanceModeService maintenanceModeService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialConfigService = materialConfigService;
        this.materialService = materialService;
        this.maintenanceModeService = maintenanceModeService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalMaterialConfig.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get(Routes.InternalMaterialConfig.USAGES, mimeType, this::usages);
            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws Exception {
        MaterialConfigs materialConfigs = materialConfigService.getMaterialConfigs(currentUsernameString());
        Map<String, Modification> modifications = materialService.getLatestModificationForEachMaterial();
        Collection<MaintenanceModeService.MaterialPerformingMDU> runningMDUs = maintenanceModeService.getRunningMDUs();
        Map<MaterialConfig, MaterialInfo> mergedMap = createMergedMap(materialConfigs, modifications, runningMDUs);

        final String etag = etagFor(mergedMap);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, writer -> MaterialWithModificationsRepresenter.toJSON(writer, mergedMap));
    }

    public String usages(Request request, Response response) throws Exception {
        String fingerprint = request.params(FINGERPRINT);
        List<String> usagesForMaterial = materialConfigService.getUsagesForMaterial(currentUsernameString(), fingerprint);
        return writerForTopLevelObject(request, response, writer -> UsagesRepresenter.toJSON(writer, fingerprint, usagesForMaterial));
    }

    private Map<MaterialConfig, MaterialInfo> createMergedMap(MaterialConfigs materialConfigs, Map<String, Modification> modificationsMap, Collection<MaintenanceModeService.MaterialPerformingMDU> runningMDUs) {
        HashMap<MaterialConfig, MaterialInfo> map = new HashMap<>();
        if (materialConfigs.isEmpty()) {
            return map;
        }
        List<String> mdus = runningMDUs.stream().map((mdu) -> mdu.getMaterial().getFingerprint()).collect(toList());
        for (MaterialConfig materialConfig : materialConfigs) {
            if (!materialConfig.getType().equals(DependencyMaterialConfig.TYPE)) {
                Modification mod = modificationsMap.getOrDefault(materialConfig.getFingerprint(), null);
                boolean isMDUInProgress = mdus.contains(materialConfig.getFingerprint());
                map.put(materialConfig, new MaterialInfo(mod, isMDUInProgress));
            }
        }
        return map;
    }

    private String etagFor(Object entity) {
        return sha512_256Hex(Integer.toString(entity.hashCode()));
    }
}
