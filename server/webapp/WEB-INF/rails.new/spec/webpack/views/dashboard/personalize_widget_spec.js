/*
 * Copyright 2018 ThoughtWorks, Inc.
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
describe("Dashboard Personalize Widget", () => {
  const m      = require("mithril");
  const $      = require("jquery");
  const _      = require("lodash");
  const Stream = require("mithril/stream");

  const PersonalizeWidget   = require("views/dashboard/personalize_widget");
  const PipelineSelectionVM = require("views/dashboard/models/pipeline_selection_view_model");

  const PipelineSelection = require('models/dashboard/pipeline_selection');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const json = {
    "pipelines":  {
      "first":  ["up42"],
      "second": ["up43", "up44"]
    },
    "selections": ["up42", "up44"],
    "blacklist":  true
  };

  let pipelineSelection, vm;
  let doRefreshImmediately, hideSelectionDropdown;

  beforeEach(() => {
    pipelineSelection     = Stream(PipelineSelection.fromJSON(json));
    vm                    = new PipelineSelectionVM();
    doRefreshImmediately  = jasmine.createSpy('do-refresh-immediately');
    hideSelectionDropdown = jasmine.createSpy('hide-selection');
    vm.initialize(pipelineSelection().pipelineGroups());

    m.mount(root, {
      view() {
        return m(PersonalizeWidget, {
          pipelineSelection,
          doRefreshImmediately,
          hideSelectionDropdown,
          vm
        });
      }
    });
    m.redraw(true);
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should render personalize view", () => {
    expect($root.find('.filter_options')).toBeInDOM();
  });

  it("should render page spinner in case of empty pipeline selections", () => {
    pipelineSelection(undefined);
    m.redraw();

    expect($root.find('.page-spinner')).toBeInDOM();
  });

  it("should render selection buttons", () => {
    expect($root.find('.select-all')).toBeInDOM();
    expect($root.find('.select-none')).toBeInDOM();
  });

  it("should render blacklist checkbox", () => {
    expect($root.find('#show-newly-created-pipelines')).toBeInDOM();
    expect($root.find('#show-newly-created-pipelines')).toBeChecked();
  });

  it("should bind blacklist checkbox to pipeline selection model", () => {
    expect(pipelineSelection().blacklist()).toBe(true);
    expect($root.find('#show-newly-created-pipelines')).toBeChecked();

    $root.find('#show-newly-created-pipelines').click();

    expect(pipelineSelection().blacklist()).toBe(false);
    expect($root.find('#show-newly-created-pipelines')).not.toBeChecked();
  });

  it("should render pipeline group checkboxes", () => {
    const pipelineGroups = $root.find('.pgroup-cb');

    expect(pipelineGroups).toHaveLength(2);
    expect(pipelineGroups.get(0)).toHaveId('pgroup_first');
    expect(pipelineGroups.get(1)).toHaveId('pgroup_second');
  });

  it("should render all pipelines from expanded group", () => {
    const pipelines = $root.find('.pipeline-cb');

    expect(pipelines).toHaveLength(1);

    expect(pipelines.get(0)).toHaveId('pipeline_up42');
  });

  it('should bind pipeline checkbox to pipeline selection model', () => {
    expect(pipelineSelection().isPipelineSelected('up42')).toBe(true);
    expect($root.find('#pipeline_up42')).toBeChecked();

    $root.find('#pipeline_up42').click();

    expect(pipelineSelection().isPipelineSelected('up42')).toBe(false);
    expect($root.find('#pipeline_up42')).not.toBeChecked();
  });

  it('should select all pipelines when pipeline group is checked', () => {
    $root.find('.arrow-right').get(0).click();

    expect($root.find('#pgroup_second')).not.toBeChecked();
    expect($root.find('#pipeline_up43')).not.toBeChecked();
    expect($root.find('#pipeline_up44')).toBeChecked();

    $root.find('#pgroup_second').click();

    expect($root.find('#pgroup_second')).toBeChecked();
    expect($root.find('#pipeline_up43')).toBeChecked();
    expect($root.find('#pipeline_up44')).toBeChecked();
  });

  it('should select none pipelines when pipeline group is unchecked', () => {
    expect($root.find('#pgroup_first')).toBeChecked();
    expect($root.find('#pipeline_up42')).toBeChecked();

    $root.find('#pgroup_first').click();

    expect($root.find('#pgroup_first')).not.toBeChecked();
    expect($root.find('#pipeline_up42')).not.toBeChecked();
  });

  it('should select all pipelines from selection', () => {
    $root.find('.arrow-right').get(0).click();

    expect($root.find('#pipeline_up42')).toBeChecked();
    expect($root.find('#pipeline_up43')).not.toBeChecked();
    expect($root.find('#pipeline_up44')).toBeChecked();

    $root.find('.select-all').click();

    expect($root.find('#pipeline_up42')).toBeChecked();
    expect($root.find('#pipeline_up43')).toBeChecked();
    expect($root.find('#pipeline_up44')).toBeChecked();
  });

  it('should select no pipelines from selection', () => {
    $root.find('.arrow-right').get(0).click();

    expect($root.find('#pipeline_up42')).toBeChecked();
    expect($root.find('#pipeline_up43')).not.toBeChecked();
    expect($root.find('#pipeline_up44')).toBeChecked();

    $root.find('.select-none').click();

    expect($root.find('#pipeline_up42')).not.toBeChecked();
    expect($root.find('#pipeline_up43')).not.toBeChecked();
    expect($root.find('#pipeline_up44')).not.toBeChecked();
  });

  it('should render apply button', () => {
    expect($root.find('.filter_footer button')).toBeInDOM();
  });

  it('should apply the selection changes', () => {
    const selections  = pipelineSelection();
    selections.update = jasmine.createSpy('update').and.returnValue($.Deferred(_.noop));

    pipelineSelection(selections);
    m.redraw();

    $root.find('.btn-small.btn-primary').click();

    expect(selections.update).toHaveBeenCalled();
  });

  it('should close the dropdown on sucessfully appling the selection changes', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest('/go/api/internal/pipeline_selection', undefined, 'PUT').andReturn({
        responseHeaders: {
          ETag:           'etag',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          204
      });

      const selections = pipelineSelection();
      pipelineSelection(selections);
      m.redraw();

      expect(doRefreshImmediately).not.toHaveBeenCalled();
      expect(hideSelectionDropdown).not.toHaveBeenCalled();

      $root.find('.btn-small.btn-primary').click();

      expect(doRefreshImmediately).toHaveBeenCalled();
      expect(hideSelectionDropdown).toHaveBeenCalled();
    });
  });

  it('should expand first pipeline group by default', () => {
    expect($root.find('.filter_pipeline-group span').get(0)).toHaveClass('arrow-down');
    expect($root.find('.filter_pipeline-group span').get(1)).toHaveClass('arrow-right');
  });

  it('should not render pipelines for collapsed group', () => {
    expect($root.find('.filter_pipeline-group span').get(0)).toHaveClass('arrow-down');
    expect($root.find('#pipeline_up42')).toBeInDOM();

    expect($root.find('.filter_pipeline-group span').get(1)).toHaveClass('arrow-right');
    expect($root.find('#pipeline_up43')).not.toBeInDOM();
    expect($root.find('#pipeline_up44')).not.toBeInDOM();
  });

  it('should expand a pipeline group', () => {
    expect($root.find('.filter_pipeline-group span').get(0)).toHaveClass('arrow-down');
    expect($root.find('#pipeline_up43')).not.toBeInDOM();
    expect($root.find('#pipeline_up44')).not.toBeInDOM();

    $root.find('.filter_pipeline-group span').get(1).click();

    expect($root.find('.filter_pipeline-group span').get(1)).toHaveClass('arrow-down');
    expect($root.find('#pipeline_up43')).toBeInDOM();
    expect($root.find('#pipeline_up44')).toBeInDOM();
  });

  it('should not close the personalize dropdown when a click happens inside it', () => {
    const personalizeDropdownDiv = $root.find('.filter_options');
    expect(personalizeDropdownDiv).toBeInDOM();
    personalizeDropdownDiv.click();
    expect(personalizeDropdownDiv).toBeInDOM();
  });
});
