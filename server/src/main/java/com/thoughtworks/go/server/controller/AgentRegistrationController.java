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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.ErrorCollector;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.UpdateEnvironmentsCommand;
import com.thoughtworks.go.config.update.UpdateResourceCommand;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.RegistrationJSONizer;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.binary.Base64;
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.*;

@Controller
public class AgentRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentRegistrationController.class);
    private final AgentService agentService;
    private final GoConfigService goConfigService;
    private final SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    private final AgentConfigService agentConfigService;
    private volatile String agentChecksum;
    private volatile String agentLauncherChecksum;
    private volatile String tfsSdkChecksum;
    private Mac mac;

    private final InputStreamSrc agentJarSrc;
    private final InputStreamSrc agentLauncherSrc;
    private final InputStreamSrc tfsImplSrc;
    private final InputStreamSrc agentPluginsZipSrc;

    @Autowired
    public AgentRegistrationController(AgentService agentService, GoConfigService goConfigService, SystemEnvironment systemEnvironment, PluginsZip pluginsZip, AgentConfigService agentConfigService) throws IOException {
        this.agentService = agentService;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.pluginsZip = pluginsZip;
        this.agentConfigService = agentConfigService;
        this.agentJarSrc = JarDetector.create(systemEnvironment, "agent.jar");
        this.agentLauncherSrc = JarDetector.create(systemEnvironment, "agent-launcher.jar");
        this.agentPluginsZipSrc = JarDetector.createFromFile(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH));
        this.tfsImplSrc = JarDetector.create(systemEnvironment, "tfs-impl-14.jar");
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
        response.setHeader("Cruise-Server-Ssl-Port", Integer.toString(systemEnvironment.getSslServerPort()));
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
                                       @RequestParam("usablespace") String usablespaceAsString,
                                       @RequestParam("operatingSystem") String operatingSystem,
                                       @RequestParam("agentAutoRegisterKey") String agentAutoRegisterKey,
                                       @RequestParam("agentAutoRegisterResources") String agentAutoRegisterResources,
                                       @RequestParam("agentAutoRegisterEnvironments") String agentAutoRegisterEnvironments,
                                       @RequestParam("agentAutoRegisterHostname") String agentAutoRegisterHostname,
                                       @RequestParam("elasticAgentId") String elasticAgentId,
                                       @RequestParam("elasticPluginId") String elasticPluginId,
                                       @RequestParam(value = "supportsBuildCommandProtocol", required = false, defaultValue = "false") boolean supportsBuildCommandProtocol,
                                       @RequestParam("token") String token, HttpServletRequest request) {
        final String ipAddress = request.getRemoteAddr();
        LOG.debug("Processing registration request from agent [{}/{}]", hostname, ipAddress);
        Registration keyEntry;
        String preferredHostname = hostname;

        try {
            if (!Base64.encodeBase64String(hmac().doFinal(uuid.getBytes())).equals(token)) {
                String message = "Not a valid token.";
                LOG.error("Rejecting request for registration. Error: HttpCode=[{}] Message=[{}] UUID=[{}] Hostname=[{}]" +
                        "ElasticAgentID=[{}] PluginID=[{}]", FORBIDDEN, message, uuid, hostname, elasticAgentId, elasticPluginId);
                return new ResponseEntity<>(message, FORBIDDEN);
            }

            if (goConfigService.serverConfig().shouldAutoRegisterAgentWith(agentAutoRegisterKey)) {
                preferredHostname = getPreferredHostname(agentAutoRegisterHostname, hostname);
            } else {
                if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                    String message = String.format("Elastic agent registration requires an auto-register agent key to be" +
                            " setup on the server. Agent-id: [%s], Plugin-id: [%s]", elasticAgentId, elasticPluginId);
                    LOG.error("Rejecting request for registration. Error: HttpCode=[{}] Message=[{}] UUID=[{}] Hostname=[{}]" +
                            "ElasticAgentID=[{}] PluginID=[{}]", UNPROCESSABLE_ENTITY, message, uuid, hostname, elasticAgentId, elasticPluginId);
                    return new ResponseEntity<>(message, UNPROCESSABLE_ENTITY);
                }
            }

            AgentConfig agentConfig = new AgentConfig(uuid, preferredHostname, ipAddress);

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

            if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                agentConfig.setElasticAgentId(elasticAgentId);
                agentConfig.setElasticPluginId(elasticPluginId);
            }

            if (goConfigService.serverConfig().shouldAutoRegisterAgentWith(agentAutoRegisterKey) && !goConfigService.hasAgent(uuid)) {
                LOG.info("[Agent Auto Registration] Auto registering agent with uuid {} ", uuid);
                GoConfigDao.CompositeConfigCommand compositeConfigCommand = new GoConfigDao.CompositeConfigCommand(
                        new AgentConfigService.AddAgentCommand(agentConfig),
                        new UpdateResourceCommand(uuid, agentAutoRegisterResources),
                        new UpdateEnvironmentsCommand(uuid, agentAutoRegisterEnvironments)
                );
                HttpOperationResult result = new HttpOperationResult();
                agentConfig = agentConfigService.updateAgent(compositeConfigCommand, uuid, result, agentService.agentUsername(uuid, ipAddress, preferredHostname));
                if (!result.isSuccess()) {
                    List<ConfigErrors> errors = ErrorCollector.getAllErrors(agentConfig);

                    ConfigErrors e = new ConfigErrors();
                    e.add("resultMessage", result.detailedMessage());

                    errors.add(e);
                    throw new GoConfigInvalidException(null, new AllConfigErrors(errors).asString());
                }
            }

            boolean registeredAlready = goConfigService.hasAgent(uuid);
            long usableSpace = Long.parseLong(usablespaceAsString);

            AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, registeredAlready, location, usableSpace, operatingSystem, supportsBuildCommandProtocol);

            if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, elasticAgentId, elasticPluginId);
            }

            keyEntry = agentService.requestRegistration(agentService.agentUsername(uuid, ipAddress, preferredHostname), agentRuntimeInfo);

            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(RegistrationJSONizer.toJson(keyEntry), httpHeaders, keyEntry.isValid() ? OK : ACCEPTED);
        } catch (Exception e) {
            LOG.error("Error occurred during agent registration process. Error: HttpCode=[{}] Message=[{}] UUID=[{}] " +
                            "Hostname=[{}] ElasticAgentID=[{}] PluginID=[{}]", UNPROCESSABLE_ENTITY, getErrorMessage(e), uuid,
                    hostname, elasticAgentId, elasticPluginId, e);
            return new ResponseEntity<>(String.format("Error occurred during agent registration process: %s", getErrorMessage(e)), UNPROCESSABLE_ENTITY);
        }
    }

    private boolean elasticAgentIdAlreadyRegistered(String elasticAgentId, String elasticPluginId) {
        return agentService.findElasticAgent(elasticAgentId, elasticPluginId) != null;
    }

    private String getErrorMessage(Exception e) {
        if (e instanceof GoConfigInvalidException) {
            return ((GoConfigInvalidException) e).getAllErrorMessages();
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
        if ((!agentInstance.isNullAgent() && agentInstance.isPending()) || goConfigService.hasAgent(uuid)) {
            String message = "A token has already been issued for this agent.";
            LOG.error("Rejecting request for token. Error: HttpCode=[{}] Message=[{}] Pending=[{}] UUID=[{}]",
                    CONFLICT, message, agentInstance.isPending(), uuid);
            return new ResponseEntity<>(message, CONFLICT);
        }
        return new ResponseEntity<>(Base64.encodeBase64String(hmac().doFinal(uuid.getBytes())), OK);
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
