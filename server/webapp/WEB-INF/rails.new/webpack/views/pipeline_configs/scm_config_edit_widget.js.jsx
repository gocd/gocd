/*
 * Copyright 2017 ThoughtWorks, Inc.
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

let m = require('mithril');
let f = require('helpers/form_helper');

const $                      = require('jquery');
const Stream                 = require('mithril/stream');
const SCMConfigAngularWidget = require('mithril');
const SCMs                   = require('models/pipeline_configs/scms');

//This has to be done to get around mithril's controller caching
const submodule = function (module, args) {
  return module.view.bind(module, new module.controller(args));
};

/*
 'scmForEdit' is a cloned copy of the actual SCM, this is required since SCMConfigEditWidget is rendered in a modal. There might be scenarios
 wherein a user can make changes to the scm but not save it and close the modal, in this scenario the changes should not
 be retained. To achieve this behaviour the widget works with a cloned copy of scm.
 */

const fetchDependentPipelines = function (id) {
  const pipelineUsedInUrl = ['/go/admin/materials/pluggable_scm/', id, '/pipelines_used_in'].join('');

  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method:      'GET',
      url:         pipelineUsedInUrl,
      contentType: false
    });

    jqXHR.then((value) => {
      deferred.resolve(m.trust(value));
    });

  }).promise();
};

let SCMGlobalCopyWarningWidget = {
  oninit (vnode) {
    const self              = this;
    self.scm                = vnode.attrs.scm;
    self.dependentPipelines = Stream('');

    this.showAssociatedPipelines = function () {
      fetchDependentPipelines(self.scm.id()).then(self.dependentPipelines);
    };
  },

  view (vnode) {
    const dependentPipelinesLink = !vnode.state.dependentPipelines() ?
      (<f.link onclick={vnode.state.showAssociatedPipelines.bind(vnode.state)}>Show pipelines using this SCM</f.link>)
      : undefined;
    return (
      <div>
        <f.row collapse>
          <f.column size={12} largeSize={12}>
            <f.warning>This is a global copy. Editing this SCM would affect all pipelines using
              it. {dependentPipelinesLink} </f.warning>
          </f.column>
        </f.row>
        <f.row collapse>
          <f.column size={12} largeSize={12}>
            {vnode.state.dependentPipelines()}
          </f.column>
        </f.row>
      </div>
    );
  }
};

const SCMConfigEditWidget = {
  oninit (vnode) {
    const self         = this;
    self.parentView    = vnode.attrs.parentView;
    self.material      = vnode.attrs.material;
    self.scmForEdit    = vnode.attrs.material.scm().clone();
    self.angularModule = submodule(SCMConfigAngularWidget, {scm: self.scmForEdit, parentView: self.parentView});
    self.vm            = new SCMs.vm();

    self.update = function () {
      self.vm.startUpdating();

      self.scmForEdit.update().then((scm) => {
        self.vm.saveSuccess();
        self.material.scm().reInitialize(JSON.parse(JSON.stringify(scm)));
        self.parentView.close();
      }, (data) => {
        self.vm.saveFailed(data);
        if (data.data) {
          self.scmForEdit.reInitialize(data.data);
        }
      });
    };

    self.parentView.onClose(() => {
      self.scmForEdit.reInitialize(JSON.parse(JSON.stringify(vnode.attrs.material.scm())));
      self.vm.reset();
    });
  },

  view (vnode) {
    const errors = vnode.state.vm.hasErrors() ? (<f.alert>{vnode.state.vm.errors()}</f.alert>) : undefined;

    return (
      <div class='modal-content'>
        <div class='modal-header'>
          <SCMGlobalCopyWarningWidget scm={vnode.state.scmForEdit}/>
          {errors}
        </div>
        <div class='modal-body'>
          <div class='key-value' size={6}>
            <label>Name</label><span>{vnode.state.scmForEdit.name()}</span>
          </div>
          <f.row>
            <f.checkBox model={vnode.state.scmForEdit}
                        attrName='autoUpdate'
                        class='align'
                        size={6}
                        end={true}/>
          </f.row>
          {vnode.state.angularModule()}
          <f.row>
            <f.button onclick={vnode.state.update.bind(vnode.state)}
                      class={`save-pipeline ${  vnode.state.vm.saveState()}`}>
              <span class={`save-state ${  vnode.state.vm.saveState()}`}/>
              Save
            </f.button>
          </f.row>
        </div>
      </div>
    );
  }
};

module.exports = SCMConfigEditWidget;
