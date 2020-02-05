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
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {GeneralOptionsTab} from "views/pages/clicky_pipeline_config/tabs/pipeline/general_options_tab";
import {TestHelper} from "views/pages/spec/test_helper";

describe("GeneralOptionsTag", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render label template", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);
    expect(pipelineConfig.labelTemplate()).toBeUndefined();
    expect(helper.byTestId("label-template")).toHaveValue("");

    helper.oninput(helper.byTestId("label-template"), "${LABEL}");

    expect(pipelineConfig.labelTemplate()).toEqual("${LABEL}");
  });

  describe("Approval", () => {
    it("should render automatic pipeline scheduling checkbox", () => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
      mount(pipelineConfig);

      expect(pipelineConfig.firstStage().approval().typeAsString()).toEqual('manual');
      expect(helper.byTestId("automatic-pipeline-scheduling")).not.toBeChecked();

      helper.clickByTestId("automatic-pipeline-scheduling");

      expect(helper.byTestId("automatic-pipeline-scheduling")).toBeChecked();
      expect(pipelineConfig.firstStage().approval().typeAsString()).toEqual("success");
    });

    it("should render approval from template when template is configured", () => {
      const pipelineConfig  = PipelineConfig.fromJSON(PipelineConfigTestData.withTemplate());
      const templateConfig  = new TemplateConfig(pipelineConfig.template()!, []);
      const stageInTemplate = new Stage("StageOne");
      templateConfig.stages(new NameableSet([stageInTemplate]));
      mount(pipelineConfig, templateConfig);

      expect(stageInTemplate.approval().typeAsString()).toEqual('manual');
      expect(helper.byTestId("automatic-pipeline-scheduling")).not.toBeChecked();

      helper.clickByTestId("automatic-pipeline-scheduling");

      expect(helper.byTestId("automatic-pipeline-scheduling")).toBeChecked();
      expect(stageInTemplate.approval().typeAsString()).toEqual("success");
    });
  });

  it("should render input for cron timer", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);

    expect(pipelineConfig.timer().spec()).toBeUndefined();
    expect(helper.byTestId("cron-timer")).toHaveValue("");

    helper.oninput(helper.byTestId("cron-timer"), "0 0/1 * 1/1 * ? *");

    expect(pipelineConfig.timer().spec()).toEqual("0 0/1 * 1/1 * ? *");
  });

  it("should render checkbox for run only on new material", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);

    expect(pipelineConfig.timer().onlyOnChanges()).toBeUndefined();
    expect(helper.byTestId("run-only-on-new-material")).not.toBeChecked();

    helper.clickByTestId("run-only-on-new-material");

    expect(pipelineConfig.timer().onlyOnChanges()).toBeTrue();
    expect(helper.byTestId("run-only-on-new-material")).toBeChecked();
  });

  it("should render pipeline lock behavior radio buttons", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);

    const unlockWhenFinished  = helper.byTestId("radio-unlockwhenfinished");
    const lockOnFailure       = helper.byTestId("radio-lockonfailure");
    const runMultipleInstance = helper.byTestId("radio-none");

    expect(pipelineConfig.lockBehavior()).toEqual("none");
    expect(unlockWhenFinished).not.toBeChecked();
    expect(lockOnFailure).not.toBeChecked();
    expect(runMultipleInstance).toBeChecked();

    helper.click(unlockWhenFinished);

    expect(pipelineConfig.lockBehavior()).toEqual("unlockWhenFinished");
    expect(unlockWhenFinished).toBeChecked();
    expect(lockOnFailure).not.toBeChecked();
    expect(runMultipleInstance).not.toBeChecked();

    helper.click(lockOnFailure);

    expect(pipelineConfig.lockBehavior()).toEqual("lockOnFailure");
    expect(unlockWhenFinished).not.toBeChecked();
    expect(lockOnFailure).toBeChecked();
    expect(runMultipleInstance).not.toBeChecked();
  });

  function mount(pipelineConfig: PipelineConfig, templateConfig = new TemplateConfig("foo", [])) {
    const routeParams = {} as PipelineConfigRouteParams;
    helper.mount(() => new GeneralOptionsTab().content(pipelineConfig, templateConfig, routeParams, true));
  }
});
