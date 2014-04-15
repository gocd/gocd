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

package com.thoughtworks.studios.shine.cruise.stage.details;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.UUID;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StageStorage implements StageGraphLoader {
    private String baseDir;
    private static final String EXTENSION = ".n3.gz";

    @Autowired
    public StageStorage(SystemEnvironment systemEnvironment) {
        this(systemEnvironment.shineDb().getAbsolutePath() + "/rdf-files/");
    }

    public StageStorage(String baseDir) {
        this.baseDir = baseDir;
    }

    // File writes are not atomic in nature. There can be cases where the file is not completely written to disk.
    // This is why we write to a tmp file and move it over to the actual file. File moves are atomic operations.
    // If rename failed because target file already exists, we just ignore it because some other thread may have generated it.
    public void save(Graph graph) {
        StageIdentifier identifier = extractStageIdentifier(graph);

        if (isStageStored(identifier)) {
            return;
        }

        synchronized (stageKey(identifier)) {
            if (isStageStored(identifier)) {
                return;
            }

            File file = new File(tmpStagePath(identifier));

            file.getParentFile().mkdirs();
            try {
                OutputStream os = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
                graph.persistToTurtle(os);
                os.flush();
                os.close();
                file.renameTo(new File(stagePath(identifier)));
            } catch (IOException e) {
                throw new ShineRuntimeException(e);
            } finally {
                file.delete();
            }
        }
    }

    private String stageKey(StageIdentifier identifier) {
        return identifier.getStageLocator().intern();
    }

    private String tmpStagePath(StageIdentifier stageIdentifier) {
        return stagePath(stageIdentifier) + "." + UUID.randomUUID();
    }

    private StageIdentifier extractStageIdentifier(Graph graph) {
        String select = "" +
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                "SELECT ?pipelineName ?pipelineCounter ?stageName ?stageCounter WHERE {" +
                "  ?pipeline a cruise:Pipeline ." +
                "  ?pipeline cruise:pipelineName ?pipelineName ." +
                "  ?pipeline cruise:pipelineCounter ?pipelineCounter ." +
                "  ?pipeline cruise:hasStage ?stage ." +
                "  ?stage cruise:stageName ?stageName ." +
                "  ?stage cruise:stageCounter ?stageCounter ." +
                "}";

        BoundVariables bv = graph.selectFirst(select);

        if (bv == null) {
            throw new ShineRuntimeException("Cannot save a stage graph without stage identification information!");
        }

        StageIdentifier stageIdentifier = new StageIdentifier(bv.getString("pipelineName"), bv.getInt("pipelineCounter")
                , bv.getString("stageName"), bv.getString("stageCounter"));
        return stageIdentifier;
    }

    public Graph load(StageIdentifier stageIdentifier) {
        String fileName = stagePath(stageIdentifier);

        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(fileName));

            Graph tempGraph = new InMemoryTempGraphFactory().createTempGraph();
            tempGraph.addTriplesFromTurtle(inputStream);
            inputStream.close();
            return tempGraph;
        } catch (IOException e) {
            throw new ShineRuntimeException(String.format("Unable to read stage n3 file for " + stageIdentifier + "(file: %s). This should never happen.", fileName), e);
        }

    }

    public boolean isStageStored(StageIdentifier stageIdentifier) {
        return new File(stagePath(stageIdentifier)).exists();
    }

    public boolean isStageStored(Graph graph) {
        return isStageStored(extractStageIdentifier(graph));
    }

    private String stagePath(StageIdentifier stageIdentifier) {
        return baseDir + File.separator + stageIdentifier.stageLocator() + EXTENSION;
    }

    // with great power comes great responsibility
    public void clear() {
        FileUtils.deleteQuietly(new File(baseDir));
    }
}

