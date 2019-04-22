/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.configrepo.migrator;

import com.bazaarvoice.jolt.Transform;
import com.thoughtworks.go.config.migration.UrlDenormalizerXSLTMigration121;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class V5 implements Transform {
    @Override
    public Object transform(Object input) {

        if (input instanceof Map) {
            Object pipelines = ((Map) input).get("pipelines");
            if (pipelines instanceof List) {
                for (Object pipeline : (List) pipelines) {
                    doTransformPipeline(pipeline);
                }
            }
        }
        return input;
    }

    private void doTransformPipeline(Object mapLike) {
        if (mapLike instanceof Map) {
            Map pipeline = (Map) mapLike;
            Object materials = pipeline.get("materials");
            if (materials instanceof List) {
                for (Object material : (List) materials) {
                    doTransformMaterial(material);
                }

            }
        }
    }

    private void doTransformMaterial(Object mapLike) {
        if (mapLike instanceof Map) {
            Map material = (Map) mapLike;

            if ("git".equals(material.get("type")) || "hg".equals(material.get("type"))) {
                doTransformGitAndHgMaterial(material);
            }
        }
    }

    private void doTransformGitAndHgMaterial(Map<Object, Object> material) {
        Object maybeUrlString = material.get("url");
        if (maybeUrlString instanceof String) {
            String url = (String) maybeUrlString;

            String newUrl = UrlDenormalizerXSLTMigration121.urlWithoutCredentials(url);
            String username = UrlDenormalizerXSLTMigration121.getUsername(url);
            String password = UrlDenormalizerXSLTMigration121.getPassword(url);

            material.put("url", newUrl);

            if (isNotBlank(username)) {
                material.put("username", username);
            }
            if (isNotBlank(password)) {
                material.put("password", password);
            }
        }
    }
}
