/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.utils.Timeout;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SystemEnvironment implements Serializable, ConfigDirProvider {

    private static final Logger LOG = Logger.getLogger(SystemEnvironment.class);

    public static final String CRUISE_LISTEN_HOST = "cruise.listen.host";
    private static final String CRUISE_DATABASE_PORT = "cruise.database.port";
    public static final String CRUISE_SERVER_PORT = "cruise.server.port";
    public static final String CRUISE_SERVER_SSL_PORT = "cruise.server.ssl.port";
    public static final String AGENT_JAR_PATH = "agent.jar";
    public static final String AGENT_LAUNCHER_JAR_PATH = "agent-launcher.jar";
    static final String AGENT_CONNECTION_TIMEOUT_IN_SECONDS = "agent.connection.timeout";
    private static final String JETTY_XML = "jetty.xml";
    public static final String CRUISE_SERVER_WAR_PROPERTY = "cruise.server.war";


    public static final String CRUISE_CONFIG_REPO_DIR = "cruise.config.repo.dir";
    private static final String DB_BASE_DIR = "db/";
    private static final String DB_DEFAULT_PATH = DB_BASE_DIR + "h2db";
    private static final String CONFIG_REPO_DEFAULT_PATH = DB_BASE_DIR + "config.git";
    public static final String CRUISE_DB_CACHE_SIZE = "cruise.db.cache.size";
    public static final String CRUISE_DB_CACHE_SIZE_DEFAULT = String.valueOf(128 * 1024); // 128MB Cache Size by default
    public static final String ACTIVEMQ_USE_JMX = "activemq.use.jmx";
    public static final String ACTIVEMQ_QUEUE_PREFETCH = "activemq.queue.prefetch";
    private static final String ACTIVEMQ_CONNECTOR_PORT = "activemq.conn.port";
    public static final int NUMBER_OF_DAYS_TO_EXPIRY = 30;

    public static final String NUMBER_OF_DAYS_TO_EXPIRY_PROPERTY = "number.of.days.to.expiry";
    public static final String PARENT_LOADER_PRIORITY = "parent.loader.priority";
    public static final String AGENT_CONTENT_MD5_HEADER = "Agent-Content-MD5";

    public static final String AGENT_LAUNCHER_CONTENT_MD5_HEADER = "Agent-Launcher-Content-MD5";

    public static final String AGENT_PLUGINS_ZIP_MD5_HEADER = "Agent-Plugins-Content-MD5";

    public static final String EMPTY_STRING = "";
    public static final String BLANK_STRING = EMPTY_STRING;
    public static final String ENFORCE_SERVERID_MUTABILITY = "go.enforce.serverId.immutability";

    public static final String CONFIGURATION_YES = "Y";

    public static final String CONFIGURATION_NO = "N";
    public static final String RESOLVE_FANIN_REVISIONS = "resolve.fanin.revisions";
    public static final String RESOLVE_FANIN_FALLBACK_TRIANGLE = "resolve.fanin.fallback.triangle";
    private String hsqlPath = null;

    public static final String ENABLE_CONFIG_MERGE_PROPERTY = "enable.config.merge";
    public static final GoSystemProperty<Boolean> ENABLE_CONFIG_MERGE_FEATURE = new CachedProperty<>(new GoBooleanSystemProperty(ENABLE_CONFIG_MERGE_PROPERTY, Boolean.TRUE));

    private String defaultDbPort = "9153";
    private static final String DB_UPGRADE_H2_DELTAS_FOLDER = "h2deltas";
    private boolean debug;

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
    public static final String CONFIG_DIR_PROPERTY = "cruise.config.dir";
    public static final String CONFIG_CIPHER = "cipher";
    public static final String HOSTNAME_SHINE_USES = "localhost";
    public static final int TFS_SOCKET_TIMEOUT_IN_MILLISECONDS = 20 * 60 * 1000;
    public static final String TFS_SOCKET_TIMEOUT_PROPERTY = "tfs.socket.block.timeout";

    public static GoSystemProperty<Integer> RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT = new CachedProperty<>(new GoIntSystemProperty("resolve.fanin.max.backtrack.limit", 100));
    public static GoSystemProperty<Integer> MATERIAL_UPDATE_INACTIVE_TIMEOUT = new CachedProperty<>(new GoIntSystemProperty("material.update.inactive.timeout", 15));

    public static GoSystemProperty<Integer> H2_DB_TRACE_LEVEL = new GoIntSystemProperty("h2.trace.level", 1);
    public static GoSystemProperty<Integer> H2_DB_TRACE_FILE_SIZE_MB = new GoIntSystemProperty("h2.trace.file.size.mb", 16);
    private static GoSystemProperty<String> CRUISE_DATABASE_DIR = new GoStringSystemProperty("cruise.database.dir", DB_DEFAULT_PATH);

    public static final String MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY = "material.update.idle.interval";
    private static GoSystemProperty<Long> MATERIAL_UPDATE_IDLE_INTERVAL = new GoLongSystemProperty(MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, 60000L);

    public static GoSystemProperty<Integer> PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS = new GoIntSystemProperty("pluginLocationMonitor.sleepTimeInSecs", -1);
    public static final String PLUGINS_PATH = "plugins";
    public static GoSystemProperty<String> PLUGIN_GO_PROVIDED_PATH = new GoStringSystemProperty("plugins.go.provided.path", PLUGINS_PATH + System.getProperty("file.separator") + "bundled");
    public static GoSystemProperty<String> PLUGIN_EXTERNAL_PROVIDED_PATH = new GoStringSystemProperty("plugins.external.provided.path", PLUGINS_PATH + System.getProperty("file.separator") + "external");
    public static GoSystemProperty<String> PLUGIN_BUNDLE_PATH = new CachedProperty<>(new GoStringSystemProperty("plugins.work.path", "plugins_work"));
    public static GoSystemProperty<String> PLUGIN_ACTIVATOR_JAR_PATH = new CachedProperty<>(new GoStringSystemProperty("plugins.activator.jar.path", "lib/go-plugin-activator.jar"));
    public static GoSystemProperty<Boolean> PLUGIN_FRAMEWORK_ENABLED = new GoBooleanSystemProperty("plugins.framework.enabled", Boolean.TRUE);
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

    public static GoSystemProperty<Integer> PLUGIN_NOTIFICATION_LISTENER_COUNT = new CachedProperty<>(new GoIntSystemProperty("plugin.notification.listener.count", 1));

    /* DATABASE CONFIGURATION - Defaults are of H2 */
    public static GoSystemProperty<String> GO_DATABASE_HOST = new GoStringSystemProperty("db.host", "localhost");
    public static GoSystemProperty<String> GO_DATABASE_PORT = new GoStringSystemProperty("db.port", "");
    public static GoSystemProperty<String> GO_DATABASE_NAME = new GoStringSystemProperty("db.name", "cruise");
    public static GoSystemProperty<String> GO_DATABASE_USER = new GoStringSystemProperty("db.user", "sa");
    public static GoSystemProperty<String> GO_DATABASE_PASSWORD = new GoStringSystemProperty("db.password", "");
    public static GoIntSystemProperty GO_DATABASE_MAX_ACTIVE = new GoIntSystemProperty("db.maxActive", 32);
    public static GoIntSystemProperty GO_DATABASE_MAX_IDLE = new GoIntSystemProperty("db.maxIdle", 32);
    public static final String H2_DATABASE = "com.thoughtworks.go.server.database.H2Database";
    public static GoStringSystemProperty GO_DATABASE_PROVIDER = new GoStringSystemProperty("go.database.provider", H2_DATABASE);
    public static GoSystemProperty<Boolean> SHOULD_VALIDATE_XML_AGAINST_DTD = new GoBooleanSystemProperty("validate.xml.against.dtd", false);
    public static GoSystemProperty<String> JETTY_XML_FILE_NAME = new GoStringSystemProperty("jetty.xml.file.name", JETTY_XML);

    public static final String JETTY9 = "com.thoughtworks.go.server.Jetty9Server";
    public static GoSystemProperty<String> APP_SERVER = new CachedProperty<>(new GoStringSystemProperty("app.server", JETTY9));
    public static GoSystemProperty<String> GO_SERVER_STATE = new GoStringSystemProperty("go.server.state", "active");
    public static GoSystemProperty<String> GO_LANDING_PAGE = new GoStringSystemProperty("go.landing.page", "/pipelines");

    public static GoSystemProperty<Boolean> FETCH_ARTIFACT_AUTO_SUGGEST = new GoBooleanSystemProperty("go.fetch-artifact.auto-suggest", true);
    public static GoSystemProperty<String> GO_SSL_TRANSPORT_PROTOCOL_TO_BE_USED_BY_AGENT = new GoStringSystemProperty("go.ssl.agent.protocol", "TLSv1.2");
    public static GoSystemProperty<Boolean> GO_SSL_CONFIG_ALLOW = new GoBooleanSystemProperty("go.ssl.config.allow", true);
    public static GoSystemProperty<String[]> GO_SSL_INCLUDE_CIPHERS = new GoStringArraySystemProperty("go.ssl.ciphers.include", null);
    public static GoSystemProperty<String[]> GO_SSL_EXCLUDE_CIPHERS = new GoStringArraySystemProperty("go.ssl.ciphers.exclude", null);
    public static GoSystemProperty<String[]> GO_SSL_INCLUDE_PROTOCOLS = new GoStringArraySystemProperty("go.ssl.protocols.include", null);
    public static GoSystemProperty<String[]> GO_SSL_EXCLUDE_PROTOCOLS = new GoStringArraySystemProperty("go.ssl.protocols.exclude", null);
    public static GoSystemProperty<Boolean> GO_SSL_RENEGOTIATION_ALLOWED = new GoBooleanSystemProperty("go.ssl.renegotiation.allowed", true);
    public static GoSystemProperty<Boolean> GO_CONFIG_REPO_GC_AGGRESSIVE = new GoBooleanSystemProperty("go.config.repo.gc.aggressive", true);
    public static GoSystemProperty<Long> GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD = new GoLongSystemProperty("go.config.repo.gc.warning.looseobject.threshold", 10000L);
    public static GoSystemProperty<Boolean> GO_CONFIG_REPO_PERIODIC_GC = new GoBooleanSystemProperty("go.config.repo.gc.periodic", false);

    public static GoSystemProperty<String> GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME = new GoStringSystemProperty("go.update.server.public.key.file.name", "go_update_server.pub");
    public static GoSystemProperty<String> GO_UPDATE_SERVER_URL = new GoStringSystemProperty("go.update.server.url", "https://update.go.cd/channels/supported/latest.json");
    public static GoSystemProperty<Boolean> GO_CHECK_UPDATES = new GoBooleanSystemProperty("go.check.updates", true);

    public static GoSystemProperty<Integer> GO_ELASTIC_PLUGIN_CREATE_AGENT_THREADS = new GoIntSystemProperty("go.elasticplugin.createagent.threads", 5);
    public static GoSystemProperty<Integer> GO_ELASTIC_PLUGIN_SERVER_PING_THREADS = new GoIntSystemProperty("go.elasticplugin.serverping.threads", 1);

    public static GoSystemProperty<Boolean> WEBSOCKET_ENABLED = new GoBooleanSystemProperty("go.agent.websocket.enabled", false);
    public static GoSystemProperty<Boolean> AUTO_REGISTER_LOCAL_AGENT_ENABLED = new GoBooleanSystemProperty("go.auto.register.local.agent.enabled", true);
    public static GoSystemProperty<Long> GO_WEBSOCKET_ACK_MESSAGE_TIMEOUT = new GoLongSystemProperty("go.websocket.ack.message.timeout", 300 * 1000L);

    public static GoSystemProperty<Long> GO_WEBSOCKET_MAX_IDLE_TIME = new GoLongSystemProperty("go.websocket.max.idle.time", 60 * 1000L);
    public static GoSystemProperty<Boolean> GO_SERVER_SHALLOW_CLONE = new GoBooleanSystemProperty("go.server.shallowClone", false);
    public static GoSystemProperty<Boolean> GO_SERVER_SCHEDULED_PIPELINE_LOADER_GLOBAL_MATERIAL_LOOKUP = new GoBooleanSystemProperty("go.server.scheduledPipelineLoader.globalMaterialLookup", false);
    public static GoSystemProperty<Boolean> PLUGIN_UPLOAD_ENABLED = new GoBooleanSystemProperty("go.plugin.upload.enabled", false);

    public static GoSystemProperty<Boolean> GO_API_WITH_SAFE_MODE = new GoBooleanSystemProperty("go.api.with.safe.mode", true);
    public static GoSystemProperty<Integer> MAX_PENDING_AGENTS_ALLOWED = new GoIntSystemProperty("max.pending.agents.allowed", 100);
    public static GoSystemProperty<Boolean> CHECK_AND_REMOVE_DUPLICATE_MODIFICATIONS = new GoBooleanSystemProperty("go.modifications.removeDuplicates", true);
    public static GoSystemProperty<String> GO_AGENT_KEYSTORE_PASSWORD = new GoStringSystemProperty("go.agent.keystore.password", "agent5s0repa55w0rd");
    private static final GoSystemProperty<Boolean> GO_AGENT_USE_SSL_CONTEXT = new GoBooleanSystemProperty("go.agent.reuse.ssl.context", true);
    public static final GoSystemProperty<? extends Boolean> ENABLE_BUILD_COMMAND_PROTOCOL = new GoBooleanSystemProperty("go.agent.enableBuildCommandProtocol", false);

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
    private volatile static Long artifactFullSizeLimit;
    private volatile static Long diskSpaceCacheRefresherInterval;
    public static final String UNRESPONSIVE_JOB_WARNING_THRESHOLD = "cruise.unresponsive.job.warning";
    private File configDir;
    private volatile Boolean enforceRevisionCompatibilityWithUpstream;
    private volatile Boolean enforceFanInFallbackTriangle;

    public SystemEnvironment() {
    }

    public SystemEnvironment(Properties properties) {
        this.properties = properties;
    }

    public SystemEnvironment(String hsqlPath, String defaultDbPort) {
        if (defaultDbPort == null) {
            throw ExceptionUtils.bomb("Default db port cannot be null");
        }
        this.hsqlPath = hsqlPath;
        this.defaultDbPort = defaultDbPort;
        debug = getPropertyImpl("DB_DEBUG_MODE") != null
                && !(getPropertyImpl("DB_DEBUG_MODE").equals("false"));

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

    public File configDir() {
        if (configDir == null) {
            configDir = new File(getPropertyImpl(CONFIG_FILE_PROPERTY, "config/cruise-config.xml")).getParentFile();
        }
        return configDir;
    }

    public File keystore() {
        return new File(configDir(), "keystore");
    }

    public File truststore() {
        return new File(configDir(), "truststore");
    }

    public File agentkeystore() {
        return new File(configDir(), "agentkeystore");
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

    public int getSslServerPort() {
        if (cruiseSSlPort != null) {
            return cruiseSSlPort;
        }
        return cruiseSSlPort = Integer.parseInt(getPropertyImpl(CRUISE_SERVER_SSL_PORT, "8154"));
    }

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

    public int getDatabaseSeverPort() {
        String port = getDatabasePort();
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            LOG.info("Could not parse port=" + port);
        }
        return Integer.parseInt(defaultDbPort);
    }

    private String getDatabasePort() {
        String port = getPropertyImpl(CRUISE_DATABASE_PORT);
        if (port == null) {
            port = defaultDbPort;
        }
        return port;
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

    public String getScriptPath() {
        return new File(getDbPath(), "cruise").getAbsolutePath();
    }

    public File getDBDeltasPath() {
        return new File(getDbFolder(), DB_UPGRADE_H2_DELTAS_FOLDER);
    }

    public File getDbPath() {
        if (hsqlPath == null) {
            hsqlPath = get(CRUISE_DATABASE_DIR);
        }
        return new File(hsqlPath);
    }

    public File getConfigRepoDir() {
        return new File(properties().getProperty(CRUISE_CONFIG_REPO_DIR, CONFIG_REPO_DEFAULT_PATH));
    }

    public File getCipherFile() {
        return new File(getConfigDir(), CONFIG_CIPHER);
    }

    public File getDbFolder() {
        return getDbPath().getParentFile();
    }

    public int getNumberOfMaterialCheckListener() {
        return Integer.parseInt(getPropertyImpl("material.check.threads", "10"));
    }

    public int getNumberOfConfigMaterialCheckListener() {
        return Integer.parseInt(getPropertyImpl("material.config.check.threads", "2"));
    }

    public int getNumberOfPluginNotificationListener() {
        return PLUGIN_NOTIFICATION_LISTENER_COUNT.getValue();
    }

    public File getAgentJarFile() {
        return new File(AGENT_JAR_PATH);
    }

    public String getAgentMd5() {
        return getPropertyImpl(GoConstants.AGENT_JAR_MD5, BLANK_STRING);
    }

    public String getGivenAgentLauncherMd5() {
        return getPropertyImpl(GoConstants.GIVEN_AGENT_LAUNCHER_JAR_MD5, BLANK_STRING);
    }

    public String getAgentPluginsMd5() {
        return getPropertyImpl(GoConstants.AGENT_PLUGINS_MD5, BLANK_STRING);
    }

    public boolean inDbDebugMode() {
        return debug;
    }

    public boolean usingRemoteDb() {
        return getPropertyImpl("DB_REMOTE") != null
                && !(getPropertyImpl("DB_REMOTE").equals("false"));
    }

    public void setDebugMode(boolean debug) {
        this.debug = debug;
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
            try(InputStream is = getClass().getResourceAsStream(CRUISE_PROPERTIES)) {
                properties.load(is);
            } catch (Exception e) {
                LOG.error("Unable to load newProperties file " + CRUISE_PROPERTIES);
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
        return Integer.valueOf(getPropertyImpl(INTERVAL, "5"));
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


    public String getEnvironmentVariable(String key) {
        return System.getenv(key);
    }

    public Map<String, String> getGitAllowedProtocols() {
        return GIT_ALLOW_PROTOCOL;
    }

    public String getBaseUrlForShine() {
        return String.format("http://%s:%s%s", HOSTNAME_SHINE_USES, getServerPort(), getWebappContextPath());
    }

    public Boolean isShineEnabled() {
        String shineEnabled = getEnvironmentVariable("SHINE_ENABLED");
        return shineEnabled == null ? false : !"false".equalsIgnoreCase(shineEnabled); //should return true for shine_enabled set to anything but false.
    }


    public String getBaseSslUrlForShineWithoutContextPath() {
        return String.format("https://%s:%s", HOSTNAME_SHINE_USES, getSslServerPort());
    }

    public String getBaseSslUrlForShine() {
        return getBaseSslUrlForShineWithoutContextPath() + getWebappContextPath();
    }

    public File shineDb() {
        return new File(properties().getProperty("cruise.shine.store.folder"));
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

    public String getCruiseDbCacheSize() {
        return getPropertyImpl(CRUISE_DB_CACHE_SIZE, CRUISE_DB_CACHE_SIZE_DEFAULT);
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

    public boolean isPluginsEnabled() {
        return GoConstants.ENABLE_PLUGINS_RESPONSE_TRUE.equals(pluginStatus());
    }

    public String pluginStatus() {
        String property = getPropertyImpl(GoConstants.ENABLE_PLUGINS_PROPERTY, GoConstants.N_NO);
        return GoConstants.Y_YES.equals(property) ? GoConstants.ENABLE_PLUGINS_RESPONSE_TRUE : GoConstants.ENABLE_PLUGINS_RESPONSE_FALSE;
    }

    public String getAgentLauncherVersion() {
        return getPropertyImpl(GoConstants.AGENT_LAUNCHER_VERSION, EMPTY_STRING);
    }

    public static final ThreadLocal<Boolean> enforceServerIdImmutability = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public boolean enforceServerIdImmutability() {
        return CONFIGURATION_YES.equals(getPropertyImpl(ENFORCE_SERVERID_MUTABILITY, CONFIGURATION_YES)) || enforceServerIdImmutability.get();
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

    public boolean enforceFanInFallbackBehaviour() {
        if (enforceFanInFallbackTriangle == null) {
            enforceFanInFallbackTriangle = CONFIGURATION_YES.equals(getPropertyImpl(RESOLVE_FANIN_FALLBACK_TRIANGLE, CONFIGURATION_NO));
        }
        return enforceFanInFallbackTriangle;
    }

    public int getCruiseDbTraceLevel() {
        return H2_DB_TRACE_LEVEL.getValue();
    }

    public int getCruiseDbTraceFileSize() {
        return H2_DB_TRACE_FILE_SIZE_MB.getValue();
    }

    public Level pluginLoggingLevel(String pluginId) {
        return Level.toLevel(getPropertyImpl("plugin." + pluginId + ".log.level", "INFO"), Level.INFO);
    }

    public File getDefaultCommandRepository() {
        return new File(get(COMMAND_REPOSITORY_DIRECTORY), "default");
    }

    public String getCommandRepositoryRootLocation() {
        return new File(get(COMMAND_REPOSITORY_DIRECTORY)).getAbsolutePath();
    }

    public String getExternalPluginAbsolutePath() {
        return new File(get(PLUGIN_EXTERNAL_PROVIDED_PATH)).getAbsolutePath();
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

    public boolean isDefaultDbProvider() {
        return GO_DATABASE_PROVIDER.getValue().equals(H2_DATABASE);
    }

    public String getDatabaseProvider() {
        return GO_DATABASE_PROVIDER.getValue();
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

    public void switchToActiveState() {
        set(GO_SERVER_STATE, "active");
    }

    public void switchToPassiveState() {
        set(GO_SERVER_STATE, "passive");
    }

    public String getUpdateServerPublicKeyPath() {
        return String.format("%s/%s", getConfigDir(), GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME.getValue());
    }

    public boolean isGOUpdateCheckEnabled() {
        return GO_CHECK_UPDATES.getValue();
    }

    public String getUpdateServerUrl() {
        return GO_UPDATE_SERVER_URL.getValue();
    }

    public boolean isWebsocketEnabled() {
        return WEBSOCKET_ENABLED.getValue();
    }

    public boolean isAutoRegisterLocalAgentEnabled() {
        return AUTO_REGISTER_LOCAL_AGENT_ENABLED.getValue();
    }

    public boolean isPluginUploadEnabled() {
        return PLUGIN_UPLOAD_ENABLED.getValue();
    }

    public Long getWebsocketMaxIdleTime() {
        return GO_WEBSOCKET_MAX_IDLE_TIME.getValue();
    }

    public Long getWebsocketAckMessageTimeout() {
        return GO_WEBSOCKET_ACK_MESSAGE_TIMEOUT.getValue();
    }

    public boolean isApiSafeModeEnabled() {
        return GO_API_WITH_SAFE_MODE.getValue();
    }

    public boolean useSslContext() {
        return GO_AGENT_USE_SSL_CONTEXT.getValue();
    }

    public static abstract class GoSystemProperty<T> {
        private String propertyName;
        private T defaultValue;

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
            return StringUtil.isBlank(propertyValueFromSystem) ? defaultValue : propertyValueFromSystem.trim().split("(\\s*)?,(\\s*)?");
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
}
