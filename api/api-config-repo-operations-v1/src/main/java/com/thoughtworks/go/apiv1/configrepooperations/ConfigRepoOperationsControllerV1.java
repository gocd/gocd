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
package com.thoughtworks.go.apiv1.configrepooperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv1.configrepooperations.representers.PreflightResultRepresenter;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.EphemeralConfigOrigin;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugin.access.configrepo.InvalidPartialConfigException;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.OPERATIONS_BASE;
import static com.thoughtworks.go.spark.Routes.ConfigRepos.PREFLIGHT_PATH;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class ConfigRepoOperationsControllerV1 extends ApiController implements SparkSpringController {
    private static final UuidGenerator UUID = new UuidGenerator();

    private final ApiAuthenticationHelper authenticationHelper;
    private final GoConfigPluginService pluginService;
    private final ConfigRepoService service;
    private final GoConfigService gcs;
    private final PartialConfigService partialConfigService;

    @Autowired
    public ConfigRepoOperationsControllerV1(ApiAuthenticationHelper authenticationHelper, GoConfigPluginService pluginService, ConfigRepoService service, GoConfigService gcs, PartialConfigService partialConfigService) {
        super(ApiVersion.v1);
        this.authenticationHelper = authenticationHelper;
        this.pluginService = pluginService;
        this.service = service;
        this.gcs = gcs;
        this.partialConfigService = partialConfigService;
    }

    @Override
    public String controllerBasePath() {
        return OPERATIONS_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, authenticationHelper::checkAdminUserAnd403);
            before("", mimeType, this::verifyContentType);

            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, authenticationHelper::checkAdminUserAnd403);
            before(PREFLIGHT_PATH, mimeType, this::setMultipartUpload);

            post(PREFLIGHT_PATH, mimeType, this::preflight);
        });
    }

    String preflight(Request req, Response res) throws IOException {
        ConfigRepoConfig repo = repoFromRequest(req);
        ConfigRepoPlugin plugin = pluginFromRequest(req);
        PartialConfigLoadContext context = configContext(repo);

        final PreflightResult result = new PreflightResult();

        try {
            Collection<Part> uploads = req.raw().getParts();
            Map<String, String> contents = new LinkedHashMap<>();

            for (Part ul : uploads) {
                if (!"files[]".equals(ul.getName())) {
                    continue;
                }

                StringWriter w = new StringWriter();
                IOUtils.copy(ul.getInputStream(), w, StandardCharsets.UTF_8);
                contents.put(ul.getSubmittedFileName(), w.toString());
            }

            if (contents.isEmpty()) {
                result.update(Collections.singletonList("No file content provided; check to make sure you POST the form data as `files[]=`"), false);
            } else {
                PartialConfig partialConfig = plugin.parseContent(contents, context);
                partialConfig.setOrigins(adHocConfigOrigin(repo));
                CruiseConfig config = partialConfigService.merge(partialConfig, context.configMaterial().getFingerprint(), gcs.clonedConfigForEdit());

                gcs.validateCruiseConfig(config);
                result.update(Collections.emptyList(), true);
            }
        } catch (RecordNotFoundException e) {
            throw e;
        } catch (InvalidPartialConfigException e) {
            result.update(Collections.singletonList(e.getErrors()), false);
        } catch (GoConfigInvalidException e) {
            result.update(e.getAllErrors(), false);
        } catch (Exception e) {
            throw HaltApiResponses.haltBecauseOfReason(e.getMessage());
        }

        return writerForTopLevelObject(req, res, w -> PreflightResultRepresenter.toJSON(w, result));
    }

    private ConfigOrigin adHocConfigOrigin(ConfigRepoConfig repo) {
        return new EphemeralConfigOrigin(format("preflighted <%s>", null == repo ? "NEW REPO" : repo.getId()));
    }

    private PartialConfigLoadContext configContext(ConfigRepoConfig repo) {
        return new PartialConfigLoadContext() {
            @Override
            public Configuration configuration() {
                return null; // don't allow parse-content to view configurations as they may expose secrets
            }

            /**
             * This is primarily used for two things:
             *
             * 1) When a pipeline definition defines its material type as `configrepo`, this
             *    provides the material configuration used by the {@link ConfigRepoConfig}
             *    itself; this means that the pipeline definition is on the same repo as the
             *    pipeline build material. For preflight, if no `repoId` is provided, we return
             *    a dummy material; this would be the case when someone is preflighting a definition
             *    in a new config repo that has not been created yet.
             *
             * 2) We use the fingerprint of the material here to match against an existing partial
             *    when merging the candidate {@link PartialConfig} into a {@link CruiseConfig}. For
             *    an existing {@link ConfigRepoConfig}, this will allow us to replace the current
             *    partial with the preflight partial so we can perform validations.
             *
             * @return a {@link MaterialConfig}
             */
            @Override
            public MaterialConfig configMaterial() {
                return null == repo ? dummyMaterial() : repo.getRepo();
            }

            private MaterialConfig dummyMaterial() {
                GitMaterialConfig gitMaterialConfig = new GitMaterialConfig();
                gitMaterialConfig.setUrl(UUID.randomUuid());
                return gitMaterialConfig;
            }
        };
    }

    private ConfigRepoPlugin pluginFromRequest(Request req) {
        String pluginId = req.queryParams("pluginId");
        if (StringUtils.isBlank(pluginId)) {
            throw HaltApiResponses.haltBecauseRequiredParamMissing("pluginId");
        }
        return (ConfigRepoPlugin) pluginService.partialConfigProviderFor(pluginId);
    }

    private ConfigRepoConfig repoFromRequest(Request req) {
        String repoId = req.queryParams("repoId");
        if (null == repoId) {
            return null;
        }

        ConfigRepoConfig repo = service.getConfigRepo(repoId);

        if (null == repo) {
            throw new RecordNotFoundException(EntityType.ConfigRepo, repoId);
        }

        return repo;
    }
}
