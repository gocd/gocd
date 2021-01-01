/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentXmlToDBMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentXmlToDBMigration.class);

    public static DataSource dataSource;
    private static List<Agent> agentList;

    private static class Agent {
        String uuid;
        String hostname;
        String ipaddress;
        boolean isDisabled;
        String elasticAgentId;
        String elasticPluginId;
        String environments;
        private final String resources;

        public Agent(String uuid, String hostname, String ipaddress, String isDisabled, String elasticAgentId, String elasticPluginId, String environments, String resources) {
            this.uuid = uuid;
            this.hostname = hostname;
            this.ipaddress = ipaddress;
            this.isDisabled = isDisabled.equalsIgnoreCase("true");
            this.elasticAgentId = elasticAgentId;
            this.elasticPluginId = elasticPluginId;
            this.environments = environments;
            this.resources = resources;
        }

        @Override
        public String toString() {
            return "Agent{" +
                    "uuid='" + uuid + '\'' +
                    ", hostname='" + hostname + '\'' +
                    ", ipaddress='" + ipaddress + '\'' +
                    ", isDisabled='" + isDisabled + '\'' +
                    ", elasticAgentId='" + elasticAgentId + '\'' +
                    ", elasticPluginId='" + elasticPluginId + '\'' +
                    ", environments='" + environments + '\'' +
                    ", resources='" + resources + '\'' +
                    '}';
        }
    }

    public static void migrateAgent(String uuid, String hostname, String ipaddress, String isDisabled, String elasticAgentId, String elasticPluginId, String environments, String resources) {
        agentList.add(new Agent(uuid, hostname, ipaddress, isDisabled, elasticAgentId, elasticPluginId, environments, resources));
    }

    public static void endTransaction() throws Exception {
        if (dataSource == null) {
            return;
        }
        logAgentInfo();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            for (Agent agent : agentList) {
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT count(*) from agents where uuid = ?");
                preparedStatement.setString(1, agent.uuid);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    long count = resultSet.getLong(1);
                    if (count == 0) {
                        insert(agent, connection);
                    } else {
                        update(agent, connection);
                    }
                } else {
                    LOGGER.error("No results found in resultset for agent with uuid: {}", agent.uuid);
                }
            }
            connection.commit();
            LOGGER.info("Successfully migrated agents from config to DB");
        } catch (Exception e) {
            LOGGER.error("There was an error migrating agents from config to DB", e);
            throw e;
        } finally {
            agentList = null;
        }
    }

    private static void logAgentInfo() {
        LOGGER.info("Migrating {} agents from config to db.", agentList.size());
        StringBuilder logBuilder = new StringBuilder();
        agentList.forEach(agent -> {
            logBuilder
                    .append(agent.toString())
                    .append('\n');
        });
        LOGGER.info(logBuilder.toString());
    }

    private static void update(Agent agent, Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE agents SET ipaddress=?, hostname=?, disabled=?, elasticAgentId=?, elasticPluginId=?, environments=?, resources=?, deleted=false WHERE uuid=?");
        preparedStatement.setString(1, agent.ipaddress);
        preparedStatement.setString(2, agent.hostname);
        preparedStatement.setBoolean(3, agent.isDisabled);
        preparedStatement.setString(4, agent.elasticAgentId);
        preparedStatement.setString(5, agent.elasticPluginId);
        preparedStatement.setString(6, agent.environments);
        preparedStatement.setString(7, agent.resources);
        preparedStatement.setString(8, agent.uuid);
        preparedStatement.execute();
    }

    private static void insert(Agent agent, Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO agents (uuid, ipaddress, hostname, disabled, elasticAgentId, elasticPluginId, cookie, environments, resources, deleted) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        preparedStatement.setString(1, agent.uuid);
        preparedStatement.setString(2, agent.ipaddress);
        preparedStatement.setString(3, agent.hostname);
        preparedStatement.setBoolean(4, agent.isDisabled);
        preparedStatement.setString(5, agent.elasticAgentId);
        preparedStatement.setString(6, agent.elasticPluginId);
        preparedStatement.setString(7, UUID.randomUUID().toString());
        preparedStatement.setString(8, agent.environments);
        preparedStatement.setString(9, agent.resources);
        preparedStatement.setBoolean(10, false);
        preparedStatement.execute();
    }

    public static void beginTransaction() {
        agentList = new ArrayList<>();
    }
}
