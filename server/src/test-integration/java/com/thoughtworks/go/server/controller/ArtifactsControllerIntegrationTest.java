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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.StubMultipartHttpServletRequest;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;

import static com.thoughtworks.go.remote.StandardHeaders.REQUEST_CONFIRM_MODIFICATION;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET_JSON;
import static java.net.HttpURLConnection.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ArtifactsControllerIntegrationTest {
    @Autowired
    private ArtifactsController artifactsController;
    @Autowired
    private ArtifactsService artifactService;
    @Autowired
    private ConsoleService consoleService;
    @Autowired
    private ZipUtil zipUtil;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    private MockHttpServletRequest request;
    private Pipeline pipeline;
    private Stage stage;
    private File artifactsRoot;
    private Long buildId;
    private JobInstance job;
    private GoConfigFileHelper configHelper;
    private String pipelineName;
    private File consoleLogFile;


    @BeforeEach
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);

        pipelineName = "pipeline-" + UUID.randomUUID();

        request = new MockHttpServletRequest();

        dbHelper.onSetUp();

        pipeline = dbHelper.saveTestPipeline(pipelineName, "stage", "build");
        dbHelper.saveBuildingStage(pipeline.getStages().byName("stage"));

        configHelper.addPipeline(pipelineName, "stage", "build");
        stage = pipeline.getStages().byName("stage");
        job = stage.getJobInstances().getByName("build");
        buildId = job.getId();
        JobIdentifier jobId = new JobIdentifier(pipeline.getName(), -2, pipeline.getLabel(),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName(), job.getId());

        artifactsRoot = artifactService.findArtifact(jobId, "");
        consoleLogFile = consoleService.consoleLogFile(jobId);

        deleteDirectory(consoleLogFile.getParentFile());
        deleteDirectory(artifactsRoot);
        artifactsRoot.mkdirs();
    }

    @AfterEach
    public void teardown() throws Exception {
        for (File f : FileUtils.listFiles(artifactsRoot, null, true)) {
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }

        if (artifactsRoot != null) {
            try {
                deleteDirectory(artifactsRoot);
            } catch (IOException e) {
                artifactsRoot.deleteOnExit();
            }
        }

        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldReturn404WhenFileNotFound() throws Exception {
        ModelAndView mav = getNonFolder("/foo.xml");

        assertThat(mav.getView().getContentType()).isEqualTo(RESPONSE_CHARSET);
        assertThat(mav.getView()).isInstanceOf(ResponseCodeView.class);
        assertThat(((ResponseCodeView) mav.getView()).getContent()).contains("Artifact '/foo.xml' is unavailable as it may have been purged by Go or deleted externally.");
    }

    @Test
    public void shouldReturn404WhenNoLatestBuildForGet() throws Exception {
        ModelAndView mav = artifactsController.getArtifactNonFolder(pipelineName, "1", "stage", "1", "build2", "/foo.xml", null);
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, "Job " + pipelineName + "/1/stage/1/build2 not found.");
    }

    private void assertValidContentAndStatus(ModelAndView mav, int responseCode, String content) {
        assertStatus(mav, responseCode);
        assertThat(((ResponseCodeView) mav.getView()).getContent()).isEqualTo(content);
    }

    private void assertStatus(ModelAndView mav, int responseCode) {
        assertThat(mav.getView()).isInstanceOf(ResponseCodeView.class);
        assertThat(((ResponseCodeView) mav.getView()).getStatusCode()).isEqualTo(responseCode);
    }

    @Test
    public void shouldReturn404WhenNoLastGoodBuildForGet() throws Exception {
        ModelAndView mav = artifactsController.getArtifactNonFolder(pipelineName, "lastgood", "stage", "1", "build", "/foo.xml", null);
        String content = "Job " + pipelineName + "/lastgood/stage/1/build not found.";
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, content);
    }

    @Test
    public void shouldReturn404WhenNotAValidBuildForGet() throws Exception {
        ModelAndView mav = artifactsController.getArtifactNonFolder(pipelineName, "whatever", "stage", "1", "build", "/foo.xml", null);
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, "Job " + pipelineName + "/whatever/stage/1/build not found.");
    }

    @Test
    public void shouldReturn404WhenNoLatestBuildForPost() throws Exception {
        request.addHeader(REQUEST_CONFIRM_MODIFICATION, "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request);
        ModelAndView mav = artifactsController.postArtifact(pipelineName, "latest", "stage", "1", "build2", null, "/foo.xml", 1, multipartRequest);
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, "Job " + pipelineName + "/latest/stage/1/build2 not found.");
    }

    @Test
    public void shouldReturn404WhenNoLatestBuildForPut() throws Exception {
        ModelAndView mav = artifactsController.putArtifact(pipelineName, "latest", "stage", "1", "build2", null, "/foo.xml", null, request);
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, "Job " + pipelineName + "/latest/stage/1/build2 not found.");
    }

    @Test
    public void shouldGetArtifactFileRestfully() throws Exception {
        createFile(artifactsRoot, "foo.xml");

        ModelAndView mav = getNonFolder("/foo.xml");
        assertThat(mav.getViewName()).isEqualTo("fileView");
    }

    @Test
    public void shouldReturn404WhenFooDotHtmlDoesNotExistButFooFileExists() throws Exception {
        createFile(artifactsRoot, "foo");

        ModelAndView view = getNonFolder("/foo.html");
        assertValidContentAndStatus(view, HTTP_NOT_FOUND, "Artifact '/foo.html' is unavailable as it may have been purged by Go or deleted externally.");
    }

    @Test
    public void shouldChooseFileOverDirectory() throws Exception {
        createFile(artifactsRoot, "foo.html");
        createFile(artifactsRoot, "foo/bar.xml");

        ModelAndView mav = getNonFolder("/foo.html");
        assertThat(mav.getViewName()).isEqualTo("fileView");

        createFile(artifactsRoot, "foo.json");
        createFile(artifactsRoot, "foo/bar.xml");

        mav = getAsJson("/foo.json");
        assertThat(mav.getViewName()).isEqualTo("fileView");
    }

    @Test
    public void shouldReturn404ForFolderInGenericArtifactView() throws Exception {
        createFile(artifactsRoot, "foo/bar.xml");

        ModelAndView mav = getNonFolder("/foo");
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, "Artifact '/foo' is unavailable as it may have been purged by Go or deleted externally.");

        mav = getNonFolder("/foo/");
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, "Artifact '/foo/' is unavailable as it may have been purged by Go or deleted externally.");
    }

    @Test
    public void shouldReturn404ForFolderInGenericArtifactViewRatherThanFile() throws Exception {
        createFile(artifactsRoot, "directory/foo");

        ModelAndView mav = getNonFolder("/directory.html");
        assertValidContentAndStatus(mav, HTTP_NOT_FOUND, "Artifact '/directory.html' is unavailable as it may have been purged by Go or deleted externally.");
    }

    @Test
    public void shouldReturnFolderInJsonView() throws Exception {
        createFile(artifactsRoot, "foo/bar.xml");

        ModelAndView mav = getAsJson("/foo");
        assertEquals(RESPONSE_CHARSET_JSON, mav.getView().getContentType());
    }

    @Test
    public void shouldReturnForbiddenWhenTryingToAccessArtifactsWithDotDot() throws Exception {
        createFile(artifactsRoot, "foo/1.xml");
        createFile(artifactsRoot, "bar/2.xml");

        ModelAndView mav = getNonFolder("/foo/../bar/2.xml");
        assertStatus(mav, HTTP_FORBIDDEN);
        // The controller already URL escapes the filePath, so this also works with %2e
    }

    @Test
    public void shouldTreatSlashSlashAsOne() throws Exception {
        createFile(artifactsRoot, "tmp/1.xml");

        ModelAndView mav = getNonFolder("//tmp/1.xml");
        assertThat(mav.getViewName()).isEqualTo("fileView");
    }

    @Test
    public void shouldCreateNewFile() throws Exception {
        createFile(artifactsRoot, "dir/foo");

        ModelAndView mav = postFile("/dir/bar.xml");
        assertThat(file(artifactsRoot, "dir/bar.xml")).exists();
        assertThat(file(artifactsRoot, "dir/bar.xml")).isFile();
        assertStatus(mav, HTTP_CREATED);

        mav = postFile("/notexists/quux.txt");
        assertThat(file(artifactsRoot, "notexists/quux.txt")).exists();
        assertThat(file(artifactsRoot, "notexists/quux.txt")).isFile();
        assertStatus(mav, HTTP_CREATED);
    }

    @Test
    public void shouldReturn403WhenPostingAlreadyExistingFile() throws Exception {
        createFile(artifactsRoot, "dir/foo.txt");
        ModelAndView view = postFile("/dir/foo.txt");
        assertValidContentAndStatus(view, HTTP_FORBIDDEN, "File /dir/foo.txt already exists.");
    }

    @Test
    public void shouldCreateAndUnzipNewFileWhenFolderAlreadyExists() throws Exception {
        artifactsRoot.mkdir();
        createFile(artifactsRoot, "dir/foo");

        createTmpFile(artifactsRoot, "dir/bar.xml");
        createTmpFile(artifactsRoot, "dir/quux.txt");

        ModelAndView view = postZipFolderFromTmp(artifactsRoot, "/dir/");

        assertStatus(view, HTTP_CREATED);
        assertThat(file(artifactsRoot, "dir/bar.xml")).exists();
        assertThat(file(artifactsRoot, "dir/bar.xml")).isFile();
        assertThat(file(artifactsRoot, "dir/quux.txt")).exists();
        assertThat(file(artifactsRoot, "dir/quux.txt")).isFile();
    }

    @Test
    public void shouldCreateAndUnzipNewFileWhenFolderDoesNotExists() throws Exception {
        createTmpFile(artifactsRoot, "notexists/bar.csv");
        createTmpFile(artifactsRoot, "notexists/quux.tmp");

        ModelAndView view = postZipFolderFromTmp(artifactsRoot, "/notexists/");

        assertThat(file(artifactsRoot, "notexists/bar.csv")).exists();
        assertThat(file(artifactsRoot, "notexists/bar.csv")).isFile();
        assertThat(file(artifactsRoot, "notexists/quux.tmp")).exists();
        assertThat(file(artifactsRoot, "notexists/quux.tmp")).isFile();
        assertStatus(view, HTTP_CREATED);
    }

    @Test
    public void shouldNotAllowPathsOutsideTheArtifactDirectory() throws Exception {
        ModelAndView mav = postFile("/dir/../../foo/bar.txt");
        assertThat(file(artifactsRoot, "foo/bar.txt")).doesNotExist();
        assertThat(file(artifactsRoot, "dir")).doesNotExist();
        assertStatus(mav, HTTP_FORBIDDEN);
    }

    @Test
    public void shouldEnforceUsingRequiredNameInMultipartRequest() throws Exception {
        ModelAndView mav = postFile("/foo/bar.txt", "badname");
        assertThat(file(artifactsRoot, "foo/bar.txt")).doesNotExist();
        assertThat(file(artifactsRoot, "notfoo/bar.txt")).doesNotExist();
        assertStatus(mav, HTTP_BAD_REQUEST);
    }

    @Test
    public void shouldPutNewFile() throws Exception {
        assertThat(file(artifactsRoot, "foo/bar.txt")).doesNotExist();

        putFile("/foo/bar.txt");
        assertThat(file(artifactsRoot, "foo/bar.txt")).exists();
        String original = readString(file(artifactsRoot, "foo/bar.txt").toPath(), UTF_8);

        putFile("/foo/bar.txt");
        assertThat(original.length()).isLessThan(readString(file(artifactsRoot, "foo/bar.txt").toPath(), UTF_8).length());
    }

    @Test
    public void shouldPutConsoleOutput_withNoNewLineAtTheAtOfTheLog() throws Exception {
        String log = "junit report\nstart\n....";
        ModelAndView mav = putConsoleLogContent("cruise-output/console.log", log);

        String consoleLogContent = readString(consoleLogFile.toPath(), UTF_8);
        String[] lines = consoleLogContent.split("\n");
        assertThat(lines.length).isEqualTo(3);
        assertThat(lines[0]).isEqualTo("junit report");
        assertThat(lines[1]).isEqualTo("start");
        assertThat(lines[2]).isEqualTo("....");
        assertStatus(mav, HTTP_OK);
    }

    @Test
    public void shouldPutConsoleOutput_withoutNewLineChar() throws Exception {
        String log = "....";
        ModelAndView mav = putConsoleLogContent("cruise-output/console.log", log);

        String consoleLogContent = readString(consoleLogFile.toPath(), UTF_8);
        String[] lines = consoleLogContent.split("\n");
        assertThat(lines.length).isEqualTo(1);
        assertThat(lines[0]).isEqualTo("....");
        assertStatus(mav, HTTP_OK);
    }

    @Test
    public void shouldReturnBuildOutputAsPlainText() throws Exception {
        String firstLine = "Chris sucks.\n";
        String secondLine = "Build succeeded.";
        prepareConsoleOut(firstLine + secondLine + "\n");
        Stage firstStage = pipeline.getFirstStage();
        long startLineNumber = 1L;
        ModelAndView view = artifactsController.consoleout(pipeline.getName(), pipeline.getLabel(),
                firstStage.getName(),
                "build", String.valueOf(firstStage.getCounter()), startLineNumber);

        assertThat(view.getView()).isInstanceOf(ConsoleOutView.class);

        HttpServletResponse response = mock(HttpServletResponse.class);
        ResponseOutput output = new ResponseOutput();
        when(response.getWriter()).thenReturn(output.getWriter());
        ConsoleOutView consoleOutView = (ConsoleOutView) view.getView();
        consoleOutView.render(mock(), mock(HttpServletRequest.class), response);

        assertEquals("Build succeeded.\n", output.getOutput());
    }

    @Test
    public void shouldStartAtBeginningWhenNoStartParameterIsGiven() throws Exception {
        String firstLine = "Chris sucks.";
        String secondLine = "Build succeeded.";
        prepareConsoleOut(firstLine + "\n" + secondLine + "\n");
        Stage firstStage = pipeline.getFirstStage();
        ModelAndView view = artifactsController.consoleout(pipeline.getName(), pipeline.getLabel(),
                firstStage.getName(),
                "build", String.valueOf(firstStage.getCounter()), null);

        assertThat(view.getView()).isInstanceOf(ConsoleOutView.class);

        HttpServletResponse response = mock(HttpServletResponse.class);
        ResponseOutput output = new ResponseOutput();
        when(response.getWriter()).thenReturn(output.getWriter());
        ConsoleOutView consoleOutView = (ConsoleOutView) view.getView();
        consoleOutView.render(mock(), mock(HttpServletRequest.class), response);

        assertEquals("Chris sucks.\nBuild succeeded.\n", output.getOutput());
    }

    @Test
    public void testConsoleOutShouldReturn404WhenJobIsNotFound() throws Exception {
        prepareConsoleOut("");
        Stage firstStage = pipeline.getFirstStage();
        long startLineNumber = 0L;
        ModelAndView view = artifactsController.consoleout("snafu", "snafu", "snafu", "build", String.valueOf(firstStage.getCounter()), startLineNumber);

        assertThat(view.getView().getContentType()).isEqualTo(RESPONSE_CHARSET);
        assertThat(view.getView()).isInstanceOf(ResponseCodeView.class);
        assertThat(((ResponseCodeView) view.getView()).getContent()).contains("Job snafu/snafu/snafu/1/build not found.");
    }

    @Test
    public void rawConsoleOutShouldReturnTempFileWhenJobIsInProgress() throws Exception {
        Stage firstStage = pipeline.getFirstStage();
        JobInstance firstJob = firstStage.getFirstJob();
        firstJob.setState(JobState.Building);
        prepareTempConsoleOut(new JobIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel(), firstStage.getName(), String.valueOf(firstStage.getCounter()), firstJob.getName()), "fantastic curly coated retriever");
        ModelAndView view = getNonFolder("cruise-output/console.log");

        assertThat(view.getViewName()).isEqualTo("fileView");
        File targetFile = (File) view.getModel().get("targetFile");
        String separator = File.separator;
        assertThat(targetFile.getPath()).isEqualTo(String.format("data%sconsole%s%s.log",
                separator, separator, DigestUtils.md5Hex(firstJob.buildLocator())));
    }

    @Test
    public void shouldSaveChecksumFileInTheCruiseOutputFolder() throws Exception {
        File fooFile = createFile(artifactsRoot, "/tmp/foobar.html");
        Files.writeString(fooFile.toPath(), "FooBarBaz...", UTF_8);
        File checksumFile = createFile(artifactsRoot, "/tmp/foobar.html.checksum");
        Files.writeString(checksumFile.toPath(), "baz/foobar.html:FooMD5\n", UTF_8);
        MockMultipartFile artifactMultipart = new MockMultipartFile("file", new FileInputStream(fooFile));
        MockMultipartFile checksumMultipart = new MockMultipartFile("file_checksum", new FileInputStream(checksumFile));
        request.addHeader(REQUEST_CONFIRM_MODIFICATION, "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request, artifactMultipart, checksumMultipart);
        postFileWithChecksum("baz/foobar.html", multipartRequest);

        assertThat(file(artifactsRoot, "baz/foobar.html")).exists();
        File uploadedChecksumFile = file(artifactsRoot, "cruise-output/md5.checksum");
        assertThat(uploadedChecksumFile).exists();
        assertThat(Files.lines(uploadedChecksumFile.toPath(), UTF_8)).first(InstanceOfAssertFactories.STRING).isEqualTo("baz/foobar.html:FooMD5");
    }

    @Test
    public void shouldAppendChecksumInTheCruiseOutputFolder() throws Exception {
        File fooFile = createFileWithContent(artifactsRoot, "/tmp/foobar.html", "FooBarBaz...");
        createFileWithContent(artifactsRoot, "cruise-output/md5.checksum", "oldbaz/foobar.html:BazMD5\n");
        File checksumFile = createFileWithContent(artifactsRoot, "/tmp/foobar.html.checksum", "baz/foobar.html:FooMD5\n");

        MockMultipartFile artifactMultipart = new MockMultipartFile("file", new FileInputStream(fooFile));
        MockMultipartFile checksumMultipart = new MockMultipartFile("file_checksum", new FileInputStream(checksumFile));
        request.addHeader(REQUEST_CONFIRM_MODIFICATION, "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request, artifactMultipart, checksumMultipart);

        postFileWithChecksum("baz/foobar.html", multipartRequest);

        assertThat(file(artifactsRoot, "baz/foobar.html")).exists();
        Path uploadedChecksumFile = file(artifactsRoot, "cruise-output/md5.checksum").toPath();
        assertThat(uploadedChecksumFile).exists();
        List<String> list = Files.readAllLines(uploadedChecksumFile, UTF_8);

        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0)).isEqualTo("oldbaz/foobar.html:BazMD5");
        assertThat(list.get(1)).isEqualTo("baz/foobar.html:FooMD5");
    }

    @Test
    public void shouldPutArtifact() throws Exception {
        request.addHeader(REQUEST_CONFIRM_MODIFICATION, "true");
        String artifactFileContent = "FooBarBaz...";
        request.setContent(artifactFileContent.getBytes());

        String filePath = "baz/foobar.html";
        ModelAndView modelAndView = artifactsController.putArtifact(pipelineName.toUpperCase(), Integer.toString(pipeline.getCounter()),
                stage.getName().toUpperCase(), Integer.toString(stage.getCounter()), job.getName().toUpperCase(), buildId, filePath,
                null, request);
        assertValidContentAndStatus(modelAndView, HTTP_OK, String.format("File %s was appended successfully", filePath));

        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, pipeline.getCounter(), null, stage.getName(), Integer.toString(stage.getCounter()), job.getName(), job.getId());
        File artifact = artifactService.findArtifact(jobIdentifier, filePath);
        assertThat(readString(artifact.toPath(), UTF_8)).isEqualTo(artifactFileContent);
    }

    @Test
    public void shouldPutConsoleLogAsArtifact() throws Exception {
        request.addHeader(REQUEST_CONFIRM_MODIFICATION, "true");
        String consoleLogContent = "Job output";
        request.setContent(consoleLogContent.getBytes());

        ModelAndView modelAndView = artifactsController.putArtifact(pipelineName.toUpperCase(), Integer.toString(pipeline.getCounter()),
                stage.getName().toUpperCase(), Integer.toString(stage.getCounter()), job.getName().toUpperCase(), buildId, "cruise-output/console.log",
                null, request);

        String md5Hex = DigestUtils.md5Hex(String.format("%s/1/stage/1/build", pipelineName));
        String path = new File("data/console/", String.format("%s.log", md5Hex)).getPath();
        assertValidContentAndStatus(modelAndView, HTTP_OK, String.format("File %s was appended successfully", path));

        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, pipeline.getCounter(),
                null, stage.getName(), Integer.toString(stage.getCounter()),
                job.getName(), job.getId());
        assertTrue(consoleService.doesLogExist(jobIdentifier));
        File consoleLogFile = consoleService.consoleLogFile(jobIdentifier);
        assertThat(readString(consoleLogFile.toPath(), UTF_8)).isEqualTo(consoleLogContent);
    }

    private File createFile(File buildIdArtifactRoot, String fileName) throws IOException {
        File newFile = new File(buildIdArtifactRoot, fileName);
        newFile.getParentFile().mkdirs();
        newFile.createNewFile();
        return newFile;
    }

    private File createFileWithContent(File root, String fileName, String content) throws IOException {
        File file = createFile(root, fileName);
        Files.writeString(file.toPath(), content, UTF_8);
        return file;
    }

    @SuppressWarnings("UnusedReturnValue")
    private File createTmpFile(File buildIdArtifactRoot, String fileName) throws IOException {
        return createFile(buildIdArtifactRoot, new File("tmp", fileName).getPath());
    }

    private File file(File buildIdArtifactRoot, String fileName) {
        return new File(buildIdArtifactRoot, fileName);
    }

    private ModelAndView getNonFolder(String file) throws Exception {
        return artifactsController.getArtifactNonFolder(pipelineName, pipeline.getLabel(), "stage", "1", "build", file, null);
    }

    private ModelAndView getAsJson(String file) throws Exception {
        return artifactsController.getArtifactAsJson(pipelineName, pipeline.getLabel(), "stage", "1", "build", file, null);
    }

    private ModelAndView postFile(String file) throws Exception {
        return postFile(file, "file");
    }

    @SuppressWarnings("UnusedReturnValue")
    private ModelAndView prepareConsoleOut(String content) throws Exception {
        return postFile("/cruise-output/console.log", "file", new ByteArrayInputStream(content.getBytes()));
    }

    @SuppressWarnings("SameParameterValue")
    private void prepareTempConsoleOut(JobIdentifier jobIdentifier, String content) throws Exception {
        File consoleLogFile = consoleService.consoleLogFile(jobIdentifier);
        consoleLogFile.getParentFile().mkdirs();
        Files.writeString(consoleLogFile.toPath(), content, Charset.defaultCharset());
    }

    private ModelAndView postZipFolderFromTmp(File root, String folder) throws Exception {
        File source = file(root, "/tmp" + folder);
        File zippedFile = zipUtil.zip(source, Files.createTempFile(source.getName(), null).toFile(), Deflater.NO_COMPRESSION);
        zippedFile.deleteOnExit();
        return postFile("", "zipfile", new FileInputStream(zippedFile));
    }

    private ModelAndView postFile(String requestFilename, String multipartFilename) throws Exception {
        return postFile(requestFilename, multipartFilename, new ByteArrayInputStream("content".getBytes()));
    }

    private ModelAndView postFile(String requestFilename, String multipartFilename, InputStream stream) throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(multipartFilename, stream);
        request.addHeader(REQUEST_CONFIRM_MODIFICATION, "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request, multipartFile);
        return artifactsController.postArtifact(pipelineName, Integer.toString(pipeline.getCounter()), "stage", "LATEST", "build", buildId,
                requestFilename,
                null, multipartRequest);
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    private ModelAndView postFileWithChecksum(String requestFileName, MultipartHttpServletRequest multipartRequest) throws Exception {
        return artifactsController.postArtifact(pipelineName, pipeline.getLabel(), "stage", "LATEST", "build", buildId, requestFileName, null, multipartRequest);
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    private ModelAndView putFile(String requestFilename) throws Exception {
        return putConsoleLogContent(requestFilename, "foo:");
    }

    private ModelAndView putConsoleLogContent(String requestFilename, String consoleLogContent) throws Exception {
        request.setContent(consoleLogContent.getBytes());
        return artifactsController.putArtifact(pipelineName, pipeline.getLabel(), "stage", "LATEST", "build", buildId, requestFilename, null, request);
    }

    static class ResponseOutput {
        private final PrintWriter writer;
        private final ByteArrayOutputStream stream;

        public ResponseOutput() {
            stream = new ByteArrayOutputStream();
            writer = new PrintWriter(stream);
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public String getOutput() {
            return stream.toString();
        }
    }
}
