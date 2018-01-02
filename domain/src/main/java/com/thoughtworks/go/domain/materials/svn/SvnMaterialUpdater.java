/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static java.lang.String.format;

public class SvnMaterialUpdater {
   private SvnMaterial material;

   public SvnMaterialUpdater(SvnMaterial material) {
      this.material = material;
   }

   public BuildCommand updateTo(String baseDir, RevisionContext revisionContext) {
      Revision revision = revisionContext.getLatestRevision();
      String workingDir = material.workingdir(new File(baseDir)).getPath();
      UrlArgument url = material.getUrlArgument();

      return compose(
              echoWithPrefix(format("Start updating %s at revision %s from %s", material.updatingTarget(), revision.getRevision(), url.forDisplay())),
              secret(url.forCommandline(), url.forDisplay()),
              secret(material.getPassword(), "*********************"),
              cleanupAndUpdate(workingDir, revision).setTest(shouldDoCleanupAndUpdate(workingDir)),
              freshCheckout(workingDir, revision).setTest(isNotRepository(workingDir)),
              freshCheckout(workingDir, revision).setTest(test("-nd", workingDir)),
              freshCheckout(workingDir, revision).setTest(repoUrlChanged(workingDir)),
              echoWithPrefix(format("Done.\n"))
      );
   }

   private BuildCommand freshCheckout(String workingDir, Revision revision) {
      return compose(
              echoWithPrefix("Checking out a fresh copy"),
              cleandir(workingDir).setTest(test("-d", workingDir)),
              mkdirs(workingDir).setTest(test("-nd", workingDir)),
              checkout(workingDir, revision)
      );
   }

   private BuildCommand checkout(String workingDir, Revision revision) {
       ArrayList<String> args = new ArrayList<>();
       addCredentials(args);
       args.add("checkout");
       args.add("--non-interactive");
       args.add("-r");
       args.add(revision.getRevision());
       args.add(material.getUrlArgument().forCommandline());
       args.add(workingDir);
       return exec("svn", args.toArray(new String[args.size()]));
   }

   private BuildCommand update(String workingDir, Revision revision) {
       ArrayList<String> args  = new ArrayList<>();
       addCredentials(args);
       args.add("update");
       args.add("--non-interactive");
       args.add("-r");
       args.add(revision.getRevision());
       args.add(workingDir);
       return exec("svn", args.toArray(new String[args.size()]));
   }

   private void addCredentials(ArrayList<String> argList) {
       String userName = material.getUserName();
       if (!StringUtils.isBlank(userName)) {
           argList.add("--username");
           argList.add(userName);
           if (!StringUtils.isBlank(material.getPassword())) {
               argList.add("--password");
               argList.add(material.getPassword());
           }
       }
   }

   private BuildCommand cleanupAndUpdate(String workingDir, Revision revision) {
      return compose(
              echo("[SVN] Cleaning up working directory %s", workingDir),
              exec("svn", "cleanup", workingDir),
              exec("svn", "revert", "--recursive", workingDir),
              echo("[SVN] Updating working copy to revision %s", revision.getRevision()),
              update(workingDir, revision)
      );
   }

   private BuildCommand shouldDoCleanupAndUpdate(String workingDir) {
      return compose(
              test("-d", workingDir),
              isRepository(workingDir),
              repoUrlUnchanged(workingDir)
      );
   }

   private BuildCommand info(String workingDir) {
      return exec("svn", "info", "--non-interactive").setWorkingDirectory(workingDir);
   }

   private BuildCommand repoUrlChanged(String workingDir) {
       return test("-nin", material.getUrl(), info(workingDir));
   }

    private BuildCommand repoUrlUnchanged(String workingDir) {
        return test("-in", material.getUrl(), info(workingDir));
    }

   private BuildCommand isRepository(String workingDir) {
      return test("-d", new File(workingDir, ".svn").getPath());
   }

   private BuildCommand isNotRepository(String workingDir) {
      return test("-nd", new File(workingDir, ".svn").getPath());
   }
}
