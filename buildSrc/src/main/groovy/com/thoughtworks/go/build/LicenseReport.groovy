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

package com.thoughtworks.go.build

import com.github.jk1.license.task.ReportTask
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider

import static com.thoughtworks.go.build.NonSpdxLicense.*
import static com.thoughtworks.go.build.SpdxLicense.*

class LicenseReport {
  private static final Logger LOGGER = Logging.getLogger(LicenseReport.class)

  private static final Set<String> ALLOWED_LICENSES = Set.<License>of(
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
    EPL_2_0,
    FSL_1_1_ALv2,
    FSL_1_1_MIT,
    GPL_2_0_CLASSPATH_EXCEPTION,
    GPL_2_0_UNIVERSAL_FOSS_EXCEPTION,
    ISC,
    LGPL_2_1,
    LGPL_3_0,
    LGPL_3_0_ONLY,
    MIT,
    MPL_1_1,
    MPL_2_0_EPL_1_0,
    OFL_1_1,
    PLEXUS,
    PUBLIC_DOMAIN,
    RUBY,
    UNLICENSE,
  ).collect { it.id }

  private final FileSystemOperations fileOps
  private final ObjectFactory objectFactory

  private final List<ReportTask> licenseReportTasks
  private final Provider<Directory> reportDir

  private final Map<String, Map<String, Object>> licensesForPackagedJarDependencies
  private final Map<String, File> extraEcosystemLicenseFiles
  private final File yarnLicenseReport
  private final GoVersions goVersions

  LicenseReport(FileSystemOperations fileOps, ObjectFactory objectFactory,
                List<ReportTask> licenseReportTasks, Provider<Directory> reportDir,
                Map<String, Map<String, Object>> licensesForPackagedJarDependencies,
                Map<String, File> extraEcosystemLicenseFiles, File yarnLicenseReport, GoVersions goVersions) {
    this.fileOps = fileOps
    this.objectFactory = objectFactory
    this.licenseReportTasks = licenseReportTasks
    this.licensesForPackagedJarDependencies = licensesForPackagedJarDependencies
    this.reportDir = reportDir
    this.extraEcosystemLicenseFiles = extraEcosystemLicenseFiles
    this.yarnLicenseReport = yarnLicenseReport
    this.goVersions = goVersions
  }

  String generate() {
    reportDir.get().file('index.html').asFile.withWriter { out ->
      def markup = new MarkupBuilder(out)

      out << "<!DOCTYPE html>\n"

      markup.html {
        head {
          title("Dependency License Report for GoCD")
          style(type: "text/css", LicenseReport.class.getResourceAsStream("/license-report.css").getText("utf-8"))
        }

        body {
          div(class: "header", "Dependency License Report for GoCD ${goVersions.goVersion}")

          licensesForPackagedJarDependencies.each { String moduleName, Map<String, Object> moduleLicenseData ->
            def moduleNameVersion = "${(moduleLicenseData.moduleName as String).split(':').first()}-${moduleLicenseData.moduleVersion}"
            licenseReportTasks.collect {
              // find a project which contains the specific module
              rt ->
                new File(rt.outputFolder, moduleNameVersion + ".jar")
            }.findAll { dependencyLicenseDir ->
              // Does it have special license files embedded within the jar?
              !objectFactory.fileTree().from(dependencyLicenseDir).isEmpty()
            }.each { File dependencyLicenseDir ->
              // Collate them into the report dir
              fileOps.copy {
                from dependencyLicenseDir
                into reportDir.get().dir(moduleNameVersion)
              }
            }

            renderModuleData(markup, "Java", moduleName, moduleLicenseData)
          }

          this.extraEcosystemLicenseFiles.each {
            new JsonSlurper().parse(it.value, "utf-8").each { String moduleName, Map<String, Object> moduleLicenseData ->
              renderModuleData(markup, it.key, moduleName, moduleLicenseData)
            }
          }

          new JsonSlurper().parse(this.yarnLicenseReport, "utf-8").each { String moduleName, Map<String, Object> moduleLicenseData ->
            def yarnLicenseReportDir = this.yarnLicenseReport.parentFile
            fileOps.copy {
              from "${yarnLicenseReportDir}/${moduleName}-${moduleLicenseData.moduleVersion}"
              into reportDir.get().dir("${moduleName}-${moduleLicenseData.moduleVersion}")
            }

            renderModuleData(markup, "Javascript", moduleName, moduleLicenseData)
          }

          def jreLicense = goVersions.packagedJavaVersion.toLicenseMetadata()
          renderModuleData(markup, "Runtime", jreLicense.moduleName, jreLicense)
        }
      }
    }
  }

  private void renderModuleData(MarkupBuilder template, String moduleEcosystem, String moduleName, Map<String, Object> moduleLicenseData) {
    template.div(class: 'module-info') {
      p(class: "module-header") {
        strong("Ecosystem:")
        span(moduleEcosystem)
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

          if (moduleLicenseData.moduleLicenses != null && !moduleLicenseData.moduleLicenses.isEmpty()) {

            checkIfLicensesAreAllowed(moduleLicenseData.moduleLicenses as List<Map<String, String>>, moduleName, moduleLicenseData.moduleVersion as String)

            p {
              strong("Manifest license(s):")
              moduleLicenseData.moduleLicenses.each { license ->
                def licenseIdentifier = normalizeLicense(license.moduleLicense as String)
                if (license.moduleLicenseUrl != null && !license.moduleLicenseUrl.isBlank()) {
                  a(href: license.moduleLicenseUrl, licenseIdentifier)
                } else {
                  span(licenseIdentifier)
                }
              }
            }
          } else {
            throw new GradleException("Missing license information for ${moduleName}:${moduleLicenseData.moduleVersion}")
          }

          def embeddedLicenseFiles = objectFactory.fileTree().from(reportDir.get().dir("${moduleName.split(':').first()}-${moduleLicenseData.moduleVersion}"))
          if (!embeddedLicenseFiles.isEmpty()) {
            p {
              strong("Embedded license file(s):")
              def baseDir = reportDir.get().asFile
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

  @SuppressWarnings('UnnecessaryQualifiedReference')
  private static String normalizeLicense(String license) {
    return SpdxLicense.normalizedLicense(license) ?: NonSpdxLicense.normalizedLicense(license) ?: license
  }

  private static checkIfLicensesAreAllowed(List<Map<String, String>> moduleLicenses, String moduleName, String moduleVersion) {
    Set<String> licenseNames = moduleLicenses.collect { it.moduleLicense }
    Set<String> normalizedLicenseNames = licenseNames
      .collect { normalizeLicense(it) }
      .findAll { it != null }

    def intersect = ALLOWED_LICENSES.intersect(normalizedLicenseNames, { o1, o2 -> o1.toLowerCase() <=> o2.toLowerCase() })

    if (intersect.isEmpty()) {
      throw new GradleException("License '${licenseNames}' (normalized to '${normalizedLicenseNames}') used by '${moduleName}:${moduleVersion}' are not approved! Allowed licenses are:\n${ALLOWED_LICENSES.collect{"  - ${it}"}.join("\n")}")
    } else {
      LOGGER.debug("License '${licenseNames}' (normalized to '${normalizedLicenseNames}') used by '${moduleName}:${moduleVersion}' is approved because of ${intersect}")
    }
  }

}
