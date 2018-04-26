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

const m                      = require('mithril');
const Modal                  = require('views/shared/new_modal');
const ArtifactStoreModalBody = require('views/artifact_stores/artifact_store_modal_body');

class ArtifactStoreModal extends Modal {

  constructor(opts) {
    const modalOpts = {
      size:    'large',
      onclose: () => this.destroy(),
      body:    () => m(ArtifactStoreModalBody, {
        pluginInfos:   opts.pluginInfos,
        artifactStore: opts.artifactStore,
        errorMessage:  opts.errorMessage
      }),
      buttons: [
        {
          text:     "Save",
          class:    'save primary',
          disabled: opts.buttonDisabled,
          onclick:  opts.onclick
        }],
      title:   opts.title
    };
    super(modalOpts);
  }

}

module.exports = ArtifactStoreModal;
