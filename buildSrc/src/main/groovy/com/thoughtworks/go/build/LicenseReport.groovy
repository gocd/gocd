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

package com.thoughtworks.go.build

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicInteger

class LicenseReport {
  private final Project project
  private final Map<String, Map<String, Object>> licensesForPackagedJarDependencies
  private final AtomicInteger counter
  private final File yarnLicenseReport
  private final File reportDir

  LicenseReport(Project project, File reportDir, Map<String, Map<String, Object>> licensesForPackagedJarDependencies, File yarnLicenseReport) {
    this.licensesForPackagedJarDependencies = licensesForPackagedJarDependencies
    this.reportDir = reportDir
    this.project = project
    this.yarnLicenseReport = yarnLicenseReport
    this.counter = new AtomicInteger(0)
  }

  String generate() {
    def rootProject = project.rootProject
    project.file("${reportDir}/index.html").withWriter { out ->
      def markup = new MarkupBuilder(out)

      out << "<!DOCTYPE html>\n"

      markup.html {
        head {
          title("Dependency License Report for GoCD")
          style(type: "text/css", LicenseReport.class.getResourceAsStream("/license-report.css").getText("utf-8"))
        }

        body {
          div(class: "header", "Dependency License Report for GoCD ${project.version}")

          licensesForPackagedJarDependencies.each { String moduleName, Map<String, Object> moduleLicenseData ->
            // find what project contains the specific module
            def additionalFiles = []
            def projectWithDependency = rootProject.subprojects.find { Project eachProject ->
              additionalFiles = eachProject.fileTree("${eachProject.licenseReport.outputDir}/${moduleLicenseData.moduleName.split(':').first()}-${moduleLicenseData.moduleVersion}.jar")
              !additionalFiles.isEmpty()
            }

            // copy the embedded license files next to the report dir
            if (projectWithDependency != null) {
              project.copy {
                from project.file("${projectWithDependency.licenseReport.outputDir}/${moduleLicenseData.moduleName.split(':').first()}-${moduleLicenseData.moduleVersion}.jar")
                into "${reportDir}/${moduleLicenseData.moduleName.split(':').first()}-${moduleLicenseData.moduleVersion}"
              }
            }

            renderModuleData(markup, counter.incrementAndGet(), moduleName, moduleLicenseData)
          }

          new JsonSlurper().parse(this.yarnLicenseReport, "utf-8").each { String moduleName, Map<String, Object> moduleLicenseData ->
            def yarnLicenseReportDir = project.file(this.yarnLicenseReport).parentFile
            project.copy {
              from "${yarnLicenseReportDir}/${moduleName}-${moduleLicenseData.moduleVersion}"
              into "${reportDir}/${moduleName}-${moduleLicenseData.moduleVersion}"
            }

            renderModuleData(markup, counter.incrementAndGet(), moduleName, moduleLicenseData)
          }

          new JsonSlurper().parse(LicenseReport.class.getResourceAsStream("/license-for-javascript-not-in-yarn.json"), "utf-8").each { String moduleName, Map<String, Object> moduleLicenseData ->
            renderModuleData(markup, counter.incrementAndGet(), moduleName, moduleLicenseData)
          }

          div(class: "footer") {
            span("This report was generated at ")
            span(class: "timestamp", new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC")))
            span(".")
          }
        }
      }
    }
  }

  private void renderModuleData(MarkupBuilder template, int counter, String moduleName, Map<String, Object> moduleLicenseData) {
    template.div(class: 'module-info') {
      p(class: "module-header") {
        strong("${counter}. ")
        strong("Name:")
        span(moduleName)
        strong(" Version:")
        span(moduleLicenseData.moduleVersion)
      }

      div(class: "module-info-body") {
        div {
          if (moduleLicenseData.moduleUrls != null && !moduleLicenseData.moduleUrls.isEmpty()) {
            p {
              strong("Project URL(s):")
              moduleLicenseData.moduleUrls.each { eachUrl ->
                a(href: eachUrl, eachUrl)
              }
            }
          }

          if (moduleLicenseData.moduleLicenses != null && !moduleLicenseData.moduleLicenses.moduleLicenses.isEmpty()) {
            p {
              strong("Manifest license URL(s):")
              moduleLicenseData.moduleLicenses.each { license ->
                a(href: license.moduleLicenseUrl, license.moduleLicense)
              }
            }
          } else {
            throw new GradleException("Missing license information for ${moduleName}:${moduleLicenseData.moduleVersion}")
          }

          def string = "${reportDir}/${moduleName.split(':').first()}-${moduleLicenseData.moduleVersion}"
          def embeddedLicenseFiles = project.fileTree(string).files

          if (!embeddedLicenseFiles.isEmpty()) {
            p {
              strong("Embedded license file(s):")
              def baseDir = project.file(reportDir)
              embeddedLicenseFiles.each { File eachLicenseFile ->
                def relativePath = baseDir.toURI().relativize(eachLicenseFile.toURI())
                a(href: relativePath, relativePath)
              }
            }
          }
        }
      }
    }
  }


}
