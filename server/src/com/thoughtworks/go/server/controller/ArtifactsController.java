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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.domain.ConsoleOut;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.cache.ZipArtifactCache;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConsoleActivityMonitor;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.server.view.artifacts.ArtifactsView;
import com.thoughtworks.go.server.view.artifacts.LocalArtifactsView;
import com.thoughtworks.go.server.web.ArtifactFolderViewFactory;
import com.thoughtworks.go.server.web.FileModelAndView;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.ArtifactLogUtil;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.server.web.ZipArtifactFolderViewFactory.zipViewFactory;
import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileName;
import static com.thoughtworks.go.util.GoConstants.CHECKSUM_MULTIPART_FILENAME;
import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_PAGE;
import static com.thoughtworks.go.util.GoConstants.REGULAR_MULTIPART_FILENAME;
import static com.thoughtworks.go.util.GoConstants.ZIP_MULTIPART_FILENAME;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Controller
public class ArtifactsController {
    private ArtifactsService artifactsService;
    private RestfulService restfulService;

    private static final Logger LOGGER = Logger.getLogger(ArtifactsController.class);
    private ScheduleService scheduleService;
    private final ConsoleActivityMonitor consoleActivityMonitor;
    private final GoConfigService goConfigService;
    private final ArtifactFolderViewFactory folderViewFactory;
    private final ArtifactFolderViewFactory jsonViewFactory;
    private final ArtifactFolderViewFactory zipViewFactory;

    @Autowired ArtifactsController(ArtifactsService artifactsService, RestfulService restfulService, ZipArtifactCache zipArtifactCache,
                                   ScheduleService scheduleService, ConsoleActivityMonitor consoleActivityMonitor, GoConfigService goConfigService) {
        this.artifactsService = artifactsService;
        this.restfulService = restfulService;
        this.scheduleService = scheduleService;
        this.consoleActivityMonitor = consoleActivityMonitor;
        this.goConfigService = goConfigService;

        this.folderViewFactory = FileModelAndView.htmlViewFactory();
        this.jsonViewFactory = FileModelAndView.jsonViewfactory();
        this.zipViewFactory = zipViewFactory(zipArtifactCache);
    }


    /* RESTful URLs */
    @RequestMapping("/restful/artifact/GET/html")
    public ModelAndView getArtifactAsHtml(@RequestParam("pipelineName") String pipelineName,
                                          @RequestParam("pipelineLabel") String counterOrLabel,
                                          @RequestParam("stageName") String stageName,
                                          @RequestParam(value = "stageCounter", required = false) String stageCounter,
                                          @RequestParam("buildName") String buildName,
                                          @RequestParam("filePath") String filePath,
                                          @RequestParam(value = "sha1", required = false) String sha,
                                          @RequestParam(value = "serverAlias", required = false) String serverAlias) throws Exception {
        return getArtifact(filePath, folderViewFactory, pipelineName, counterOrLabel, stageName, stageCounter, buildName, sha, serverAlias);
    }

    @RequestMapping("/restful/artifact/GET/json")
    public ModelAndView getArtifactAsJson(@RequestParam("pipelineName") String pipelineName,
                                          @RequestParam("pipelineLabel") String counterOrLabel,
                                          @RequestParam("stageName") String stageName,
                                          @RequestParam(value = "stageCounter", required = false) String stageCounter,
                                          @RequestParam("buildName") String buildName,
                                          @RequestParam("filePath") String filePath,
                                          @RequestParam(value = "sha1", required = false) String sha
    ) throws Exception {
        return getArtifact(filePath, jsonViewFactory, pipelineName, counterOrLabel, stageName, stageCounter, buildName, sha, null);
    }

    @RequestMapping("/restful/artifact/GET/zip")
    public ModelAndView getArtifactAsZip(@RequestParam("pipelineName") String pipelineName,
                                         @RequestParam("pipelineLabel") String counterOrLabel,
                                         @RequestParam("stageName") String stageName,
                                         @RequestParam(value = "stageCounter", required = false) String stageCounter,
                                         @RequestParam("buildName") String buildName,
                                         @RequestParam("filePath") String filePath,
                                         @RequestParam(value = "sha1", required = false) String sha
    ) throws Exception {
        return getArtifact(filePath, zipViewFactory, pipelineName, counterOrLabel, stageName, stageCounter, buildName, sha, null);
    }

    @RequestMapping("/restful/artifact/GET/*")
    public void fetch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.getRequestDispatcher("/repository/restful/artifact/GET/html").forward(request, response);
    }

    @RequestMapping("/restful/artifact/POST/*")
    public ModelAndView postArtifact(@RequestParam("pipelineName") String pipelineName,
                                     @RequestParam("pipelineLabel") String counterOrLabel,
                                     @RequestParam("stageName") String stageName,
                                     @RequestParam(value = "stageCounter", required = false) String stageCounter,
                                     @RequestParam("buildName") String buildName,
                                     @RequestParam(value = "buildId", required = false) Long buildId,
                                     @RequestParam("filePath") String filePath,
                                     @RequestParam(value = "attempt", required = false) Integer attempt,
                                     MultipartHttpServletRequest request) throws Exception {
        JobIdentifier jobIdentifier = null;
        try {
            jobIdentifier = restfulService.findJob(pipelineName, counterOrLabel, stageName, stageCounter,
                    buildName, buildId);
        } catch (Exception e) {
            return buildNotFound(pipelineName, counterOrLabel, stageName, stageCounter,
                    buildName);
        }

        int convertedAttempt = attempt == null ? 1 : attempt;

        try {
            File artifact = artifactsService.findArtifact(jobIdentifier, filePath);
            if (artifact.exists() && artifact.isFile()) {
                return FileModelAndView.fileAlreadyExists(filePath);
            }

            MultipartFile multipartFile = multipartFile(request);
            if (multipartFile == null) {
                return FileModelAndView.invalidUploadRequest();
            }

            boolean success = saveFile(convertedAttempt, artifact, multipartFile, shouldUnzipStream(multipartFile));

            if (!success) {
                return FileModelAndView.errorSavingFile(filePath);
            }

            success = updateChecksumFile(request, jobIdentifier, filePath);

            if (!success) {
                return FileModelAndView.errorSavingChecksumFile(filePath);
            }

            return FileModelAndView.fileCreated(filePath);

        } catch (IllegalArtifactLocationException e) {
            return FileModelAndView.forbiddenUrl(filePath);
        }
    }

    private boolean updateChecksumFile(MultipartHttpServletRequest request, JobIdentifier jobIdentifier, String filePath) throws IOException, IllegalArtifactLocationException {
        MultipartFile checksumMultipartFile = getChecksumFile(request);
        if (checksumMultipartFile != null) {
            String checksumFilePath = String.format("%s/%s/%s", artifactsService.findArtifactRoot(jobIdentifier), ArtifactLogUtil.CRUISE_OUTPUT_FOLDER, ArtifactLogUtil.MD5_CHECKSUM_FILENAME);
            File checksumFile = artifactsService.getArtifactLocation(checksumFilePath);
            synchronized (checksumFilePath.intern()) {
                return artifactsService.saveOrAppendFile(checksumFile, checksumMultipartFile.getInputStream());
            }
        }
        else {
            LOGGER.warn(String.format("[Artifacts Upload] Checksum file not uploaded for artifact at path '%s'", filePath));
        }
        return true;
    }

    private boolean saveFile(int convertedAttempt, File artifact, MultipartFile multipartFile, boolean shouldUnzip) throws IOException {
        InputStream inputStream = null;
        boolean success;
        try {
            inputStream = multipartFile.getInputStream();
            success = artifactsService.saveFile(artifact, inputStream, shouldUnzip, convertedAttempt);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return success;
    }

    @RequestMapping("/restful/artifact/PUT/*")
    public ModelAndView putArtifact(@RequestParam("pipelineName") String pipelineName,
                                    @RequestParam("pipelineLabel") String counterOrLabel,
                                    @RequestParam("stageName") String stageName,
                                    @RequestParam(value = "stageCounter", required = false) String stageCounter,
                                    @RequestParam("buildName") String buildName,
                                    @RequestParam(value = "buildId", required = false) Long buildId,
                                    @RequestParam("filePath") String filePath,
                                    @RequestParam(value = "agentId", required = false) String agentId,
                                    HttpServletRequest request
    ) throws Exception {
        if (filePath.contains("..")) {
            return FileModelAndView.forbiddenUrl(filePath);
        }

        JobIdentifier jobIdentifier = null;
        try {
            jobIdentifier = restfulService.findJob(pipelineName, counterOrLabel, stageName, stageCounter, buildName, buildId);
        } catch (Exception e) {
            return buildNotFound(pipelineName, counterOrLabel, stageName, stageCounter, buildName);
        }

        if (isConsoleOutput(filePath)) {
            return putConsoleOutput(jobIdentifier, filePath, request.getInputStream());
        } else {
            return putArtifact(jobIdentifier, filePath, request.getInputStream());
        }
    }

    /* Other URLs */

    @RequestMapping(value = "/**/consoleout.json", method = RequestMethod.GET)
    public ModelAndView consoleout(@RequestParam("pipelineName") String pipelineName,
                                   @RequestParam("pipelineLabel") String counterOrLabel,
                                   @RequestParam("stageName") String stageName,
                                   @RequestParam("buildName") String buildName,
                                   @RequestParam(value = "stageCounter", required = false) String stageCounter,
                                   @RequestParam(value = "startLineNumber", required = false) Integer start
    ) throws Exception {

        int startLine = start == null ? 0 : start;
        try {
            JobIdentifier identifier = restfulService.findJob(pipelineName, counterOrLabel, stageName, stageCounter,
                    buildName);
            String consoleOutPath = ArtifactLogUtil.CRUISE_OUTPUT_FOLDER + '/' + ArtifactLogUtil.CONSOLE_LOG_FILE_NAME;
            File artifact = artifactsService.findArtifact(identifier, consoleOutPath);
            ConsoleOut consoleOut = artifactsService.getConsoleOut(artifact, startLine);
            return new ModelAndView(new ConsoleOutView(consoleOut.calculateNextStart(), consoleOut.output()));
        } catch (FileNotFoundException e) {
            return new ModelAndView(new ConsoleOutView(0, ""));
        }
    }

    @ErrorHandler
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response, Exception e) {
        LOGGER.error("Error loading artifacts: ", e);
        Map model = new HashMap();
        model.put(ERROR_FOR_PAGE, "Artifact does not exist.");
        return new ModelAndView("exceptions_page", model);
    }

    ModelAndView getArtifact(String filePath, ArtifactFolderViewFactory folderViewFactory, String pipelineName, String counterOrLabel, String stageName, String stageCounter, String buildName, String sha, String serverAlias) throws Exception {
        LOGGER.info(String.format("[Artifact Download] Trying to resolve '%s' for '%s/%s/%s/%s/%s'", filePath, pipelineName, counterOrLabel, stageName, stageCounter, buildName));
        long before = System.currentTimeMillis();
        ArtifactsView view = null;
        //Work out the job that we are trying to retrieve
        JobIdentifier translatedId;
        try {
            translatedId = restfulService.findJob(pipelineName, counterOrLabel, stageName, stageCounter, buildName);
        } catch (Exception e) {
            return buildNotFound(pipelineName, counterOrLabel, stageName, stageCounter, buildName);
        }

        if (filePath.contains("..")) {
            return FileModelAndView.forbiddenUrl(filePath);
        }

        view = new LocalArtifactsView(folderViewFactory, artifactsService, translatedId);

        ModelAndView createdView = view.createView(filePath, sha);
        LOGGER.info(String.format("[Artifact Download] Successfully resolved '%s' for '%s/%s/%s/%s/%s'. It took: %sms", filePath, pipelineName, counterOrLabel, stageName, stageCounter, buildName,
                System.currentTimeMillis() - before));
        return createdView;
    }

    private boolean shouldUnzipStream(MultipartFile multipartFile) {
        return multipartFile.getName().equals(ZIP_MULTIPART_FILENAME);
    }

    private MultipartFile multipartFile(MultipartHttpServletRequest request) throws IOException {
        MultipartFile multipartFile = request.getFile(REGULAR_MULTIPART_FILENAME);
        if (multipartFile == null) {
            multipartFile = request.getFile(ZIP_MULTIPART_FILENAME);
        }
        return multipartFile;
    }

    private MultipartFile getChecksumFile(MultipartHttpServletRequest request) throws IOException {
        return request.getFile(CHECKSUM_MULTIPART_FILENAME);
    }

    private boolean isConsoleOutput(String filePath) {
        return getConsoleOutputFolderAndFileName().equalsIgnoreCase(filePath);
    }

    private ModelAndView putConsoleOutput(final JobIdentifier jobIdentifier, final String filePath, final InputStream inputStream) throws Exception {
        File artifact = artifactsService.findArtifact(jobIdentifier, filePath);
        boolean updated = artifactsService.updateConsoleLog(artifact, inputStream, ArtifactsService.LineListener.NO_OP_LINE_LISTENER);
        if (updated) {
            consoleActivityMonitor.consoleUpdatedFor(jobIdentifier);
            return FileModelAndView.fileAppended(filePath);
        } else {
            return FileModelAndView.errorSavingFile(filePath);
        }
    }

    private ModelAndView putArtifact(JobIdentifier jobIdentifier, String filePath,
                                     InputStream inputStream) throws Exception {
        File artifact = artifactsService.findArtifact(jobIdentifier, filePath);
        if (artifactsService.saveOrAppendFile(artifact, inputStream)) {
            return FileModelAndView.fileAppended(filePath);
        } else {
            return FileModelAndView.errorSavingFile(filePath);
        }
    }

    private ModelAndView buildNotFound(String pipelineName, String counterOrLabel, String stageName,
                                       String stageCounter,
                                       String buildName) {
        return ResponseCodeView.create(SC_NOT_FOUND, String.format("Job %s/%s/%s/%s/%s not found.", pipelineName,
                counterOrLabel, stageName, stageCounter, buildName));
    }
}
