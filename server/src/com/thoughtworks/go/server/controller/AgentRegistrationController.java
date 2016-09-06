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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.UpdateEnvironmentsCommand;
import com.thoughtworks.go.config.update.UpdateResourceCommand;
import com.thoughtworks.go.domain.AllConfigErrors;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.RegistrationJSONizer;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.FileDigester.copyAndDigest;
import static com.thoughtworks.go.util.FileDigester.md5DigestOfStream;
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
    private volatile String agentChecksum;
    private volatile String agentLauncherChecksum;

    @Autowired
    public AgentRegistrationController(AgentService agentService, GoConfigService goConfigService, SystemEnvironment systemEnvironment, PluginsZip pluginsZip, AgentConfigService agentConfigService) {
        this.agentService = agentService;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.pluginsZip = pluginsZip;
        this.agentConfigService = agentConfigService;
    }

    @RequestMapping(value = "/admin/latest-agent.status", method = RequestMethod.HEAD)
    public void checkAgentStatus(HttpServletResponse response) throws IOException {
        populateAgentChecksum();
        response.setHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, agentChecksum);
        populateLauncherChecksum();
        response.setHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, agentLauncherChecksum);
        response.setHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, pluginsZip.md5());
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/latest-agent.status", method = RequestMethod.GET)
    public void latestAgentStatus(HttpServletResponse response) throws IOException {
        checkAgentStatus(response);
    }

    @RequestMapping(value = "/admin/agent", method = RequestMethod.HEAD)
    public void checkAgentVersion(HttpServletResponse response) throws IOException {
        populateAgentChecksum();
        response.setHeader("Content-MD5", agentChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/agent-launcher.jar", method = RequestMethod.HEAD)
    public void checkAgentLauncherVersion(HttpServletResponse response) throws IOException {
        populateLauncherChecksum();
        response.setHeader("Content-MD5", agentLauncherChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/admin/agent-plugins.zip", method = RequestMethod.HEAD)
    public void checkAgentPluginsZipStatus(HttpServletResponse response) throws IOException {
        response.setHeader("Content-MD5", pluginsZip.md5());
        setOtherHeaders(response);
    }

    private void populateLauncherChecksum() throws IOException {
        if (agentLauncherChecksum == null) {
            agentLauncherChecksum = getChecksumFor(new AgentLauncherSrc());
        }
    }

    private void populateAgentChecksum() throws IOException {
        if (agentChecksum == null) {
            agentChecksum = getChecksumFor(new AgentJarSrc());
        }
    }

    private String getChecksumFor(final InputStreamSrc src) throws IOException {
        InputStream inputStream = null;
        String checksum = null;
        try {
            inputStream = src.invoke();
            checksum = md5DigestOfStream(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        assert (checksum != null);
        return checksum;
    }

    private void setOtherHeaders(HttpServletResponse response) {
        response.setHeader("Cruise-Server-Ssl-Port", Integer.toString(systemEnvironment.getSslServerPort()));
    }

    @RequestMapping(value = "/admin/agent", method = RequestMethod.GET)
    public ModelAndView downloadAgent() throws IOException {
        return getDownload(new AgentJarSrc());
    }

    @RequestMapping(value = "/admin/agent-launcher.jar", method = RequestMethod.GET)
    public ModelAndView downloadAgentLauncher() throws IOException {
        return getDownload(new AgentLauncherSrc());
    }

    @RequestMapping(value = "/admin/agent-plugins.zip", method = RequestMethod.GET)
    public ModelAndView downloadPluginsZip() throws IOException {
        return getDownload(new AgentPluginsZipSrc());
    }

    private ModelAndView getDownload(final InputStreamSrc inStreamSrc) throws FileNotFoundException {
        return new ModelAndView(new View() {
            public String getContentType() {
                return "application/octet-stream";
            }

            public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws IOException {
                InputStream rawIS = null;
                BufferedInputStream is = null;
                BufferedOutputStream os = null;
                try {
                    rawIS = inStreamSrc.invoke();
                    is = new BufferedInputStream(rawIS);
                    os = new BufferedOutputStream(response.getOutputStream());

                    String md5 = copyAndDigest(is, os);
                    response.setHeader("Content-MD5", md5);
                    setOtherHeaders(response);
                    os.flush();
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                    IOUtils.closeQuietly(rawIS);
                }
            }
        });
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing registration request from agent [{}/{}]", hostname, ipAddress);
        }
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
                LOG.info(String.format("[Agent Auto Registration] Auto registering agent with uuid %s ", uuid));
                GoConfigDao.CompositeConfigCommand compositeConfigCommand = new GoConfigDao.CompositeConfigCommand(
                        new AgentConfigService.AddAgentCommand(agentConfig),
                        new UpdateResourceCommand(uuid, agentAutoRegisterResources),
                        new UpdateEnvironmentsCommand(uuid, agentAutoRegisterEnvironments)
                );
                HttpOperationResult result = new HttpOperationResult();
                agentConfig = agentConfigService.updateAgent(compositeConfigCommand, uuid, result, agentService.agentUsername(uuid, ipAddress, preferredHostname));
                if (!result.isSuccess()) {
                    List<ConfigErrors> errors = com.thoughtworks.go.config.ErrorCollector.getAllErrors(agentConfig);
                    throw new GoConfigInvalidException(null, new AllConfigErrors(errors).asString());
                }
            }

            boolean registeredAlready = goConfigService.hasAgent(uuid);
            long usablespace = Long.parseLong(usablespaceAsString);

            AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, registeredAlready, location, usablespace, operatingSystem, supportsBuildCommandProtocol);

            if (elasticAgentAutoregistrationInfoPresent(elasticAgentId, elasticPluginId)) {
                agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, elasticAgentId, elasticPluginId);
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
                if (!registration.isValid()) {
                    response.setStatus(HttpServletResponse.SC_ACCEPTED);
                }
                response.getWriter().print(RegistrationJSONizer.toJson(registration));
            }
        });
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
        InputStream invoke() throws FileNotFoundException;
    }

    private class AgentJarSrc implements InputStreamSrc {
        public InputStream invoke() throws FileNotFoundException {
            return agentService.agentJarInputStream();
        }
    }

    private class AgentLauncherSrc implements InputStreamSrc {
        public InputStream invoke() throws FileNotFoundException {
            return agentService.agentLauncherJarInputStream();
        }
    }

    private class AgentPluginsZipSrc implements InputStreamSrc {
        public InputStream invoke() throws FileNotFoundException {
            return new FileInputStream(systemEnvironment.get(SystemEnvironment.ALL_PLUGINS_ZIP_PATH));
        }
    }


}
