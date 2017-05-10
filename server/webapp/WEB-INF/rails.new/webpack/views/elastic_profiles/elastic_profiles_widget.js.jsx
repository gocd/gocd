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

let m                       = require('mithril');
let f                       = require('helpers/form_helper');
let ElasticProfileWidget    = require('views/elastic_profiles/elastic_profile_widget');
let ElasticProfileModalBody = require('views/elastic_profiles/elastic_profile_modal_body');

const Stream          = require('mithril/stream');
const _               = require('lodash');
const Modal           = require('views/shared/new_modal');
const ComponentMixins = require('helpers/mithril_component_mixins');
const ElasticProfiles = require('models/elastic_profiles/elastic_profiles');

require('jquery-textcomplete');

const showErrors = (modal, ctrl, newProfile) => {
  return function (errorMessageOrProfileWithErrors) {
    if (_.isString(errorMessageOrProfileWithErrors)) {
      modal.destroy();
      ctrl.message({type: 'alert', message: errorMessageOrProfileWithErrors});
    } else {
      ctrl.clearMessage();
      newProfile(errorMessageOrProfileWithErrors);
    }
  };
};

function showSuccess(modal, ctrl, message) {
  return function () {
    modal.destroy();
    ctrl.reloadPage();
    ctrl.message({
      type: 'success',
      message
    });
  };
}

const ElasticProfilesWidget = {
  oninit (vnode) {
    const ctrl = vnode.state;
    ComponentMixins.ManagesCollection.call(this, {as: 'Profile'});
    ComponentMixins.HasViewModel.call(this);

    this.modal    = null;
    this.profiles = Stream(new ElasticProfiles());

    this.message = Stream({type: undefined, message: undefined});

    this.clearMessage = function () {
      this.message({});
    };

    this.reloadPage = function () {
      ElasticProfiles.all().then((profiles) => {
        ctrl.profiles(profiles);
      }, (message) => {
        ctrl.message({type: 'alert', message});
      }).always(m.redraw);
    };

    this.reloadPage();

    this.edit = function (profile) {
      const newProfile   = Stream();
      const pluginInfo   = Stream(vnode.attrs.pluginInfos().findById(profile.pluginId()));
      const saveDisabled = Stream(false);
      const errorMessage = Stream();

      ctrl.clearMessage();

      const modal = new Modal({
        size:    'large',
        title:   `Edit profile ${  profile.id()}`,
        body:    () => (<ElasticProfileModalBody profile={newProfile}
                                                 pluginInfo={pluginInfo}
                                                 newProfile={false}
                                                 pluginInfos={vnode.attrs.pluginInfos}
                                                 errorMessage={errorMessage}
                                                 saveDisabled={saveDisabled}/>),
        onclose: () => modal.destroy(),
        buttons: () => {
          if (!newProfile()) {
            return [];
          }
          return [
            {
              text:     'Save',
              class:    'save primary',
              onclick () {
                newProfile().update().then(showSuccess(modal, ctrl, `The profile ${  newProfile().id()  } was updated successfully.`), showErrors(modal, ctrl, newProfile)).always(m.redraw);
              },
              disabled: saveDisabled
            }
          ];
        }
      });

      const onFulfilled = function (profileFromAjax) {
        newProfile(profileFromAjax);
        errorMessage(null);
      };

      modal.render();

      ElasticProfiles.Profile.get(profile.id()).then(onFulfilled, errorMessage).always(m.redraw);
    };

    this.add = function () {
      const newProfile    = Stream(new ElasticProfiles.Profile({}));
      const newPluginInfo = Stream(null);
      const saveDisabled  = Stream(true);

      const modal = new Modal({
        size:    'large',
        title:   'Add a new profile',
        body:    () => (<ElasticProfileModalBody profile={newProfile}
                                                 newProfile={true}
                                                 pluginInfos={vnode.attrs.pluginInfos}
                                                 pluginInfo={newPluginInfo}
                                                 saveDisabled={saveDisabled}/>),
        onclose: () => modal.destroy(),
        buttons: [
          {
            text:     'Save',
            class:    'save primary',
            onclick () {
              newProfile().create().then(showSuccess(modal, ctrl, `The profile ${  newProfile().id()  } was created successfully.`), showErrors(modal, ctrl, newProfile)).always(m.redraw);
            },
            disabled: saveDisabled
          }
        ]
      });
      modal.render();
    };

    const deleteInProgress = Stream(false);

    const performDelete = function (modal, profile) {
      const onSuccess = function (message) {
        modal.destroy();
        ctrl.deleteVm(profile.id());
        ctrl.reloadPage();
        ctrl.message({type: 'success', message});
        deleteInProgress(false);
      };

      const onFailure = function (message) {
        modal.destroy();
        ctrl.message({type: 'alert', message});
        deleteInProgress(false);
      };

      deleteInProgress(true);
      m.redraw();
      profile.delete().then(onSuccess, onFailure);
    };

    this.deleteConfirm = function (profile) {
      const modal = new Modal({
        title:    'Are you sure?',
        body:     () => (<div>Are you sure you want to delete the profile <strong>{profile.id()}</strong>?</div>),
        oncancel: () => modal.destroy(),
        buttons:  () => [
          {
            text:  'Delete',
            class: deleteInProgress() ? 'delete-profile in-progress' : 'delete-profile',
            onclick () {
              performDelete(modal, profile);
            }
          }
        ]
      });
      modal.render();
    };
  },

  view (vnode) {
    let message;
    if (vnode.state.message().message) {
      message = (
        <f.row>
          <f.callout type={vnode.state.message().type}>
            {vnode.state.message().message}

            <button class="close-button" aria-label="Dismiss alert" type="button"
                    onclick={vnode.state.clearMessage.bind(vnode.state)}>
              <span aria-hidden="true">&times;</span>
            </button>
          </f.callout>
        </f.row>
      );
    }

    return (
      <div>
        <div class="header-panel">
          <header class="page-header">
            <f.row>
              <f.column size={6}>
                <h1>Elastic Agent Profiles</h1>
              </f.column>

              <f.column size={6}>
                <f.button onclick={vnode.state.add.bind(vnode.state)} class="add-profile">
                  Add
                </f.button>
              </f.column>
            </f.row>
          </header>
        </div>


        <div class="elastic-profiles">
          {message}
          <f.row>

            {vnode.state.profiles().mapProfiles((profile) => {
              const pluginInfo = vnode.attrs.pluginInfos().findById(profile.pluginId());

              return (
                <ElasticProfileWidget
                  profile={profile}
                  pluginInfo={pluginInfo}
                  key={profile.id()}
                  vm={vnode.state.vmState(profile.id())}
                  onEdit={vnode.state.edit.bind(vnode.state, profile)}
                  onDelete={vnode.state.deleteConfirm.bind(vnode.state, profile)}/>
              );
            })}
          </f.row>
        </div>
      </div>
    );
  }
};

module.exports = ElasticProfilesWidget;
