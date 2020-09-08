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
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.internalmaterials.models.MaterialInfo;
import com.thoughtworks.go.apiv1.internalmaterials.representers.MaterialWithModificationsRepresenter;
import com.thoughtworks.go.apiv1.internalmaterials.representers.UsagesRepresenter;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.MaterialConfigService;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.serverhealth.HealthStateScope.*;
import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@Component
public class InternalMaterialsControllerV1 extends ApiController implements SparkSpringController {
    public static final String FINGERPRINT = "fingerprint";
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final MaterialConfigService materialConfigService;
    private final MaterialService materialService;
    private final MaintenanceModeService maintenanceModeService;
    private final MaterialUpdateService materialUpdateService;
    private final MaterialConfigConverter materialConfigConverter;
    private final ServerHealthService serverHealthService;

    @Autowired
    public InternalMaterialsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, MaterialConfigService materialConfigService, MaterialService materialService, MaintenanceModeService maintenanceModeService, MaterialUpdateService materialUpdateService, MaterialConfigConverter materialConfigConverter, ServerHealthService serverHealthService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialConfigService = materialConfigService;
        this.materialService = materialService;
        this.maintenanceModeService = maintenanceModeService;
        this.materialUpdateService = materialUpdateService;
        this.materialConfigConverter = materialConfigConverter;
        this.serverHealthService = serverHealthService;
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
            post(Routes.InternalMaterialConfig.TRIGGER_UPDATE, mimeType, this::triggerUpdate);
            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws Exception {
        Map<MaterialConfig, Boolean> materialConfigs = materialConfigService.getMaterialConfigsWithPermissions(currentUsernameString());
        Map<String, Modification> modifications = materialService.getLatestModificationForEachMaterial();
        Collection<MaintenanceModeService.MaterialPerformingMDU> runningMDUs = maintenanceModeService.getRunningMDUs();
        ServerHealthStates logs = serverHealthService.logs();
        Map<MaterialConfig, MaterialInfo> mergedMap = createMergedMap(materialConfigs, modifications, runningMDUs, logs);

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

    public String triggerUpdate(Request request, Response response) {
        String fingerprint = request.params(FINGERPRINT);
        MaterialConfig materialConfig = materialConfigService.getMaterialConfig(currentUsernameString(), fingerprint);
        if (materialUpdateService.updateMaterial(materialConfigConverter.toMaterial(materialConfig))) {
            response.status(HttpStatus.CREATED.value());
            return MessageJson.create("OK");
        } else {
            response.status(HttpStatus.CONFLICT.value());
            return MessageJson.create("Update already in progress.");
        }
    }

    private Map<MaterialConfig, MaterialInfo> createMergedMap(Map<MaterialConfig, Boolean> materialConfigs, Map<String, Modification> modificationsMap, Collection<MaintenanceModeService.MaterialPerformingMDU> runningMDUs, ServerHealthStates allLogs) {
        HashMap<MaterialConfig, MaterialInfo> map = new HashMap<>();
        if (materialConfigs.isEmpty()) {
            return map;
        }

        materialConfigs.forEach((materialConfig, hasOperatePermission) -> {
            if (!materialConfig.getType().equals(DependencyMaterialConfig.TYPE)) {
                Material material = materialConfigConverter.toMaterial(materialConfig);
                List<HealthStateScope> scopes = asList(forMaterial(material), forMaterialUpdate(material), forMaterialConfig(materialConfig));
                List<ServerHealthState> logs = allLogs.stream().filter((log) -> scopes.contains(log.getType().getScope())).collect(toList());
                Modification mod = modificationsMap.getOrDefault(materialConfig.getFingerprint(), null);
                MaintenanceModeService.MaterialPerformingMDU mduInfo = runningMDUs.stream()
                        .filter((mdu) -> mdu.getMaterial().getFingerprint().equals(materialConfig.getFingerprint()))
                        .findFirst()
                        .orElse(null);
                boolean isMDUInProgress = mduInfo != null;
                Timestamp updateStartTime = isMDUInProgress ? mduInfo.getTimestamp() : null;
                map.put(materialConfig, new MaterialInfo(mod, hasOperatePermission, isMDUInProgress, updateStartTime, logs));
            }
        });
        return map;
    }

    private String etagFor(Object entity) {
        return sha512_256Hex(Integer.toString(entity.hashCode()));
    }
}
