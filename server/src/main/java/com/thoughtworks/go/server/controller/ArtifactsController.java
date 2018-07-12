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

import com.thoughtworks.go.domain.ConsoleConsumer;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.cache.ZipArtifactCache;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.security.HeaderConstraint;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConsoleActivityMonitor;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.util.ErrorHandler;
import com.thoughtworks.go.server.view.artifacts.ArtifactsView;
import com.thoughtworks.go.server.view.artifacts.LocalArtifactsView;
import com.thoughtworks.go.server.web.ArtifactFolderViewFactory;
import com.thoughtworks.go.server.web.FileModelAndView;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.web.ZipArtifactFolderViewFactory.zipViewFactory;
import static com.thoughtworks.go.util.ArtifactLogUtil.isConsoleOutput;
import static com.thoughtworks.go.util.GoConstants.*;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Controller
public class ArtifactsController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsController.class);
    private static final String FILE_FORMAT_JSON = "json";
    private static final String FILE_FORMAT_ZIP = "zip";

    private final JobInstanceDao jobInstanceDao;
    private final ConsoleActivityMonitor consoleActivityMonitor;
    private final ArtifactFolderViewFactory folderViewFactory;
    private final ArtifactFolderViewFactory jsonViewFactory;
    private final ArtifactFolderViewFactory zipViewFactory;
    private final Charset consoleLogCharset;
    private ArtifactsService artifactsService;
    private RestfulService restfulService;
    private ConsoleService consoleService;
    private HeaderConstraint headerConstraint;

    @Autowired
    ArtifactsController(ArtifactsService artifactsService, RestfulService restfulService, ZipArtifactCache zipArtifactCache, JobInstanceDao jobInstanceDao,
                        ConsoleActivityMonitor consoleActivityMonitor, ConsoleService consoleService, SystemEnvironment systemEnvironment) {
        this.artifactsService = artifactsService;
        this.restfulService = restfulService;
        this.jobInstanceDao = jobInstanceDao;
        this.consoleActivityMonitor = consoleActivityMonitor;
        this.consoleService = consoleService;

        this.folderViewFactory = FileModelAndView.htmlViewFactory();
        this.jsonViewFactory = FileModelAndView.jsonViewfactory();
        this.zipViewFactory = zipViewFactory(zipArtifactCache);
        this.headerConstraint = new HeaderConstraint(systemEnvironment);
        this.consoleLogCharset = systemEnvironment.consoleLogCharsetAsCharset();
    }

    @RequestMapping(value = {
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName:.+}",
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/*",
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/**"
    }, method = RequestMethod.GET)
    public ModelAndView getArtifacts(@PathVariable("pipelineName") String pipelineName,
                                     @PathVariable("pipelineLabel") String counterOrLabel,
                                     @PathVariable("stageName") String stageName,
                                     @PathVariable("stageCounter") String stageCounter,
                                     @PathVariable("buildName") String buildName,
                                     @RequestParam(value = "sha1", required = false) String sha,
                                     @RequestParam(value = "serverAlias", required = false) String serverAlias,
                                     HttpServletRequest request) throws Exception {
        String filePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        ArtifactFolderViewFactory viewFactory;
        if (!StringUtils.isEmpty(filePath)) {
            viewFactory = getViewFactory(filePath);
        } else {
            viewFactory = getViewFactory(buildName);
            buildName = FilenameUtils.getBaseName(buildName);
        }
        return getArtifact(filePath, viewFactory, pipelineName, counterOrLabel, stageName, stageCounter, buildName, sha, serverAlias);
    }

    @RequestMapping(value = {
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName:.+}",
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/*",
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/**"
    }, params = "startLineNumber", method = RequestMethod.GET)
    public ModelAndView getConsoleJson(@PathVariable("pipelineName") String pipelineName,
                                       @PathVariable("pipelineLabel") String counterOrLabel,
                                       @PathVariable("stageName") String stageName,
                                       @PathVariable("stageCounter") String stageCounter,
                                       @PathVariable("buildName") String buildName,
                                       @RequestParam(value = "startLineNumber", required = false) Long start) {
        start = (start == null) ? 0L : start;
        try {
            JobIdentifier identifier = restfulService.findJob(pipelineName, counterOrLabel, stageName, stageCounter, buildName);
            if (jobInstanceDao.isJobCompleted(identifier) && !consoleService.doesLogExist(identifier)) {
                return logsNotFound(identifier);
            }
            ConsoleConsumer streamer = consoleService.getStreamer(start, identifier);
            return new ModelAndView(new ConsoleOutView(streamer, consoleLogCharset));
        } catch (Exception e) {
            return buildNotFound(pipelineName, counterOrLabel, stageName, stageCounter, buildName);
        }
    }

    @RequestMapping(value = {
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/*",
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/**"
    }, method = RequestMethod.POST)
    public ModelAndView postArtifact(@PathVariable("pipelineName") String pipelineName,
                                     @PathVariable("pipelineLabel") String counterOrLabel,
                                     @PathVariable("stageName") String stageName,
                                     @PathVariable("stageCounter") String stageCounter,
                                     @PathVariable("buildName") String buildName,
                                     @RequestParam(value = "buildId", required = false) Long buildId,
                                     @RequestParam(value = "attempt", required = false) Integer attempt,
                                     MultipartHttpServletRequest request) throws Exception {
        if (!headerConstraint.isSatisfied(request)) {
            return ResponseCodeView.create(HttpServletResponse.SC_BAD_REQUEST, "Missing required header 'Confirm'");
        }
        JobIdentifier jobIdentifier;
        try {
            jobIdentifier = restfulService.findJob(pipelineName, counterOrLabel, stageName, stageCounter, buildName, buildId);
        } catch (Exception e) {
            return buildNotFound(pipelineName, counterOrLabel, stageName, stageCounter, buildName);
        }

        int convertedAttempt = (attempt == null) ? 1 : attempt;

        String filePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
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

    @RequestMapping(value = {
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/*",
            "/files/{pipelineName}/{pipelineLabel}/{stageName}/{stageCounter}/{buildName}/**"
    }, method = RequestMethod.PUT)
    public ModelAndView putArtifact(@PathVariable("pipelineName") String pipelineName,
                                    @PathVariable("pipelineLabel") String counterOrLabel,
                                    @PathVariable("stageName") String stageName,
                                    @PathVariable("stageCounter") String stageCounter,
                                    @PathVariable("buildName") String buildName,
                                    @RequestParam(value = "buildId", required = false) Long buildId,
                                    HttpServletRequest request
    ) throws Exception {
        String filePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (filePath == null || filePath.contains("..")) {
            return FileModelAndView.forbiddenUrl(filePath);
        }

        JobIdentifier jobIdentifier;
        try {
            jobIdentifier = restfulService.findJob(pipelineName, counterOrLabel, stageName, stageCounter, buildName, buildId);
        } catch (Exception e) {
            return buildNotFound(pipelineName, counterOrLabel, stageName, stageCounter, buildName);
        }

        if (isConsoleOutput(filePath)) {
            return putConsoleOutput(jobIdentifier, request.getInputStream());
        } else {
            return putArtifact(jobIdentifier, filePath, request.getInputStream());
        }
    }

    private ArtifactFolderViewFactory getViewFactory(String pathWithExtension) {
        switch (FilenameUtils.getExtension(pathWithExtension)) {
            case FILE_FORMAT_JSON:
                return jsonViewFactory;
            case FILE_FORMAT_ZIP:
                return zipViewFactory;
            default:
                return folderViewFactory;
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
        } else {
            LOGGER.warn("[Artifacts Upload] Checksum file not uploaded for artifact at path '{}'", filePath);
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

    @ErrorHandler
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response, Exception e) {
        LOGGER.error("Error loading artifacts: ", e);
        Map model = new HashMap();
        model.put(ERROR_FOR_PAGE, "Artifact does not exist.");
        return new ModelAndView("exceptions_page", model);
    }

    ModelAndView getArtifact(String filePath, ArtifactFolderViewFactory folderViewFactory, String pipelineName, String counterOrLabel, String stageName, String stageCounter, String buildName, String sha, String serverAlias) throws Exception {
        LOGGER.info("[Artifact Download] Trying to resolve '{}' for '{}/{}/{}/{}/{}'", filePath, pipelineName, counterOrLabel, stageName, stageCounter, buildName);
        long before = System.currentTimeMillis();
        ArtifactsView view;
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

        view = new LocalArtifactsView(folderViewFactory, artifactsService, translatedId, consoleService);

        ModelAndView createdView = view.createView(filePath, sha);
        LOGGER.info("[Artifact Download] Successfully resolved '{}' for '{}/{}/{}/{}/{}'. It took: {}ms", filePath, pipelineName, counterOrLabel, stageName, stageCounter, buildName, System.currentTimeMillis() - before);
        return createdView;
    }

    private boolean shouldUnzipStream(MultipartFile multipartFile) {
        return multipartFile.getName().equals(ZIP_MULTIPART_FILENAME);
    }

    private MultipartFile multipartFile(MultipartHttpServletRequest request) {
        MultipartFile multipartFile = request.getFile(REGULAR_MULTIPART_FILENAME);
        if (multipartFile == null) {
            multipartFile = request.getFile(ZIP_MULTIPART_FILENAME);
        }
        return multipartFile;
    }

    private MultipartFile getChecksumFile(MultipartHttpServletRequest request) {
        return request.getFile(CHECKSUM_MULTIPART_FILENAME);
    }

    private ModelAndView putConsoleOutput(final JobIdentifier jobIdentifier, final InputStream inputStream) throws Exception {
        File consoleLogFile = consoleService.consoleLogFile(jobIdentifier);
        boolean updated = consoleService.updateConsoleLog(consoleLogFile, inputStream);
        if (updated) {
            consoleActivityMonitor.consoleUpdatedFor(jobIdentifier);
            return FileModelAndView.fileAppended(consoleLogFile.getPath());
        } else {
            return FileModelAndView.errorSavingFile(consoleLogFile.getPath());
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

    private ModelAndView logsNotFound(JobIdentifier identifier) {
        String notFound = String.format("Console log for %s is unavailable as it may have been purged by Go or deleted externally.", identifier.toFullString());
        return ResponseCodeView.create(SC_NOT_FOUND, notFound);
    }
}
