/* ************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.view.velocity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class BuildDetailPageVelocityTemplateTest {
    public static final String TEMPLATE_PATH = "/WEB-INF/vm/build_detail/build_detail_page.vm";

    @Test
    public void shouldLoadBuildDetailPageTemplateWithoutAnException() throws Exception {
        HashMap<String, Object> data = new HashMap<String, Object>();

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);
        view.setupAdditionalRealTemplate("shared/_header.vm");
        view.setupAdditionalRealTemplate("shared/_footer.vm");
        view.setupAdditionalRealTemplate("shared/_page_intro_top.vm");
        view.setupAdditionalRealTemplate("shared/_page_intro_bottom.vm");
        view.setupAdditionalRealTemplate("shared/_flash_message.vm");
        view.setupAdditionalRealTemplate("shared/_artifacts.vm");
        view.setupAdditionalRealTemplate("shared/_artifact_entry.vm");
        view.setupAdditionalRealTemplate("shared/_package_material_revision_comment.vm");
        view.setupAdditionalRealTemplate("shared/_job_details_breadcrumbs.vm");

        view.setupAdditionalRealTemplate("sidebar/_sidebar_build_list.vm");

        view.setupAdditionalRealTemplate("build_detail/_buildoutput.vm");
        view.setupAdditionalRealTemplate("build_detail/_build_output_raw.vm");
        view.setupAdditionalRealTemplate("build_detail/_tests.vm");
        view.setupAdditionalRealTemplate("build_detail/_test_output_config.vm");
        view.setupAdditionalRealTemplate("build_detail/_failures.vm");
        view.setupAdditionalRealTemplate("build_detail/_artifacts.vm");
        view.setupAdditionalRealTemplate("build_detail/_materials.vm");
        view.setupAdditionalRealTemplate("build_detail/_properties.vm");
        view.setupAdditionalRealTemplate("build_detail/_material_revisions_jstemplate.vm");
        view.setupAdditionalRealTemplate("build_detail/_build_detail_summary_jstemplate.vm");

        view.setupAdditionalFakeTemplate("admin/admin_version.txt.vm", "some-version");

        Document actualDoc = Jsoup.parse(view.render());

        assertThat(actualDoc.select("#footer li.last").first().html(), containsString("some-version"));
    }

}
