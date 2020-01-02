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
package com.thoughtworks.go.plugin.access.elastic.v5;

import com.google.gson.Gson;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static java.lang.String.format;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ElasticAgentExtensionConverterV5Test {
    private JobIdentifier jobIdentifier;
    private Map<String, String> clusterProfile;

    @Before
    public void setUp() throws Exception {
        clusterProfile = Collections.singletonMap("key", "value");
        jobIdentifier = new JobIdentifier("test-pipeline", 1, "Test Pipeline", "test-stage", "1", "test-job");
        jobIdentifier.setBuildId(100L);
    }

    @Test
    public void shouldUnJSONizeCanHandleResponseBody() {
        assertTrue(new Gson().fromJson("true", Boolean.class));
        assertFalse(new Gson().fromJson("false", Boolean.class));
    }

    @Test
    public void shouldUnJSONizeShouldAssignWorkResponseFromBody() {
        assertTrue(new ElasticAgentExtensionConverterV5().shouldAssignWorkResponseFromBody("true"));
        assertFalse(new ElasticAgentExtensionConverterV5().shouldAssignWorkResponseFromBody("false"));
    }

    @Test
    public void shouldJSONizeCreateAgentRequestBody() throws Exception {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("key1", "value1");
        configuration.put("key2", "value2");

        Map<String, String> clusterProfileConfiguration = new HashMap<>();
        clusterProfileConfiguration.put("key1", "value1");
        clusterProfileConfiguration.put("key2", "value2");

        String json = new ElasticAgentExtensionConverterV5().createAgentRequestBody("secret-key", "prod", configuration, clusterProfileConfiguration, jobIdentifier);

        assertThatJson(json).isEqualTo("{" +
                "  \"auto_register_key\":\"secret-key\"," +
                "  \"elastic_agent_profile_properties\":{" +
                "    \"key1\":\"value1\"," +
                "    \"key2\":\"value2\"" +
                "    }," +
                "  \"cluster_profile_properties\":{" +
                "    \"key1\":\"value1\"," +
                "    \"key2\":\"value2\"" +
                "    }," +
                "  \"environment\":\"prod\"," +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void shouldJSONizeShouldAssignWorkRequestBody() throws Exception {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("property_name", "property_value");
        HashMap<String, String> clusterProfileProperties = new HashMap<>();
        clusterProfileProperties.put("property_name", "property_value");

        String actual = new ElasticAgentExtensionConverterV5().shouldAssignWorkRequestBody(elasticAgent(), "prod", configuration, clusterProfileProperties, jobIdentifier);
        String expected = "{" +
                "  \"environment\":\"prod\"," +
                "  \"agent\":{" +
                "    \"agent_id\":\"52\"," +
                "    \"agent_state\":\"Idle\"," +
                "    \"build_state\":\"Idle\"," +
                "    \"config_state\":\"Enabled\"" +
                "  }," +
                "  \"elastic_agent_profile_properties\":{" +
                "    \"property_name\":\"property_value\"" +
                "  }," +
                "  \"cluster_profile_properties\":{" +
                "    \"property_name\":\"property_value\"" +
                "  }," +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  }\n" +
                "}";

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeJobCompletionRequestBody() throws Exception {
        HashMap<String, String> elasticProfileConfiguration = new HashMap<>();
        elasticProfileConfiguration.put("property_name", "property_value");
        HashMap<String, String> clusterProfileConfiguration = new HashMap<>();
        clusterProfileConfiguration.put("property_name", "property_value");
        String actual = new ElasticAgentExtensionConverterV5().getJobCompletionRequestBody("ea1", jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);

        String expected = "{" +
                "  \"elastic_agent_id\":\"ea1\"," +
                "  \"elastic_agent_profile_properties\":{" +
                "    \"property_name\":\"property_value\"" +
                "  }," +
                "  \"cluster_profile_properties\":{" +
                "    \"property_name\":\"property_value\"" +
                "  }," +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  }\n" +
                "}";

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeServerPingRequestBody() throws Exception {
        HashMap<String, String> clusterProfileConfiguration1 = new HashMap<>();
        clusterProfileConfiguration1.put("property_name", "property_value");
        HashMap<String, String> clusterProfileConfiguration2 = new HashMap<>();
        clusterProfileConfiguration2.put("property_name_1", "property_value_1");
        clusterProfileConfiguration2.put("property_name_2", "property_value_2");
        String actual = new ElasticAgentExtensionConverterV5().serverPingRequestBody(Arrays.asList(clusterProfileConfiguration1, clusterProfileConfiguration2));

        String expected = "{" +
                "  \"all_cluster_profile_properties\":[" +
                "      {" +
                "        \"property_name\":\"property_value\"" +
                "      }," +
                "      {" +
                "        \"property_name_1\":\"property_value_1\"," +
                "        \"property_name_2\":\"property_value_2\"" +
                "      }" +
                "    ]" +
                "}";

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeElasticAgentStatusReportRequestBodyWhenElasticAgentIdIsProvided() throws Exception {
        String elasticAgentId = "my-fancy-elastic-agent-id";
        String actual = new ElasticAgentExtensionConverterV5().getAgentStatusReportRequestBody(null, elasticAgentId, clusterProfile);
        String expected = format("{" +
                "\"cluster_profile_properties\":{" +
                "   \"key\":\"value\"" +
                "   }," +
                "  \"elastic_agent_id\": \"%s\"" +
                "}", elasticAgentId);

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeElasticAgentStatusReportRequestBodyWhenJobIdentifierIsProvided() throws Exception {
        String actual = new ElasticAgentExtensionConverterV5().getAgentStatusReportRequestBody(jobIdentifier, null, clusterProfile);
        String expected = "{" +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  },\n" +
                "\"cluster_profile_properties\":" +
                "  {" +
                "     \"key\":\"value\"" +
                "  }" +
                "}";

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeClusterStatusReportRequestBody() throws Exception {
        String actual = new ElasticAgentExtensionConverterV5().getClusterStatusReportRequestBody(Collections.singletonMap("key1", "value1"));
        String expected = "{" +
                "   \"cluster_profile_properties\":{" +
                "       \"key1\":\"value1\"" +
                "   }" +
                "}";

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldConstructValidationRequest() {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("key1", "value1");
        configuration.put("key2", "value2");
        configuration.put("key3", null);
        String requestBody = new ElasticAgentExtensionConverterV5().validateElasticProfileRequestBody(configuration);
        assertThatJson(requestBody).isEqualTo("{\"key3\":null,\"key2\":\"value2\",\"key1\":\"value1\"}");
    }

    @Test
    public void shouldHandleValidationResponse() {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"error on key one\"}, {\"key\":\"key-two\",\"message\":\"error on key two\"}]";
        ValidationResult result = new ElasticAgentExtensionConverterV5().getElasticProfileValidationResultResponseFromBody(responseBody);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getErrors().size(), is(2));
        assertThat(result.getErrors().get(0).getKey(), is("key-one"));
        assertThat(result.getErrors().get(0).getMessage(), is("error on key one"));
        assertThat(result.getErrors().get(1).getKey(), is("key-two"));
        assertThat(result.getErrors().get(1).getMessage(), is("error on key two"));
    }

    @Test
    public void shouldUnJSONizeGetProfileViewResponseFromBody() {
        String template = new ElasticAgentExtensionConverterV5().getProfileViewResponseFromBody("{\"template\":\"foo\"}");
        assertThat(template, is("foo"));
    }

    @Test
    public void shouldUnJSONizeGetImageResponseFromBody() {
        com.thoughtworks.go.plugin.domain.common.Image image = new ElasticAgentExtensionConverterV5().getImageResponseFromBody("{\"content_type\":\"foo\", \"data\":\"bar\"}");
        assertThat(image.getContentType(), is("foo"));
        assertThat(image.getData(), is("bar"));
    }

    @Test
    public void shouldGetStatusReportViewFromResponseBody() {
        String template = new ElasticAgentExtensionConverterV5().getStatusReportView("{\"view\":\"foo\"}");
        assertThat(template, is("foo"));
    }

    @Test
    public void shouldGetCapabilitiesFromResponseBody() {
        String responseBody = "{" +
                "    \"supports_plugin_status_report\":\"true\"," +
                "    \"supports_cluster_status_report\":\"true\"," +
                "    \"supports_agent_status_report\":\"true\"" +
                "}";

        Capabilities capabilities = new ElasticAgentExtensionConverterV5().getCapabilitiesFromResponseBody(responseBody);

        assertTrue(capabilities.supportsPluginStatusReport());
        assertTrue(capabilities.supportsClusterStatusReport());
        assertTrue(capabilities.supportsAgentStatusReport());
    }

    @Test
    public void shouldGetRequestBodyForMigrateCall_withOldConfig() throws CryptoException {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"));
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new EncryptedConfigurationValue(new GoCipher().encrypt("password")));

        Configuration configuration = new Configuration();
        configuration.add(property1);
        configuration.add(property2);
        Map<String, String> pluginSettings = configuration.getConfigurationAsMap(true);

        List<ClusterProfile> clusterProfiles = new ArrayList<>();
        clusterProfiles.add(new ClusterProfile("prod-cluster", "plugin_id"));

        List<ElasticProfile> elasticAgentProfiles = new ArrayList<>();
        elasticAgentProfiles.add(new ElasticProfile("profile_id", "prod-cluster", new ConfigurationProperty(new ConfigurationKey("some_key"), new ConfigurationValue("some_value")), new ConfigurationProperty(new ConfigurationKey("some_key2"), new EncryptedConfigurationValue(new GoCipher().encrypt("some_value2")))));

        ElasticAgentInformation elasticAgentInformation = new ElasticAgentInformation(pluginSettings, clusterProfiles, elasticAgentProfiles);

        ElasticAgentInformationDTO elasticAgentInformationDTO = new ElasticAgentExtensionConverterV5().getElasticAgentInformationDTO(elasticAgentInformation);
        String requestBody = elasticAgentInformationDTO.toJSON().toString();

        String expectedRequestBody = "{" +
                "    \"plugin_settings\":{" +
                "        \"key2\":\"password\", " +
                "        \"key\":\"value\"" +
                "    }," +
                "    \"cluster_profiles\":[" +
                "        {" +
                "            \"id\":\"prod-cluster\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"properties\":{" +
                "            }" +
                "        }" +
                "    ]," +
                "    \"elastic_agent_profiles\":[" +
                "        {" +
                "            \"id\":\"profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"cluster_profile_id\": \"prod-cluster\"," +
                "            \"properties\":{" +
                "                \"some_key\":\"some_value\", " +
                "                \"some_key2\":\"some_value2\"" +
                "            }" +
                "        }" +
                "    ]" +
                "}\n";

        assertThatJson(expectedRequestBody).isEqualTo(requestBody);
    }

    @Test
    public void shouldGetRequestBodyForMigrateCall_withNewConfig() throws CryptoException {
        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"));
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new EncryptedConfigurationValue(new GoCipher().encrypt("password")));

        Configuration configuration = new Configuration();
        configuration.add(property1);
        configuration.add(property2);
        Map<String, String> pluginSettings = configuration.getConfigurationAsMap(true);

        List<ClusterProfile> clusterProfiles = new ArrayList<>();
        clusterProfiles.add(new ClusterProfile("cluster_profile_id", "plugin_id", new ConfigurationProperty(new ConfigurationKey("some_key"), new ConfigurationValue("some_value")), new ConfigurationProperty(new ConfigurationKey("some_key2"), new EncryptedConfigurationValue(new GoCipher().encrypt("some_value2")))));

        List<ElasticProfile> elasticAgentProfiles = new ArrayList<>();
        elasticAgentProfiles.add(new ElasticProfile("profile_id", "cluster_profile_id", new ConfigurationProperty(new ConfigurationKey("some_key"), new ConfigurationValue("some_value")), new ConfigurationProperty(new ConfigurationKey("some_key2"), new EncryptedConfigurationValue(new GoCipher().encrypt("some_value2")))));

        ElasticAgentInformation elasticAgentInformation = new ElasticAgentInformation(pluginSettings, clusterProfiles, elasticAgentProfiles);

        ElasticAgentInformationDTO elasticAgentInformationDTO = new ElasticAgentExtensionConverterV5().getElasticAgentInformationDTO(elasticAgentInformation);
        String requestBody = elasticAgentInformationDTO.toJSON().toString();

        String expectedRequestBody = "{" +
                "    \"plugin_settings\":{" +
                "        \"key2\":\"password\", " +
                "        \"key\":\"value\"" +
                "    }," +
                "    \"cluster_profiles\":[" +
                "        {" +
                "            \"id\":\"cluster_profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"properties\":{" +
                "                \"some_key\":\"some_value\"," +
                "                \"some_key2\":\"some_value2\"" +
                "            }" +
                "         }" +
                "    ]," +
                "    \"elastic_agent_profiles\":[" +
                "        {" +
                "            \"id\":\"profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"cluster_profile_id\":\"cluster_profile_id\"," +
                "            \"properties\":{" +
                "                \"some_key\":\"some_value\", " +
                "                \"some_key2\":\"some_value2\"" +
                "            }" +
                "        }" +
                "    ]" +
                "}\n";

        assertThatJson(expectedRequestBody).isEqualTo(requestBody);
    }

    @Test
    public void shouldGetTheElasticAgentInformationFromResponseBodyOfMigrateCall() throws CryptoException {
        String responseBody = "{" +
                "    \"plugin_settings\":{" +
                "        \"key2\":\"password\", " +
                "        \"key\":\"value\"" +
                "    }," +
                "    \"cluster_profiles\":[" +
                "        {" +
                "            \"id\":\"cluster_profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"properties\":{" +
                "                \"some_key\":\"some_value\", " +
                "                \"some_key2\":\"some_value2\"" +
                "            }" +
                "         }" +
                "    ]," +
                "    \"elastic_agent_profiles\":[" +
                "        {" +
                "            \"id\":\"profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"cluster_profile_id\":\"cluster_profile_id\"," +
                "            \"properties\":{" +
                "                \"some_key\":\"some_value\"," +
                "                \"some_key2\":\"some_value2\"" +
                "            }" +
                "        }" +
                "    ]" +
                "}\n";

        ElasticAgentMetadataStore store = ElasticAgentMetadataStore.instance();
        PluggableInstanceSettings elasticAgentProfileSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("some_key", new Metadata(true, true))));
        PluggableInstanceSettings clusterProfileSettings = new PluggableInstanceSettings(Arrays.asList(new PluginConfiguration("some_key2", new Metadata(true, true))));
        store.setPluginInfo(new ElasticAgentPluginInfo(pluginDescriptor("plugin_id"), elasticAgentProfileSettings, clusterProfileSettings, null, null, null));

        ElasticAgentInformation elasticAgentInformation = new ElasticAgentExtensionConverterV5().getElasticAgentInformationFromResponseBody(responseBody);

        ConfigurationProperty property1 = new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"));
        ConfigurationProperty property2 = new ConfigurationProperty(new ConfigurationKey("key2"), new EncryptedConfigurationValue(new GoCipher().encrypt("password")));
        Configuration configuration = new Configuration();
        configuration.add(property1);
        configuration.add(property2);

        Map<String, String> pluginSettings = configuration.getConfigurationAsMap(true);

        List<ClusterProfile> clusterProfiles = new ArrayList<>();
        clusterProfiles.add(new ClusterProfile("cluster_profile_id", "plugin_id", new ConfigurationProperty(new ConfigurationKey("some_key"), new ConfigurationValue("some_value")), new ConfigurationProperty(new ConfigurationKey("some_key2"), new EncryptedConfigurationValue(new GoCipher().encrypt("some_value2")))));

        List<ElasticProfile> elasticAgentProfiles = new ArrayList<>();
        //do not worry about encryption, it is handled during config save (migrate-config) call
        elasticAgentProfiles.add(new ElasticProfile("profile_id", "cluster_profile_id", new ConfigurationProperty(new ConfigurationKey("some_key"), new ConfigurationValue("some_value")), new ConfigurationProperty(new ConfigurationKey("some_key2"), new ConfigurationValue("some_value2"))));

        ElasticAgentInformation expectedElasticAgentInformation = new ElasticAgentInformation(pluginSettings, clusterProfiles, elasticAgentProfiles);

        assertThat(elasticAgentInformation, is(expectedElasticAgentInformation));
    }

    @Test
    public void shouldJSONizePluginStatusRequestBody() {
        Map<String, String> clusterProfile1 = new HashMap<>();
        clusterProfile1.put("key1", "value1");
        clusterProfile1.put("key2", "value2");

        List<Map<String, String>> clusterProfileConfigurations = new ArrayList<>();
        clusterProfileConfigurations.add(clusterProfile1);
        clusterProfileConfigurations.add(clusterProfile1);

        String json = new ElasticAgentExtensionConverterV5().getPluginStatusReportRequestBody(clusterProfileConfigurations);

        assertThatJson(json).isEqualTo("{" +
                "  \"all_cluster_profiles_properties\":[" +
                "    {" +
                "      \"key1\":\"value1\"," +
                "      \"key2\":\"value2\"" +
                "    }," +
                "    {" +
                "      \"key1\":\"value1\"," +
                "      \"key2\":\"value2\"" +
                "    }" +
                "  ]" +
                "}");
    }

    @Test
    public void shouldGetClusterProfilesChangedRequestBodyWhenClusterProfileIsCreated() {
        ClusterProfilesChangedStatus status = ClusterProfilesChangedStatus.CREATED;
        Map<String, String> oldClusterProfile = null;
        Map<String, String> newClusterProfile = Collections.singletonMap("key1", "key2");

        String json = new ElasticAgentExtensionConverterV5().getClusterProfileChangedRequestBody(status, oldClusterProfile, newClusterProfile);

        assertThatJson(json).isEqualTo("{" +
                "  \"status\":\"created\"," +
                "  \"cluster_profiles_properties\":{" +
                "    \"key1\":\"key2\"" +
                "  }" +
                "}");
    }

    @Test
    public void shouldGetClusterProfilesChangedRequestBodyWhenClusterProfileIsUpdated() {
        ClusterProfilesChangedStatus status = ClusterProfilesChangedStatus.UPDATED;
        Map<String, String> oldClusterProfile = Collections.singletonMap("old_key1", "old_key2");
        Map<String, String> newClusterProfile = Collections.singletonMap("key1", "key2");

        String json = new ElasticAgentExtensionConverterV5().getClusterProfileChangedRequestBody(status, oldClusterProfile, newClusterProfile);

        assertThatJson(json).isEqualTo("{" +
                "  \"status\":\"updated\"," +
                "  \"old_cluster_profiles_properties\":{" +
                "    \"old_key1\":\"old_key2\"" +
                "  }," +
                "  \"cluster_profiles_properties\":{" +
                "    \"key1\":\"key2\"" +
                "  }" +
                "}");
    }

    @Test
    public void shouldGetClusterProfilesChangedRequestBodyWhenClusterProfileIsDeleted() {
        ClusterProfilesChangedStatus status = ClusterProfilesChangedStatus.DELETED;
        Map<String, String> oldClusterProfile = Collections.singletonMap("key1", "key2");
        Map<String, String> newClusterProfile = null;

        String json = new ElasticAgentExtensionConverterV5().getClusterProfileChangedRequestBody(status, oldClusterProfile, newClusterProfile);

        assertThatJson(json).isEqualTo("{" +
                "  \"status\":\"deleted\"," +
                "  \"cluster_profiles_properties\":{" +
                "    \"key1\":\"key2\"" +
                "  }" +
                "}");
    }

    private AgentMetadata elasticAgent() {
        return new AgentMetadata("52", "Idle", "Idle", "Enabled");
    }

    private PluginDescriptor pluginDescriptor(String pluginId) {
        return new PluginDescriptor() {
            @Override
            public String id() {
                return pluginId;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public About about() {
                return null;
            }
        };
    }
}
