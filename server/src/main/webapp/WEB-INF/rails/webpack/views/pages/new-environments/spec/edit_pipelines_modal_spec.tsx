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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {EnvironmentVariables} from "models/environment_variables/types";
import {
  PipelineGroups, Pipelines,
  PipelineStructureJSON,
  PipelineWithOrigin
} from "models/internal_pipeline_structure/pipeline_structure";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import {ModalState} from "views/components/modal";
import {EditPipelinesModal} from "views/pages/new-environments/edit_pipelines_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Edit Pipelines Modal", () => {
  const helper = new TestHelper();
  let modal: EditPipelinesModal;
  let pipelineGroupsJSON: PipelineStructureJSON;

  beforeEach(() => {
    jasmine.Ajax.install();

    pipelineGroupsJSON = data.pipeline_groups_json();

    const pipelineJSON       = data.pipeline_association_in_xml_json();
    const pipelineWithOrigin = PipelineWithOrigin.fromJSON(pipelineJSON);
    pipelineGroupsJSON.groups[0].pipelines.push(pipelineJSON);

    const anotherEnvPipelines = new Pipelines(pipelineWithOrigin);

    const anotherEnv = new EnvironmentWithOrigin("another", true, [], [], anotherEnvPipelines, new EnvironmentVariables());

    const pipelines = new Pipelines(
      PipelineWithOrigin.fromJSON(pipelineGroupsJSON.groups[0].pipelines[0]),
      PipelineWithOrigin.fromJSON(pipelineGroupsJSON.groups[1].pipelines[1])
    );

    const environment  = new EnvironmentWithOrigin("UAT", true, [], [], pipelines, new EnvironmentVariables());
    const environments = new Environments(environment, anotherEnv);

    modal = new EditPipelinesModal(environment, environments, jasmine.createSpy("onSuccessfulSave"));
    modal.pipelinesVM.pipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON.groups));
    helper.mount(() => modal.view());
  });

  afterEach(() => {
    helper.unmount();
    jasmine.Ajax.uninstall();
  });

  it("should render available pipelines", () => {
    const availablePipelinesSection = helper.byTestId(`available-pipelines`);
    const pipeline1Selector         = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const pipeline2Selector         = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;

    expect(availablePipelinesSection).toBeInDOM();
    expect(availablePipelinesSection).toContainText("Available Pipelines");
    expect(helper.byTestId(pipeline1Selector, availablePipelinesSection)).toBeInDOM();
    expect(helper.byTestId(pipeline2Selector, availablePipelinesSection)).toBeInDOM();
  });

  it("should render pipelines associated with current environment in config repository", () => {
    const configRepoAssociated = helper.byTestId(`pipelines-associated-with-this-environment-in-configuration-repository`);
    const expectedMsg          = "Pipelines associated with this environment in configuration repository:";
    const pipelineName         = pipelineGroupsJSON.groups[1].pipelines[1].name;
    const configRepoId         = pipelineGroupsJSON.groups[1].pipelines[1].origin.id;
    const pipeline1Selector    = `pipeline-checkbox-for-${pipelineName}`;
    const configRepoLink       = helper.q("a", helper.byTestId(`pipeline-list-item-for-${pipelineName}`, configRepoAssociated));
    expect(configRepoAssociated).toBeInDOM();
    expect(configRepoAssociated).toContainText(expectedMsg);
    expect(helper.byTestId(pipeline1Selector, configRepoAssociated)).toBeInDOM();

    expect(configRepoLink).toHaveAttr("href", SparkRoutes.ConfigRepoViewPath(configRepoId));
  });

  it("should not render config repo pipelines section when no config repo associated pipelines are available", () => {
    modal.pipelinesVM.environment.pipelines().pop();
    m.redraw.sync();

    const configRepoAssociated = helper.byTestId(`pipelines-associated-with-this-environment-in-configuration-repository`);
    expect(configRepoAssociated).toBeFalsy();
  });

  it("should render unavailable pipelines which are associated in another environment with link", () => {
    const otherEnvAssociated = helper.byTestId(`unavailable-pipelines-already-associated-with-environments`);
    const expectedMsg        = "Unavailable pipelines (Already associated with environments):";
    const pipelines          = pipelineGroupsJSON.groups[0].pipelines;
    const pipelineSelector   = `pipeline-list-item-for-${pipelines[2].name}`;

    expect(otherEnvAssociated).toBeInDOM();
    expect(otherEnvAssociated).toContainText(expectedMsg);
    expect(helper.byTestId(pipelineSelector, otherEnvAssociated)).toBeInDOM();
    expect(helper.q("a", helper.byTestId(pipelineSelector, otherEnvAssociated))).toHaveAttr("href", SparkRoutes.getEnvironmentPathOnSPA("another"));
  });

  it("should not render unavailable pipelines which are associated in other environment when none present", () => {
    modal.pipelinesVM.pipelineGroups()![0].pipelines().pop();
    m.redraw.sync();

    const otherEnvAssociated = helper.byTestId(`unavailable-pipelines-already-associated-with-environments`);
    expect(otherEnvAssociated).toBeFalsy();
  });

  it("should render unavailable pipelines which are defined in config repository", () => {
    const definedInConfigRepo = helper.byTestId(`unavailable-pipelines-defined-in-config-repository`);
    const expectedMsg         = "Unavailable pipelines (Defined in config repository):";
    const pipelineSelector    = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;

    expect(definedInConfigRepo).toBeInDOM();
    expect(definedInConfigRepo).toContainText(expectedMsg);
    expect(helper.byTestId(pipelineSelector, definedInConfigRepo)).toBeInDOM();
  });

  it("should not render unavailable pipelines which are defined in config repository when none present", () => {
    //pop twice to remove the second last item.
    modal.pipelinesVM.pipelineGroups()![0].pipelines().pop();
    modal.pipelinesVM.pipelineGroups()![0].pipelines().pop();
    m.redraw.sync();

    const definedInConfigRepo = helper.byTestId(`unavailable-pipelines-defined-in-config-repository`);
    expect(definedInConfigRepo).toBeFalsy();
  });

  it("should render pipeline search box", () => {
    const searchInput = helper.byTestId("form-field-input-pipeline-search");
    expect(searchInput).toBeInDOM();
    expect(searchInput.getAttribute("placeholder")).toBe("pipeline name");
  });

  it("should bind search text with pipelines vm", () => {
    const searchText = "search-text";
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();
    const searchInput = helper.byTestId("form-field-input-pipeline-search");
    expect(searchInput).toHaveValue(searchText);
  });

  it("should search for a particular pipeline", () => {
    const selectorForPipeline1 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const selectorForPipeline2 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;
    const selectorForPipeline3 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[2].name}`;
    const selectorForPipeline4 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;
    const selectorForPipeline5 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[1].name}`;

    expect(helper.byTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline5)).toBeInDOM();

    const searchText = pipelineGroupsJSON.groups[0].pipelines[0].name;
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();

    expect(helper.byTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline2)).toBeFalsy();
    expect(helper.byTestId(selectorForPipeline3)).toBeFalsy();
    expect(helper.byTestId(selectorForPipeline4)).toBeFalsy();
    expect(helper.byTestId(selectorForPipeline5)).toBeFalsy();
  });

  it("should search for a partial pipeline name match", () => {
    const selectorForPipeline1 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const selectorForPipeline2 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;
    const selectorForPipeline3 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[2].name}`;
    const selectorForPipeline4 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;
    const selectorForPipeline5 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[1].name}`;

    expect(helper.byTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline5)).toBeInDOM();

    const searchText = "pipeline-";
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();

    expect(helper.byTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline5)).toBeInDOM();
  });

  it("should show no pipelines text message when no pipelines are available", () => {
    modal.pipelinesVM.pipelineGroups(new PipelineGroups());
    m.redraw.sync();

    const expectedMessage = "There are no pipelines available!";
    expect(helper.textByTestId("flash-message-info")).toContain(expectedMessage);
  });

  it("should show no pipelines matching search text message when no pipelines matched the search text", () => {
    const selectorForPipeline1 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const selectorForPipeline2 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;
    const selectorForPipeline3 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[2].name}`;
    const selectorForPipeline4 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;
    const selectorForPipeline5 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[1].name}`;

    expect(helper.byTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.byTestId(selectorForPipeline5)).toBeInDOM();

    const searchText = "blah-is-my-pipeline-name";
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();

    expect(helper.byTestId(selectorForPipeline1)).toBeFalsy();
    expect(helper.byTestId(selectorForPipeline2)).toBeFalsy();
    expect(helper.byTestId(selectorForPipeline3)).toBeFalsy();
    expect(helper.byTestId(selectorForPipeline4)).toBeFalsy();
    expect(helper.byTestId(selectorForPipeline5)).toBeFalsy();

    const expectedMessage = "No pipelines matching search text 'blah-is-my-pipeline-name' found!";
    expect(helper.textByTestId("flash-message-info")).toContain(expectedMessage);
  });

  it('should render buttons', () => {
    expect(helper.byTestId("cancel-button")).toBeInDOM();
    expect(helper.byTestId("cancel-button")).toHaveText("Cancel");
    expect(helper.byTestId("save-button")).toBeInDOM();
    expect(helper.byTestId("save-button")).toHaveText("Save");
  });

  it("should render config repo pipelines with config repo link", () => {
    const configRepoLink = helper.q("a", helper.byTestId("unavailable-pipelines-defined-in-config-repository"));
    const configRepoId   = pipelineGroupsJSON.groups[0].pipelines[1].origin.id;
    expect(configRepoLink).toHaveText(configRepoId!);
    expect(configRepoLink.getAttribute("href")).toBe(`/go/admin/config_repos#!/${configRepoId}`);
  });

  it('should disable save and cancel button if modal state is loading', () => {
    modal.modalState = ModalState.LOADING;
    m.redraw.sync();
    expect(helper.byTestId("save-button")).toBeDisabled();
    expect(helper.byTestId("cancel-button")).toBeDisabled();
    expect(helper.byTestId("spinner")).toBeInDOM();
  });
});
