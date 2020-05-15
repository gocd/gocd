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
import {Origin, OriginType} from "models/origin";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {GeneralOptionsTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/general_options_tab";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {OperationState} from "views/pages/page_operations";
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

      expect(helper.byTestId("automatic-pipeline-scheduling")).not.toBeDisabled();

      expect(pipelineConfig.firstStage().approval().typeAsString()).toEqual("manual");
      expect(helper.byTestId("automatic-pipeline-scheduling")).not.toBeChecked();

      helper.clickByTestId("automatic-pipeline-scheduling");

      expect(helper.byTestId("automatic-pipeline-scheduling")).toBeChecked();
      expect(pipelineConfig.firstStage().approval().typeAsString()).toEqual("success");

      const helpText = "If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.";
      expect(helper.qa('span')).toContainText(helpText);
    });

    it("should render approval from template when template is configured", () => {
      const pipelineConfig  = PipelineConfig.fromJSON(PipelineConfigTestData.withTemplate());
      const templateConfig  = new TemplateConfig(pipelineConfig.template()!, []);
      const stageInTemplate = new Stage("StageOne");
      stageInTemplate.approval().state(false);
      templateConfig.stages(new NameableSet([stageInTemplate]));
      mount(pipelineConfig, templateConfig);

      expect(helper.byTestId("automatic-pipeline-scheduling")).toBeDisabled();

      expect(stageInTemplate.approval().typeAsString()).toEqual("manual");
      expect(helper.byTestId("automatic-pipeline-scheduling")).not.toBeChecked();

      const helpText = "If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual. Since this pipeline is based on 'template' template, automatic/manual behaviour of the pipeline is determined by the template's first stage.";
      expect(helper.qa('span')).toContainText(helpText);
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
    expect(helper.byTestId("run-only-on-new-material")).toBeDisabled();

    helper.oninput(helper.byTestId("cron-timer"), "0 0/1 * 1/1 * ? *");
    expect(helper.byTestId("run-only-on-new-material")).not.toBeDisabled();

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

  describe("Read Only", () => {
    beforeEach(() => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
      pipelineConfig.origin(new Origin(OriginType.ConfigRepo, "repo1"));
      mount(pipelineConfig);
    });

    it("should render disabled label template", () => {
      expect(helper.byTestId("label-template")).toBeDisabled();
    });

    it("should render disabled automatic pipeline scheduling", () => {
      expect(helper.byTestId("automatic-pipeline-scheduling")).toBeDisabled();
    });

    it("should render disabled cron timer specification", () => {
      expect(helper.byTestId("cron-timer")).toBeDisabled();
    });

    it("should render disabled run only on new material", () => {
      expect(helper.byTestId("run-only-on-new-material")).toBeDisabled();
    });

    it("should render disabled pipeline locking", () => {
      expect(helper.byTestId("radio-unlockwhenfinished")).toBeDisabled();
      expect(helper.byTestId("radio-lockonfailure")).toBeDisabled();
      expect(helper.byTestId("radio-none")).toBeDisabled();
    });
  });

  function mount(pipelineConfig: PipelineConfig, templateConfig = new TemplateConfig("foo", [])) {
    const routeParams = {} as PipelineConfigRouteParams;
    helper.mount(() => new GeneralOptionsTabContent().content(pipelineConfig,
                                                              templateConfig,
                                                              routeParams,
                                                              Stream<OperationState>(OperationState.UNKNOWN),
                                                              new FlashMessageModelWithTimeout(),
                                                              jasmine.createSpy(),
                                                              jasmine.createSpy()));
  }
});
