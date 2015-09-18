/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.update.ApproveAgentCommand;
import com.thoughtworks.go.config.update.UpdateEnvironmentsCommand;
import com.thoughtworks.go.config.update.UpdateResourceCommand;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

import static com.thoughtworks.go.util.FileDigester.copyAndDigest;
import static com.thoughtworks.go.util.FileDigester.md5DigestOfStream;

@Controller
public class AgentRegistrationController {
    private static final Log LOG = LogFactory.getLog(AgentRegistrationController.class);
    private final AgentService agentService;
    private final GoConfigService goConfigService;
    private final SystemEnvironment systemEnvironment;
    private PluginsZip pluginsZip;
    private volatile String agentChecksum;
    private volatile String agentLauncherChecksum;

    @Autowired
    public AgentRegistrationController(AgentService agentService, GoConfigService goConfigService, SystemEnvironment systemEnvironment, PluginsZip pluginsZip) {
        this.agentService = agentService;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.pluginsZip = pluginsZip;
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
                                     @RequestParam("usablespace") String usablespace,
                                     @RequestParam("operating_system") String operatingSystem,
                                     @RequestParam("agentAutoRegisterKey") String agentAutoRegisterKey,
                                     @RequestParam("agentAutoRegisterResources") String agentAutoRegisterResources,
                                     @RequestParam("agentAutoRegisterEnvironments") String agentAutoRegisterEnvironments,
                                     @RequestParam("agentAutoRegisterHostname") String agentAutoRegisterHostname,
                                     HttpServletRequest request) throws IOException {
        final String ipAddress = request.getRemoteAddr();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Processing registration request from agent [%s/%s]", hostname, ipAddress));
        }
        Registration keyEntry;
        String preferredHostname = hostname;
        try {
            if (goConfigService.serverConfig().shouldAutoRegisterAgentWith(agentAutoRegisterKey)) {
                preferredHostname = getPreferredHostname(agentAutoRegisterHostname, hostname);
                LOG.info(String.format("[Agent Auto Registration] Auto registering agent with uuid %s ", uuid));
                GoConfigDao.CompositeConfigCommand compositeConfigCommand = new GoConfigDao.CompositeConfigCommand(
                        new ApproveAgentCommand(uuid, ipAddress, preferredHostname),
                        new UpdateResourceCommand(uuid, agentAutoRegisterResources),
                        new UpdateEnvironmentsCommand(uuid, agentAutoRegisterEnvironments)
                );
                goConfigService.updateConfig(compositeConfigCommand);
            }
            keyEntry = agentService.requestRegistration(
                    AgentRuntimeInfo.fromServer(new AgentConfig(uuid, preferredHostname, ipAddress), goConfigService.hasAgent(uuid), location,
                            Long.parseLong(usablespace), operatingSystem));
        } catch (Exception e) {
            keyEntry = Registration.createNullPrivateKeyEntry();
            LOG.error("Error occured during agent registration process: ", e);
        }

        final Registration anotherCopy = keyEntry;
        return new ModelAndView(new View() {
            public String getContentType() {
                return "application/x-java-serialized-object";
            }

            public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws IOException {
                ServletOutputStream servletOutputStream = null;
                ObjectOutputStream objectOutputStream = null;
                try {
                    servletOutputStream = response.getOutputStream();
                    objectOutputStream = new ObjectOutputStream(servletOutputStream);
                    objectOutputStream.writeObject(anotherCopy);
                } finally {
                    IOUtils.closeQuietly(servletOutputStream);
                    IOUtils.closeQuietly(objectOutputStream);
                }
            }
        });
    }

    private String getPreferredHostname(String agentAutoRegisterHostname, String hostname) {
        return !StringUtil.isBlank(agentAutoRegisterHostname) ? agentAutoRegisterHostname : hostname;
    }

    public static interface InputStreamSrc {
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
