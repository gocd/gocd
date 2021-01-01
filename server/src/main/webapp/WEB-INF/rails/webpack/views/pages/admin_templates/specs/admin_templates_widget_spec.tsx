/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {docsUrl} from "gen/gocd_version";
import _ from "lodash";
import m from "mithril";
import {TemplateSummary} from "models/admin_templates/templates";
import {PipelineStructure} from "models/internal_pipeline_structure/pipeline_structure";
import {ScrollManager} from "views/components/anchor/anchor";
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";
import {AdminTemplatesWidget} from "../admin_templates_widget";

describe('AdminTemplatesSpecs', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  it('should render info message when no templates are passed', () => {
    mount([]);

    const infoMsgElement = helper.byTestId("no-template-present-msg");
    expect(infoMsgElement).toBeInDOM();
    expect(infoMsgElement.innerText).toBe("Either no templates have been set up or you are not authorized to view the same. Learn More");

    expect(helper.q("a", infoMsgElement)).toBeInDOM();
    expect(helper.q("a", infoMsgElement).getAttribute("href")).toBe(docsUrl("configuration/pipeline_templates.html"));
  });

  it('should render error msg if the element specified in scroll manager does not exist', () => {
    let sm: ScrollManager;
    sm = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => "template1"),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };

    mount([], sm);

    const errorMsgElement = helper.byTestId("anchor-template-not-present");
    expect(errorMsgElement).toBeInDOM();
    expect(errorMsgElement.innerText).toBe("Either 'template1' template has not been set up or you are not authorized to view the same. Learn More");

    expect(helper.q("a", errorMsgElement)).toBeInDOM();
    expect(helper.q("a", errorMsgElement).getAttribute("href")).toBe(docsUrl("configuration/pipeline_templates.html"));
  });

  function mount(templates: TemplateSummary.TemplateSummaryTemplate[], sm: ScrollManager = stubAllMethods(["hasTarget"])) {
    const pipelineStructure = new PipelineStructure([], []);
    const editSpy           = jasmine.createSpy("edit_pipeline");
    const scrollOpts        = {
      sm,
      shouldOpenReadOnlyView: false
    };
    helper.mount(() => <AdminTemplatesWidget doEditPipeline={editSpy}
                                             doShowTemplate={_.noop}
                                             templates={templates}
                                             pipelineStructure={pipelineStructure}
                                             editPermissions={_.noop}
                                             onSuccessfulSave={_.noop}
                                             onEdit={_.noop}
                                             onDelete={_.noop}
                                             onError={_.noop}
                                             onCreate={_.noop}
                                             scrollOptions={scrollOpts}/>);
  }
});
