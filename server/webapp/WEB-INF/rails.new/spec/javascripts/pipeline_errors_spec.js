/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END**********************************/
describe("pipeline_errors", function () {
    beforeEach(function () {
        setFixtures("<div id='pipeline_pipeline1_panel' class='pipeline'>\n" +
            "  <div class='pipeline_header'>\n" +
            "    <div class='pipeline_name_link'>\n" +
            "      <a href='/go/tab/pipeline/history/pipeline1'>pipeline1</a>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "  <div class='pipeline_operations'></div>\n" +
            "</div>\n" +
            "<div id='cruise_message_body' class='cruise_message_body'>\n" +
            "  <div class='error' data-pipelines=''>\n" +
            "    <div class='message'>global msg</div>\n" +
            "    <div class='description'>global desc</div>\n" +
            "  </div>\n" +
            "  <div class='error' data-pipelines='[&quot;pipeline1&quot;]'>\n" +
            "    <div class='message'>pipeline1 msg</div>\n" +
            "    <div class='description'>pipeline1 desc</div>\n" +
            "  </div>\n" +
            "</div>");
    });

    beforeEach(function () {
        PipelineErrors.initialize();
    });

    afterEach(function () {
        if (Modalbox.initialized) {
            Modalbox.hide();
        }
    });


    xit("should show error icon in pipeline panel", function () {
        assertEquals(1, jQuery("#pipeline_pipeline1_panel .pipeline_operations .pipeline-error").length);
    });

    xit("should show errors related to pipeline when click error icon in pipeline panel", function () {
        PipelineErrors.showPipelineErrors(jQuery("#pipeline_pipeline1_panel .pipeline_operations .pipeline-error"));
        assertEquals(1, jQuery("#MB_content .cruise_message_body .error").length);
        assertEquals("pipeline1 msg", jQuery("#MB_content .cruise_message_body .error .message").text());
    });
});
