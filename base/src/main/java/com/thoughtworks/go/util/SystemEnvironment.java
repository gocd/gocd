/*
 * Copyright Thoughtworks, Inc.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.*;

public class SystemEnvironment implements Serializable, ConfigDirProvider {

    public static final String CRUISE_LISTEN_HOST = "cruise.listen.host";
    public static final String CRUISE_SERVER_PORT = "cruise.server.port";

    private static final String JETTY_XML = "jetty.xml";
    public static final String CRUISE_SERVER_WAR_PROPERTY = "cruise.server.war";

    public static final String CRUISE_CONFIG_REPO_DIR = "cruise.config.repo.dir";
    public static final String DB_BASE_DIR = "db/";
    private static final String CONFIG_REPO_DEFAULT_PATH = DB_BASE_DIR + "config.git";

    public static final String ACTIVEMQ_USE_JMX = "activemq.use.jmx";
    public static final String ACTIVEMQ_QUEUE_PREFETCH = "activemq.queue.prefetch";
    private static final String ACTIVEMQ_CONNECTOR_PORT = "activemq.conn.port";

    public static final String PARENT_LOADER_PRIORITY = "parent.loader.priority";

    public static final String ENFORCE_SERVER_IMMUTABILITY = "go.enforce.server.immutability";

    public static final String CONFIGURATION_YES = "Y";
    public static final String CONFIGURATION_NO = "N";

    public static final String RESOLVE_FANIN_REVISIONS = "resolve.fanin.revisions";

    public static final String ENABLE_CONFIG_MERGE_PROPERTY = "enable.config.merge";
    public static final GoSystemProperty<Boolean> ENABLE_CONFIG_MERGE_FEATURE = new CachedProperty<>(new GoBooleanSystemProperty(ENABLE_CONFIG_MERGE_PROPERTY, Boolean.TRUE));

    public static final String CRUISE_PROPERTIES = "/cruise.properties";

    public static final String USE_COMPRESSED_JAVASCRIPT = "rails.use.compressed.js";

    public static final String AGENT_SIZE_LIMIT = "agent.size.limit";
    public static final String ARTIFACT_FULL_SIZE_LIMIT = "artifact.full.size.limit";
    public static final String DATABASE_FULL_SIZE_LIMIT = "db.full.size.limit";
    public static final String ARTIFACT_WARNING_SIZE_LIMIT = "artifact.warning.size.limit";
    public static final String DATABASE_WARNING_SIZE_LIMIT = "db.warning.size.limit";
    private static final String DISK_SPACE_CACHE_REFRESHER_INTERVAL = "disk.space.cache.refresher.interval";


    public static final String CONFIG_FILE_PROPERTY = "cruise.config.file";
    public static final String CONSOLE_PUBLISH_INTERVAL_SECONDS = "cruise.console.publish.interval";


    public static final String SERVICE_URL = "serviceUrl";
    public static final String AGENT_SSL_VERIFICATION_MODE = "sslVerificationMode";
    public static final String AGENT_ROOT_CERT_FILE = "rootCertFile";
    public static final String AGENT_PRIVATE_KEY = "sslPrivateKeyFile";
    public static final String AGENT_PRIVATE_KEY_PASSPHRASE_FILE = "sslPrivateKeyPassphraseFile";
    public static final String AGENT_SSL_CERTIFICATE = "sslCertificateFile";
    static final String AGENT_CONNECTION_TIMEOUT_IN_SECONDS = "agent.connection.timeout";
    public static final String AGENT_JAR_MD5 = "agent.binary.md5";
    public static final String AGENT_PLUGINS_MD5 = "agent.plugins.md5";
    public static final String AGENT_BOOTSTRAPPER_VERSION = "agent.bootstrapper.version";
    public static final String AGENT_TFS_IMPL_MD5 = "agent.tfs.md5";
    public static final String AGENT_LAUNCHER_JAR_MD5 = "agent.launcher.md5";

    public static final String CONFIG_DIR_PROPERTY = "cruise.config.dir";
    public static final String DES_CONFIG_CIPHER = "cipher";
    public static final String AES_CONFIG_CIPHER = "cipher.aes";
    public static final long TFS_SOCKET_TIMEOUT_IN_MILLIS = MINUTES.toMillis(20);
    public static final String TFS_SOCKET_TIMEOUT_PROPERTY = "tfs.socket.block.timeout";

    static final String UNRESPONSIVE_JOB_WARNING_THRESHOLD = "cruise.unresponsive.job.warning";

    public static final int DEFAULT_MAIL_SENDER_TIMEOUT_IN_MILLIS = (int) SECONDS.toMillis(60);
    private static final GoSystemProperty<Integer> MAIL_SENDER_TIMEOUT_IN_MILLIS = new GoIntSystemProperty("cruise.mail.sender.timeout", DEFAULT_MAIL_SENDER_TIMEOUT_IN_MILLIS);

    public static final GoSystemProperty<Integer> RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT = new CachedProperty<>(new GoIntSystemProperty("resolve.fanin.max.backtrack.limit", 100));
    public static final GoSystemProperty<Integer> MATERIAL_UPDATE_INACTIVE_TIMEOUT_IN_MINUTES = new CachedProperty<>(new GoIntSystemProperty("material.update.inactive.timeout", 15));

    public static final String MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY = "material.update.idle.interval";
    private static final GoSystemProperty<Long> MATERIAL_UPDATE_IDLE_INTERVAL_IN_MILLIS = new GoLongSystemProperty(MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, SECONDS.toMillis(60));

    public static final GoSystemProperty<Integer> PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS = new GoIntSystemProperty("pluginLocationMonitor.sleepTimeInSecs", -1);
    public static final String PLUGINS_PATH = "plugins";
    public static final GoSystemProperty<String> PLUGIN_GO_PROVIDED_PATH = new GoStringSystemProperty("plugins.go.provided.path", PLUGINS_PATH + FileSystems.getDefault().getSeparator() + "bundled");
    public static final GoSystemProperty<String> PLUGIN_EXTERNAL_PROVIDED_PATH = new GoStringSystemProperty("plugins.external.provided.path", PLUGINS_PATH + FileSystems.getDefault().getSeparator() + "external");
    public static final GoSystemProperty<String> PLUGIN_WORK_DIR = new CachedProperty<>(new GoStringSystemProperty("plugins.work.path", "plugins_work"));
    public static final GoSystemProperty<String> PLUGIN_ACTIVATOR_JAR_PATH = new CachedProperty<>(new GoStringSystemProperty("plugins.activator.jar.path", "lib/go-plugin-activator.jar"));
    public static final GoSystemProperty<String> ALL_PLUGINS_ZIP_PATH = new GoStringSystemProperty("plugins.all.zip.path", new File(PLUGINS_PATH, "go-plugins-all.zip").getAbsolutePath());
    public static final GoSystemProperty<String> ADDONS_PATH = new GoStringSystemProperty("addons.path", "addons");
    public static final GoSystemProperty<String> AVAILABLE_FEATURE_TOGGLES_FILE_PATH = new GoStringSystemProperty("available.toggles.path", "/available.toggles");
    public static final GoSystemProperty<String> USER_FEATURE_TOGGLES_FILE_PATH_RELATIVE_TO_CONFIG_DIR = new GoStringSystemProperty("user.toggles.path", "go.feature.toggles");

    public static final GoSystemProperty<String> DEFAULT_PLUGINS_ZIP = new CachedProperty<>(new GoStringSystemProperty("default.plugins.zip.location", "/defaultFiles/plugins.zip"));
    public static final GoSystemProperty<String> AGENT_PLUGINS_PATH = new CachedProperty<>(new GoStringSystemProperty("agent.plugins.path", PLUGINS_PATH));
    public static final GoSystemProperty<Long> GO_SERVER_CONNECTION_IDLE_TIMEOUT_IN_MILLIS = new GoLongSystemProperty("idle.timeout", SECONDS.toMillis(30));
    public static final GoSystemProperty<Integer> RESPONSE_BUFFER_SIZE = new GoIntSystemProperty("response.buffer.size", 32 * 1024);
    public static final GoSystemProperty<Integer> API_REQUEST_IDLE_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("api.request.idle.timeout.seconds", (int) MINUTES.toSeconds(5));
    public static final GoSystemProperty<Integer> AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("agent.request.idle.timeout.seconds", 30);
    public static final GoSystemProperty<Integer> GO_SERVER_SESSION_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("go.server.session.timeout.seconds", (int) DAYS.toSeconds(14));
    public static final GoSystemProperty<Integer> GO_SERVER_SESSION_COOKIE_MAX_AGE_IN_SECONDS = new GoIntSystemProperty("go.sessioncookie.maxage.seconds", (int) DAYS.toSeconds(14));
    public static final GoSystemProperty<Boolean> GO_SERVER_SESSION_COOKIE_SECURE = new GoBooleanSystemProperty("go.sessioncookie.secure", false);
    public static final GoSystemProperty<String> AGENT_EXTRA_PROPERTIES = new GoStringSystemProperty("gocd.agent.extra.properties", "");
    public static final GoSystemProperty<Long> JMS_LISTENER_BACKOFF_TIME_IN_MILLIS = new GoLongSystemProperty("go.jms.listener.backoff.time.in.milliseconds", SECONDS.toMillis(5));

    public static final GoSystemProperty<Integer> GO_SERVER_AUTHORIZATION_EXTENSION_CALLS_CACHE_TIMEOUT_IN_SECONDS = new GoIntSystemProperty("go.server.authorization.extension.calls.cache.timeout.in.secs", 60);

    public static final GoSystemProperty<String> JETTY_XML_FILE_NAME = new GoStringSystemProperty("jetty.xml.file.name", JETTY_XML);

    public static final String JETTY = "com.thoughtworks.go.server.JettyServer";
    public static final GoSystemProperty<String> APP_SERVER = new CachedProperty<>(new GoStringSystemProperty("app.server", JETTY));
    public static final GoSystemProperty<String> GO_LANDING_PAGE = new GoStringSystemProperty("go.landing.page", "/pipelines");

    public static final GoSystemProperty<Boolean> FETCH_ARTIFACT_AUTO_SUGGEST = new GoBooleanSystemProperty("go.fetch-artifact.auto-suggest", true);
    public static final GoSystemProperty<Boolean> GO_FETCH_ARTIFACT_TEMPLATE_AUTO_SUGGEST = new GoBooleanSystemProperty("go.fetch-artifact.template.auto-suggest", true);

    public static final GoSystemProperty<Boolean> GO_CONFIG_REPO_GC_AGGRESSIVE = new GoBooleanSystemProperty("go.config.repo.gc.aggressive", true);
    public static final GoSystemProperty<Long> GO_CONFIG_REPO_GC_EXPIRE_IN_HOURS = new GoLongSystemProperty("go.config.repo.gc.expire", 24L);
    public static final GoSystemProperty<Long> GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD = new GoLongSystemProperty("go.config.repo.gc.warning.looseobject.threshold", 10000L);
    public static final GoSystemProperty<Boolean> GO_CONFIG_REPO_PERIODIC_GC = new GoBooleanSystemProperty("go.config.repo.gc.periodic", false);

    public static final GoSystemProperty<String> GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME = new GoStringSystemProperty("go.update.server.public.key.file.name", "go_update_server.pub");
    public static final GoSystemProperty<String> GO_UPDATE_SERVER_URL = new GoStringSystemProperty("go.update.server.url", "https://update.gocd.org/channels/supported/latest.json");
    public static final GoSystemProperty<Boolean> GO_CHECK_UPDATES = new GoBooleanSystemProperty("go.check.updates", true);

    public static final GoSystemProperty<Integer> GO_ELASTIC_PLUGIN_CREATE_AGENT_THREADS = new GoIntSystemProperty("go.elasticplugin.createagent.threads", 5);
    public static final GoSystemProperty<Integer> GO_ELASTIC_PLUGIN_SERVER_PING_THREADS = new GoIntSystemProperty("go.elasticplugin.serverping.threads", 1);
    public static final GoSystemProperty<Integer> GO_ENCRYPTION_API_MAX_REQUESTS = new GoIntSystemProperty("go.encryption.api.max.requests", 30);

    public static final GoSystemProperty<String> GO_ANALYTICS_PLUGIN_EXTERNAL_ASSETS = new GoStringSystemProperty("go.analytics.plugin.external.assets", "./analytics-assets");
    public static final GoSystemProperty<Boolean> AUTO_REGISTER_LOCAL_AGENT_ENABLED = new GoBooleanSystemProperty("go.auto.register.local.agent.enabled", true);

    public static final GoSystemProperty<Boolean> GO_SERVER_SHALLOW_CLONE = new GoBooleanSystemProperty("go.server.shallowClone", false);

    public static final GoSystemProperty<Integer> MAX_PENDING_AGENTS_ALLOWED = new GoIntSystemProperty("max.pending.agents.allowed", 100);
    public static final GoSystemProperty<Boolean> CHECK_AND_REMOVE_DUPLICATE_MODIFICATIONS = new GoBooleanSystemProperty("go.modifications.removeDuplicates", true);
    public static final GoSystemProperty<Boolean> GO_DIAGNOSTICS_MODE = new GoBooleanSystemProperty("go.diagnostics.mode", false);

    public static final GoSystemProperty<Integer> DEPENDENCY_MATERIAL_UPDATE_LISTENERS = new GoIntSystemProperty("dependency.material.check.threads", 3);

    public static final GoSystemProperty<Integer> CONFIG_MATERIAL_POST_UPDATE_LISTENERS = new GoIntSystemProperty("config.material.post.update.threads", 2);

    public static final GoSystemProperty<String> GO_SERVER_MODE = new GoStringSystemProperty("go.server.mode", "production");
    public static final GoSystemProperty<Boolean> REAUTHENTICATION_ENABLED = new GoBooleanSystemProperty("go.security.reauthentication.enabled", true);
    public static final GoSystemProperty<Long> REAUTHENTICATION_TIME_INTERVAL_IN_MILLIS = new GoLongSystemProperty("go.security.reauthentication.interval", MINUTES.toMillis(30));
    public static final GoSystemProperty<Boolean> CONSOLE_OUT_TO_STDOUT = new GoBooleanSystemProperty("go.console.stdout", false);
    private static final GoSystemProperty<String> CONSOLE_LOG_CHARSET = new GoStringSystemProperty("go.console.log.charset", "utf-8");
    public static final GoSystemProperty<Integer> CONSOLE_LOG_MAX_LINE_LENGTH = new GoIntSystemProperty("go.console.log.max.line.length", 1_000_000);
    private static final GoSystemProperty<Boolean> AGENT_STATUS_API_ENABLED = new GoBooleanSystemProperty("go.agent.status.api.enabled", true);
    private static final GoSystemProperty<String> AGENT_STATUS_API_BIND_HOST = new GoStringSystemProperty("go.agent.status.api.bind.host", "localhost");
    private static final GoSystemProperty<Integer> AGENT_STATUS_API_BIND_PORT = new GoIntSystemProperty("go.agent.status.api.bind.port", 8152);

    private static final GoSystemProperty<Integer> GO_SPA_TIMEOUT_IN_MILLIS = new GoIntSystemProperty("go.spa.timeout", (int) SECONDS.toMillis(60));
    private static final GoSystemProperty<Integer> GO_SPA_REFRESH_INTERVAL_IN_MILLIS = new GoIntSystemProperty("go.spa.refresh.interval", (int) SECONDS.toMillis(5));
    private static final GoSystemProperty<Long> GO_PAC_CLONE_TIMEOUT_IN_MILLIS = new GoLongSystemProperty("go.pac.clone.timeout", SECONDS.toMillis(30));

    private static final GoSystemProperty<Boolean> ENABLE_ANALYTICS_ONLY_FOR_ADMINS = new GoBooleanSystemProperty("go.enable.analytics.only.for.admins", false);
    public static final GoSystemProperty<Long> NOTIFICATION_PLUGIN_MESSAGES_TTL_IN_MILLIS = new GoLongSystemProperty("plugins.notification.message.ttl.millis", MINUTES.toMillis(2));
    public static final GoSystemProperty<Boolean> ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP = new GoBooleanSystemProperty("allow.everyone.to.view.operate.groups.with.no.authorization.setup", false);

    public static final GoSystemProperty<Boolean> ENABLE_HSTS_HEADER = new GoBooleanSystemProperty("gocd.enable.hsts.header", false);
    public static final GoSystemProperty<Long> HSTS_HEADER_MAX_AGE_IN_SECONDS = new GoLongSystemProperty("gocd.hsts.header.max.age", DAYS.toSeconds(365));
    public static final GoSystemProperty<Boolean> HSTS_HEADER_INCLUDE_SUBDOMAINS = new GoBooleanSystemProperty("gocd.hsts.header.include.subdomains", false);
    public static final GoSystemProperty<Boolean> HSTS_HEADER_PRELOAD = new GoBooleanSystemProperty("gocd.hsts.header.preload", false);
    public static final GoSystemProperty<Long> EPHEMERAL_AUTO_REGISTER_KEY_EXPIRY_IN_MILLIS = new GoLongSystemProperty("gocd.ephemeral.auto.register.key.expiry.millis", MINUTES.toMillis(30));
    public static final GoSystemProperty<Float> MDU_EXPONENTIAL_BACKOFF_MULTIPLIER = new GoFloatSystemProperty("gocd.mdu.exponential.backoff.multiplier", 1.5f);

    public static final GoSystemProperty<Boolean> START_IN_MAINTENANCE_MODE = new GoBooleanSystemProperty("gocd.server.start.in.maintenance.mode", false);

    public static final GoSystemProperty<Boolean> INITIALIZE_CONFIG_REPOSITORIES_ON_STARTUP = new GoBooleanSystemProperty("gocd.initialize.config.repositories.on.startup", true);

    private static final Map<String, String> GIT_ALLOW_PROTOCOL = Map.of(
        "GIT_ALLOW_PROTOCOL",
        System.getenv("GIT_ALLOW_PROTOCOL") == null ? "http:https:ssh:git:file:rsync" : System.getenv("GIT_ALLOW_PROTOCOL")
    );

    private volatile static Integer agentConnectionTimeout;
    private volatile static String cruiseConfigDir;
    private volatile static Long databaseFullSizeLimit;
    private volatile static Charset consoleLogCharset;
    private volatile static Long artifactFullSizeLimit;
    private volatile static Long diskSpaceCacheRefresherInterval;

    private Properties properties;
    private File configDir;
    private volatile Boolean enforceRevisionCompatibilityWithUpstream;

    public SystemEnvironment() {
    }

    public SystemEnvironment(Properties properties) {
        this.properties = properties;
    }

    public <T> @NotNull T get(@NotNull GoSystemProperty<T> systemProperty) {
        return systemProperty.getValue();
    }


    @TestOnly
    public <T> void set(GoSystemProperty<T> systemProperty, @NotNull T value) {
        System.setProperty(systemProperty.propertyName, String.valueOf(value));
        systemProperty.clearIfNecessary();
    }

    @TestOnly
    public void set(GoSystemProperty<Boolean> systemProperty, boolean value) {
        System.setProperty(systemProperty.propertyName, value ? "Y" : "N");
        systemProperty.clearIfNecessary();
    }

    @TestOnly
    public <T> void reset(GoSystemProperty<T> systemProperty) {
        System.clearProperty(systemProperty.propertyName());
        systemProperty.clearIfNecessary();
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

    public int getMailSenderTimeoutMillis() {
        return get(MAIL_SENDER_TIMEOUT_IN_MILLIS);
    }

    public long getArtifactRepositoryFullLimit() {
        return Objects.requireNonNullElseGet(artifactFullSizeLimit,
            () -> artifactFullSizeLimit = Long.parseLong(trimMegaFromSize(getPropertyImpl(ARTIFACT_FULL_SIZE_LIMIT, "100M"))));
    }

    public long getDatabaseDiskSpaceFullLimit() {
        return Objects.requireNonNullElseGet(databaseFullSizeLimit,
            () -> databaseFullSizeLimit = Long.parseLong(trimMegaFromSize(getPropertyImpl(DATABASE_FULL_SIZE_LIMIT, "100M"))));
    }

    public long getDiskSpaceCacheRefresherInterval() {
        return Objects.requireNonNullElseGet(diskSpaceCacheRefresherInterval,
            () -> diskSpaceCacheRefresherInterval = Long.parseLong(getPropertyImpl(DISK_SPACE_CACHE_REFRESHER_INTERVAL, "5000")));
    }

    public boolean consoleOutToStdout() {
        return get(CONSOLE_OUT_TO_STDOUT);
    }

    @TestOnly
    public void setDiskSpaceCacheRefresherInterval(long interval) {
        diskSpaceCacheRefresherInterval = interval;
    }

    public long getAgentSizeLimit() {
        return Long.parseLong(trimMegaFromSize(getPropertyImpl(AGENT_SIZE_LIMIT, "100M"))) * 1024 * 1024;
    }

    public long getArtifactRepositoryWarningLimit() {
        return Long.parseLong(trimMegaFromSize(getPropertyImpl(ARTIFACT_WARNING_SIZE_LIMIT, "1024M")));
    }

    public long getDatabaseDiskSpaceWarningLimit() {
        return Long.parseLong(trimMegaFromSize(getPropertyImpl(DATABASE_WARNING_SIZE_LIMIT, "1024M")));
    }

    private String trimMegaFromSize(String sizeInMega) {
        return sizeInMega.replaceFirst("[mM]$", "");
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

    public String getPropertyImpl(String property, String defaultValue) {
        return System.getProperty(property, defaultValue);
    }

    public String getPropertyImpl(String property) {
        return System.getProperty(property);
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends Enum<T>> T safeToEnum(Class<T> enumClass, String value, T defaultValue) {
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getOperatingSystemFamilyJvmName() {
        return System.getProperty("os.name").startsWith("Windows") ? "Windows" : System.getProperty("os.name");
    }

    public int getActivemqQueuePrefetch() {
        return Integer.parseInt(getPropertyImpl(ACTIVEMQ_QUEUE_PREFETCH, "0"));
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
        return getPropertyImpl(AGENT_JAR_MD5, "");
    }

    public String getTfsImplMd5() {
        return getPropertyImpl(AGENT_TFS_IMPL_MD5, "");
    }

    public String getGivenAgentLauncherMd5() {
        return getPropertyImpl(AGENT_LAUNCHER_JAR_MD5, "");
    }

    public String getAgentPluginsMd5() {
        return getPropertyImpl(AGENT_PLUGINS_MD5, "");
    }

    private Properties properties() {
        if (properties == null) {
            properties = new Properties();
            try (InputStream is = getClass().getResourceAsStream(CRUISE_PROPERTIES)) {
                properties.load(is);
            } catch (Exception e) {
                // Deliberately avoiding initializing the logger during class load to allow for manual
                // logger configuration during startup
                LoggerFactory.getLogger(SystemEnvironment.class).error("Unable to load newProperties file {}", CRUISE_PROPERTIES);
            }
        }
        return properties;
    }

    public String getCruiseConfigFile() {
        return getPropertyImpl(CONFIG_FILE_PROPERTY, getConfigDir() + "/cruise-config.xml");
    }

    public int getAgentConnectionTimeout() {
        return Objects.requireNonNullElseGet(agentConnectionTimeout,
            () -> agentConnectionTimeout = Integer.parseInt(getPropertyImpl(AGENT_CONNECTION_TIMEOUT_IN_SECONDS, "300")));
    }

    public Integer getConsolePublishIntervalSeconds() {
        return Integer.valueOf(getPropertyImpl(CONSOLE_PUBLISH_INTERVAL_SECONDS, "10"));
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
        return "https://localhost:8153" + getWebappContextPath();
    }

    public boolean useCompressedJs() {
        return Boolean.parseBoolean(getPropertyImpl(USE_COMPRESSED_JAVASCRIPT, "true"));
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
        cruiseConfigDir = null;
        databaseFullSizeLimit = null;
        artifactFullSizeLimit = null;
        consoleLogCharset = null;
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

    public Duration getUnresponsiveJobWarningThreshold() {
        return Duration.ofMinutes(Long.parseLong(getPropertyImpl(UNRESPONSIVE_JOB_WARNING_THRESHOLD, "5")));
    }

    public boolean getParentLoaderPriority() {
        return Boolean.parseBoolean(getPropertyImpl(PARENT_LOADER_PRIORITY, "false"));
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
        return Integer.parseInt(getPropertyImpl(TFS_SOCKET_TIMEOUT_PROPERTY, String.valueOf(TFS_SOCKET_TIMEOUT_IN_MILLIS)));
    }

    public Level pluginLoggingLevel(String pluginId) {
        return safeToEnum(Level.class, getPropertyImpl("plugin." + pluginId + ".log.level", "INFO"), Level.INFO);
    }

    public Charset consoleLogCharset() {
        if (consoleLogCharset == null) {
            consoleLogCharset = Charset.forName(get(CONSOLE_LOG_CHARSET));
        }
        return consoleLogCharset;
    }

    public static Integer getGoServerAuthorizationExtensionCallsCacheTimeoutInSeconds() {
        return GO_SERVER_AUTHORIZATION_EXTENSION_CALLS_CACHE_TIMEOUT_IN_SECONDS.getValue();
    }

    public long getMaterialUpdateIdleInterval() {
        return MATERIAL_UPDATE_IDLE_INTERVAL_IN_MILLIS.getValue();
    }

    public String landingPage() {
        return GO_LANDING_PAGE.getValue();
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

    public long getConfigGitGcExpireInMillis() {
        return HOURS.toMillis(GO_CONFIG_REPO_GC_EXPIRE_IN_HOURS.getValue());
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

    public boolean isReAuthenticationEnabled() {
        return REAUTHENTICATION_ENABLED.getValue();
    }

    public long getReAuthenticationTimeInterval() {
        return REAUTHENTICATION_TIME_INTERVAL_IN_MILLIS.getValue();
    }

    public Boolean getAgentStatusEnabled() {
        return AGENT_STATUS_API_ENABLED.getValue();
    }

    public long getPacCloneTimeout() {
        return GO_PAC_CLONE_TIMEOUT_IN_MILLIS.getValue();
    }

    public String getAgentStatusHostname() {
        return AGENT_STATUS_API_BIND_HOST.getValue();
    }

    public int getAgentStatusPort() {
        return AGENT_STATUS_API_BIND_PORT.getValue();
    }

    public static Integer goSpaRefreshInterval() {
        return GO_SPA_REFRESH_INTERVAL_IN_MILLIS.getValue();
    }

    public static Integer goSpaTimeout() {
        return GO_SPA_TIMEOUT_IN_MILLIS.getValue();
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

    public long hstsHeaderMaxAgeInSeconds() {
        return HSTS_HEADER_MAX_AGE_IN_SECONDS.getValue();
    }

    public boolean hstsHeaderIncludeSubdomains() {
        return HSTS_HEADER_INCLUDE_SUBDOMAINS.getValue();
    }

    public boolean hstsHeaderPreload() {
        return HSTS_HEADER_PRELOAD.getValue();
    }

    public Optional<String> wrapperConfigDirPath() {
        return Optional.ofNullable(System.getenv("WRAPPER_CONF_DIR"));
    }

    public long getEphemeralAutoRegisterKeyExpiryInMillis() {
        return EPHEMERAL_AUTO_REGISTER_KEY_EXPIRY_IN_MILLIS.getValue();
    }

    public float getMDUExponentialBackOffMultiplier() {
        return MDU_EXPONENTIAL_BACKOFF_MULTIPLIER.getValue();
    }

    public boolean shouldStartServerInMaintenanceMode() {
        return START_IN_MAINTENANCE_MODE.getValue();
    }

    public boolean shouldInitializeConfigRepositoriesOnStartup() {
        return INITIALIZE_CONFIG_REPOSITORIES_ON_STARTUP.getValue();
    }

    public long getPluginLocationMonitorIntervalInMillis() {
        return SECONDS.toMillis(PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS.getValue());
    }

    public static abstract class GoSystemProperty<T> {
        private final @NotNull String propertyName;
        private final @NotNull T defaultValue;

        GoSystemProperty(@NotNull String propertyName, @NotNull T defaultValue) {
            this.propertyName = propertyName;
            this.defaultValue = defaultValue;
        }

        @NotNull T getValue() {
            return convertValue(System.getProperty(propertyName), defaultValue);
        }

        abstract @NotNull T convertValue(@Nullable String propertyValueFromSystem, @NotNull T defaultValue);

        public @NotNull String propertyName() {
            return propertyName;
        }

        void clearIfNecessary() {}
    }

    private static class GoIntSystemProperty extends GoSystemProperty<Integer> {
        public GoIntSystemProperty(String propertyName, Integer defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected @NotNull Integer convertValue(@Nullable String propertyValueFromSystem, @NotNull Integer defaultValue) {
            try {
                return propertyValueFromSystem == null ? defaultValue : Integer.parseInt(propertyValueFromSystem);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    private static class GoLongSystemProperty extends GoSystemProperty<Long> {
        public GoLongSystemProperty(@NotNull String propertyName, @NotNull Long defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected @NotNull Long convertValue(@Nullable String propertyValueFromSystem, @NotNull Long defaultValue) {
            try {
                return propertyValueFromSystem == null ? defaultValue : Long.parseLong(propertyValueFromSystem);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    private static class GoFloatSystemProperty extends GoSystemProperty<Float> {
        public GoFloatSystemProperty(@NotNull String propertyName, @NotNull Float defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        protected @NotNull Float convertValue(@Nullable String propertyValueFromSystem, @NotNull Float defaultValue) {
            try {
                return propertyValueFromSystem == null ? defaultValue : Float.parseFloat(propertyValueFromSystem);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    private static class GoStringSystemProperty extends GoSystemProperty<String> {
        public GoStringSystemProperty(@NotNull String propertyName, @NotNull String defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        @NotNull String convertValue(@Nullable String propertyValueFromSystem, @NotNull String defaultValue) {
            return propertyValueFromSystem == null ? defaultValue : propertyValueFromSystem;
        }
    }

    private static class CachedProperty<T> extends GoSystemProperty<T> {
        @NotNull private final GoSystemProperty<T> wrappedProperty;
        private final AtomicReference<T> cachedValue = new AtomicReference<>();

        public CachedProperty(@NotNull GoSystemProperty<T> goSystemProperty) {
            super(goSystemProperty.propertyName, goSystemProperty.defaultValue);
            wrappedProperty = goSystemProperty;
        }

        @Override
        @NotNull T convertValue(@Nullable String propertyValueFromSystem, @NotNull T defaultValue) {
            T value = cachedValue.get();
            if (value == null) {
                value = wrappedProperty.convertValue(propertyValueFromSystem, defaultValue);
                cachedValue.set(value);
            }
            return value;
        }

        @Override
        public void clearIfNecessary() {
            cachedValue.set(null);
        }
    }

    private static class GoBooleanSystemProperty extends GoSystemProperty<Boolean> {
        public GoBooleanSystemProperty(@NotNull String propertyName, @NotNull Boolean defaultValue) {
            super(propertyName, defaultValue);
        }

        @Override
        @NotNull Boolean convertValue(@Nullable String propertyValueFromSystem, @NotNull Boolean defaultValue) {
            return propertyValueFromSystem == null ? defaultValue : "Y".equalsIgnoreCase(propertyValueFromSystem) || "true".equalsIgnoreCase(propertyValueFromSystem);
        }
    }
}
