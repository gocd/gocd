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

import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineGroup, PipelineGroups, PipelineJSON, Pipelines, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {Origin, OriginType} from "models/origin";
import {ScrollManager} from "views/components/anchor/anchor";
import {Attrs, PipelineGroupsWidget} from "views/pages/admin_pipelines/admin_pipelines_widget";
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";

describe("PipelineGroupsWidget", () => {
  const helper = new TestHelper();
  let attrs: Attrs;

  beforeEach(() => {
    attrs = {
      createPipelineGroup:   jasmine.createSpy("createPipelineGroup"),
      createPipelineInGroup: jasmine.createSpy("createPipelineInGroup"),
      doClonePipeline:       jasmine.createSpy("doClonePipeline"),
      doDeleteGroup:         jasmine.createSpy("doDeleteGroup"),
      doDeletePipeline:      jasmine.createSpy("doDeletePipeline"),
      doDownloadPipeline:    jasmine.createSpy("doDownloadPipeline"),
      doEditPipeline:        jasmine.createSpy("doEditPipeline"),
      doEditPipelineGroup:   jasmine.createSpy("doEditPipelineGroup"),
      doExtractPipeline:     jasmine.createSpy("doExtractPipeline"),
      doMovePipeline:        jasmine.createSpy("doMovePipeline"),
      pipelineGroups:        Stream(new PipelineGroups()),
      onError:               jasmine.createSpy("onError"),
      onSuccessfulSave:      jasmine.createSpy("onSuccessfulSave"),
      scrollOptions:         {sm: stubAllMethods(["hasTarget", "shouldScroll"]), shouldOpenEditView: false}
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

    expect(helper.textByTestId("flash-message-info")).toEqual("Either no pipelines have been defined or you are not authorized to view the same. Learn More");
    expect(helper.q("a", helper.byTestId("flash-message-info")).getAttribute("href")).toEqual(docsUrl("configuration/pipelines.html"));
  });

  it("should render an empty pipeline group", () => {
    attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines()));
    helper.mount(() => {
      return <PipelineGroupsWidget {...attrs}/>;
    });

    expect(helper.textByTestId("flash-message-info")).toContain("There are no pipelines defined in this pipeline group.");

    expect(helper.textByTestId(`pipeline-group-name-foo`)).toEqual("Pipeline Group:foo");
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
    const pipelineInXml = new PipelineWithOrigin("in-config", undefined, new Origin(OriginType.GoCD), [], null , []);

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
    const pipelineInJson = new PipelineWithOrigin("in-json", undefined, new Origin(OriginType.ConfigRepo, "foo"), [], null, []);

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

  it('should render error msg if the the anchor element is not found', () => {
    let scrollManager: ScrollManager;
    scrollManager          = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => "grp-test"),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };
    attrs.scrollOptions.sm = scrollManager;

    helper.mount(() => {
      return <PipelineGroupsWidget {...attrs}/>;
    });

    const anchorErrorMsgElement = helper.byTestId("anchor-pipeline-grp-not-present");
    expect(anchorErrorMsgElement).toBeInDOM();
    expect(anchorErrorMsgElement.innerText).toBe("Either 'grp-test' pipeline group has not been set up or you are not authorized to view the same. Learn More");

    expect(helper.q("a", anchorErrorMsgElement)).toBeInDOM();
    expect(helper.q("a", anchorErrorMsgElement).getAttribute("href")).toBe(docsUrl("configuration/pipeline_group_admin_config.html"));
  });

  describe('PipelineWidget', () => {
    let pipelineJSON: PipelineJSON;

    beforeEach(() => {
      pipelineJSON = {
        name:                "some-pipeline",
        origin:              {type: OriginType.GoCD},
        stages:              [],
        environment:         null,
        dependant_pipelines: []
      };
    });

    it('should be able to delete pipeline', () => {
      attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines(PipelineWithOrigin.fromJSON(pipelineJSON))));
      helper.mount(() => {
        return <PipelineGroupsWidget {...attrs}/>;
      });

      const deletePipelineElement = helper.byTestId(`delete-pipeline-${pipelineJSON.name}`);

      expect(deletePipelineElement).not.toBeDisabled();
      expect(deletePipelineElement).toHaveAttr('title', "Delete pipeline 'some-pipeline'");
    });

    it('should not be able to delete pipeline if defined remotely', () => {
      pipelineJSON.origin.type = OriginType.ConfigRepo;
      pipelineJSON.origin.id   = 'config-repo-id';
      attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines(PipelineWithOrigin.fromJSON(pipelineJSON))));
      helper.mount(() => {
        return <PipelineGroupsWidget {...attrs}/>;
      });

      const deletePipelineElement = helper.byTestId(`delete-pipeline-${pipelineJSON.name}`);

      expect(deletePipelineElement).toBeDisabled();
      expect(deletePipelineElement).toHaveAttr('title', "Cannot delete pipeline 'some-pipeline' defined in configuration repository 'config-repo-id'.");
    });

    it('should not be able to delete pipeline if added in an environment', () => {
      pipelineJSON.environment = 'test_environment';
      attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines(PipelineWithOrigin.fromJSON(pipelineJSON))));
      helper.mount(() => {
        return <PipelineGroupsWidget {...attrs}/>;
      });

      const deletePipelineElement = helper.byTestId(`delete-pipeline-${pipelineJSON.name}`);

      expect(deletePipelineElement).toBeDisabled();
      expect(deletePipelineElement).toHaveAttr('title', "Cannot delete pipeline 'some-pipeline' as it is present in environment 'test_environment'.");
    });

    it('should not be able to delete pipeline if there are dependant pipelines', () => {
      pipelineJSON.dependant_pipelines = ['pipeline1', 'pipeline2'];
      attrs.pipelineGroups().push(new PipelineGroup("foo", new Pipelines(PipelineWithOrigin.fromJSON(pipelineJSON))));
      helper.mount(() => {
        return <PipelineGroupsWidget {...attrs}/>;
      });

      const deletePipelineElement = helper.byTestId(`delete-pipeline-${pipelineJSON.name}`);

      expect(deletePipelineElement).toBeDisabled();
      expect(deletePipelineElement).toHaveAttr('title', "Cannot delete pipeline 'some-pipeline' as pipeline(s) 'pipeline1,pipeline2' depends on it.");
    });
  });
});
