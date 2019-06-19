/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import freemarker.cache.ClassTemplateLoader
import freemarker.core.PlainTextOutputFormat
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

enum ImageType {
  server,
  agent
}

class BuildDockerImageTask extends DefaultTask {
  Distro distro
  DistroVersion distroVersion
  String tiniVersion
  String gosuVersion
  File artifactZip
  ImageType imageType
  File outputDir
  Closure templateHelper

  BuildDockerImageTask() {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }
  }

  @TaskAction
  def perform() {
    project.delete(gitRepoDirectory)
    def credentials = "${System.getenv("GIT_USER")}:${System.getenv("GIT_PASSWORD")}"
    project.exec {
      workingDir = project.rootProject.projectDir
      commandLine = ["git", "clone", "--depth=1", "--quiet", "https://${credentials}@github.com/gocd/${gitHubRepoName}", gitRepoDirectory]
    }

    if (templateHelper != null) {
      templateHelper.call()
    }

    project.copy {
      from artifactZip
      into gitRepoDirectory
    }

    writeTemplateToFile(templateFile(), dockerfile)

    if (!project.hasProperty('skipDockerBuild')) {
      // build image
      executeInGitRepo("docker", "build", "--pull", ".", "--tag", imageNameWithTag)

      // export to tar
      project.mkdir(imageTarFile.parentFile)

      project.exec {
        workingDir = project.rootProject.projectDir
        commandLine = ["docker", "save", imageNameWithTag, "--output", imageTarFile]
      }

      // compress the tar
      project.exec {
        workingDir = project.rootProject.projectDir
        commandLine = ["gzip", "--force", imageTarFile]
      }

      // delete the image, to save space
      project.exec {
        workingDir = project.rootProject.projectDir
        commandLine = ["docker", "rmi", imageNameWithTag]
      }

      if (System.getenv('GO_SERVER_URL')) {
        // delete the parent image, to save space
        project.exec {
          workingDir = project.rootProject.projectDir
          commandLine = ["docker", "rmi", "${distro.name()}:${distroVersion.releaseName}"]
        }
      }
    }

    project.delete("${gitRepoDirectory}/${artifactZip.name}")

    if (project.hasProperty('dockerGitPush') && project.dockerGitPush == 'I_REALLY_WANT_TO_DO_THIS') {
      executeInGitRepo("git", "add", ".")
      executeInGitRepo("git", "commit", "-m", "Bump to version ${project.fullVersion}", "--author", "GoCD CI User <godev+gocd-ci-user@thoughtworks.com>")
      executeInGitRepo("git", "tag", "v${project.goVersion}")
      executeInGitRepo("git", "push")
      executeInGitRepo("git", "push", "--tags")
    }
  }

  def executeInGitRepo(Object... args) {
    project.exec {
      workingDir = gitRepoDirectory
      commandLine = args
    }
  }

  protected GString getImageNameWithTag() {
    "${dockerImageName}:${imageTag}"
  }

  GString getImageTag() {
    "v${project.fullVersion}"
  }

  File getImageTarFile() {
    project.file("${outputDir}/gocd-${imageType.name()}-${dockerImageName}-v${project.fullVersion}.tar")
  }

  void writeTemplateToFile(String templatefile, File outputFile) {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_28)
    configuration.setDefaultEncoding("utf-8")
    configuration.setLogTemplateExceptions(true)
    configuration.setNumberFormat("computer")
    configuration.setOutputFormat(PlainTextOutputFormat.INSTANCE)
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
    configuration.setTemplateLoader(new ClassTemplateLoader(BuildDockerImageTask.classLoader, "/gocd-docker-${imageType.name()}"))

    Template template = configuration.getTemplate(templatefile, "utf-8")

    def map = [
      distro         : distro,
      distroVersion  : distroVersion,
      project        : project,
      goVersion      : project.goVersion,
      fullVersion    : project.fullVersion,
      gitRevision    : project.gitRevision,
      additionalFiles: additionalFiles,
      imageName      : dockerImageName,
      copyrightYear  : project.copyrightYear,
      useFromArtifact: !project.hasProperty('dockerGitPush')
    ]

    project.mkdir(project.buildDir)

    outputFile.withWriter("utf-8") { writer ->
      template.process(map, writer)
    }
  }

  private GString templateFile() {
    "Dockerfile.${imageType.name()}.ftl"
  }

  File getGitRepoDirectory() {
    project.file("${project.buildDir}/${gitHubRepoName}")
  }

  String getGitHubRepoName() {
    if (imageType == ImageType.agent) {
      if (distro == Distro.docker) {
        return "docker-gocd-agent-dind"
      } else {
        return "docker-gocd-agent-${distro.name()}-${distroVersion.version}"
      }
    }
    if (distro == Distro.alpine) {
      return "docker-gocd-server"
    } else {
      return "docker-gocd-server-${distro.name()}-${distroVersion.version}"
    }
  }

  String getDockerImageName() {
    if (imageType == ImageType.agent) {
      if (distro == Distro.docker) {
        return "gocd-agent-${distro.name()}-dind"
      } else {
        return "gocd-agent-${distro.name()}-${distroVersion.version}"
      }
    }
    if (distro == Distro.alpine) {
      return "gocd-server"
    } else {
      return "gocd-server-${distro.name()}-${distroVersion.version}"
    }
  }

  protected File getDockerfile() {
    project.file("${gitRepoDirectory}/Dockerfile")
  }

  Map<String, Map<String, String>> getAdditionalFiles() {
    return [
      '/usr/local/sbin/tini': [
        url  : "https://github.com/krallin/tini/releases/download/v${tiniVersion}/tini-static-amd64".toString(),
        mode : '0755',
        owner: 'root',
        group: 'root'
      ]
    ]
  }
}
