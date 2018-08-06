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

  new Modal({
    title: existing ? `Edit ${opts.name}`: "Create new view",
    size: "personalize-editor",
    body: () => {
      return m(PersonalizationModalWidget, { vm, save });
    },
    buttons: [
      {text: "Save", disabled: vm.invalid, onclick: save},
      {text: "Cancel", class: "btn-link"}
    ]
  });
}

module.exports = { open: personalizeEditor };
