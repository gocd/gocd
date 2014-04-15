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

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.controller.beans.MaterialFactory;
import com.thoughtworks.go.server.controller.beans.PipelineBean;
import com.thoughtworks.go.server.controller.beans.PipelineNameBean;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.CheckConnectionSubprocessExecutionContext;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.materials.MaterialConnectivityService;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.validation.Validator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;

@Controller
public class SimplePipelineWizardController {
    public static final String DUMMY_VIEW = "DummyP4View";
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private MaterialFactory materialFactory;
    private final MaterialConnectivityService materialConnectivityService;
    private static final Logger LOGGER = Logger.getLogger(SimplePipelineWizardController.class);
    private PipelinePauseService pipelinePauseService;
    private final CheckConnectionSubprocessExecutionContext executionConstant;

    @Autowired
    public SimplePipelineWizardController(GoConfigService goConfigService, SecurityService securityService, PipelinePauseService pipelinePauseService, MaterialConnectivityService materialConnectivityService, MaterialConfigConverter materialConfigConverter) {
        this(goConfigService, securityService, new MaterialFactory(), pipelinePauseService, materialConnectivityService);
    }

    public SimplePipelineWizardController(GoConfigService goConfigService, SecurityService securityService, MaterialFactory materialFactory, PipelinePauseService pipelinePauseService,
                                          MaterialConnectivityService materialConnectivityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.pipelinePauseService = pipelinePauseService;
        this.materialFactory = materialFactory;
        this.materialConnectivityService = materialConnectivityService;
        this.executionConstant = new CheckConnectionSubprocessExecutionContext();
    }

    /*
     * @Deprecated This was used by the old pipeline creation wizard
     */
    @RequestMapping(value = "/admin/pipelines/management.json", method = RequestMethod.POST)
    public ModelAndView createPipeline(@RequestParam(value = "name") String pipelineName,
                                       @RequestParam(value = "pipelineGroup", required = false) String pipelineGroup,
                                       @RequestParam(value = "scm", required = false) String scm,
                                       @RequestParam(value = "url") String url,
                                       @RequestParam(value = "username", required = false) String username,
                                       @RequestParam(value = "password", required = false) String password,
                                       @RequestParam(value = "builder", required = false) String builder,
                                       @RequestParam(value = "buildfile", required = false) String buildfile,
                                       @RequestParam(value = "target", required = false) String target,
                                       @RequestParam(value = "source", required = false) String[] src,
                                       @RequestParam(value = "dest", required = false) String[] dest,
                                       @RequestParam(value = "artifactType", required = false) String[] type,
                                       @RequestParam(value = "command", required = false) String command,
                                       @RequestParam(value = "arguments", required = false) String arguments,
                                       @RequestParam(value = "useTickets", required = false) Boolean useTickets,
                                       @RequestParam(value = "view", required = false) String view,
                                       @RequestParam(value = "branch", required = false) String branch,
                                       @RequestParam(value = "projectPath", required = false) String projectPath,
                                       @RequestParam(value = "domain", required = false) String domain,
                                       HttpServletResponse response) {
        ValidationBean validationBean = getPipelineBean(pipelineName).validate();
        if (!validationBean.isValid()) {
            return JsonAction.jsonBadRequest(validationBean).createView();
        }
        validationBean = Validator.PIPELINEGROUP.validate(pipelineGroup);
        if (!validationBean.isValid()) {
            return JsonAction.jsonBadRequest(validationBean).createView();
        }
        CaseInsensitiveString currentUserName = currentUser().getUsername();
        if(!securityService.isUserAdmin(currentUser()) && !securityService.isUserAdminOfGroup(currentUserName, pipelineGroup)){
            String message = String.format("User '%s' is not authorised to add pipelines to pipeline group %s", currentUserName, pipelineGroup);
            JsonMap jsonMap = new JsonMap();
            jsonMap.put("error", message);
            return JsonAction.jsonBadRequest(jsonMap).createView();
        }
        MaterialConfig materialConfigsBean = materialFactory.getMaterial(scm, url, username, password, useTickets, view, branch, projectPath, domain);
        PipelineBean bean = new PipelineBean(pipelineName.trim(), materialConfigsBean, builder, buildfile,
                target, src, dest, type, command, arguments);
        pipelinePauseService.pause(pipelineName, "Under construction", currentUser());
        goConfigService.addPipeline(bean.getPipelineConfig(), StringUtils.defaultIfEmpty(pipelineGroup, DEFAULT_GROUP));
        return JsonAction.jsonCreated(ValidationBean.valid("Pipeline successfully created.")).respond(response);
    }

    private Username currentUser() {
        return UserHelper.getUserName();
    }

    private PipelineNameBean getPipelineBean(String pipelineName) {
        return new PipelineNameBean(pipelineName, goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName)));
    }

    @RequestMapping(value = "/**/vcs.json", method = RequestMethod.GET)
    public ModelAndView checkSCMConnection(@RequestParam(value = "pipelineName", required = false) String pipelineName,
                                           @RequestParam("scm") String scm,
                                           @RequestParam("url") String url,
                                           @RequestParam(value = "username", required = false) String username,
                                           @RequestParam(value = "password", required = false) String password,
                                           @RequestParam(value = "isEncrypted", required = false) String isEncrypted,
                                           @RequestParam(value = "projectPath", required = false) String projectPath,
                                           @RequestParam(value = "domain", required = false) String domain,
                                           @RequestParam(value = "view", required = false) String view,
                                           HttpServletResponse response) {
        MaterialConfig materialConfig = materialFactory.getMaterial(scm, url, username, decryptPasswordIfRequired(password, isEncrypted), false, view, null, projectPath, domain);
        if (pipelineName != null) {
            CruiseConfig config = new Cloner().deepClone(goConfigService.getConfigForEditing());
            PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
            pipelineConfig.addMaterialConfig(materialConfig);
            MagicalGoConfigXmlLoader.preprocess(config);
        }
        JsonAction jsonAction;
        ConfigErrors errors = materialConfig.errors();
        if (!errors.isEmpty()) {
            List<String> all = errors.getAll();
            jsonAction = JsonAction.jsonFound(ValidationBean.notValid("There were errors while processing the material configuration. " + all));
        } else {
            JsonAware jsonAware = materialConnectivityService.checkConnection(materialConfig, executionConstant);
            jsonAction = JsonAction.jsonFound(jsonAware);
        }
        return jsonAction.respond(response);
    }

    private String decryptPasswordIfRequired(String password, String isEncrypted) {
        if (Boolean.parseBoolean(isEncrypted) && !StringUtils.isBlank(password)) {
            try {
                return new GoCipher().decrypt(password);
            } catch (InvalidCipherTextException e) {
                throw new RuntimeException("Could not decrypt the password while trying to check connection.", e);
            }
        }
        return password;
    }

    @ErrorHandler
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response, Exception e) {
        LOGGER.error("error happens", e);
        return JsonAction.jsonBadRequest(ValidationBean.notValid(e)).respond(response);
    }


}
