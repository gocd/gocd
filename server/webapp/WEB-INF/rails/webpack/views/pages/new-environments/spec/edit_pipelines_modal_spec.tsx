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
import {EnvironmentVariables} from "models/new-environments/environment_environment_variables";
import {Pipelines, PipelineWithOrigin} from "models/new-environments/environment_pipelines";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {PipelineGroups, PipelineGroupsJSON} from "models/new-environments/pipeline_groups";
import data from "models/new-environments/spec/test_data";
import {EditPipelinesModal} from "views/pages/new-environments/edit_pipelines_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Edit Pipelines Modal", () => {
  const helper = new TestHelper();

  let modal: EditPipelinesModal;
  let pipelineGroupsJSON: PipelineGroupsJSON;

  beforeEach(() => {
    jasmine.Ajax.install();

    pipelineGroupsJSON = data.pipeline_groups_json();

    const pipelineJSON       = data.pipeline_association_in_xml_json();
    const pipelineWithOrigin = PipelineWithOrigin.fromJSON(pipelineJSON);
    pipelineGroupsJSON.groups[0].pipelines.push(pipelineJSON);

    const anotherEnvPipelines = new Pipelines(pipelineWithOrigin);

    const anotherEnv = new EnvironmentWithOrigin("another", [], [], anotherEnvPipelines, new EnvironmentVariables());

    const pipelines = new Pipelines(
      PipelineWithOrigin.fromJSON(pipelineGroupsJSON.groups[0].pipelines[0]),
      PipelineWithOrigin.fromJSON(pipelineGroupsJSON.groups[1].pipelines[1])
    );

    const environment  = new EnvironmentWithOrigin("UAT", [], [], pipelines, new EnvironmentVariables());
    const environments = new Environments(environment, anotherEnv);

    modal = new EditPipelinesModal(environment, environments);
    modal.pipelinesVM.pipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON));
    helper.mount(() => modal.body());
  });

  afterEach(() => {
    helper.unmount();
    jasmine.Ajax.uninstall();
  });

  it("should render available pipelines", () => {
    const availablePipelinesSection = helper.findByDataTestId(`available-pipelines`);
    const pipeline1Selector         = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const pipeline2Selector         = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;

    expect(availablePipelinesSection).toBeInDOM();
    expect(availablePipelinesSection).toContainText("Available Pipelines");
    expect(helper.findIn(availablePipelinesSection, pipeline1Selector)).toBeInDOM();
    expect(helper.findIn(availablePipelinesSection, pipeline2Selector)).toBeInDOM();
  });

  it("should render pipelines associated with current environment in config repository", () => {
    const configRepoAssociated = helper.findByDataTestId(`pipelines-associated-with-this-environment-in-configuration-repository`);
    const expectedMsg          = "Pipelines associated with this environment in configuration repository:";
    const pipeline1Selector    = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[1].name}`;

    expect(configRepoAssociated).toBeInDOM();
    expect(configRepoAssociated).toContainText(expectedMsg);
    expect(helper.findIn(configRepoAssociated, pipeline1Selector)).toBeInDOM();
  });

  it("should not render config repo pipelines section when no config repo associated pipelines are available", () => {
    modal.pipelinesVM.environment.pipelines().pop();
    m.redraw.sync();

    const configRepoAssociated = helper.findByDataTestId(`pipelines-associated-with-this-environment-in-configuration-repository`);
    expect(configRepoAssociated).not.toBeInDOM();
  });

  it("should render unavailable pipelines which are associated in another environment", () => {
    const otherEnvAssociated = helper.findByDataTestId(`unavailable-pipelines-already-associated-with-environments`);
    const expectedMsg        = "Unavailable pipelines (Already associated with environments):";
    const pipelines          = pipelineGroupsJSON.groups[0].pipelines;
    const pipelineSelector   = `pipeline-list-item-for-${pipelines[2].name}`;

    expect(otherEnvAssociated).toBeInDOM();
    expect(otherEnvAssociated).toContainText(expectedMsg);
    expect(helper.findIn(otherEnvAssociated, pipelineSelector)).toBeInDOM();
  });

  it("should not render unavailable pipelines which are associated in other environment when none present", () => {
    modal.pipelinesVM.pipelineGroups()![0].pipelines().pop();
    m.redraw.sync();

    const otherEnvAssociated = helper.findByDataTestId(`unavailable-pipelines-already-associated-with-environments`);
    expect(otherEnvAssociated).not.toBeInDOM();
  });

  it("should render unavailable pipelines which are defined in config repository", () => {
    const definedInConfigRepo = helper.findByDataTestId(`unavailable-pipelines-defined-in-config-repository`);
    const expectedMsg         = "Unavailable pipelines (Defined in config repository):";
    const pipelineSelector    = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;

    expect(definedInConfigRepo).toBeInDOM();
    expect(definedInConfigRepo).toContainText(expectedMsg);
    expect(helper.findIn(definedInConfigRepo, pipelineSelector)).toBeInDOM();
  });

  it("should not render unavailable pipelines which are defined in config repository when none present", () => {
    //pop twice to remove the second last item.
    modal.pipelinesVM.pipelineGroups()![0].pipelines().pop();
    modal.pipelinesVM.pipelineGroups()![0].pipelines().pop();
    m.redraw.sync();

    const definedInConfigRepo = helper.findByDataTestId(`unavailable-pipelines-defined-in-config-repository`);
    expect(definedInConfigRepo).not.toBeInDOM();
  });

  it("should render pipeline search box", () => {
    const searchInput = helper.findByDataTestId("form-field-input-pipeline-search")[0] as HTMLInputElement;
    expect(searchInput).toBeInDOM();
    expect(searchInput.getAttribute("placeholder")).toEqual("pipeline name");
  });

  it("should bind search text with pipelines vm", () => {
    const searchText = "search-text";
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();
    const searchInput = helper.findByDataTestId("form-field-input-pipeline-search")[0] as HTMLInputElement;
    expect(searchInput).toHaveValue(searchText);
  });

  it("should search for a particular pipeline", () => {
    const selectorForPipeline1 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const selectorForPipeline2 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;
    const selectorForPipeline3 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[2].name}`;
    const selectorForPipeline4 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;
    const selectorForPipeline5 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[1].name}`;

    expect(helper.findByDataTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline5)).toBeInDOM();

    const searchText = pipelineGroupsJSON.groups[0].pipelines[0].name;
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();

    expect(helper.findByDataTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline2)).not.toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline3)).not.toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline4)).not.toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline5)).not.toBeInDOM();
  });

  it("should search for a partial pipeline name match", () => {
    const selectorForPipeline1 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const selectorForPipeline2 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;
    const selectorForPipeline3 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[2].name}`;
    const selectorForPipeline4 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;
    const selectorForPipeline5 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[1].name}`;

    expect(helper.findByDataTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline5)).toBeInDOM();

    const searchText = "pipeline-";
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();

    expect(helper.findByDataTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline5)).toBeInDOM();
  });

  it("should show no pipelines matching search text message when no pipelines matched the search text", () => {
    const selectorForPipeline1 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[0].pipelines[0].name}`;
    const selectorForPipeline2 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[1].name}`;
    const selectorForPipeline3 = `pipeline-list-item-for-${pipelineGroupsJSON.groups[0].pipelines[2].name}`;
    const selectorForPipeline4 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[0].name}`;
    const selectorForPipeline5 = `pipeline-checkbox-for-${pipelineGroupsJSON.groups[1].pipelines[1].name}`;

    expect(helper.findByDataTestId(selectorForPipeline1)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline2)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline3)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline4)).toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline5)).toBeInDOM();

    const searchText = "blah-is-my-pipeline-name";
    modal.pipelinesVM.searchText(searchText);
    m.redraw.sync();

    expect(helper.findByDataTestId(selectorForPipeline1)).not.toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline2)).not.toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline3)).not.toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline4)).not.toBeInDOM();
    expect(helper.findByDataTestId(selectorForPipeline5)).not.toBeInDOM();

    const expectedMessage = "No pipelines matching search text 'blah-is-my-pipeline-name' found!";
    expect(helper.findByDataTestId("flash-message-info")).toContainText(expectedMessage);
  });
});
