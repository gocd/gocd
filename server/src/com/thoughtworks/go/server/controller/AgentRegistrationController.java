/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.AllConfigErrors;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.tfs.TFSJarDetector;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.RegistrationJSONizer;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Controller
public class AgentRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(AgentRegistrationController.class);
    private final AgentService agentService;
    private final GoConfigService goConfigService;
    private final SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    private final AgentConfigService agentConfigService;
    private TimeProvider timeProvider;
    private volatile String agentChecksum;
    private volatile String agentLauncherChecksum;
    private volatile String tfsSdkChecksum;

    @Autowired
    public AgentRegistrationController(AgentService agentService, GoConfigService goConfigService, SystemEnvironment systemEnvironment, PluginsZip pluginsZip, AgentConfigService agentConfigService, TimeProvider timeProvider) {
        this.agentService = agentService;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.pluginsZip = pluginsZip;
        this.agentConfigService = agentConfigService;
        this.timeProvider = timeProvider;
    }

    @RequestMapping(value = "/admin/latest-agent.status", method = {RequestMethod.HEAD, RequestMethod.GET})
    public void checkAgentStatus(HttpServletResponse response) throws IOException {
        response.setHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, agentChecksum);
        response.setHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, agentLauncherChecksum);
        response.setHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, pluginsZip.md5());
        response.setHeader(SystemEnvironment.AGENT_TFS_SDK_MD5_HEADER, tfsSdkChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/agent", method = RequestMethod.HEAD)
    public void checkAgentVersion(HttpServletResponse response) throws IOException {
        response.setHeader("Content-MD5", agentChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/agent-launcher.jar", method = RequestMethod.HEAD)
    public void checkAgentLauncherVersion(HttpServletResponse response) throws IOException {
        response.setHeader("Content-MD5", agentLauncherChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/tfs-impl.jar", method = RequestMethod.HEAD)
    public void checkTfsImplVersion(HttpServletResponse response) throws IOException {
        response.setHeader("Content-MD5", tfsSdkChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/tfs-impl.jar", method = RequestMethod.GET)
    public void downloadTfsImplJar(HttpServletResponse response) throws IOException {
        checkTfsImplVersion(response);
        sendFile(new TFSImplSrc(), response);
    }

    @RequestMapping(value = "/admin/agent-plugins.zip", method = RequestMethod.HEAD)
    public void checkAgentPluginsZipStatus(HttpServletResponse response) throws IOException {
        response.setHeader("Content-MD5", pluginsZip.md5());
        setOtherHeaders(response);
    }

    @PostConstruct
    public void populateLauncherChecksum() throws IOException {
        if (agentLauncherChecksum == null) {
            agentLauncherChecksum = getChecksumFor(new AgentLauncherSrc());
        }
    }

    @PostConstruct
    public void populateAgentChecksum() throws IOException {
        if (agentChecksum == null) {
            agentChecksum = getChecksumFor(new AgentJarSrc());
        }
    }

    @PostConstruct
    public void populateTFSSDKChecksum() throws IOException {
        if (tfsSdkChecksum == null) {
            tfsSdkChecksum = getChecksumFor(new TFSImplSrc());
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

        sendFile(new AgentJarSrc(), response);
    }

    @RequestMapping(value = "/admin/agent-launcher.jar", method = RequestMethod.GET)
    public void downloadAgentLauncher(HttpServletResponse response) throws IOException {
        checkAgentLauncherVersion(response);

        sendFile(new AgentLauncherSrc(), response);

    }

    @RequestMapping(value = "/admin/agent-plugins.zip", method = RequestMethod.GET)
    public void downloadPluginsZip(HttpServletResponse response) throws IOException {
        checkAgentPluginsZipStatus(response);

        sendFile(new AgentPluginsZipSrc(), response);
    }

    @RequestMapping(value = "/admin/agent", method = RequestMethod.POST)
    public ModelAndView agentRequest(@RequestParam("hostname") String hostname,
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
                                     HttpServletRequest request) throws IOException {
        final String ipAddress = request.getRemoteAddr();
        LOG.debug("Processing registration request from agent [{}/{}]", hostname, ipAddress);
        Registration keyEntry;
        String preferredHostname = hostname;

        try {
            if (goConfigService.serverConfig().shouldAutoRegisterAgentWith(agentAutoRegisterKey)) {
                preferredHostname = getPreferredHostname(agentAutoRegisterHostname, hostname);
                LOG.info("[Agent Auto Registration] Auto registering agent with uuid {} ", uuid);
            } else {
                if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                    throw new RuntimeException(String.format("Elastic agent registration requires an auto-register agent key to be setup on the server. Agent-id: [%s], Plugin-id: [%s]", elasticAgentId, elasticPluginId));
                }
            }

            AgentConfig agentConfig = new AgentConfig(uuid, preferredHostname, ipAddress);

            if (partialElasticAgentAutoregistrationInfo(elasticAgentId, elasticPluginId)) {
                throw new RuntimeException("Elastic agents must submit both elasticAgentId and elasticPluginId");
            }

            if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                agentConfig.setElasticAgentId(elasticAgentId);
                agentConfig.setElasticPluginId(elasticPluginId);
            }

            if (goConfigService.serverConfig().shouldAutoRegisterAgentWith(agentAutoRegisterKey)) {
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
            long usablespace = Long.parseLong(usablespaceAsString);

            AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, registeredAlready, location, usablespace, operatingSystem, supportsBuildCommandProtocol, timeProvider);

            if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, elasticAgentId, elasticPluginId, timeProvider);
            }

            keyEntry = agentService.requestRegistration(agentService.agentUsername(uuid, ipAddress, preferredHostname), agentRuntimeInfo);
        } catch (Exception e) {
            keyEntry = Registration.createNullPrivateKeyEntry();
            LOG.error("Error occured during agent registration process: ", e);
        }

        return render(keyEntry);
    }

    private ModelAndView render(final Registration registration) {
        return new ModelAndView(new View() {
            public String getContentType() {
                return "application/json";
            }

            public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.setContentType(getContentType());
                if (!registration.isValid()) {
                    response.setStatus(HttpServletResponse.SC_ACCEPTED);
                }
                response.getWriter().print(RegistrationJSONizer.toJson(registration));
            }
        });
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

    public interface InputStreamSrc {
        InputStream invoke() throws IOException;
    }

    private class AgentJarSrc implements InputStreamSrc {
        public InputStream invoke() throws IOException {
            return JarDetector.create(systemEnvironment, "agent.jar");
        }
    }

    private class AgentLauncherSrc implements InputStreamSrc {
        public InputStream invoke() throws IOException {
            return JarDetector.create(systemEnvironment, "agent-launcher.jar");
        }
    }

    private class AgentPluginsZipSrc implements InputStreamSrc {
        public InputStream invoke() throws FileNotFoundException {
            return new FileInputStream(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH));
        }
    }

    private class TFSImplSrc implements InputStreamSrc {
        @Override
        public InputStream invoke() throws IOException {
            return TFSJarDetector.create(systemEnvironment).getJarURL().openStream();
        }
    }
}
