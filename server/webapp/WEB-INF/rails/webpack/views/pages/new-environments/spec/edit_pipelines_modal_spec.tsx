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
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {PipelineGroups} from "models/new-environments/pipeline_groups";
import data from "models/new-environments/spec/test_data";
import * as simulateEvent from "simulate-event";
import {EditPipelinesModal} from "views/pages/new-environments/edit_pipelines_modal";
import {TestHelper} from "views/pages/spec/test_helper";

const pipelineGroupsJSON = data.pipeline_groups_json();

describe("Edit Pipelines Modal", () => {
  const helper = new TestHelper();

  let modal: EditPipelinesModal;
  beforeEach(() => {
    jasmine.Ajax.install();
    const pipelines = new Pipelines(
      PipelineWithOrigin.fromJSON(pipelineGroupsJSON.groups[0].pipelines[0]),
      PipelineWithOrigin.fromJSON(pipelineGroupsJSON.groups[1].pipelines[1])
    );
    modal           = new EditPipelinesModal(new EnvironmentWithOrigin("up42",
                                                                       [],
                                                                       [],
                                                                       pipelines,
                                                                       new EnvironmentVariables()));
    modal.pipelinesVM.pipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON));
    helper.mount(() => modal.body());
  });

  afterEach(() => {
    helper.unmount();
    jasmine.Ajax.uninstall();
  });

  describe("Edit Pipelines View", () => {
    it("should render all the available pipeline groups", () => {
      expect(helper.findByDataTestId(`form-field-label-${pipelineGroupsJSON.groups[0].name}`)).toBeInDOM();
      expect(helper.findByDataTestId(`form-field-label-${pipelineGroupsJSON.groups[1].name}`)).toBeInDOM();
    });

    it("should render all the available pipeline groups in collapsed form", () => {
      const pipelineGroupCheckboxes = helper.findByDataTestId(`pipeline-group-checkbox`);
      expect(pipelineGroupCheckboxes[0].getAttribute("data-test-expanded-state")).toEqual("off");
      expect(pipelineGroupCheckboxes[1].getAttribute("data-test-expanded-state")).toEqual("off");
    });

    it("should render checkboxes for all the available pipelines", () => {
      const group1 = pipelineGroupsJSON.groups[0];
      const group2 = pipelineGroupsJSON.groups[1];

      expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[0].name}`)).toBeInDOM();
      expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[1].name}`)).toBeInDOM();
      expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[0].name}`)).toBeInDOM();
      expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[1].name}`)).toBeInDOM();
    });

    describe("Pipeline Checkbox States", () => {
      const group1 = pipelineGroupsJSON.groups[0];
      const group2 = pipelineGroupsJSON.groups[1];

      beforeEach(() => {
        const expandCollapsibleToggleIcons = helper.findByDataTestId(`Chevron Right-icon`);
        simulateEvent.simulate(expandCollapsibleToggleIcons[0], "click");
        simulateEvent.simulate(expandCollapsibleToggleIcons[1], "click");
        m.redraw.sync();
      });

      it("should show indeterminate state for group checkbox when some of the pipelines are selected", () => {
        const group1Checkbox = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        const group2Checkbox = helper.findByDataTestId(`form-field-input-${group2.name}`)[0] as HTMLInputElement;

        const group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        const group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        const group2pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group2.pipelines[0].name}`)[0] as HTMLInputElement;
        const group2pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group2.pipelines[1].name}`)[0] as HTMLInputElement;

        //assert some pipelines from each group are selected
        expect(group1pipeline1Checkbox.checked).toEqual(true);
        expect(group1pipeline2Checkbox.checked).toEqual(false);
        expect(group2pipeline1Checkbox.checked).toEqual(false);
        expect(group2pipeline2Checkbox.checked).toEqual(true);

        expect(group1Checkbox.indeterminate).toEqual(true);
        expect(group2Checkbox.indeterminate).toEqual(true);
      });

      it("should show checked state for pipeline group once all pipelines within the group are selected", () => {
        let group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        let group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        let group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(true);
        expect(group1pipeline2Checkbox.checked).toEqual(false);
        expect(group1Checkbox.indeterminate).toEqual(true);

        simulateEvent.simulate(group1pipeline2Checkbox, "click");
        m.redraw.sync();

        group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(true);
        expect(group1pipeline2Checkbox.checked).toEqual(true);
        expect(group1Checkbox.indeterminate).toEqual(false);
        expect(group1Checkbox.checked).toEqual(true);
      });

      it("should show unchecked state for pipeline group once none pipelines within the group are selected", () => {
        let group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        let group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        let group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(true);
        expect(group1pipeline2Checkbox.checked).toEqual(false);
        expect(group1Checkbox.indeterminate).toEqual(true);

        simulateEvent.simulate(group1pipeline1Checkbox, "click");
        m.redraw.sync();

        group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(false);
        expect(group1pipeline2Checkbox.checked).toEqual(false);
        expect(group1Checkbox.indeterminate).toEqual(false);
        expect(group1Checkbox.checked).toEqual(false);
      });

      it("should check all the pipelines within the group on checking the pipeline group checkbox", () => {
        let group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        let group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        let group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(true);
        expect(group1pipeline2Checkbox.checked).toEqual(false);
        expect(group1Checkbox.indeterminate).toEqual(true);

        simulateEvent.simulate(group1Checkbox, "click");
        m.redraw.sync();

        group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(true);
        expect(group1pipeline2Checkbox.checked).toEqual(true);
        expect(group1Checkbox.indeterminate).toEqual(false);
        expect(group1Checkbox.checked).toEqual(true);
      });

      it("should uncheck all the pipelines within the group on un-checking the pipeline group checkbox", () => {
        let group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        let group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        let group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(true);
        expect(group1pipeline2Checkbox.checked).toEqual(false);
        expect(group1Checkbox.indeterminate).toEqual(true);

        //clicking once will check the checkbox, clicking again will uncheck the checkbox
        simulateEvent.simulate(group1Checkbox, "click");
        simulateEvent.simulate(group1Checkbox, "click");
        m.redraw.sync();

        group1Checkbox          = helper.findByDataTestId(`form-field-input-${group1.name}`)[0] as HTMLInputElement;
        group1pipeline1Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[0].name}`)[0] as HTMLInputElement;
        group1pipeline2Checkbox = helper.findByDataTestId(`form-field-input-${group1.pipelines[1].name}`)[0] as HTMLInputElement;

        expect(group1pipeline1Checkbox.checked).toEqual(false);
        expect(group1pipeline2Checkbox.checked).toEqual(false);
        expect(group1Checkbox.indeterminate).toEqual(false);
        expect(group1Checkbox.checked).toEqual(false);
      });
    });

    describe("search", () => {
      const group1 = pipelineGroupsJSON.groups[0];
      const group2 = pipelineGroupsJSON.groups[1];

      it("should render pipeline search box", () => {
        const searchInput = helper.findByDataTestId("form-field-input-pipeline-search")[0] as HTMLInputElement;
        expect(searchInput).toBeInDOM();
        expect(searchInput.getAttribute("placeholder")).toEqual("Search for a pipeline");
      });

      it("should bind search text with pipelines vm", () => {
        const searchText = "search-text";
        modal.pipelinesVM.searchText(searchText);
        m.redraw.sync();
        const searchInput = helper.findByDataTestId("form-field-input-pipeline-search")[0] as HTMLInputElement;
        expect(searchInput).toHaveValue(searchText);
      });

      it("should filter pipelines based on the search text", () => {
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[1].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[1].name}`)).toBeInDOM();

        const searchText = group1.pipelines[0].name;
        modal.pipelinesVM.searchText(searchText);
        m.redraw.sync();

        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[1].name}`)).not.toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[0].name}`)).not.toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[1].name}`)).not.toBeInDOM();
      });

      it("should show no pipelines when none of the pipelines match search text", () => {
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[1].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[1].name}`)).toBeInDOM();

        const searchText = "my-fancy-pipeline-which-does-not-exist";
        modal.pipelinesVM.searchText(searchText);
        m.redraw.sync();

        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[0].name}`)).not.toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[1].name}`)).not.toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[0].name}`)).not.toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[1].name}`)).not.toBeInDOM();
      });

      it("should perform partial text matching for the provided search value", () => {
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[1].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[1].name}`)).toBeInDOM();

        const searchText = "pipeline-";
        modal.pipelinesVM.searchText(searchText);
        m.redraw.sync();

        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group1.pipelines[1].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[0].name}`)).toBeInDOM();
        expect(helper.findByDataTestId(`pipeline-checkbox-for-${group2.pipelines[1].name}`)).toBeInDOM();
      });
    });

    describe("Expand Collapsible", () => {
      it("should render all pipeline groups in collapsed state by default", () => {
        const expandedAttr = "data-test-expanded-state";

        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[0].getAttribute(expandedAttr)).toEqual("off");
        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[1].getAttribute(expandedAttr)).toEqual("off");
      });

      it("should allow toggling the expanded state of a pipeline group", () => {
        const expandedAttr                 = "data-test-expanded-state";
        const expandCollapsibleToggleIcons = helper.findByDataTestId(`Chevron Right-icon`);

        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[0].getAttribute(expandedAttr)).toEqual("off");
        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[1].getAttribute(expandedAttr)).toEqual("off");

        simulateEvent.simulate(expandCollapsibleToggleIcons[0], "click");
        m.redraw.sync();

        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[0].getAttribute(expandedAttr)).toEqual("on");
        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[1].getAttribute(expandedAttr)).toEqual("off");

        simulateEvent.simulate(expandCollapsibleToggleIcons[1], "click");
        m.redraw.sync();

        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[0].getAttribute(expandedAttr)).toEqual("on");
        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[1].getAttribute(expandedAttr)).toEqual("on");
      });

      it("should expand all the pipeline groups when search text is provided", () => {
        const expandedAttr = "data-test-expanded-state";
        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[0].getAttribute(expandedAttr)).toEqual("off");
        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[1].getAttribute(expandedAttr)).toEqual("off");

        const searchText = "pipeline-";
        modal.pipelinesVM.searchText(searchText);
        m.redraw.sync();

        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[0].getAttribute(expandedAttr)).toEqual("on");
        expect(helper.findByDataTestId(`pipeline-group-checkbox`)[1].getAttribute(expandedAttr)).toEqual("on");
      });
    });
  });
});
