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

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.VerifyAction
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable

class DownloadFile extends Download {
  @Input
  @Optional
  Object checksum

  @TaskAction
  @Override
  void download() {

    String expectedChecksum

    if (checksum instanceof Callable) {
      expectedChecksum = checksum.call()
    } else if (checksum instanceof String) {
      expectedChecksum = checksum.trim()
    }

    def shouldDownload = true

    if (dest.exists()) {
      def actualChecksum = dest.withInputStream { is -> DigestUtils.sha256Hex(is) }
      if (expectedChecksum != null) {
        project.logger.info("Verifying checksum. Actual: ${actualChecksum}, expected: ${expectedChecksum}.")
        shouldDownload = actualChecksum != expectedChecksum
      } else {
        shouldDownload = false
      }
    }

    if (shouldDownload) {
      project.logger.info("Attempting download of ${src} into ${dest}")

      super.download()

      project.logger.info("Verifying checksum of ${dest}")

      if (expectedChecksum != null) {
        def action = new VerifyAction(project)
        action.checksum(expectedChecksum)
        action.algorithm('SHA-256')
        action.src(dest)
        action.execute()
      }
    } else {
      project.logger.info("Skipping download of ${src}")
    }
  }
}
