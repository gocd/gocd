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

import Stream from "mithril/stream";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {GeneralOptionsTabContent} from "views/pages/clicky_pipeline_config/tabs/template/general_tab_content";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Template General Tab Content", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render template name", () => {
    const template = new TemplateConfig("test", []);
    mount(template);

    expect(template.name()).toBe("test");
    expect(helper.byTestId("template-name")).toHaveValue("test");
    expect(helper.byTestId("template-name")).toBeDisabled();
  });

  function mount(template: TemplateConfig) {
    const routeParams = {} as PipelineConfigRouteParams;

    helper.mount(() => {
      return new GeneralOptionsTabContent().content(template,
                                                    {} as TemplateConfig,
                                                    routeParams,
                                                    Stream<OperationState>(OperationState.UNKNOWN),
                                                    new FlashMessageModelWithTimeout(),
                                                    jasmine.createSpy(),
                                                    jasmine.createSpy());
    });
  }
});
