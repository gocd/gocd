/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.FileUtil;


public abstract class TestRepo {

    protected static List<File> tmpFolders = new ArrayList<File>();

    public static void internalTearDown() {
        for (File tmpFolder : tmpFolders) {
            FileUtil.deleteFolder(tmpFolder);
        }
    }

    public abstract String projectRepositoryUrl();

    public void tearDown() {
        TestRepo.internalTearDown();
        onTearDown();
    }

    public void onTearDown() {
    }

    public abstract List<Modification> checkInOneFile(String fileName, String comment) throws Exception;

    public abstract List<Modification> latestModification();

    public void onSetup() throws Exception {

    }

    public abstract Material material();
}


