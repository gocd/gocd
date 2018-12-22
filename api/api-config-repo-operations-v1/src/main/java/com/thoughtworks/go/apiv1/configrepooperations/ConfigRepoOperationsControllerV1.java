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

package com.thoughtworks.go.apiv1.configrepooperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv1.configrepooperations.representers.PreflightResultRepresenter;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugin.access.PluginNotFoundException;
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
import java.util.*;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.OPERATIONS_BASE;
import static com.thoughtworks.go.spark.Routes.ConfigRepos.PREFLIGHT_PATH;
import static spark.Spark.*;

@Component
public class ConfigRepoOperationsControllerV1 extends ApiController implements SparkSpringController {
    private static final UuidGenerator UUID = new UuidGenerator();
    private static final GoConfigCloner CLONER = new GoConfigCloner();

    private final ApiAuthenticationHelper authenticationHelper;
    private final GoConfigPluginService pluginService;
    private final ConfigRepoService service;
    private final GoConfigService gcs;
    private final GoPartialConfig partialConfigService;

    @Autowired
    public ConfigRepoOperationsControllerV1(ApiAuthenticationHelper authenticationHelper, GoConfigPluginService pluginService, ConfigRepoService service, GoConfigService gcs, GoPartialConfig partialConfigService) {
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
            before("", this::verifyContentType);

            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, authenticationHelper::checkAdminUserAnd403);
            before(PREFLIGHT_PATH, this::setMultpipartUpload);

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
            List<Map<String, String>> contents = new ArrayList<>();

            for (Part ul : uploads) {
                if (!"files[]".equals(ul.getName())) {
                    continue;
                }

                StringWriter w = new StringWriter();
                IOUtils.copy(ul.getInputStream(), w, StandardCharsets.UTF_8);
                contents.add(Collections.singletonMap(ul.getSubmittedFileName(), w.toString()));
            }

            if (contents.isEmpty()) {
                result.update(Collections.singletonList("No file content provided; check to make sure you POST the form data as `files[]=`"), false);
            } else {
                PartialConfig partialConfig = plugin.parseContent(contents, context);
                CruiseConfig config = partialConfigService.merge(partialConfig, context.configMaterial().getFingerprint(), CLONER.deepClone(gcs.getConfigForEditing()));

                gcs.validateCruiseConfig(config);
                result.update(Collections.emptyList(), true);
            }
        } catch (PluginNotFoundException e) {
            throw HaltApiResponses.haltBecauseNotFound(e.getMessage());
        } catch (InvalidPartialConfigException e) {
            result.update(Collections.singletonList(e.getErrors()), false);
        } catch (GoConfigInvalidException e) {
            result.update(e.getAllErrors(), false);
        } catch (Exception e) {
            throw HaltApiResponses.haltBecauseOfReason(e.getMessage());
        }

        return writerForTopLevelObject(req, res, w -> PreflightResultRepresenter.toJSON(w, result));
    }

    private PartialConfigLoadContext configContext(ConfigRepoConfig repo) {
        return new PartialConfigLoadContext() {
            @Override
            public Configuration configuration() {
                return null;
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
                return null == repo ? dummyMaterial() : repo.getMaterialConfig();
            }

            private MaterialConfig dummyMaterial() {
                return new GitMaterialConfig(UUID.randomUuid());
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
            throw HaltApiResponses.haltBecauseNotFound("Could not find a config-repo with id `%s`", repoId);
        }

        return repo;
    }
}