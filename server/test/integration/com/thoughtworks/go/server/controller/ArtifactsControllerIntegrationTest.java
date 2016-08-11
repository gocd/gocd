/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.helper.StubMultipartHttpServletRequest;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;

import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET_JSON;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/spring-tabs-servlet.xml",
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ArtifactsControllerIntegrationTest {
    @Autowired private ArtifactsController artifactsController;
    @Autowired private ArtifactsService artifactService;
    @Autowired private ConsoleService consoleService;
    @Autowired private AgentService agentService;
    @Autowired private ZipUtil zipUtil;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Pipeline pipeline;
    private Stage stage;
    private File artifactsRoot;
    private Long buildId;
    private JobInstance job;
    private GoConfigFileHelper configHelper;
    private String pipelineName;
    private File consoleLogFile;


    @Before public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);

        pipelineName = "pipeline-" + UUID.randomUUID().toString();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

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

    @After public void teardown() throws Exception {
        if (artifactsRoot != null) {
            deleteDirectory(artifactsRoot);
        }
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test public void shouldReturn404WhenFileNotFound() throws Exception {
        ModelAndView mav = getFileAsHtml("/foo.xml");

        assertThat(mav.getView().getContentType(), is(RESPONSE_CHARSET));
        assertThat(mav.getView(), is(instanceOf((ResponseCodeView.class))));
        assertThat(((ResponseCodeView) mav.getView()).getContent(), containsString("Artifact '/foo.xml' is unavailable as it may have been purged by Go or deleted externally."));
    }

    @Test
    public void shouldReturn404WhenNoLatestBuildForGet() throws Exception {
        ModelAndView mav = artifactsController.getArtifactAsHtml(pipelineName, "1", "stage", "1", "build2", "/foo.xml", null, null);
        assertValidContentAndStatus(mav, SC_NOT_FOUND, "Job " + pipelineName + "/1/stage/1/build2 not found.");
    }

    private void assertValidContentAndStatus(ModelAndView mav, int responseCode, String content) {
        assertStatus(mav, responseCode);
        assertThat(((ResponseCodeView) mav.getView()).getContent(), is(content));
    }

    private void assertStatus(ModelAndView mav, int responseCode) {
        assertThat(mav.getView(), is(IsInstanceOf.<Object>instanceOf(ResponseCodeView.class)));
        assertThat(((ResponseCodeView) mav.getView()).getStatusCode(), is(responseCode));
    }

    @Test
    public void shouldReturn404WhenNoLastGoodBuildForGet() throws Exception {
        ModelAndView mav = artifactsController.getArtifactAsHtml(pipelineName, "lastgood", "stage", "1", "build", "/foo.xml", null, null);
        int status = SC_NOT_FOUND;
        String content = "Job " + pipelineName + "/lastgood/stage/1/build not found.";
        assertValidContentAndStatus(mav, status, content);
    }

    @Test
    public void shouldReturn404WhenNotAValidBuildForGet() throws Exception {
        ModelAndView mav = artifactsController.getArtifactAsHtml(pipelineName, "whatever", "stage", "1", "build",
                "/foo.xml",
                null, null);
        assertValidContentAndStatus(mav, SC_NOT_FOUND, "Job " + pipelineName + "/whatever/stage/1/build not found.");
    }

    @Test
    public void shouldHaveJobIdentifierInModelForHtmlFolderView() throws Exception {
        ModelAndView mav = artifactsController.getArtifactAsHtml(pipeline.getName(), pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter()), job.getName(), "", null, null);
        assertThat((JobIdentifier) mav.getModel().get("jobIdentifier"), is(new JobIdentifier(pipeline, stage, job)));
        assertThat(mav.getViewName(), is("rest/html"));
    }

    @Test
    public void shouldReturn404WhenNoLatestBuildForPost() throws Exception {
        request.addHeader("Confirm", "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request);
        ModelAndView mav = artifactsController.postArtifact(pipelineName, "latest", "stage", "1", "build2", null, "/foo.xml", 1, multipartRequest);
        assertValidContentAndStatus(mav, SC_NOT_FOUND, "Job " + pipelineName + "/latest/stage/1/build2 not found.");
    }

    @Test
    public void shouldReturn404WhenNoLatestBuildForPut() throws Exception {
        ModelAndView mav = artifactsController.putArtifact(pipelineName, "latest", "stage", "1", "build2", null, "/foo.xml", null, request);
        assertValidContentAndStatus(mav, SC_NOT_FOUND, "Job " + pipelineName + "/latest/stage/1/build2 not found.");
    }

    //2779
    @Test
    @Ignore
    public void shouldUpdateLastHeardTimeFromAgentForPut() throws Exception {
        Date olderTime = updateHeardTime();
        Thread.sleep(1);
        Date latestTime = updateHeardTime();
        assertThat(latestTime.after(olderTime), is(true));
    }

    private Date updateHeardTime() throws Exception {
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "127.0.0.1"),
                false, "/var/lib", 0L, "linux", false));
        agentService.approve("uuid");
        artifactsController.putArtifact(pipelineName, "latest", "stage", null, "build2", null, "/foo.xml",
                "uuid", request);
        Date olderTime = agentService.findAgentAndRefreshStatus("uuid").getLastHeardTime();
        return olderTime;
    }


    @Test public void shouldGetArtifactFileRestfully() throws Exception {
        createFile(artifactsRoot, "foo.xml");

        ModelAndView mav = getFileAsHtml("/foo.xml");
        assertThat(mav.getViewName(), is("fileView"));
    }

    @Test public void shouldGetDirectoryWithHtmlView() throws Exception {
        createFile(artifactsRoot, "directory/foo");

        ModelAndView mav = getFileAsHtml("/directory.html");
        assertThat(mav.getViewName(), is("rest/html"));
    }

    @Test public void shouldReturn404WhenFooDotHtmlDoesNotExistButFooFileExists() throws Exception {
        createFile(artifactsRoot, "foo");

        ModelAndView view = getFileAsHtml("/foo.html");
        assertStatus(view, SC_NOT_FOUND);
    }

    @Test public void shouldChooseFileOverDirectory() throws Exception {
        createFile(artifactsRoot, "foo.html");
        createFile(artifactsRoot, "foo/bar.xml");

        ModelAndView mav = getFileAsHtml("/foo.html");
        assertThat(mav.getViewName(), is("fileView"));
    }

    @Test public void shouldReturnFolderInHtmlView() throws Exception {
        createFile(artifactsRoot, "foo/bar.xml");

        ModelAndView mav = getFileAsHtml("/foo");
        assertThat(mav.getViewName(), is("rest/html"));
    }

    @Test public void shouldReturnFolderInJsonView() throws Exception {
        createFile(artifactsRoot, "foo/bar.xml");

        ModelAndView mav = getFolderAsJson("/foo");
        assertEquals(RESPONSE_CHARSET_JSON, mav.getView().getContentType());
    }

    @Test public void shouldReturnFolderInHtmlViewWithPathBasedRepository() throws Exception {
        createFile(artifactsRoot, "foo/bar.xml");

        ModelAndView mav = getFileAsHtml("/foo");
        assertThat(mav.getViewName(), is("rest/html"));
    }

    @Test public void shouldReturnForbiddenWhenTryingToAccessArtifactsWithDotDot() throws Exception {
        createFile(artifactsRoot, "foo/1.xml");
        createFile(artifactsRoot, "bar/2.xml");

        ModelAndView mav = getFileAsHtml("/foo/../bar/2.xml");
        assertStatus(mav, SC_FORBIDDEN);
        // The controller already URL escapes the filePath, so this also works with %2e
    }

    @Test public void shouldTreatSlashSlashAsOne() throws Exception {
        createFile(artifactsRoot, "tmp/1.xml");

        ModelAndView mav = getFileAsHtml("//tmp/1.xml");
        assertThat(mav.getViewName(), is("fileView"));
    }

    @Test public void shouldCreateNewFile() throws Exception {
        createFile(artifactsRoot, "dir/foo");

        ModelAndView mav = postFile("/dir/bar.xml");
        assertThat(file(artifactsRoot, "dir/bar.xml"), exists());
        assertThat(file(artifactsRoot, "dir/bar.xml"), is(not(directory())));
        assertStatus(mav, SC_CREATED);

        mav = postFile("/notexists/quux.txt");
        assertThat(file(artifactsRoot, "notexists/quux.txt"), exists());
        assertThat(file(artifactsRoot, "notexists/quux.txt"), is(not(directory())));
        assertStatus(mav, SC_CREATED);
    }

    @Test public void shouldReturn403WhenPostingAlreadyExistingFile() throws Exception {
        createFile(artifactsRoot, "dir/foo.txt");
        ModelAndView view = postFile("/dir/foo.txt");
        assertValidContentAndStatus(view, SC_FORBIDDEN, "File /dir/foo.txt already directoryExists.");
    }

    @Test public void shouldCreateAndUnzipNewFileWhenFolderAlreadyExists() throws Exception {
        artifactsRoot.mkdir();
        createFile(artifactsRoot, "dir/foo");

        createTmpFile(artifactsRoot, "dir/bar.xml");
        createTmpFile(artifactsRoot, "dir/quux.txt");

        ModelAndView view = postZipFolderFromTmp(artifactsRoot, "/dir/");

        assertStatus(view, SC_CREATED);
        assertThat(file(artifactsRoot, "dir/bar.xml"), exists());
        assertThat(file(artifactsRoot, "dir/bar.xml"), is(not(directory())));
        assertThat(file(artifactsRoot, "dir/quux.txt"), exists());
        assertThat(file(artifactsRoot, "dir/quux.txt"), is(not(directory())));
    }

    @Test public void shouldCreateAndUnzipNewFileWhenFolderDoesNotExists() throws Exception {
        createTmpFile(artifactsRoot, "notexists/bar.csv");
        createTmpFile(artifactsRoot, "notexists/quux.tmp");

        ModelAndView view = postZipFolderFromTmp(artifactsRoot, "/notexists/");

        assertThat(file(artifactsRoot, "notexists/bar.csv"), exists());
        assertThat(file(artifactsRoot, "notexists/bar.csv"), is(not(directory())));
        assertThat(file(artifactsRoot, "notexists/quux.tmp"), exists());
        assertThat(file(artifactsRoot, "notexists/quux.tmp"), is(not(directory())));
        assertStatus(view, SC_CREATED);
    }

    @Test public void shouldNotAllowPathsOutsideTheArtifactDirectory() throws Exception {
        ModelAndView mav = postFile("/dir/../../foo/bar.txt");
        assertThat(file(artifactsRoot, "foo/bar.txt"), not(exists()));
        assertThat(file(artifactsRoot, "dir"), not(exists()));
        assertStatus(mav, SC_FORBIDDEN);
    }

    @Test public void shouldEnforceUsingRequiredNameInMultipartRequest() throws Exception {
        ModelAndView mav = postFile("/foo/bar.txt", "badname");
        assertThat(file(artifactsRoot, "foo/bar.txt"), not(exists()));
        assertThat(file(artifactsRoot, "notfoo/bar.txt"), not(exists()));
        assertStatus(mav, SC_BAD_REQUEST);
    }

    @Test public void shouldPutNewFile() throws Exception {
        assertThat(file(artifactsRoot, "foo/bar.txt"), not(exists()));

        putFile("/foo/bar.txt");
        assertThat(file(artifactsRoot, "foo/bar.txt"), exists());
        String original = readFileToString(file(artifactsRoot, "foo/bar.txt"));

        putFile("/foo/bar.txt");
        assertThat(original.length(), is(lessThan(readFileToString(file(artifactsRoot, "foo/bar.txt")).length())));
    }

    @Test
    public void shouldPutConsoleOutput_whenContentMoreThanBufferSizeUsed() throws Exception {
        StringBuilder builder = new StringBuilder();
        String str = "This is one full line of text. With 2 sentences without newline separating them.\n";
        int numberOfLines = ConsoleService.DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE / 10;
        for (int i = 0; i < numberOfLines; i++) {
            builder.append(str);
        }
        for (int i = 0; i < numberOfLines; i++) {
            builder.append(str);
        }
        ModelAndView mav = putConsoleLogContent("cruise-output/console.log", builder.toString());

        String consoleLogContent = FileUtils.readFileToString(file(consoleLogFile));
        String[] lines = consoleLogContent.split("\n");
        assertThat(lines.length, is(2 * numberOfLines));
        String hundredThLine = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == numberOfLines) {
                hundredThLine = line;
            } else {
                assertThat("Line " + i + " doesn't have desired content.", line + "\n", is(str));
            }
        }
        assertStatus(mav, SC_OK);
    }

    @Test
    public void shouldPutConsoleOutput_withHugeSingleLine() throws Exception {
        StringBuilder builder = new StringBuilder();
        String str = "a ";
        int numberOfChars = ConsoleService.DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE * 4;

        StringBuilder longLine = new StringBuilder();
        for (int i = 0; i < numberOfChars; i++) {
            longLine.append(str);
        }
        String longLineStr = longLine.toString();

        builder.append(longLineStr);
        builder.append("\nTesting:\n");
        builder.append(longLineStr);

        ModelAndView mav = putConsoleLogContent("cruise-output/console.log", builder.toString());

        String consoleLogContent = FileUtils.readFileToString(file(consoleLogFile));
        String[] lines = consoleLogContent.split("\n");
        assertThat(lines.length, is(3));
        assertThat(lines[0], is(longLineStr));
        assertThat(lines[1] + "\n", is("Testing:\n"));
        assertThat(lines[2], is(longLineStr));
        assertStatus(mav, SC_OK);
    }

    @Test
    public void shouldPutConsoleOutput_withNoNewLineAtTheAtOfTheLog() throws Exception {
        String log = "junit report\nstart\n....";
        ModelAndView mav = putConsoleLogContent("cruise-output/console.log", log);

        String consoleLogContent = FileUtils.readFileToString(file(consoleLogFile));
        String[] lines = consoleLogContent.split("\n");
        assertThat(lines.length, is(3));
        assertThat(lines[0], is("junit report"));
        assertThat(lines[1], is("start"));
        assertThat(lines[2], is("...."));
        assertStatus(mav, SC_OK);
    }

    @Test
    public void shouldPutConsoleOutput_withoutNewLineChar() throws Exception {
        String log = "....";
        ModelAndView mav = putConsoleLogContent("cruise-output/console.log", log);

        String consoleLogContent = FileUtils.readFileToString(file(consoleLogFile));
        String[] lines = consoleLogContent.split("\n");
        assertThat(lines.length, is(1));
        assertThat(lines[0], is("...."));
        assertStatus(mav, SC_OK);
    }

    @Test
    public void shouldReturnBuildOutputAsPlainText() throws Exception {
        String firstLine = "Chris sucks.\n";
        String secondLine = "Build succeeded.";
        prepareConsoleOut(firstLine + secondLine + "\n");
        Stage firstStage = pipeline.getFirstStage();
        int startLineNumber = 1;
        ModelAndView view = artifactsController.consoleout(pipeline.getName(), pipeline.getLabel(),
                firstStage.getName(),
                "build", String.valueOf(firstStage.getCounter()), startLineNumber);

        assertThat(view.getView(), is(instanceOf(ConsoleOutView.class)));
        assertThat(((ConsoleOutView) view.getView()).getOffset(), is(2));
        assertThat(((ConsoleOutView) view.getView()).getContent(), containsString("Build succeeded."));
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

        assertThat(view.getView(), is(instanceOf(ConsoleOutView.class)));
        assertThat(((ConsoleOutView) view.getView()).getOffset(), is(2));
        assertThat(((ConsoleOutView) view.getView()).getContent(), containsString("Chris sucks."));
        assertThat(((ConsoleOutView) view.getView()).getContent(), containsString("Build succeeded."));
    }

    @Test
    public void nextLineShouldEqualsStartLineWhenNoOutputReturns() throws Exception {
        prepareConsoleOut("");
        Stage firstStage = pipeline.getFirstStage();
        int startLineNumber = 0;
        ModelAndView view = artifactsController.consoleout(pipeline.getName(), pipeline.getLabel(),
                firstStage.getName(),
                "build", String.valueOf(firstStage.getCounter()), startLineNumber);

        assertThat(view.getView(), is(instanceOf(ConsoleOutView.class)));
        assertThat(((ConsoleOutView) view.getView()).getOffset(), is(0));
        assertThat(((ConsoleOutView) view.getView()).getContent(), is(""));
    }

    @Test
    public void testConsoleOutShouldReturn404WhenJobIsNotFound() throws Exception {
        prepareConsoleOut("");
        Stage firstStage = pipeline.getFirstStage();
        int startLineNumber = 0;
        ModelAndView view = artifactsController.consoleout("snafu", "snafu", "snafu", "build", String.valueOf(firstStage.getCounter()), startLineNumber);

        assertThat(view.getView().getContentType(), is(RESPONSE_CHARSET));
        assertThat(view.getView(), is(instanceOf((ResponseCodeView.class))));
        assertThat(((ResponseCodeView) view.getView()).getContent(), containsString("Job snafu/snafu/snafu/1/build not found."));
    }

    @Test
    public void shouldSaveChecksumFileInTheCruiseOutputFolder() throws Exception {
        File fooFile = createFile(artifactsRoot, "/tmp/foobar.html");
        FileUtils.writeStringToFile(fooFile, "FooBarBaz...");
        File checksumFile = createFile(artifactsRoot, "/tmp/foobar.html.checksum");
        FileUtils.writeStringToFile(checksumFile, "baz/foobar.html:FooMD5\n");
        MockMultipartFile artifactMultipart = new MockMultipartFile("file", new FileInputStream(fooFile));
        MockMultipartFile checksumMultipart = new MockMultipartFile("file_checksum", new FileInputStream(checksumFile));
        request.addHeader("Confirm", "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request, artifactMultipart, checksumMultipart);
        postFileWithChecksum("baz/foobar.html", multipartRequest);

        assertThat(file(artifactsRoot, "baz/foobar.html"), exists());
        File uploadedChecksumFile = file(artifactsRoot, "cruise-output/md5.checksum");
        assertThat(uploadedChecksumFile, exists());
        assertThat(FileUtils.readLines(uploadedChecksumFile).get(0).toString(), is("baz/foobar.html:FooMD5"));
    }

    @Test
    public void shouldAppendChecksumInTheCruiseOutputFolder() throws Exception {
        File fooFile = createFileWithContent(artifactsRoot, "/tmp/foobar.html", "FooBarBaz...");
        createFileWithContent(artifactsRoot, "cruise-output/md5.checksum", "oldbaz/foobar.html:BazMD5\n");
        File checksumFile = createFileWithContent(artifactsRoot, "/tmp/foobar.html.checksum", "baz/foobar.html:FooMD5\n");

        MockMultipartFile artifactMultipart = new MockMultipartFile("file", new FileInputStream(fooFile));
        MockMultipartFile checksumMultipart = new MockMultipartFile("file_checksum", new FileInputStream(checksumFile));
        request.addHeader("Confirm", "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request, artifactMultipart, checksumMultipart);

        postFileWithChecksum("baz/foobar.html", multipartRequest);

        assertThat(file(artifactsRoot, "baz/foobar.html"), exists());
        File uploadedChecksumFile = file(artifactsRoot, "cruise-output/md5.checksum");
        assertThat(uploadedChecksumFile, exists());
        List list = FileUtils.readLines(uploadedChecksumFile);

        assertThat(list.size(), is(2));
        assertThat(list.get(0).toString(), is("oldbaz/foobar.html:BazMD5"));
        assertThat(list.get(1).toString(), is("baz/foobar.html:FooMD5"));
    }

    private File createFile(File buildIdArtifactRoot, String fileName) throws IOException {
        File newFile = new File(buildIdArtifactRoot, fileName);
        newFile.getParentFile().mkdirs();
        newFile.createNewFile();
        return newFile;
    }

    private File createFileWithContent(File root, String fileName, String content) throws IOException {
        File file = createFile(root, fileName);
        FileUtils.writeStringToFile(file, content);
        return file;
    }

    private File createTmpFile(File buildIdArtifactRoot, String fileName) throws IOException {
        return createFile(buildIdArtifactRoot, new File("tmp", fileName).getPath());
    }

    private File file(File buildIdArtifactRoot, String fileName) {
        return new File(buildIdArtifactRoot, fileName);
    }

    private File file(File buildIdArtifactRoot) {
        return new File(buildIdArtifactRoot, "");
    }

    private ModelAndView getFileAsHtml(String file) throws Exception {
        return artifactsController.getArtifactAsHtml(pipelineName, pipeline.getLabel(), "stage", "1", "build", file, null, null);
    }

    private ModelAndView getFolderAsJson(String file) throws Exception {
        return artifactsController.getArtifactAsJson(pipelineName, pipeline.getLabel(), "stage", "1", "build", file, null);
    }

    private ModelAndView postFile(String file) throws Exception {
        return postFile(file, "file");
    }

    private ModelAndView prepareConsoleOut(String content) throws Exception {
        return postFile("/cruise-output/console.log", "file", new ByteArrayInputStream(content.getBytes()),
                new MockHttpServletResponse());
    }

    private ModelAndView postZipFolderFromTmp(File root, String folder) throws Exception {
        File source = file(root, "/tmp" + folder);
        File zippedFile = zipUtil.zip(source, TestFileUtil.createUniqueTempFile(source.getName()),
                Deflater.NO_COMPRESSION);
        zippedFile.deleteOnExit();
        return postFile("", "zipfile", new FileInputStream(zippedFile), response);
    }

    private ModelAndView postFile(String requestFilename, String multipartFilename) throws Exception {
        return postFile(requestFilename, multipartFilename, new ByteArrayInputStream("content".getBytes()), response);
    }

    private ModelAndView postFile(String requestFilename, String multipartFilename, InputStream stream,
                                  MockHttpServletResponse response) throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(multipartFilename, stream);
        request.addHeader("Confirm", "true");
        StubMultipartHttpServletRequest multipartRequest = new StubMultipartHttpServletRequest(request, multipartFile);
        return artifactsController.postArtifact(pipelineName, pipeline.getLabel(), "stage", "LATEST", "build", buildId,
                requestFilename,
                null, multipartRequest);
    }

    private ModelAndView postFileWithChecksum(String requestFileName, MultipartHttpServletRequest multipartRequest) throws Exception {
        return artifactsController.postArtifact(pipelineName, pipeline.getLabel(), "stage", "LATEST", "build", buildId, requestFileName, null, multipartRequest);
    }

    private ModelAndView putFile(String requestFilename) throws Exception {
        return putConsoleLogContent(requestFilename, "foo:");
    }

    private ModelAndView putConsoleLogContent(String requestFilename, String consoleLogContent) throws Exception {
        request.setContent(consoleLogContent.getBytes());
        return artifactsController.putArtifact(pipelineName, pipeline.getLabel(), "stage", "LATEST", "build", buildId, requestFilename, null, request);
    }

    private TypeSafeMatcher<File> exists() {
        return new TypeSafeMatcher<File>() {
            public boolean matchesSafely(File file) {
                return file.exists();
            }

            public void describeTo(Description description) {
                description.appendText("file should exist but does not");
            }
        };
    }

    private TypeSafeMatcher<File> directory() {
        return new TypeSafeMatcher<File>() {
            public boolean matchesSafely(File file) {
                return file.isDirectory();
            }

            public void describeTo(Description description) {
                description.appendText("a directory");
            }
        };
    }

}
