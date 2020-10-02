/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import static com.thoughtworks.go.build.NonSpdxLicense.*
import static com.thoughtworks.go.build.SpdxLicense.*

class LicenseReport {

  private static Set<String> LICENSE_EXCEPTIONS = [
    'Apple License',
    'Bouncy Castle Licence',
    'BSD',
    'Custom: https://raw.github.com/bjoerge/deferred.js/master/dist/dfrrd.js',
    'dom4j BSD license',
    'Similar to Apache License but with the acknowledgment clause removed',
    'The OpenSymphony Software License 1.1',
    '(OFL-1.1 AND MIT)',
    'MPL 2.0 or EPL 1.0',
  ]

  private static Set<String> ALLOWED_LICENSES = (LICENSE_EXCEPTIONS + [
    APACHE_1_1,
    APACHE_2_0,
    BSD_0,
    BSD_2_CLAUSE,
    BSD_2_CLAUSE_FREEBSD,
    BSD_3_CLAUSE,
    CDDL_1_0,
    CDDL_1_1,
    EDL_1_0,
    EPL_1_0,
    GPL_2_0_CLASSPATH_EXCEPTION,
    LGPL_2_1,
    LGPL_3_0,
    LGPL_3_0_ONLY,
    MIT,
    MPL_1_1,
    UNLICENSE,
    PUBLIC_DOMAIN,
    ISC
  ].collect { it.id })

  private final Project project
  private final Map<String, Map<String, Object>> licensesForPackagedJarDependencies
  private final AtomicInteger counter
  private final File yarnLicenseReport
  private final File reportDir
  private final File rubygemsLicenseReport

  LicenseReport(Project project, File reportDir, Map<String, Map<String, Object>> licensesForPackagedJarDependencies, File yarnLicenseReport, File rubygemsLicenseReport) {
    this.licensesForPackagedJarDependencies = licensesForPackagedJarDependencies
    this.reportDir = reportDir
    this.project = project
    this.yarnLicenseReport = yarnLicenseReport
    this.rubygemsLicenseReport = rubygemsLicenseReport
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
            def projectWithDependency = rootProject.allprojects.find { Project eachProject ->
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

          new JsonSlurper().parse(LicenseReport.class.getResourceAsStream("/license-for-misc-things.json"), "utf-8").each { String moduleName, Map<String, Object> moduleLicenseData ->
            renderModuleData(markup, counter.incrementAndGet(), moduleName, moduleLicenseData)
          }

          new JsonSlurper().parse(this.rubygemsLicenseReport, "utf-8").each { String moduleName, Map<String, Object> moduleLicenseData ->
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
        strong("Version:")
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

            checkIfLicensesAreAllowed(moduleLicenseData.moduleLicenses, moduleName, moduleLicenseData.moduleVersion)

            p {
              strong("Manifest license URL(s):")
              moduleLicenseData.moduleLicenses.each { license ->
                a(href: license.moduleLicenseUrl, normalizeLicense(license.moduleLicense))
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

  private static String normalizeLicense(String license) {
    return SpdxLicense.normalizedLicense(license) ?: NonSpdxLicense.normalizedLicense(license) ?: license
  }

  private checkIfLicensesAreAllowed(List<Map<String, String>> moduleLicenses, String moduleName, String moduleVersion) {
    Set<String> licenseNames = moduleLicenses.collect { it.moduleLicense }
    Set<String> normalizedLicenseNames = licenseNames
      .collect { normalizeLicense(it) }
      .findAll { it != null }

    def intersect = ALLOWED_LICENSES.intersect(normalizedLicenseNames, new Comparator<String>() {
      @Override
      int compare(String o1, String o2) {
        return o1.toLowerCase() <=> o2.toLowerCase()
      }
    })

    if (intersect.isEmpty()) {
      throw new GradleException("License '${licenseNames}' (normalized to '${normalizedLicenseNames}') used by '${moduleName}:${moduleVersion}' are not approved! Allowed licenses are:\n${ALLOWED_LICENSES.collect{"  - ${it}"}.join("\n")}")
    } else {
      project.getLogger().debug("License '${licenseNames}' (normalized to '${normalizedLicenseNames}') used by '${moduleName}:${moduleVersion}' is approved because of ${intersect}")
    }
  }

}
