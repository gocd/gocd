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

package com.thoughtworks.go.build.docker

import com.thoughtworks.go.build.Architecture
import freemarker.cache.ClassTemplateLoader
import freemarker.core.PlainTextOutputFormat
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject

enum ImageType {
  server,
  agent
}

abstract class BuildDockerImageTask extends DefaultTask {
  @Input Distro distro
  @Input DistroVersion distroVersion
  @Input String tiniVersion
  @InputFile abstract RegularFileProperty getArtifactZip()
  @Input ImageType imageType

  // We don't declare an output here, only an input. We dont want it to be cached.
  // Multiple tasks share dir from parent with unique tarballs per distribution, so they are not "owned" by the task.
  @Internal abstract DirectoryProperty getOutputDir()

  @Internal Closure templateHelper
  @Internal Closure verifyHelper
  @Inject abstract ExecOperations getExecOps()
  @Inject abstract FileSystemOperations getFileOps()

  private final Provider<Directory> buildDirectory = project.layout.buildDirectory

  private final boolean skipBuild = project.hasProperty('skipDockerBuild')
  private final boolean skipNonNativeVerify = project.hasProperty('dockerBuildSkipNonNativeVerify')
  private final boolean keepImages = project.hasProperty('dockerBuildKeepImages')
  private final String gitPush = project.findProperty('dockerGitPush')

  private final projectFullVersion = project.fullVersion
  private final projectGoVersion = project.goVersion
  private final projectGitRevision = project.gitRevision
  private final projectPackagedJavaVersion = project.packagedJavaVersion

  BuildDockerImageTask() {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }
  }

  @TaskAction
  def perform() {
    if (!skipBuild && distroVersion.pastEolGracePeriod) {
      throw new RuntimeException("The image $distro:v$distroVersion.version is unsupported. EOL was ${distroVersion.eolDate}, and GoCD build grace period has passed.")
    }

    if (!skipBuild && distroVersion.eol && !distroVersion.continueToBuild) {
      throw new RuntimeException("The image $distro:v$distroVersion.version was EOL on ${distroVersion.eolDate}. Set :continueToBuild option to continue building through the grace period.")
    }

    if (distroVersion.aboutToEol) {
      println("WARNING: The image $distro:v$distroVersion.version is supposed to be EOL on ${distroVersion.eolDate}. Derived GoCD image will be marked as deprecated.")
    }

    fileOps.delete { it.delete(gitRepoDirectory) }
    gitRepoDirectory.get().asFile.mkdirs()
    def credentials = "${System.getenv("GIT_USER")}:${System.getenv("GIT_PASSWORD")}"
    execOps.exec {
      workingDir = this.gitRepoDirectory.get().asFile.parentFile
      commandLine = ["git", "clone", "--depth=1", "--quiet", "https://${credentials}@github.com/gocd/${gitHubRepoName}", this.gitRepoDirectory.get().asFile]
    }

    if (templateHelper != null) {
      templateHelper.call()
    }

    fileOps.copy {
      from artifactZip
      into gitRepoDirectory
    }

    writeTemplateToFile("Dockerfile.${imageType.name()}.ftl", "Dockerfile")

    if (!skipBuild) {
      logger.lifecycle("Building ${distro} image for ${distro.supportedArchitectures}. (Current build architecture is ${Architecture.current()}).")

      // build image
      imageTarFile.parentFile.mkdirs()
      executeInGitRepo("docker", "buildx", "build",
        "--pull",
        "--platform", supportedPlatforms.join(","),
        "--output", "type=oci,dest=${imageTarFile}",
        ".",
        "--tag", imageNameWithTag
      )

      // verify image
      def isNativeVerify = distro.dockerVerifyArchitecture == Architecture.current()
      if (verifyHelper != null && (isNativeVerify || !skipNonNativeVerify)) {
        // Load image  into local docker from buildx for sanity checking
        executeInGitRepo("docker", "buildx", "build",
          "--quiet",
          "--load",
          "--platform", "linux/${distro.dockerVerifyArchitecture.dockerAlias}",
          ".",
          "--tag", imageNameWithTag
        )

        logger.lifecycle("\nVerifying ${imageNameWithTag} image for ${distro.dockerVerifyArchitecture}. (Current build architecture is ${Architecture.current()}).\n")
        verifyHelper.call()
        logger.lifecycle("\nVerification of ${imageNameWithTag} image on ${distro.dockerVerifyArchitecture} successful.")
      }

      logger.lifecycle("Cleaning up...")
      // delete the image, to save space
      if (!keepImages) {
        execOps.exec {
          commandLine = ["docker", "rmi", imageNameWithTag]
        }
      }
    }

    fileOps.delete { it.delete(this.gitRepoDirectory.map { it.file(artifactZip.get().asFile.name) }) }

    if (gitPush == 'I_REALLY_WANT_TO_DO_THIS') {
      logger.lifecycle("Pushing changed Dockerfile for ${imageNameWithTag} to ${gitHubRepoName}...")
      executeInGitRepo("git", "add", ".")

      if (execOps.exec {
        it.workingDir = gitRepoDirectory
        it.commandLine = ["git", "diff-index", "--quiet", "HEAD"]
        it.ignoreExitValue = true
      }.exitValue != 0) {
        executeInGitRepo("git", "commit", "-m", "Bump to version ${projectFullVersion}", "--author", "GoCD CI User <12554687+gocd-ci-user@users.noreply.github.com>")
        executeInGitRepo("git", "tag", "v${projectGoVersion}")
        executeInGitRepo("git", "push")
        executeInGitRepo("git", "push", "--tags")
        logger.lifecycle("Updated Dockerfile for for ${imageNameWithTag} at ${gitHubRepoName}.")
      } else {
        logger.lifecycle("No changes to Docker build for ${imageNameWithTag} at ${gitHubRepoName}.")
      }
    }
  }

  def verifyProcessInContainerStarted(String expectedProcess, String expectedOutput = "") {
    // run a `ps aux`
    ByteArrayOutputStream psOutput = new ByteArrayOutputStream()
    execOps.exec {
      commandLine = ["docker", "exec", dockerImageName, "ps", "aux"]
      standardOutput = psOutput
      errorOutput = psOutput
      ignoreExitValue = true
    }

    ByteArrayOutputStream containerOutput = new ByteArrayOutputStream()
    execOps.exec {
      commandLine = ["docker", "logs", dockerImageName]
      standardOutput = containerOutput
      errorOutput = containerOutput
      ignoreExitValue = true
    }

    // assert if process was running
    def processList = psOutput.toString()
    def containerLog = containerOutput.toString()

    if (!processList.contains(expectedProcess)) {
      throw new GradleException("Expected process output to contain [${expectedProcess}], but was: [${processList}]\n\nContainer output:\n${containerOutput.toString()}")
    }
    if (expectedOutput != "" && !(containerLog =~ expectedOutput)) {
      throw new GradleException("Process was up, but expected container output to match /${expectedOutput}/. Was: \n${containerOutput.toString()}")
    }
  }

  def executeInGitRepo(Object... args) {
    execOps.exec {
      workingDir = gitRepoDirectory
      commandLine = args
    }
  }

  @Internal
  GString getImageNameWithTag() {
    "${dockerImageName}:${imageTag}"
  }

  @Internal
  Set<GString> getSupportedPlatforms() {
    distro.supportedArchitectures.collect { "linux/${it.dockerAlias}" }
  }

  @Input
  GString getImageTag() {
    "v${projectFullVersion}"
  }

  @Internal
  File getImageTarFile() {
    outputDir.get().file("gocd-${imageType.name()}-${dockerImageName}-v${projectFullVersion}.tar").asFile
  }

  void writeTemplateToFile(String templateFile, String outputFile) {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_34)
    configuration.setDefaultEncoding("utf-8")
    configuration.setLogTemplateExceptions(true)
    configuration.setNumberFormat("computer")
    configuration.setOutputFormat(PlainTextOutputFormat.INSTANCE)
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
    configuration.setTemplateLoader(new ClassTemplateLoader(BuildDockerImageTask.classLoader, "/gocd-docker-${imageType.name()}"))

    Template template = configuration.getTemplate(templateFile, "utf-8")

    def templateVars = [
      distro                         : distro,
      distroVersion                  : distroVersion,
      goVersion                      : projectGoVersion,
      fullVersion                    : projectFullVersion,
      gitRevision                    : projectGitRevision,
      packagedJavaVersion            : projectPackagedJavaVersion,
      additionalFiles                : additionalFiles,
      imageName                      : dockerImageName,
      useFromArtifact                : gitPush == null,
      dockerAliasToWrapperArchAsShell: Architecture.dockerAliasToWrapperArchAsShell(),
    ]

    resolveGitRepoFileFor(outputFile).withWriter("utf-8") { writer ->
      template.process(templateVars, writer)
    }
  }

  @Internal
  Provider<Directory> getGitRepoDirectory() {
    buildDirectory.dir(gitHubRepoName)
  }

  void deleteGitRepoDirectoryContents() {
    fileOps.delete { it.delete(gitRepoDirectory.map { it.asFileTree }) }
  }

  File resolveGitRepoFileFor(String fileName) {
    gitRepoDirectory.map { it.file(fileName) }.get().asFile
  }

  @Internal
  String getGitHubRepoName() {
    return "docker-${dockerImageName}"
  }

  @Internal
  String getDockerImageName() {
    if (imageType == ImageType.agent) {
      return distro.isContinuousRelease() ? "gocd-agent-${distro.name()}" : "gocd-agent-${distro.name()}-${distroVersion.version}"
    } else if (imageType == ImageType.server) {
      return distro == Distro.wolfi ? "gocd-server" : "gocd-server-${distro.name()}-${distroVersion.version}"
    }
  }

  @Internal
  Map<String, Map<String, String>> getAdditionalFiles() {
    return [
      '/usr/local/sbin/tini': [
        url  : "https://github.com/krallin/tini/releases/download/v${tiniVersion}/tini-static-\${TARGETARCH}".toString(),
        mode : '0755',
        owner: 'root',
        group: 'root'
      ]
    ]
  }

}
