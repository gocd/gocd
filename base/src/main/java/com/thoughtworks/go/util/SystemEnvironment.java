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
package com.thoughtworks.go.util;

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.utils.Timeout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class SystemEnvironment implements Serializable, ConfigDirProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SystemEnvironment.class);
    private static final long ONE_YEAR = 60 * 60 * 24 * 365;

    public static final String CRUISE_LISTEN_HOST = "cruise.listen.host";
    public static final String CRUISE_SERVER_PORT = "cruise.server.port";
    static final String AGENT_CONNECTION_TIMEOUT_IN_SECONDS = "agent.connection.timeout";
    private static final String JETTY_XML = "jetty.xml";
    public static final String CRUISE_SERVER_WAR_PROPERTY = "cruise.server.war";


    public static final String CRUISE_CONFIG_REPO_DIR = "cruise.config.repo.dir";
    public static final String DB_BASE_DIR = "db/";
    private static final String CONFIG_REPO_DEFAULT_PATH = DB_BASE_DIR + "config.git";
    public static final String ACTIVEMQ_USE_JMX = "activemq.use.jmx";
    public static final String ACTIVEMQ_QUEUE_PREFETCH = "activemq.queue.prefetch";
    private static final String ACTIVEMQ_CONNECTOR_PORT = "activemq.conn.port";

    public static final String PARENT_LOADER_PRIORITY = "parent.loader.priority";
    public static final String AGENT_CONTENT_MD5_HEADER = "Agent-Content-MD5";

    public static final String AGENT_LAUNCHER_CONTENT_MD5_HEADER = "Agent-Launcher-Content-MD5";

    public static final String AGENT_PLUGINS_ZIP_MD5_HEADER = "Agent-Plugins-Content-MD5";
    public static final String AGENT_TFS_SDK_MD5_HEADER = "TFS-SDK-Content-MD5";
    public static final String AGENT_EXTRA_PROPERTIES_HEADER = "GoCD-Agent-Extra-Properties";

    public static final String EMPTY_STRING = "";
    public static final String BLANK_STRING = EMPTY_STRING;
    public static final String ENFORCE_SERVER_IMMUTABILITY = "go.enforce.server.immutability";

    public static final String CONFIGURATION_YES = "Y";

    public static final String CONFIGURATION_NO = "N";
    public static final String RESOLVE_FANIN_REVISIONS = "resolve.fanin.revisions";

    public static final String ENABLE_CONFIG_MERGE_PROPERTY = "enable.config.merge";
    public static final GoSystemProperty<Boolean> ENABLE_CONFIG_MERGE_FEATURE = new CachedProperty<>(new GoBooleanSystemProperty(ENABLE_CONFIG_MERGE_PROPERTY, Boolean.TRUE));

    public static final String CRUISE_PROPERTIES = "/cruise.properties";
    private Properties properties;
    public static final String CRUISE_EXPERIMENTAL_ENABLE_ALL = "cruise.experimental.enable-all";

    public static final String ARTIFACT_FULL_SIZE_LIMIT = "artifact.full.size.limit";
    public static final String DATABASE_FULL_SIZE_LIMIT = "db.full.size.limit";
    public static final String ARTIFACT_WARNING_SIZE_LIMIT = "artifact.warning.size.limit";
    public static final String DATABASE_WARNING_SIZE_LIMIT = "db.warning.size.limit";
    public static final String AGENT_SIZE_LIMIT = "agent.size.limit";
    private static final String DISK_SPACE_CACHE_REFRESHER_INTERVAL = "disk.space.cache.refresher.interval";
    private static final String COMMAND_REPOSITORY_WARNING_TIMEOUT = "command.repo.warning.timeout";
    public static final String AGENT_SOCKET_TYPE_NIO = "nio";

    private static final String AGENT_SOCKET_TYPE_PROPERTY = "agent.socket.type";

    public static final String CONFIG_FILE_PROPERTY = "cruise.config.file";
    public static final String INTERVAL = "cruise.console.publish.interval";
    public static final String SERVICE_URL = "serviceUrl";
    public static final String AGENT_SSL_VERIFICATION_MODE = "sslVerificationMode";
    public static final String AGENT_ROOT_CERT_FILE = "rootCertFile";
    public static final String AGENT_PRIVATE_KEY = "sslPrivateKeyFile";
    public static final String AGENT_PRIVATE_KEY_PASSPHRASE_FILE = "sslPrivateKeyPassphraseFile";
    public static final String AGENT_SSL_CERTIFICATE = "sslCertificateFile";

    public static final String CONFIG_DIR_PROPERTY = "cruise.config.dir";
    public static final String DES_CONFIG_CIPHER = "cipher";
    public static final String AES_CONFIG_CIPHER = "cipher.aes";
    public static final int TFS_SOCKET_TIMEOUT_IN_MILLISECONDS = 20 * 60 * 1000;
    public static final String TFS_SOCKET_TIMEOUT_PROPERTY = "tfs.socket.block.timeout";

    public static GoSystemProperty<Integer> RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT = new CachedProperty<>(new GoIntSystemProperty("resolve.fanin.max.backtrack.limit", 100));
    public static GoSystemProperty<Integer> MATERIAL_UPDATE_INACTIVE_TIMEOUT = new CachedProperty<>(new GoIntSystemProperty("material.update.inactive.timeout", 15));

    public static final String MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY = "material.update.idle.interval";
    private static GoSystemProperty<Long> MATERIAL_UPDATE_IDLE_INTERVAL = new GoLongSystemProperty(MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, 60000L);

    public static GoSystemProperty<Integer> PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS = new GoIntSystemProperty("pluginLocationMonitor.sleepTimeInSecs", -1);
    public static final String PLUGINS_PATH = "plugins";
    public static GoSystemProperty<String> PLUGIN_GO_PROVIDED_PATH = new GoStringSystemProperty("plugins.go.provided.path", PLUGINS_PATH + System.getProperty("file.separator") + "bundled");
    public static GoSystemProperty<String> PLUGIN_EXTERNAL_PROVIDED_PATH = new GoStringSystemProperty("plugins.external.provided.path", PLUGINS_PATH + System.getProperty("file.separator") + "external");
    public static GoSystemProperty<String> PLUGIN_WORK_DIR = new CachedProperty<>(new GoStringSystemProperty("plugins.work.path", "plugins_work"));
    public static GoSystemProperty<String> PLUGIN_ACTIVATOR_JAR_PATH = new CachedProperty<>(new GoStringSystemProperty("plugins.activator.jar.path", "lib/go-plugin-activator.jar"));
    public static GoSystemProperty<String> ALL_PLUGINS_ZIP_PATH = new GoStringSystemProperty("plugins.all.zip.path", new File(PLUGINS_PATH, "go-plugins-all.zip").getAbsolutePath());
    public static GoSystemProperty<String> ADDONS_PATH = new GoStringSystemProperty("addons.path", "addons");
    public static GoSystemProperty<String> AVAILABLE_FEATURE_TOGGLES_FILE_PATH = new GoStringSystemProperty("available.toggles.path", "/available.toggles");
    public static GoSystemProperty<String> USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR = new GoStringSystemProperty("user.toggles.path", "go.feature.toggles");

    public static GoSystemProperty<String> DEFAULT_COMMAND_SNIPPETS_ZIP = new CachedProperty<>(
            new GoStringSystemProperty("default.command.snippets.zip.location", "/defaultFiles/defaultCommandSnippets.zip"));
    public static GoSystemProperty<String> DEFAULT_PLUGINS_ZIP = new CachedProperty<>(
            new GoStringSystemProperty("default.plugins.zip.location", "/defaultFiles/plugins.zip"));
    public static final GoSystemProperty<String> AGENT_PLUGINS_PATH = new CachedProperty<>(new GoStringSystemProperty("agent.plugins.path", PLUGINS_PATH));
    public static GoSystemProperty<String> VERSION_FILE_IN_DEFAULT_COMMAND_REPOSITORY = new CachedProperty<>(new GoStringSystemProperty("version.file.in.command.repository", "version.txt"));
    public static GoSystemProperty<Integer> COMMAND_REPOSITORY_CACHE_TIME_IN_SECONDS = new CachedProperty<>(new GoIntSystemProperty("command.repo.cache.timeout.in.secs", 30 * 60));
    public static GoSystemProperty<String> COMMAND_REPOSITORY_DIRECTORY = new CachedProperty<>(new GoStringSystemProperty("command.repo.dir", DB_BASE_DIR + "command_repository"));
    public static GoSystemProperty<Integer> IDLE_TIMEOUT = new GoIntSystemProperty("idle.timeout", 30000);
    public static GoSystemProperty<Integer> RESPONSE_BUFFER_SIZE = new GoIntSystemProperty("response.buffer.size", 32768);
    public static final GoSystemProperty<Integer> API_REQUEST_IDLE_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("api.request.idle.timeout.seconds", 300);
    public static final GoSystemProperty<Integer> AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("agent.request.idle.timeout.seconds", 30);
    public static final GoSystemProperty<Integer> GO_SERVER_SESSION_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("go.server.session.timeout.seconds", 60 * 60 * 24 * 14);
    public static final GoSystemProperty<Integer> GO_SERVER_SESSION_COOKIE_MAX_AGE_IN_SECONDS = new GoIntSystemProperty("go.sessioncookie.maxage.seconds", 60 * 60 * 24 * 14);
    public static final GoSystemProperty<Boolean> GO_SERVER_SESSION_COOKIE_SECURE = new GoBooleanSystemProperty("go.sessioncookie.secure", false);
    public static final GoSystemProperty<String> AGENT_EXTRA_PROPERTIES = new GoStringSystemProperty("gocd.agent.extra.properties", "");
    public static final GoSystemProperty<Integer> JMS_LISTENER_BACKOFF_TIME = new GoIntSystemProperty("go.jms.listener.backoff.time.in.milliseconds", 5000);

    public static final GoSystemProperty<Integer> GO_SERVER_AUTHORIZATION_EXTENSION_CALLS_CACHE_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("go.server.authorization.extension.calls.cache.timeout.in.secs", 60 * 30);

    public static GoSystemProperty<String> JETTY_XML_FILE_NAME = new GoStringSystemProperty("jetty.xml.file.name", JETTY_XML);

    public static final String JETTY9 = "com.thoughtworks.go.server.Jetty9Server";
    public static GoSystemProperty<String> APP_SERVER = new CachedProperty<>(new GoStringSystemProperty("app.server", JETTY9));
    public static GoSystemProperty<String> GO_SERVER_STATE = new GoStringSystemProperty("go.server.state", "active");
    public static GoSystemProperty<String> GO_LANDING_PAGE = new GoStringSystemProperty("go.landing.page", "/pipelines");

    public static GoSystemProperty<Boolean> FETCH_ARTIFACT_AUTO_SUGGEST = new GoBooleanSystemProperty("go.fetch-artifact.auto-suggest", true);
    public static GoSystemProperty<Boolean> GO_FETCH_ARTIFACT_TEMPLATE_AUTO_SUGGEST = new GoBooleanSystemProperty("go.fetch-artifact.template.auto-suggest", true);

    public static GoSystemProperty<Boolean> GO_CONFIG_REPO_GC_AGGRESSIVE = new GoBooleanSystemProperty("go.config.repo.gc.aggressive", true);
    public static GoSystemProperty<Long> GO_CONFIG_REPO_GC_EXPIRE = new GoLongSystemProperty("go.config.repo.gc.expire", 24L);
    public static GoSystemProperty<Long> GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD = new GoLongSystemProperty("go.config.repo.gc.warning.looseobject.threshold", 10000L);
    public static GoSystemProperty<Boolean> GO_CONFIG_REPO_PERIODIC_GC = new GoBooleanSystemProperty("go.config.repo.gc.periodic", false);

    public static GoSystemProperty<String> GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME = new GoStringSystemProperty("go.update.server.public.key.file.name", "go_update_server.pub");
    public static GoSystemProperty<String> GO_UPDATE_SERVER_URL = new GoStringSystemProperty("go.update.server.url", "https://update.gocd.org/channels/supported/latest.json");
    public static GoSystemProperty<Boolean> GO_CHECK_UPDATES = new GoBooleanSystemProperty("go.check.updates", true);

    public static GoSystemProperty<Integer> GO_ELASTIC_PLUGIN_CREATE_AGENT_THREADS = new GoIntSystemProperty("go.elasticplugin.createagent.threads", 5);
    public static GoSystemProperty<Integer> GO_ELASTIC_PLUGIN_SERVER_PING_THREADS = new GoIntSystemProperty("go.elasticplugin.serverping.threads", 1);
    public static GoSystemProperty<Integer> GO_ENCRYPTION_API_MAX_REQUESTS = new GoIntSystemProperty("go.encryption.api.max.requests", 30);

    public static GoSystemProperty<String> GO_ANALYTICS_PLUGIN_EXTERNAL_ASSETS = new GoStringSystemProperty("go.analytics.plugin.external.assets", "./analytics-assets");
    public static GoSystemProperty<Boolean> AUTO_REGISTER_LOCAL_AGENT_ENABLED = new GoBooleanSystemProperty("go.auto.register.local.agent.enabled", true);

    public static GoSystemProperty<Boolean> GO_SERVER_SHALLOW_CLONE = new GoBooleanSystemProperty("go.server.shallowClone", false);
    public static GoSystemProperty<Boolean> GO_SERVER_SCHEDULED_PIPELINE_LOADER_GLOBAL_MATERIAL_LOOKUP = new GoBooleanSystemProperty("go.server.scheduledPipelineLoader.globalMaterialLookup", false);

    public static GoSystemProperty<Boolean> GO_API_WITH_SAFE_MODE = new GoBooleanSystemProperty("go.api.with.safe.mode", true);
    public static GoSystemProperty<Integer> MAX_PENDING_AGENTS_ALLOWED = new GoIntSystemProperty("max.pending.agents.allowed", 100);
    public static GoSystemProperty<Boolean> CHECK_AND_REMOVE_DUPLICATE_MODIFICATIONS = new GoBooleanSystemProperty("go.modifications.removeDuplicates", true);
    public static GoSystemProperty<String> GO_AGENT_KEYSTORE_PASSWORD = new GoStringSystemProperty("go.agent.keystore.password", "agent5s0repa55w0rd");
    public static GoSystemProperty<String> GO_SERVER_KEYSTORE_PASSWORD = new GoStringSystemProperty("go.server.keystore.password", "serverKeystorepa55w0rd");
    public static final GoSystemProperty<Boolean> GO_DIAGNOSTICS_MODE = new GoBooleanSystemProperty("go.diagnostics.mode", false);

    public static GoIntSystemProperty DEPENDENCY_MATERIAL_UPDATE_LISTENERS = new GoIntSystemProperty("dependency.material.check.threads", 3);

    public static GoIntSystemProperty CONFIG_MATERIAL_POST_UPDATE_LISTENERS = new GoIntSystemProperty("config.material.post.update.threads", 2);

    public static GoSystemProperty<String> GO_SERVER_MODE = new GoStringSystemProperty("go.server.mode", "production");
    public static GoBooleanSystemProperty REAUTHENTICATION_ENABLED = new GoBooleanSystemProperty("go.security.reauthentication.enabled", true);
    public static GoSystemProperty<Long> REAUTHENTICATION_TIME_INTERVAL = new GoLongSystemProperty("go.security.reauthentication.interval", 1800 * 1000L);
    public static GoSystemProperty<Boolean> CONSOLE_OUT_TO_STDOUT = new GoBooleanSystemProperty("go.console.stdout", false);
    private static GoSystemProperty<String> CONSOLE_LOG_CHARSET = new GoStringSystemProperty("go.console.log.charset", "utf-8");
    private static GoSystemProperty<Boolean> AGENT_STATUS_API_ENABLED = new GoBooleanSystemProperty("go.agent.status.api.enabled", true);
    private static GoSystemProperty<String> AGENT_STATUS_API_BIND_HOST = new GoStringSystemProperty("go.agent.status.api.bind.host", "localhost");
    private static GoSystemProperty<Integer> AGENT_STATUS_API_BIND_PORT = new GoIntSystemProperty("go.agent.status.api.bind.port", 8152);

    private static GoSystemProperty<Integer> GO_SPA_TIMEOUT = new GoIntSystemProperty("go.spa.timeout", 60000);
    private static GoSystemProperty<Integer> GO_SPA_REFRESH_INTERVAL = new GoIntSystemProperty("go.spa.refresh.interval", 10000);
    private static GoSystemProperty<Long> GO_PAC_CLONE_TIMEOUT = new GoLongSystemProperty("go.pac.clone.timeout", 30 * 1000L);

    private static GoSystemProperty<Boolean> ENABLE_ANALYTICS_ONLY_FOR_ADMINS = new GoBooleanSystemProperty("go.enable.analytics.only.for.admins", false);
    public static final GoSystemProperty<Boolean> FAIL_STARTUP_ON_DATA_ERROR = new GoBooleanSystemProperty("gocd.fail.startup.on.data.error", false);
    private static final GoSystemProperty<Boolean> JOB_DETAILS_USE_IFRAME_SANDBOX = new GoBooleanSystemProperty("gocd.job.details.sandbox", true);
    private static GoSystemProperty<Boolean> GO_PLUGIN_CLASSLOADER_OLD = new GoBooleanSystemProperty("gocd.plugins.classloader.old", false);
    public static final GoSystemProperty<String> LOADING_PAGE = new GoStringSystemProperty("loading.page.resource.path", "/loading_pages/new.loading.page.html");
    public static GoSystemProperty<Long> NOTIFICATION_PLUGIN_MESSAGES_TTL = new GoLongSystemProperty("plugins.notification.message.ttl.millis", 2 * 60 * 1000L);
    public static final GoSystemProperty<Boolean> ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP = new GoBooleanSystemProperty("allow.everyone.to.view.operate.groups.with.no.authorization.setup", false);

    public static GoSystemProperty<Boolean> ENABLE_HSTS_HEADER = new GoBooleanSystemProperty("gocd.enable.hsts.header", false);
    public static GoSystemProperty<Long> HSTS_HEADER_MAX_AGE = new GoLongSystemProperty("gocd.hsts.header.max.age", ONE_YEAR);
    public static GoSystemProperty<Boolean> HSTS_HEADER_INCLUDE_SUBDOMAINS = new GoBooleanSystemProperty("gocd.hsts.header.include.subdomains", false);
    public static GoSystemProperty<Boolean> HSTS_HEADER_PRELOAD = new GoBooleanSystemProperty("gocd.hsts.header.preload", false);
    public static GoSystemProperty<Long> EPHEMERAL_AUTO_REGISTER_KEY_EXPIRY = new GoLongSystemProperty("gocd.ephemeral.auto.register.key.expiry.millis", 30 * 60 *1000L);

    private final static Map<String, String> GIT_ALLOW_PROTOCOL;

    static {
        Map<String, String> map = new HashMap<String, String>() {{
            put("GIT_ALLOW_PROTOCOL", System.getenv("GIT_ALLOW_PROTOCOL") == null ?
                    "http:https:ssh:git:file:rsync" : System.getenv("GIT_ALLOW_PROTOCOL"));
        }};
        GIT_ALLOW_PROTOCOL = Collections.unmodifiableMap(map);
    }

    private volatile static Integer agentConnectionTimeout;
    private volatile static Integer cruiseSSlPort;
    private volatile static String cruiseConfigDir;
    private volatile static Long databaseFullSizeLimit;
    private volatile static Charset consoleLogCharsetAsCharset;
    private volatile static Long artifactFullSizeLimit;
    private volatile static Long diskSpaceCacheRefresherInterval;
    public static final String UNRESPONSIVE_JOB_WARNING_THRESHOLD = "cruise.unresponsive.job.warning";
    private File configDir;
    private volatile Boolean enforceRevisionCompatibilityWithUpstream;

    public SystemEnvironment() {
    }

    public SystemEnvironment(Properties properties) {
        this.properties = properties;
    }

    public <T> T get(GoSystemProperty<T> systemProperty) {
        return systemProperty.getValue();
    }


    public <T> void set(GoSystemProperty<T> systemProperty, T value) {
        System.setProperty(systemProperty.propertyName, "" + value);
        if (systemProperty instanceof CachedProperty) {
            ((CachedProperty) systemProperty).clear();
        }
    }

    public void set(GoSystemProperty<Boolean> systemProperty, boolean value) {
        System.setProperty(systemProperty.propertyName, value ? "Y" : "N");
        if (systemProperty instanceof CachedProperty) {
            ((CachedProperty) systemProperty).clear();
        }
    }

    @Override
    public File configDir() {
        if (configDir == null) {
            configDir = new File(getPropertyImpl(CONFIG_FILE_PROPERTY, "config/cruise-config.xml")).getParentFile();
        }
        return configDir;
    }

    public int getServerPort() {
        return Integer.parseInt(getPropertyImpl(CRUISE_SERVER_PORT, "8153"));
    }

    public String getListenHost() {
        return getPropertyImpl(CRUISE_LISTEN_HOST);
    }

    public long getArtifactReposiotryFullLimit() {
        if (artifactFullSizeLimit != null) {
            return artifactFullSizeLimit;
        }
        return artifactFullSizeLimit = Long.parseLong(trimMegaFromSize(getPropertyImpl(ARTIFACT_FULL_SIZE_LIMIT, "100M")));
    }

    public long getDatabaseDiskSpaceFullLimit() {
        if (databaseFullSizeLimit != null) {
            return databaseFullSizeLimit;
        }
        return databaseFullSizeLimit = Long.parseLong(trimMegaFromSize(getPropertyImpl(DATABASE_FULL_SIZE_LIMIT, "100M")));
    }

    public long getDiskSpaceCacheRefresherInterval() {
        if (diskSpaceCacheRefresherInterval != null) {
            return diskSpaceCacheRefresherInterval;
        }
        return diskSpaceCacheRefresherInterval = Long.parseLong(getPropertyImpl(DISK_SPACE_CACHE_REFRESHER_INTERVAL, "5000"));
    }

    public boolean consoleOutToStdout() {
        return get(CONSOLE_OUT_TO_STDOUT);
    }

    //Used in Tests
    public void setDiskSpaceCacheRefresherInterval(long interval) {
        diskSpaceCacheRefresherInterval = interval;
    }

    public long getAgentSizeLimit() {
        return Long.parseLong(trimMegaFromSize(getPropertyImpl(AGENT_SIZE_LIMIT, "100M"))) * 1024 * 1024;
    }

    public long getArtifactReposiotryWarningLimit() {
        return Long.parseLong(trimMegaFromSize(getPropertyImpl(ARTIFACT_WARNING_SIZE_LIMIT, "1024M")));
    }

    public long getDatabaseDiskSpaceWarningLimit() {
        return Long.parseLong(trimMegaFromSize(getPropertyImpl(DATABASE_WARNING_SIZE_LIMIT, "1024M")));
    }

    private String trimMegaFromSize(String sizeInMega) {
        return StringUtils.removeEndIgnoreCase(sizeInMega, "M");
    }

    @Override
    public String getConfigDir() {
        if (cruiseConfigDir != null) {
            return cruiseConfigDir;
        }
        return cruiseConfigDir = getPropertyImpl(CONFIG_DIR_PROPERTY, "config");
    }

    public File getJettyConfigFile() {
        return new File(getConfigDir(), get(JETTY_XML_FILE_NAME));
    }

    /**
     * @deprecated use <code>new SystemEnvironment().getXXXXX()</code> instead.
     */
    public static String getProperty(String property, String defaultValue) {
        return new SystemEnvironment().getPropertyImpl(property, defaultValue);
    }

    /**
     * @deprecated use <code>new SystemEnvironment().getXXXXX()</code> instead.
     */
    public static String getProperty(String property) {
        return new SystemEnvironment().getPropertyImpl(property);
    }

    private String getPropertyImpl(String property, String defaultValue) {
        return System.getProperty(property, defaultValue);
    }

    public String getPropertyImpl(String property) {
        return System.getProperty(property);
    }

    public String getOperatingSystemFamilyName() {
        return OperatingSystem.getFamilyName();
    }

    public String getOperatingSystemCompleteName() {
        return OperatingSystem.getCompleteName();
    }

    public String getOperatingSystemName() {
        return getPropertyImpl("os.name");
    }

    public boolean getEnableRequestTimeLogging() {
        return Boolean.parseBoolean(getPropertyImpl("cruise.request.time.logging", "false"));
    }

    public int getActivemqQueuePrefetch() {
        return Integer.parseInt(getPropertyImpl(ACTIVEMQ_QUEUE_PREFETCH, "0"));
    }

    public long getCommandRepoWarningTimeout() {
        return Long.parseLong(getPropertyImpl(COMMAND_REPOSITORY_WARNING_TIMEOUT, String.valueOf(Timeout.ONE_HOUR.inMillis())));
    }

    public boolean getActivemqUseJmx() {
        return Boolean.parseBoolean(getPropertyImpl(ACTIVEMQ_USE_JMX, "false"));
    }

    public int getActivemqConnectorPort() {
        return Integer.parseInt(getPropertyImpl(ACTIVEMQ_CONNECTOR_PORT, "1099"));
    }

    public File getConfigRepoDir() {
        return new File(properties().getProperty(CRUISE_CONFIG_REPO_DIR, CONFIG_REPO_DEFAULT_PATH));
    }

    public File getDESCipherFile() {
        return new File(getConfigDir(), DES_CONFIG_CIPHER);
    }

    public File getAESCipherFile() {
        return new File(getConfigDir(), AES_CONFIG_CIPHER);
    }

    public int getNumberOfMaterialCheckListener() {
        return Integer.parseInt(getPropertyImpl("material.check.threads", "10"));
    }

    public int getNumberOfConfigMaterialCheckListener() {
        return Integer.parseInt(getPropertyImpl("material.config.check.threads", "2"));
    }

    public int getNumberOfDependencyMaterialUpdateListeners() {
        return DEPENDENCY_MATERIAL_UPDATE_LISTENERS.getValue();
    }

    public int getNumberOfConfigMaterialPostUpdateListeners() {
        return CONFIG_MATERIAL_POST_UPDATE_LISTENERS.getValue();
    }

    public String getAgentMd5() {
        return getPropertyImpl(GoConstants.AGENT_JAR_MD5, BLANK_STRING);
    }

    public String getTfsImplMd5() {
        return getPropertyImpl(GoConstants.TFS_IMPL_MD5, BLANK_STRING);
    }

    public String getGivenAgentLauncherMd5() {
        return getPropertyImpl(GoConstants.GIVEN_AGENT_LAUNCHER_JAR_MD5, BLANK_STRING);
    }

    public String getAgentPluginsMd5() {
        return getPropertyImpl(GoConstants.AGENT_PLUGINS_MD5, BLANK_STRING);
    }

    public boolean isFeatureEnabled(String value) {
        if (Boolean.parseBoolean(properties().getProperty(CRUISE_EXPERIMENTAL_ENABLE_ALL, "false"))) {
            return true;
        }
        return Boolean.parseBoolean(properties().getProperty(value, "false"));
    }

    private Properties properties() {
        if (properties == null) {
            properties = new Properties();
            try (InputStream is = getClass().getResourceAsStream(CRUISE_PROPERTIES)) {
                properties.load(is);
            } catch (Exception e) {
                LOG.error("Unable to load newProperties file {}", CRUISE_PROPERTIES);
            }
        }
        return properties;
    }

    public boolean useNioSslSocket() {
        String socketType = getPropertyImpl(AGENT_SOCKET_TYPE_PROPERTY, AGENT_SOCKET_TYPE_NIO);
        return AGENT_SOCKET_TYPE_NIO.equalsIgnoreCase(socketType);
    }


    public String getCruiseConfigFile() {
        return getPropertyImpl(CONFIG_FILE_PROPERTY, getConfigDir() + "/cruise-config.xml");
    }

    public int getAgentConnectionTimeout() {
        if (agentConnectionTimeout != null) {
            return agentConnectionTimeout;
        }
        return agentConnectionTimeout = Integer.parseInt(getPropertyImpl(AGENT_CONNECTION_TIMEOUT_IN_SECONDS, "300"));
    }

    public Integer getConsolePublishInterval() {
        return Integer.valueOf(getPropertyImpl(INTERVAL, "10"));
    }

    public String getServiceUrl() {
        return getPropertyImpl(SERVICE_URL, defaultRemotingUrl());
    }

    public File getRootCertFile() {
        if (getPropertyImpl(AGENT_ROOT_CERT_FILE) == null) {
            return null;
        }
        return new File(getPropertyImpl(AGENT_ROOT_CERT_FILE));
    }

    public File getAgentPrivateKeyFile() {
        if (getPropertyImpl(AGENT_PRIVATE_KEY) == null) {
            return null;
        }
        return new File(getPropertyImpl(AGENT_PRIVATE_KEY));
    }

    public File getAgentSslPrivateKeyPassphraseFile() {
        if (getPropertyImpl(AGENT_PRIVATE_KEY_PASSPHRASE_FILE) == null) {
            return null;
        }
        return new File(getPropertyImpl(AGENT_PRIVATE_KEY_PASSPHRASE_FILE));
    }

    public File getAgentSslCertificate() {
        if (getPropertyImpl(AGENT_SSL_CERTIFICATE) == null) {
            return null;
        }
        return new File(getPropertyImpl(AGENT_SSL_CERTIFICATE));
    }


    public SslVerificationMode getAgentSslVerificationMode() {
        if (getPropertyImpl(AGENT_SSL_VERIFICATION_MODE) == null) {
            return SslVerificationMode.NONE;
        }
        return SslVerificationMode.valueOf(getPropertyImpl(AGENT_SSL_VERIFICATION_MODE));
    }


    private String defaultRemotingUrl() {
        return "https://localhost:8443" + getWebappContextPath();
    }

    public boolean useCompressedJs() {
        return Boolean.parseBoolean(getPropertyImpl(GoConstants.USE_COMPRESSED_JAVASCRIPT, "true"));
    }


    public String getEnvironmentVariable(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    public Map<String, String> getGitAllowedProtocols() {
        return GIT_ALLOW_PROTOCOL;
    }

    public void setProperty(String name, String value) {
        clearCachedSystemEnvironment();
        System.setProperty(name, value);
    }

    public void clearProperty(String name) {
        clearCachedSystemEnvironment();
        System.clearProperty(name);
    }

    private void clearCachedSystemEnvironment() {
        agentConnectionTimeout = null;
        cruiseSSlPort = null;
        cruiseConfigDir = null;
        databaseFullSizeLimit = null;
        artifactFullSizeLimit = null;
        consoleLogCharsetAsCharset = null;
    }

    public String getWebappContextPath() {
        return getPropertyImpl("cruise.server.context", "/go");
    }

    public String pathFor(String appPath) {
        return (getWebappContextPath() + "/" + appPath).replaceAll("//", "/");
    }

    public String getCruiseWar() {
        return getPropertyImpl(CRUISE_SERVER_WAR_PROPERTY, "cruise.war");
    }

    public long getUnresponsiveJobWarningThreshold() {
        return Long.parseLong(getPropertyImpl(UNRESPONSIVE_JOB_WARNING_THRESHOLD, "5")) * 60 * 1000;//mins to mills
    }

    public boolean getParentLoaderPriority() {
        return Boolean.parseBoolean(getPropertyImpl(PARENT_LOADER_PRIORITY, "false"));
    }

    public String getUserDirectory() {
        return getPropertyImpl("user.dir");
    }

    public static final ThreadLocal<Boolean> enforceServerImmutability = ThreadLocal.withInitial(() -> false);

    public static int getMaxEncryptionAPIRequestsPerMinute() {
        return GO_ENCRYPTION_API_MAX_REQUESTS.getValue();
    }

    public boolean enforceServerImmutability() {
        return CONFIGURATION_YES.equals(getPropertyImpl(ENFORCE_SERVER_IMMUTABILITY, CONFIGURATION_YES)) || enforceServerImmutability.get();
    }

    public boolean enforceRevisionCompatibilityWithUpstream() {
        if (enforceRevisionCompatibilityWithUpstream == null) {
            enforceRevisionCompatibilityWithUpstream = CONFIGURATION_YES.equals(getPropertyImpl(RESOLVE_FANIN_REVISIONS, CONFIGURATION_YES));
        }
        return enforceRevisionCompatibilityWithUpstream;
    }

    public int getTfsSocketTimeout() {
        return Integer.parseInt(getPropertyImpl(TFS_SOCKET_TIMEOUT_PROPERTY, String.valueOf(TFS_SOCKET_TIMEOUT_IN_MILLISECONDS)));
    }

    public Level pluginLoggingLevel(String pluginId) {
        return Level.toLevel(getPropertyImpl("plugin." + pluginId + ".log.level", "INFO"), Level.INFO);
    }

    public File getDefaultCommandRepository() {
        return new File(get(COMMAND_REPOSITORY_DIRECTORY), "default");
    }

    public String consoleLogCharset() {
        return consoleLogCharsetAsCharset().name();
    }

    public Charset consoleLogCharsetAsCharset() {
        if (consoleLogCharsetAsCharset == null) {
            consoleLogCharsetAsCharset = Charset.forName(get(CONSOLE_LOG_CHARSET));
        }
        return consoleLogCharsetAsCharset;
    }

    public String getCommandRepositoryRootLocation() {
        return new File(get(COMMAND_REPOSITORY_DIRECTORY)).getAbsolutePath();
    }

    public String getExternalPluginAbsolutePath() {
        return new File(get(PLUGIN_EXTERNAL_PROVIDED_PATH)).getAbsolutePath();
    }

    public static Integer getGoServerAuthorizationExtensionCallsCacheTimeoutInSeconds() {
        return GO_SERVER_AUTHORIZATION_EXTENSION_CALLS_CACHE_TIMEOUT_IN_SECONDS.getValue();
    }

    public String getBundledPluginAbsolutePath() {
        return new File(get(PLUGIN_GO_PROVIDED_PATH)).getAbsolutePath();
    }

    public <T> void reset(GoSystemProperty<T> systemProperty) {
        System.clearProperty(systemProperty.propertyName());
        if (systemProperty instanceof CachedProperty) {
            ((CachedProperty) systemProperty).clear();
        }
    }

    public long getMaterialUpdateIdleInterval() {
        return MATERIAL_UPDATE_IDLE_INTERVAL.getValue();
    }

    public String landingPage() {
        return GO_LANDING_PAGE.getValue();
    }

    public boolean usingJetty9() {
        return get(APP_SERVER).equals(JETTY9);
    }

    public boolean isServerActive() {
        return GO_SERVER_STATE.getValue().equalsIgnoreCase("active");
    }

    @Deprecated
    //changing GO_SERVER_STATE to active requires a restart as timer-threads are not scheduled when the server is
    // in passive mode.
    // Changing GO_SERVER_STATE to active without server restart can have inadvertent behavior.
    public void switchToActiveState() {
        set(GO_SERVER_STATE, "active");
    }

    @Deprecated
    //changing GO_SERVER_STATE to passive requires a restart as timer-threads are scheduled when the server is
    // in active mode.
    // Changing GO_SERVER_STATE to active without server restart may result into functioning of some parts of GoCD subsystems.
    public void switchToPassiveState() {
        set(GO_SERVER_STATE, "passive");
    }

    public String getUpdateServerPublicKeyPath() {
        return String.format("%s/%s", getConfigDir(), GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME.getValue());
    }

    public boolean isGOUpdateCheckEnabled() {
        return GO_CHECK_UPDATES.getValue();
    }

    public boolean isFetchArtifactTemplateAutoSuggestEnabled() {
        return GO_FETCH_ARTIFACT_TEMPLATE_AUTO_SUGGEST.getValue();
    }

    public String getUpdateServerUrl() {
        return GO_UPDATE_SERVER_URL.getValue();
    }

    public boolean isAutoRegisterLocalAgentEnabled() {
        return AUTO_REGISTER_LOCAL_AGENT_ENABLED.getValue();
    }

    public Long getConfigGitGCExpireTime() {
        Long hour = GO_CONFIG_REPO_GC_EXPIRE.getValue();
        return hour * 60 * 60 * 1000;
    }

    public boolean isApiSafeModeEnabled() {
        return GO_API_WITH_SAFE_MODE.getValue();
    }

    public String getAgentKeyStorePassword() {
        return get(SystemEnvironment.GO_AGENT_KEYSTORE_PASSWORD);
    }

    public String getServerKeyStorePassword() {
        return get(SystemEnvironment.GO_SERVER_KEYSTORE_PASSWORD);
    }

    public int sessionTimeoutInSeconds() {
        return GO_SERVER_SESSION_TIMEOUT_IN_SECONDS.getValue();
    }

    public int sessionCookieMaxAgeInSeconds() {
        return GO_SERVER_SESSION_COOKIE_MAX_AGE_IN_SECONDS.getValue();
    }

    public boolean isSessionCookieSecure() {
        return GO_SERVER_SESSION_COOKIE_SECURE.getValue();
    }

    public boolean isProductionMode() {
        return GO_SERVER_MODE.getValue().equalsIgnoreCase("production");
    }

    public boolean isServerInStandbyMode() {
        return GO_SERVER_MODE.getValue().equalsIgnoreCase("standby");
    }

    public boolean isReAuthenticationEnabled() {
        return REAUTHENTICATION_ENABLED.getValue();
    }

    public long getReAuthenticationTimeInterval() {
        return REAUTHENTICATION_TIME_INTERVAL.getValue();
    }

    public Boolean getAgentStatusEnabled() {
        return AGENT_STATUS_API_ENABLED.getValue();
    }

    public long getPacCloneTimeout() {
        return GO_PAC_CLONE_TIMEOUT.getValue();
    }

    public String getAgentStatusHostname() {
        if (isBlank(AGENT_STATUS_API_BIND_HOST.getValue())) {
            return null;
        } else {
            return AGENT_STATUS_API_BIND_HOST.getValue();
        }
    }

    public int getAgentStatusPort() {
        return AGENT_STATUS_API_BIND_PORT.getValue();
    }

    public static Integer goSpaRefreshInterval() {
        return GO_SPA_REFRESH_INTERVAL.getValue();
    }

    public static Integer goSpaTimeout() {
        return GO_SPA_TIMEOUT.getValue();
    }

    public Integer getNotificationListenerCountForPlugin(String pluginId) {
        return Integer.parseInt(getPropertyImpl("plugin." + pluginId + ".notifications.listener.count", "1"));
    }

    public boolean enableAnalyticsOnlyForAdmins() {
        return ENABLE_ANALYTICS_ONLY_FOR_ADMINS.getValue();
    }

    public boolean enableHstsHeader() {
        return ENABLE_HSTS_HEADER.getValue();
    }

    public long hstsHeaderMaxAge() {
        return HSTS_HEADER_MAX_AGE.getValue();
    }

    public boolean hstsHeaderIncludeSubdomains() {
        return HSTS_HEADER_INCLUDE_SUBDOMAINS.getValue();
    }

    public boolean hstsHeaderPreload() {
        return HSTS_HEADER_PRELOAD.getValue();
    }

    public boolean shouldFailStartupOnDataError() {
        return get(FAIL_STARTUP_ON_DATA_ERROR);
    }

    public boolean useIframeSandbox() {
        return JOB_DETAILS_USE_IFRAME_SANDBOX.getValue();
    }

    public boolean pluginClassLoaderHasOldBehaviour() {
        return GO_PLUGIN_CLASSLOADER_OLD.getValue();
    }

    public Optional<String> wrapperConfigDirPath() {
        return Optional.ofNullable(System.getenv("WRAPPER_CONF_DIR"));
    }

    public long getEphemeralAutoRegisterKeyExpiryInMillis() {
        return EPHEMERAL_AUTO_REGISTER_KEY_EXPIRY.getValue();
    }


    public static abstract class GoSystemProperty<T> {
        private String propertyName;
        protected T defaultValue;

        protected GoSystemProperty(String propertyName, T defaultValue) {
            this.propertyName = propertyName;
            this.defaultValue = defaultValue;
        }

        T getValue() {
            String propertyValue = System.getProperty(propertyName);
            return convertValue(propertyValue, defaultValue);
        }

        protected abstract T convertValue(String propertyValueFromSystem, T defaultValue);

        public String propertyName() {
            return propertyName;
        }
    }

    private static class GoIntSystemProperty extends GoSystemProperty<Integer> {
        public GoIntSystemProperty(String propertyName, Integer defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected Integer convertValue(String propertyValueFromSystem, Integer defaultValue) {
            try {
                return Integer.parseInt(propertyValueFromSystem);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    private static class GoLongSystemProperty extends GoSystemProperty<Long> {
        public GoLongSystemProperty(String propertyName, Long defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected Long convertValue(String propertyValueFromSystem, Long defaultValue) {
            try {
                return Long.parseLong(propertyValueFromSystem);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    private static class GoStringSystemProperty extends GoSystemProperty<String> {
        public GoStringSystemProperty(String propertyName, String defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected String convertValue(String propertyValueFromSystem, String defaultValue) {
            return propertyValueFromSystem == null ? defaultValue : propertyValueFromSystem;
        }
    }

    protected static class GoStringArraySystemProperty extends GoSystemProperty<String[]> {
        public GoStringArraySystemProperty(String propertyName, String[] defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected String[] convertValue(String propertyValueFromSystem, String[] defaultValue) {
            return isBlank(propertyValueFromSystem) ? defaultValue : propertyValueFromSystem.trim().split("(\\s*)?,(\\s*)?");
        }
    }

    private static class CachedProperty<T> extends GoSystemProperty<T> {
        private GoSystemProperty<T> wrappedProperty;
        private T cachedValue;

        public CachedProperty(GoSystemProperty<T> goSystemProperty) {
            super(goSystemProperty.propertyName, goSystemProperty.defaultValue);
            wrappedProperty = goSystemProperty;
        }

        @Override
        protected T convertValue(String propertyValueFromSystem, T defaultValue) {
            if (cachedValue == null) {
                cachedValue = wrappedProperty.convertValue(propertyValueFromSystem, defaultValue);
            }
            return cachedValue;
        }

        public void clear() {
            cachedValue = null;
        }
    }

    private static class GoBooleanSystemProperty extends GoSystemProperty<Boolean> {
        public GoBooleanSystemProperty(String propertyName, Boolean defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected Boolean convertValue(String propertyValueFromSystem, Boolean defaultValue) {
            if (propertyValueFromSystem == null) {
                return defaultValue;
            }
            return "Y".equalsIgnoreCase(propertyValueFromSystem) || "true".equalsIgnoreCase(propertyValueFromSystem);
        }
    }

    @Deprecated // retained for BC
    public File agentkeystore() {
        return new File(configDir(), "agentkeystore");
    }

}
