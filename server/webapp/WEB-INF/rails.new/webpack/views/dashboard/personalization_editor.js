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

const m = require("mithril");

const PersonalizeEditorVM        = require("views/dashboard/models/personalize_editor_vm");
const PersonalizationModalWidget = require("views/dashboard/personalization_modal_widget");
const Modal                      = require("views/shared/schmodal");

function personalizeEditor(opts, personalization, model) {
  // evaluate every time in case personalization is updated while the modal is open
  opts.names = () => personalization().names();

  const vm = new PersonalizeEditorVM(opts, personalization().pipelineGroups());
  const existing = opts.name;

  model.updatePipelineGroups().then(() => {
    vm.onLoadPipelines(model.model().pipelineGroups());
    m.redraw();
  });

  function save() {
    vm.validate();
    if (vm.invalid()) { return; }

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
    new Modal({
      title: "Delete View",
      size: "delete-view",
      body: () => {
        return <span>
          Do you want to delete <span class="view-name">{existing}</span> view?
        </span>;
      },
      buttons: [{
        text: "Yes",
        onclick: () => {
          personalization().removeFilter(existing, model.etag()).done((data) => {
            model.currentView("Default");
            model.names(personalization().names());
            model.checksum(data.contentHash);

            Modal.close();
            model.onchange();
          }).fail((xhr) => {
            const reason = JSON.parse(xhr.responseText).message;
            that.replace({
              title: "Delete View",
              size: "delete-view",
              body: () => {
                return <span class="server-error-response">
                  <i class="icon_alert"></i>
                  <span class="reason">
                    Failed to delete view <span class="view-name">{name}</span>: {reason}
                  </span>
                </span>;
              },
              buttons: [{text: "Close"}]
            });
          }).always(() => {
            m.redraw();
          });
        }}, {text: "Cancel", class: "btn-link"}]
    });
  }

  this.modal = new Modal({
    title: existing ? `Edit ${opts.name}`: "Create new view",
    size: "overlay-personalize-editor",
    body: () => m(PersonalizationModalWidget, { vm, save }),
    buttons: existing ? [
      {text: "Delete View", class:"delete-button", onclick: deleteView },
      {text: "Save", disabled: vm.invalid, onclick: save, tooltipText: vm.firstError},
      {text: "Cancel", class: "btn-cancel btn-link"}
    ] : [
      {text: "Save", disabled: vm.invalid, onclick: save, tooltipText: vm.firstError},
      {text: "Cancel", class: "btn-cancel btn-link"}
    ]
  });
}

module.exports = { open: personalizeEditor };
