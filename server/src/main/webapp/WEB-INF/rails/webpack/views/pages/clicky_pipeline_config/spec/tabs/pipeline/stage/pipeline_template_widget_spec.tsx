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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Template} from "models/pipeline_configs/templates_cache";
import {PipelineTemplateWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/pipeline_template_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Template Widget", () => {
  const helper         = new TestHelper();
  let pipelineConfig: PipelineConfig;

  beforeEach(() => {
    pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
  });

  afterEach(helper.unmount.bind(helper));

  it("should render templates dropdown", () => {
    mount(["template1"]);

    expect(helper.byTestId("form-field-label-template")).toContainText("Template");

    expect(helper.byTestId("form-field-input-template")).toBeInDOM();

    expect(helper.qa("option")).toHaveLength(1);
    expect(helper.qa("option")[0]).toContainText("template1");
  });

  it("should render no templates found message", () => {
    mount();

    const expectedMsg = "There are no templates configured or you are unauthorized to view the existing templates. Add one via the templates page.";

    const flash = helper.byTestId("flash-message-warning");
    expect(flash).toBeInDOM();
    expect(helper.text("code", flash)).toBe(expectedMsg);
  });

  it("should not render templates dropdown when non exists", () => {
    mount();

    expect(helper.byTestId("form-field-label-template")).not.toBeInDOM();
    expect(helper.byTestId("form-field-input-template")).not.toBeInDOM();
  });

  it("should bind template dropdown to the pipeline config", () => {
    mount(["template1"]);
    expect(helper.byTestId("form-field-input-template")).toHaveValue("");

    pipelineConfig.template("template1");
    m.redraw.sync();

    expect(helper.byTestId("form-field-input-template")).toHaveValue("template1");
  });

  it("should render template view and edit option", () => {
    mount(["template1"]);

    expect(helper.byTestId("view-template")).toBeInDOM();
    expect(helper.byTestId("edit-template")).toBeInDOM();
  });

  it("should not render template view and edit option when no templates are available", () => {
    mount();

    expect(helper.byTestId("view-template")).not.toBeInDOM();
    expect(helper.byTestId("edit-template")).not.toBeInDOM();
  });

  it("should render save and reset buttons", () => {
    mount(["template1"]);

    expect(helper.byTestId("cancel")).toBeInDOM();
    expect(helper.byTestId("save")).toBeInDOM();
  });

  it("should render save confirmation popup on hitting save", () => {
    mount(["template1"]);

    helper.onchange('select', 'template1');

    expect(helper.byTestId("save")).toBeInDOM();

    helper.clickByTestId("save");

    const title = "Confirm Save";
    const body  = "Switching to a template will cause all of the currently defined stages in this pipeline to be lost. Are you sure you want to continue?";
    expect(helper.byTestId("modal-title", document.body)).toContainText(title);
    expect(helper.byTestId("modal-body", document.body)).toContainText(body);

    helper.clickByTestId("cancel-action-button", document.body);
  });

  describe("Read Only", () => {
    beforeEach(() => {
      mount(["template1"], true);
    });

    it("should render disabled template dropdown", () => {
      expect(helper.byTestId("form-field-input-template")).toBeDisabled();
    });

    it("should not render save and reset buttons", () => {
      expect(helper.byTestId("cancel")).not.toBeInDOM();
      expect(helper.byTestId("save")).not.toBeInDOM();
    });
  });

  function mount(templateNames: string[] = [], readonly: boolean = false) {
    helper.mount(() => {
      const templates = templateNames.map(n => ({name: n, parameters: []} as Template));
      return <PipelineTemplateWidget pipelineConfig={pipelineConfig}
                                     readonly={readonly}
                                     isPipelineDefinedOriginallyFromTemplate={Stream<boolean>(true)}
                                     pipelineConfigSave={jasmine.createSpy().and.returnValue(Promise.resolve())}
                                     pipelineConfigReset={jasmine.createSpy().and.returnValue(Promise.resolve())}
                                     templates={Stream(templates)}/>;
    });
  }

});
