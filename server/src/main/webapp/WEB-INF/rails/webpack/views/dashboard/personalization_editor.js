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
import m from "mithril";
import {PersonalizeEditorVM} from "views/dashboard/models/personalize_editor_vm";
import {PersonalizationModalWidget} from "views/dashboard/personalization_modal_widget";
import {Modal} from "views/shared/schmodal";
import {mrequest} from "helpers/mrequest";

function personalizeEditor(opts, personalization, model) {
  // evaluate every time in case personalization is updated while the modal is open
  opts.names = () => personalization().names();

  const vm       = new PersonalizeEditorVM(opts, personalization().pipelineGroups());
  const existing = opts.name;

  model.updatePipelineGroups().then(() => {
    vm.onLoadPipelines(model.model().pipelineGroups());
  });

  function save() {
    vm.validate();
    if (vm.invalid()) {
      return;
    }

    const newFilter = vm.asFilter();

    vm.errorResponse(null);
    personalization().addOrReplaceFilter(existing, newFilter, model.etag()).done((data) => {
      model.currentView(newFilter.name);
      model.names(personalization().names());
      model.checksum(data.contentHash);

      setTimeout(Modal.close, 0);
      model.onchange();
    }).fail((xhr) => {
      vm.errorResponse(JSON.parse(xhr.responseText).message);
      m.redraw();
    });
  }

  function deleteView() {
    const dialog = new Modal({
      title:   "Delete View",
      size:    "overlay-delete-view",
      body:    () => m("span", null, [
        "Do you want to delete view ",
        m("span", {"class": "personalization-view-name"}, existing),
        "?"
      ]),
      buttons: [{
        text:    "Yes",
        onclick: () => {
          personalization().removeFilter(existing, model.etag()).done((data) => {
            model.names(personalization().names());
            model.selectFirstView();
            model.checksum(data.contentHash);

            Modal.closeAll();
            model.onchange();
          }).fail((xhr) => {
            const reason = mrequest.unwrapErrorExtractMessage(xhr.responseText);
            dialog.replace({
              title:   "Delete View",
              size:    "overlay-delete-view",
              body:    () => m("span", {"class": "server-error-response"}, [
                m("i", {"class": "icon_alert"}),
                m("span", {"class": "reason"}, [
                  "Failed to delete view ",
                  m("span", {"class": "personalization-view-name"}, name),
                  ": ", reason
                ])
              ]),
              buttons: [{text: "Close"}]
            });
          }).always(m.redraw);
        }
      }, {text: "Cancel", class: "btn-link"}]
    });
  }

  const buttons = [
    {text: "Save", class: "btn-save", disabled: vm.invalid, onclick: save, tooltipText: vm.firstError},
    {text: "Cancel", class: "btn-cancel btn-link"}
  ];

  const disabled    = () => (model.names().length < 2);
  const tooltipText = () => {
    if (disabled()) {
      return "Cannot delete the last view. You must have at least one.";
    }
  };

  if (existing) {
    buttons.unshift({text: "Delete View", class: "btn-delete", onclick: deleteView, disabled, tooltipText});
  }

  this.modal = new Modal({
    title:       existing ? `Edit ${opts.name}` : "Create new view",
    size:        "overlay-personalize-editor",
    body:        () => m(PersonalizationModalWidget, {vm, save}),
    globalClick: () => {
      if (vm.tooltip()) {
        vm.tooltip(null);
        setTimeout(m.redraw, 5);
      }
    },
    buttons
  });
}

export const Editor = {open: personalizeEditor};
