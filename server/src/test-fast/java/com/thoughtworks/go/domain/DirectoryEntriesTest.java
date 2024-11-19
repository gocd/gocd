/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.server.presentation.html.HtmlRenderer;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryEntriesTest {

    @Test
    public void shouldReturnAMessageWhenThereAreNoArtifacts() throws Exception {
        HtmlRenderer renderer = new HtmlRenderer("context");
        DirectoryEntries directoryEntries = new DirectoryEntries();
        directoryEntries.add(new FolderDirectoryEntry("cruise-output", "", new DirectoryEntries()));
        directoryEntries.setIsArtifactsDeleted(true);
        directoryEntries.render(renderer);
        Element document = getRenderedDocument(renderer);
        assertThat(document.getChildren().size()).isEqualTo(2);
        assertThat(document.getChild("p").getTextNormalize()).contains("Artifacts for this job instance are unavailable as they may have been or deleted externally. Re-run the stage or job to generate them again.");
        assertThat(document.getChild("ul").getChild("div").getChild("span").getChild("a").getTextNormalize()).isEqualTo("cruise-output");
    }

    @Test
    public void shouldReturnAMessageWhenAllArtifactsArePurgedIncludingCruiseOutput() throws Exception {
        HtmlRenderer renderer = new HtmlRenderer("context");
        DirectoryEntries directoryEntries = new DirectoryEntries();
        directoryEntries.setIsArtifactsDeleted(true);
        directoryEntries.render(renderer);
        Element document = getRenderedDocument(renderer);
        assertThat(document.getChildren().size()).isEqualTo(1);
        assertThat(document.getChild("p").getTextNormalize()).contains("Artifacts for this job instance are unavailable as they may have been or deleted externally. Re-run the stage or job to generate them again.");
    }

    @Test
    public void shouldReturnAMessageWhenAllArtifactsHaveBeenDeletedButArtifactsDeletedFlagHasNotBeenSet() throws Exception {
        HtmlRenderer renderer = new HtmlRenderer("context");
        DirectoryEntries directoryEntries = new DirectoryEntries();
        directoryEntries.render(renderer);
        Element document = getRenderedDocument(renderer);
        assertThat(document.getChildren().size()).isEqualTo(1);
        assertThat(document.getChild("p").getTextNormalize()).contains("Artifacts for this job instance are unavailable as they may have been or deleted externally. Re-run the stage or job to generate them again.");
    }


    @Test
    public void shouldListAllArtifactsWhenArtifactsNotPurged() throws Exception {
        HtmlRenderer renderer = new HtmlRenderer("context");
        DirectoryEntries directoryEntries = new DirectoryEntries();
        directoryEntries.add(new FolderDirectoryEntry("cruise-output", "", new DirectoryEntries()));
        directoryEntries.add(new FolderDirectoryEntry("some-artifact", "", new DirectoryEntries()));

        directoryEntries.render(renderer);
        Element document = getRenderedDocument(renderer);

        assertThat(document.getChildren().size()).isEqualTo(2);
        Element cruiseOutputElement = document.getChildren().get(0);
        assertThat(cruiseOutputElement.getChild("div").getChild("span").getChild("a").getTextNormalize()).isEqualTo("cruise-output");
        Element artifactElement = document.getChildren().get(1);
        assertThat(artifactElement.getChild("div").getChild("span").getChild("a").getTextNormalize()).isEqualTo("some-artifact");
    }

    @Test
    public void shouldAddFolder() {
        DirectoryEntries directoryEntries = new DirectoryEntries();
        directoryEntries.addFolder("cruise-output");

        assertThat(directoryEntries).contains(new FolderDirectoryEntry("cruise-output", "", new DirectoryEntries()));
    }

    @Test
    public void shouldAddFile() {
        DirectoryEntries directoryEntries = new DirectoryEntries();
        directoryEntries.addFile("console.log", "path");

        assertThat(directoryEntries).contains(new FileDirectoryEntry("console.log", "path"));
    }

    private Element getRenderedDocument(HtmlRenderer renderer) throws JDOMException, IOException {
        String renderedString = "<div>" + renderer.asString() + "</div>";
        return new SAXBuilder().build(new StringReader(renderedString)).getRootElement();
    }

}
