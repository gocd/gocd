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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.AGENT_EXTRA_PROPERTIES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.*;

@Controller
public class AgentRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentRegistrationController.class);
    static final int MAX_HEADER_LENGTH = 4096;
    private final AgentService agentService;
    private final GoConfigService goConfigService;
    private final SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    private volatile String agentChecksum;
    private volatile String agentLauncherChecksum;
    private volatile String tfsSdkChecksum;
    private volatile String agentExtraProperties;
    private Mac mac;

    private final InputStreamSrc agentJarSrc;
    private final InputStreamSrc agentLauncherSrc;
    private final InputStreamSrc tfsImplSrc;
    private final EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService;
    private final InputStreamSrc agentPluginsZipSrc;
    private static final Object HMAC_GENERATION_MUTEX = new Object();

    @Autowired
    public AgentRegistrationController(AgentService agentService, GoConfigService goConfigService, SystemEnvironment systemEnvironment,
                                       PluginsZip pluginsZip, EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService) throws IOException {
        this.agentService = agentService;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.pluginsZip = pluginsZip;
        this.agentJarSrc = JarDetector.create(systemEnvironment, "agent.jar");
        this.agentLauncherSrc = JarDetector.create(systemEnvironment, "agent-launcher.jar");
        this.agentPluginsZipSrc = JarDetector.createFromFile(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH));
        this.tfsImplSrc = JarDetector.create(systemEnvironment, "tfs-impl-14.jar");
        this.ephemeralAutoRegisterKeyService = ephemeralAutoRegisterKeyService;
    }

    private Mac hmac() {
        if (mac == null) {
            try {
                mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKey = new SecretKeySpec(goConfigService.serverConfig().getTokenGenerationKey().getBytes(), "HmacSHA256");
                mac.init(secretKey);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
        return mac;
    }

    @RequestMapping(value = "/admin/latest-agent.status", method = {RequestMethod.HEAD, RequestMethod.GET})
    public void checkAgentStatus(HttpServletResponse response) {
        LOG.debug("Processing '/admin/latest-agent.status' request with values [{}:{}], [{}:{}], [{}:{}], [{}:{}]",
                SystemEnvironment.AGENT_CONTENT_MD5_HEADER, agentChecksum,
                SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, agentLauncherChecksum,
                SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, pluginsZip.md5(),
                SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER, tfsSdkChecksum);

        response.setHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, agentChecksum);
        response.setHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, agentLauncherChecksum);
        response.setHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, pluginsZip.md5());
        response.setHeader(SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER, tfsSdkChecksum);
        response.setHeader(SystemEnvironment.AGENT_EXTRA_PROPERTIES_HEADER, getAgentExtraProperties());
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/agent", method = RequestMethod.HEAD)
    public void checkAgentVersion(HttpServletResponse response) {
        response.setHeader("Content-MD5", agentChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/agent-launcher.jar", method = RequestMethod.HEAD)
    public void checkAgentLauncherVersion(HttpServletResponse response) {
        response.setHeader("Content-MD5", agentLauncherChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/tfs-impl.jar", method = RequestMethod.HEAD)
    public void checkTfsImplVersion(HttpServletResponse response) {
        response.setHeader("Content-MD5", tfsSdkChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/tfs-impl.jar", method = RequestMethod.GET)
    public void downloadTfsImplJar(HttpServletResponse response) throws IOException {
        checkTfsImplVersion(response);
        sendFile(tfsImplSrc, response);
    }

    @RequestMapping(value = "/admin/agent-plugins.zip", method = RequestMethod.HEAD)
    public void checkAgentPluginsZipStatus(HttpServletResponse response) {
        response.setHeader("Content-MD5", pluginsZip.md5());
        setOtherHeaders(response);
    }

    @PostConstruct
    public void populateLauncherChecksum() throws IOException {
        if (agentLauncherChecksum == null) {
            agentLauncherChecksum = getChecksumFor(agentLauncherSrc);
        }
    }

    @PostConstruct
    public void populateAgentChecksum() throws IOException {
        if (agentChecksum == null) {
            agentChecksum = getChecksumFor(agentJarSrc);
        }
    }

    @PostConstruct
    public void populateTFSSDKChecksum() throws IOException {
        if (tfsSdkChecksum == null) {
            tfsSdkChecksum = getChecksumFor(tfsImplSrc);
        }
    }

    private String getChecksumFor(final InputStreamSrc src) throws IOException {
        String checksum;
        try (InputStream inputStream = src.invoke()) {
            checksum = DigestUtils.md5Hex(inputStream);
        }
        assert (checksum != null);
        return checksum;
    }

    private void setOtherHeaders(HttpServletResponse response) {
        response.setHeader(SystemEnvironment.AGENT_EXTRA_PROPERTIES_HEADER, getAgentExtraProperties());
    }

    @RequestMapping(value = "/admin/agent", method = RequestMethod.GET)
    public void downloadAgent(HttpServletResponse response) throws IOException {
        checkAgentVersion(response);

        sendFile(agentJarSrc, response);
    }

    @RequestMapping(value = "/admin/agent-launcher.jar", method = RequestMethod.GET)
    public void downloadAgentLauncher(HttpServletResponse response) throws IOException {
        checkAgentLauncherVersion(response);

        sendFile(agentLauncherSrc, response);

    }

    @RequestMapping(value = "/admin/agent-plugins.zip", method = RequestMethod.GET)
    public void downloadPluginsZip(HttpServletResponse response) throws IOException {
        checkAgentPluginsZipStatus(response);

        sendFile(agentPluginsZipSrc, response);
    }

    @RequestMapping(value = "/admin/agent", method = RequestMethod.POST)
    public ResponseEntity agentRequest(@RequestParam("hostname") String hostname,
                                       @RequestParam("uuid") String uuid,
                                       @RequestParam("location") String location,
                                       @RequestParam("usablespace") String usableSpaceStr,
                                       @RequestParam("operatingSystem") String os,
                                       @RequestParam("agentAutoRegisterKey") String agentAutoRegisterKey,
                                       @RequestParam("agentAutoRegisterResources") String agentAutoRegisterResources,
                                       @RequestParam("agentAutoRegisterEnvironments") String agentAutoRegisterEnvs,
                                       @RequestParam("agentAutoRegisterHostname") String agentAutoRegisterHostname,
                                       @RequestParam("elasticAgentId") String elasticAgentId,
                                       @RequestParam("elasticPluginId") String elasticPluginId,
                                       @RequestParam("token") String token, HttpServletRequest request) {
        final String ipAddress = request.getRemoteAddr();
        LOG.debug("Processing registration request from agent [{}/{}]", hostname, ipAddress);
        boolean keyEntry;
        String preferredHostname = hostname;
        boolean isElasticAgent = elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId);

        try {
            if (!encodeBase64String(hmac().doFinal(uuid.getBytes())).equals(token)) {
                String message = "Not a valid token.";
                LOG.error("Rejecting request for registration. Error: HttpCode=[{}] Message=[{}] UUID=[{}] Hostname=[{}]" +
                        "ElasticAgentID=[{}] PluginID=[{}]", FORBIDDEN, message, uuid, hostname, elasticAgentId, elasticPluginId);
                return new ResponseEntity<>(message, FORBIDDEN);
            }

            boolean shouldAutoRegister = shouldAutoRegister(agentAutoRegisterKey, isElasticAgent);

            if (shouldAutoRegister) {
                preferredHostname = getPreferredHostname(agentAutoRegisterHostname, hostname);
            } else {
                if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                    String message = String.format("Elastic agent registration requires an auto-register agent key to be" +
                            " setup on the server. The agentAutoRegisterKey: [%s] is either not provided or expired. Agent-id: [%s], Plugin-id: [%s]"
                            , agentAutoRegisterKey, elasticAgentId, elasticPluginId);
                    LOG.error("Rejecting request for registration. Error: HttpCode=[{}] Message=[{}] UUID=[{}] Hostname=[{}]" +
                            "ElasticAgentID=[{}] PluginID=[{}]", UNPROCESSABLE_ENTITY, message, uuid, hostname, elasticAgentId, elasticPluginId);
                    return new ResponseEntity<>(message, UNPROCESSABLE_ENTITY);
                }
            }

            Agent agent = createAgentFromRequest(uuid, preferredHostname, ipAddress, elasticAgentId, elasticPluginId);
            agent.validate();
            if (agent.hasErrors()) {
                List<ConfigErrors> errors = agent.errorsAsList();
                throw new GoConfigInvalidException(null, new AllConfigErrors(errors));
            }

            if (partialElasticAgentAutoregistrationInfo(elasticAgentId, elasticPluginId)) {
                String message = "Elastic agents must submit both elasticAgentId and elasticPluginId.";
                LOG.error("Rejecting request for registration. Error: HttpCode=[{}] Message=[{}] UUID=[{}] Hostname=[{}]" +
                        "ElasticAgentID=[{}] PluginID=[{}]", UNPROCESSABLE_ENTITY, message, uuid, hostname, elasticAgentId, elasticPluginId);
                return new ResponseEntity<>(message, UNPROCESSABLE_ENTITY);
            }

            if (elasticAgentIdAlreadyRegistered(elasticAgentId, elasticPluginId)) {
                String message = "Duplicate Elastic agent Id used to register elastic agent.";
                LOG.error("Rejecting request for registration. Error: HttpCode=[{}] Message=[{}] UUID=[{}] Hostname=[{}]" +
                        "ElasticAgentID=[{}] PluginID=[{}]", UNPROCESSABLE_ENTITY, message, uuid, hostname, elasticAgentId, elasticPluginId);
                return new ResponseEntity<>(message, UNPROCESSABLE_ENTITY);
            }

            if (shouldAutoRegister && !agentService.isRegistered(uuid)) {
                LOG.info("[Agent Auto Registration] Auto registering agent with uuid {} ", uuid);
                agent.setEnvironments(agentAutoRegisterEnvs);
                agent.setResources(agentAutoRegisterResources);
                agentService.register(agent);
                if (agent.hasErrors()) {
                    throw new GoConfigInvalidException(null, new AllConfigErrors(agent.errorsAsList()).asString());
                }
            }

            boolean registeredAlready = agentService.isRegistered(uuid);
            long usableSpace = Long.parseLong(usableSpaceStr);

            AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agent, registeredAlready, location, usableSpace, os);

            if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, elasticAgentId, elasticPluginId);
            }

            keyEntry = agentService.requestRegistration(agentRuntimeInfo);

            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>("", httpHeaders, keyEntry ? OK : ACCEPTED);
        } catch (Exception e) {
            LOG.error("Error occurred during agent registration process. Error: HttpCode=[{}] Message=[{}] UUID=[{}] " +
                            "Hostname=[{}] ElasticAgentID=[{}] PluginID=[{}]", UNPROCESSABLE_ENTITY, getErrorMessage(e), uuid,
                    hostname, elasticAgentId, elasticPluginId, e);
            return new ResponseEntity<>(String.format("Error occurred during agent registration process: %s", getErrorMessage(e)), UNPROCESSABLE_ENTITY);
        }
    }

    private boolean shouldAutoRegister(String agentAutoRegisterKey, boolean isElasticAgent) {
        if (isElasticAgent) {
            return ephemeralAutoRegisterKeyService.validateAndRevoke(agentAutoRegisterKey);
        }

        return goConfigService.serverConfig().shouldAutoRegisterAgentWith(agentAutoRegisterKey);
    }

    private boolean elasticAgentIdAlreadyRegistered(String elasticAgentId, String elasticPluginId) {
        return agentService.findElasticAgent(elasticAgentId, elasticPluginId) != null;
    }

    private String getErrorMessage(Exception e) {
        if (e instanceof GoConfigInvalidException) {
            GoConfigInvalidException exception = (GoConfigInvalidException) e;
            return StringUtils.join(exception.getAllErrors(), ", ");
        }
        return e.getMessage();
    }

    @RequestMapping(value = "/admin/agent/token", method = RequestMethod.GET)
    public ResponseEntity getToken(@RequestParam("uuid") String uuid) {
        if (StringUtils.isBlank(uuid)) {
            String message = "UUID cannot be blank.";
            LOG.error("Rejecting request for token. Error: HttpCode=[{}] Message=[{}] UUID=[{}]", CONFLICT, message, uuid);
            return new ResponseEntity<>(message, CONFLICT);
        }
        final AgentInstance agentInstance = agentService.findAgent(uuid);
        if ((!agentInstance.isNullAgent() && agentInstance.isPending()) || agentService.isRegistered(uuid)) {
            String message = "A token has already been issued for this agent.";
            LOG.error("Rejecting request for token. Error: HttpCode=[{}] Message=[{}] Pending=[{}] UUID=[{}]",
                    CONFLICT, message, agentInstance.isPending(), uuid);
            return new ResponseEntity<>(message, CONFLICT);
        }
        String token;
        synchronized (HMAC_GENERATION_MUTEX) {
            token = encodeBase64String(hmac().doFinal(uuid.getBytes()));
        }

        return new ResponseEntity<>(token, OK);
    }

    private Agent createAgentFromRequest(String uuid, String hostname, String ip, String elasticAgentId, String elasticPluginId) {
        Agent agent = new Agent(uuid, hostname, ip);

        if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
            agent.setElasticAgentId(elasticAgentId);
            agent.setElasticPluginId(elasticPluginId);
        }
        return agent;
    }

    private String getAgentExtraProperties() {
        if (agentExtraProperties == null) {
            String base64OfSystemProperty = encodeBase64String(systemEnvironment.get(AGENT_EXTRA_PROPERTIES).getBytes(UTF_8));
            String base64OfEmptyString = encodeBase64String("".getBytes(UTF_8));

            this.agentExtraProperties = base64OfSystemProperty.length() >= MAX_HEADER_LENGTH ? base64OfEmptyString : base64OfSystemProperty;
        }
        return agentExtraProperties;
    }

    private void sendFile(InputStreamSrc input, HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        try (InputStream in = input.invoke()) {
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    private boolean partialElasticAgentAutoregistrationInfo(String elasticAgentId, String elasticPluginId) {
        return (isBlank(elasticAgentId) && isNotBlank(elasticPluginId)) || (isNotBlank(elasticAgentId) && isBlank(elasticPluginId));
    }

    private boolean elasticAgentAutoregistrationInfoPresent(String elasticAgentId, String elasticPluginId) {
        return isNotBlank(elasticAgentId) && isNotBlank(elasticPluginId);
    }

    private String getPreferredHostname(String agentAutoRegisterHostname, String hostname) {
        return isNotBlank(agentAutoRegisterHostname) ? agentAutoRegisterHostname : hostname;
    }

}
