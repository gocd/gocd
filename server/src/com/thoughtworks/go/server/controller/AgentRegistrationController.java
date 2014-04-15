/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.GoConfigFileDao;
import com.thoughtworks.go.config.update.ApproveAgentCommand;
import com.thoughtworks.go.config.update.UpdateEnvironmentsCommand;
import com.thoughtworks.go.config.update.UpdateResourceCommand;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.web.JsonView;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.json.JsonMap;
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

import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static com.thoughtworks.go.util.FileDigester.copyAndDigest;
import static com.thoughtworks.go.util.FileDigester.md5DigestOfStream;

@Controller
public class AgentRegistrationController {
    private static final Log LOG = LogFactory.getLog(AgentRegistrationController.class);
    private final AgentService agentService;
    private final GoConfigService goConfigService;
    private final SystemEnvironment systemEnvironment;
    private volatile String agentChecksum;
    private volatile String agentLauncherChecksum;
    private volatile String agentPluginsChecksum;

    @Autowired
    public AgentRegistrationController(AgentService agentService, GoConfigService goConfigService, SystemEnvironment systemEnvironment) {
        this.agentService = agentService;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
    }

    @RequestMapping(value = "/latest-agent.status", method = RequestMethod.HEAD)
    public void checkAgentStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        populateAgentChecksum();
        response.setHeader(SystemEnvironment.AGENT_CONTENT_MD5_HEADER, agentChecksum);
        populateLauncherChecksum();
        response.setHeader(SystemEnvironment.AGENT_LAUNCHER_CONTENT_MD5_HEADER, agentLauncherChecksum);
        populateAgentPluginsChecksum();
        response.setHeader(SystemEnvironment.AGENT_PLUGINS_ZIP_MD5_HEADER, agentPluginsChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/latest-agent.status", method = RequestMethod.GET)
    public void latestAgentStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {

        checkAgentStatus(request, response);
    }

    @RequestMapping(value = "/agent", method = RequestMethod.HEAD)
    public void checkAgentVersion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        populateAgentChecksum();
        response.setHeader("Content-MD5", agentChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/agent-launcher.jar", method = RequestMethod.HEAD)
    public void checkAgentLauncherVersion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        populateLauncherChecksum();
        response.setHeader("Content-MD5", agentLauncherChecksum);
        setOtherHeaders(response);
    }

    @RequestMapping(value = "/agent-plugins.zip", method = RequestMethod.HEAD)
    public void checkAgentPluginsZipStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        populateAgentPluginsChecksum();
        response.setHeader("Content-MD5", agentPluginsChecksum);
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

    @RequestMapping(value = "/agent", method = RequestMethod.GET)
    public ModelAndView downloadAgent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return getDownload(new AgentJarSrc());
    }

    @RequestMapping(value = "/agent-launcher.jar", method = RequestMethod.GET)
    public ModelAndView downloadAgentLauncher(HttpServletRequest request,
                                              HttpServletResponse response) throws IOException {
        return getDownload(new AgentLauncherSrc());
    }

    @RequestMapping(value = "/agent-plugins.zip", method = RequestMethod.GET)
    public ModelAndView downloadPluginsZip(HttpServletRequest request,
                                           HttpServletResponse response) throws IOException {
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

    @RequestMapping(value = "/agent", method = RequestMethod.POST)
    public ModelAndView agentRequest(@RequestParam("hostname") String hostname,
                                     @RequestParam("uuid") String uuid,
                                     @RequestParam("location") String location,
                                     @RequestParam("usablespace") String usablespace,
                                     @RequestParam("operating_system") String operatingSystem,
                                     @RequestParam("agentAutoRegisterKey") String agentAutoRegisterKey,
                                     @RequestParam("agentAutoRegisterResources") String agentAutoRegisterResources,
                                     @RequestParam("agentAutoRegisterEnvironments") String agentAutoRegisterEnvironments,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        final String ipAddress = request.getRemoteAddr();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Processing registration request from agent [%s/%s]", hostname, ipAddress));
        }
        Registration keyEntry;
        try {
            if (goConfigService.serverConfig().shouldAutoRegisterAgentWith(agentAutoRegisterKey)) {
                LOG.info(String.format("[Agent Auto Registration] Auto registering agent with uuid %s ", uuid));
                GoConfigFileDao.CompositeConfigCommand compositeConfigCommand = new GoConfigFileDao.CompositeConfigCommand(
                        new ApproveAgentCommand(uuid, ipAddress, hostname),
                        new UpdateResourceCommand(uuid, agentAutoRegisterResources),
                        new UpdateEnvironmentsCommand(uuid, agentAutoRegisterEnvironments)
                );
                goConfigService.updateConfig(compositeConfigCommand);
            }
            keyEntry = agentService.requestRegistration(
                    AgentRuntimeInfo.fromServer(new AgentConfig(uuid, hostname, ipAddress), goConfigService.hasAgent(uuid), location,
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

    @RequestMapping(value = "/**/registerAgent.json", method = RequestMethod.POST)
    public ModelAndView registerAgent(HttpServletResponse response,
                                      @RequestParam("uuid") String uuid) {
        try {
            agentService.approve(uuid);
            JsonMap result = JsonView.getSimpleAjaxResult("result", "success");
            return JsonAction.jsonCreated(result).respond(response);
        } catch (Exception ex) {
            String message = ex.getMessage();
            LOG.error(String.format("Error approving agent [%s]", uuid), ex);
            JsonMap result = JsonView.getSimpleAjaxResult("result", "failed");
            result.put(ERROR_FOR_JSON, message);
            return JsonAction.jsonNotAcceptable(result).respond(response);
        }
    }

    @RequestMapping(value = "/**/denyAgent.json", method = RequestMethod.POST)
    public ModelAndView denyAgent(HttpServletResponse response, @RequestParam("uuid") String uuid) {
        try {
            agentService.disableAgents(UserHelper.getUserName(), new HttpOperationResult(), Arrays.asList(uuid));
            JsonMap result = JsonView.getSimpleAjaxResult("result", "success");
            return JsonAction.jsonCreated(result).respond(response);
        } catch (Exception ex) {
            String message = ex.getMessage();
            JsonMap result = JsonView.getSimpleAjaxResult("result", "failed");
            result.put(ERROR_FOR_JSON, message);
            return JsonAction.jsonNotAcceptable(result).respond(response);
        }
    }

    private void populateAgentPluginsChecksum() throws IOException {
        if (agentPluginsChecksum == null) {
            agentPluginsChecksum = getChecksumFor(new AgentPluginsZipSrc());
        }
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
