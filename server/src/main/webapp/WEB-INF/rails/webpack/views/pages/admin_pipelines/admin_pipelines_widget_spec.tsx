/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {
  PipelineGroup,
  PipelineGroups,
  Pipelines,
  PipelineWithOrigin
} from "models/internal_pipeline_structure/pipeline_structure";
import {Origin, OriginType} from "models/origin";
import {Attrs, PipelineGroupsWidget} from "views/pages/admin_pipelines/admin_pipelines_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("PipelineGroupsWidget", () => {
  const helper = new TestHelper();
  let attrs: Attrs;

  beforeEach(() => {
    attrs = {
      createPipelineGroup: jasmine.createSpy("createPipelineGroup"),
      createPipelineInGroup: jasmine.createSpy("createPipelineInGroup"),
      doClonePipeline: jasmine.createSpy("doClonePipeline"),
      doDeleteGroup: jasmine.createSpy("doDeleteGroup"),
      doDeletePipeline: jasmine.createSpy("doDeletePipeline"),
      doDownloadPipeline: jasmine.createSpy("doDownloadPipeline"),
      doEditPipeline: jasmine.createSpy("doEditPipeline"),
      doEditPipelineGroup: jasmine.createSpy("doEditPipelineGroup"),
      doExtractPipeline: jasmine.createSpy("doExtractPipeline"),
      doMovePipeline: jasmine.createSpy("doMovePipeline"),
      pipelineGroups: Stream(new PipelineGroups()),
      onError: jasmine.createSpy("onError"),
      onSuccessfulSave: jasmine.createSpy("onSuccessfulSave"),
    };
  });

  afterEach(() => {
    helper.unmount();
    // ModalManager.closeAll();
  });

  it("should render a message when no pipeline groups are present", () => {
    helper.mount(() => {
      return <PipelineGroupsWidget {...attrs}/>;
    });

    expect(helper.textByTestId("flash-message-info")).toEqual("There are no pipelines defined.");
  });

  it("should render an empty pipeline group", () => {
    attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines()));
    helper.mount(() => {
      return <PipelineGroupsWidget {...attrs}/>;
    });

    expect(helper.textByTestId("flash-message-info"))
      .toContain("There are no pipelines defined in this pipeline group.");

    expect(helper.textByTestId(`pipeline-group-name-foo`)).toEqual("Group: foo");
    expect(helper.textByTestId(`create-pipeline-in-group-foo`)).toEqual("Add new pipeline");
    expect(helper.byTestId(`edit-pipeline-group-foo`)).toBeInDOM();
    expect(helper.byTestId(`delete-pipeline-group-foo`)).toBeInDOM();
  });

  it("should perform pipeline group interactions", () => {
    attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines()));
    helper.mount(() => {
      return <PipelineGroupsWidget {...attrs}/>;
    });

    helper.clickByTestId(`delete-pipeline-group-foo`);
    expect(attrs.doDeleteGroup).toHaveBeenCalled();

    helper.clickByTestId(`edit-pipeline-group-foo`);
    expect(attrs.doEditPipelineGroup).toHaveBeenCalled();

    helper.clickByTestId(`create-pipeline-in-group-foo`);
    expect(attrs.createPipelineInGroup).toHaveBeenCalled();
  });

  it("should perform pipeline interactions for pipelines defined in config", () => {
    const pipelineInXml = new PipelineWithOrigin("in-config", undefined, new Origin(OriginType.GoCD), []);

    attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines(pipelineInXml)));

    helper.mount(() => {
      return <PipelineGroupsWidget {...attrs}/>;
    });

    helper.clickByTestId(`edit-pipeline-in-config`);
    expect(attrs.doEditPipeline).toHaveBeenCalled();

    helper.clickByTestId(`download-pipeline-in-config`);
    expect(attrs.doDownloadPipeline).toHaveBeenCalled();

    helper.clickByTestId(`clone-pipeline-in-config`);
    expect(attrs.doClonePipeline).toHaveBeenCalled();

    helper.clickByTestId(`delete-pipeline-in-config`);
    expect(attrs.doDeletePipeline).toHaveBeenCalled();

    helper.clickByTestId(`extract-template-from-pipeline-in-config`);
    expect(attrs.doExtractPipeline).toHaveBeenCalled();
  });

  it("should perform pipeline interactions for pipelines defined in config repos", () => {
    const pipelineInJson = new PipelineWithOrigin("in-json", undefined, new Origin(OriginType.ConfigRepo, "foo"), []);

    attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines(pipelineInJson)));

    helper.mount(() => {
      return <PipelineGroupsWidget {...attrs}/>;
    });

    helper.clickByTestId(`edit-pipeline-in-json`);
    expect(helper.byTestId(`edit-pipeline-in-json`)).not.toBeDisabled();

    helper.clickByTestId(`download-pipeline-in-json`);
    expect(attrs.doDownloadPipeline).toHaveBeenCalled();

    expect(helper.byTestId(`clone-pipeline-in-json`)).toBeDisabled();
    expect(helper.byTestId(`delete-pipeline-in-json`)).toBeDisabled();
    expect(helper.byTestId(`extract-template-from-pipeline-in-json`)).toBeDisabled();
  });

});
