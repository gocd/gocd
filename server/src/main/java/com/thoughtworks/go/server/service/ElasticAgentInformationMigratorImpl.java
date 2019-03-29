package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.ReplaceElasticAgentInformationCommand;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.infra.ElasticAgentInformationMigrator;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;

@Component
public class ElasticAgentInformationMigratorImpl implements ElasticAgentInformationMigrator {
    private PluginService pluginService;
    private final ClusterProfilesService clusterProfilesService;
    private final ElasticProfileService elasticProfileService;
    private ElasticAgentExtension elasticAgentExtension;
    private PluginManager pluginManager;
    private GoConfigService goConfigService;

    @Autowired
    public ElasticAgentInformationMigratorImpl(PluginService pluginService, ClusterProfilesService clusterProfilesService, ElasticProfileService elasticProfileService, ElasticAgentExtension elasticAgentExtension, PluginManager pluginManager, GoConfigService goConfigService) {
        this.pluginService = pluginService;
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

        ReplaceElasticAgentInformationCommand command = new ReplaceElasticAgentInformationCommand(pluginService, clusterProfilesService, elasticProfileService, elasticAgentExtension, goConfigService, pluginDescriptor);
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
