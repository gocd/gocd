/* ************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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
 * ************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.domain.Matcher;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MyGoControllerTest {
    public static final String USERNAME = "sriki";
    public static final long USERID = 1L;
    @Mock UserService userService;
    @Mock SecurityService securityService;
    @Mock PipelineConfigService pipelineConfigService;
    @Mock Localizer localizer;
    private MyGoController controller;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        controller = new MyGoControllerWithMockedUser(userService, pipelineConfigService, localizer);
    }

    @Test
    public void shouldRenderPipelineStageCombinationJSONSortedByPipelineNameIgnoringCase() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        User user = new User(USERNAME, "Srikanth Maga", new String[]{"rope", "srikanth"}, "sriki@tw.com", true);
        user.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.All, true));
        user.addNotificationFilter(new NotificationFilter("p2", "s2", StageEvent.Fails, true));
        when(userService.load(USERID)).thenReturn(user);

        List<PipelineConfigs> groups = new ArrayList<PipelineConfigs>();
        groups.add(PipelineConfigMother.createGroup("g3", PipelineConfigMother.createPipelineConfigWithStages("pipeline3-1", "stage3-1")));
        groups.add(PipelineConfigMother.createGroup("g1", PipelineConfigMother.createPipelineConfigWithStages("PIPELINE2-1", "stage2-1")));
        groups.add(PipelineConfigMother.createGroup("g1", PipelineConfigMother.createPipelineConfigWithStages("pipeline1-1", "stage1-1", "stage1-2")));

        when(pipelineConfigService.viewableGroupsFor(new Username(new CaseInsensitiveString(user.getName())))).thenReturn(groups);

        ModelAndView modelAndView = controller.handleRequest(null, request);

        assertThat((Matcher) modelAndView.getModel().get("matchers"), is(new Matcher("rope,srikanth")));
        assertThat((String) modelAndView.getModel().get("email"), is("sriki@tw.com"));
        assertThat((Boolean) modelAndView.getModel().get("emailMe"), is(true));
        assertThat((List<NotificationFilter>) modelAndView.getModel().get("notificationFilters"), is(user.getNotificationFilters()));
        assertThat((Localizer) modelAndView.getModel().get("l"), is(localizer));

        assertThat((String) modelAndView.getModel().get("pipelines"),
                is("[{\"name\":\"" + GoConstants.ANY_PIPELINE + "\",\"stages\":[{\"stageName\":\"" + GoConstants.ANY_STAGE + "\"}]},"
                        + "{\"name\":\"pipeline1-1\",\"stages\":[{\"stageName\":\"" + GoConstants.ANY_STAGE + "\"},{\"stageName\":\"stage1-1\"},{\"stageName\":\"stage1-2\"}]},"
                        + "{\"name\":\"PIPELINE2-1\",\"stages\":[{\"stageName\":\"" + GoConstants.ANY_STAGE + "\"},{\"stageName\":\"stage2-1\"}]},"
                        + "{\"name\":\"pipeline3-1\",\"stages\":[{\"stageName\":\"" + GoConstants.ANY_STAGE + "\"},{\"stageName\":\"stage3-1\"}]}]"));
    }

    private class MyGoControllerWithMockedUser extends MyGoController {
        public MyGoControllerWithMockedUser(UserService userService, PipelineConfigService pipelineConfigService, Localizer localizer) {
            super(userService, pipelineConfigService, localizer);
        }

        @Override
        protected Long getUserId(HttpServletRequest request) {
            return USERID;
        }

        @Override
        protected Username getUserName() {
            return new Username(new CaseInsensitiveString(USERNAME));
        }
    }
}
