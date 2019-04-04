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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.ReplaceElasticAgentInformationCommand;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.infra.ElasticAgentInformationMigrator;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.json.JsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;

@Component
public class ElasticAgentInformationMigratorImpl implements ElasticAgentInformationMigrator {
    private final ClusterProfilesService clusterProfilesService;
    private final ElasticProfileService elasticProfileService;
    private ElasticAgentExtension elasticAgentExtension;
    private PluginManager pluginManager;
    private GoConfigService goConfigService;
    private PluginSqlMapDao pluginSqlMapDao;

    @Autowired
    public ElasticAgentInformationMigratorImpl(PluginSqlMapDao pluginSqlMapDao, ClusterProfilesService clusterProfilesService, ElasticProfileService elasticProfileService, ElasticAgentExtension elasticAgentExtension, PluginManager pluginManager, GoConfigService goConfigService) {
        this.pluginSqlMapDao = pluginSqlMapDao;
        this.clusterProfilesService = clusterProfilesService;
        this.elasticProfileService = elasticProfileService;
        this.elasticAgentExtension = elasticAgentExtension;
        this.pluginManager = pluginManager;
        this.goConfigService = goConfigService;
    }

    @Override
    public void migrate(GoPluginDescriptor pluginDescriptor) {
        String pluginId = pluginDescriptor.id();
        boolean isElasticAgentPlugin = pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, pluginId);

        if (!isElasticAgentPlugin) {
            return;
        }

        Plugin plugin = pluginSqlMapDao.findPlugin(pluginId);
        String pluginConfiguration = plugin.getConfiguration();
        HashMap<String, String> pluginSettings = (pluginConfiguration == null) ? new HashMap<>() : JsonHelper.fromJson(pluginConfiguration, HashMap.class);
        ReplaceElasticAgentInformationCommand command = new ReplaceElasticAgentInformationCommand(clusterProfilesService, elasticProfileService, elasticAgentExtension, goConfigService, pluginDescriptor, pluginSettings);

        update(command);
    }

    private void update(EntityConfigUpdateCommand command) {
        try {
            goConfigService.updateConfig(command, new Username("GoCD"));
        } catch (Exception e) {
            //todo: mark plugin invalid if migration fails
//            e.getMessage()
        }
    }
}
