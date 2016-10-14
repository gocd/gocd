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

import com.thoughtworks.go.domain.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MyCruisePreferencePageVelocityTemplateTest {
    public static final String TEMPLATE_PATH = "/WEB-INF/vm/mycruise/mycruise-tab.vm";

    @Test
    public void shouldLoadPreferencesPageTemplateWithoutAnException() throws Exception {
        HashMap<String, Object> data = new HashMap<String, Object>();

        User user = new User("name", "display-name", new String[]{"matcher1", "matcher2"}, "email1", true);
        user.populateModel(data);
        data.put("pipelines", "[]");
        data.put("l", null);

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);
        view.setupAdditionalRealTemplate("shared/_header.vm");
        view.setupAdditionalRealTemplate("shared/_breadcrumbs.vm");
        view.setupAdditionalRealTemplate("shared/_copyright_license_info.vm");
        view.setupAdditionalRealTemplate("shared/_footer.vm");
        view.setupAdditionalRealTemplate("shared/_page_intro_top.vm");
        view.setupAdditionalRealTemplate("shared/_page_intro_bottom.vm");
        view.setupAdditionalRealTemplate("mycruise/_notification.vm");


        Document actualDoc = Jsoup.parse(view.render());

        assertThat(actualDoc.getElementById("email").attr("value"), is("email1"));
    }

}
