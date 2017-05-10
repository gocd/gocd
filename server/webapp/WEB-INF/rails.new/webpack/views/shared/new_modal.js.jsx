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

const _ = require('lodash');
const $ = require('jquery');
const s = require('string-plus');
// instead of each modal being a top level component of its own (and causing a redraw of everything else on the page)
// we manage a single component that's always mounted as soon as the page loads up, creating a new modal will create
// a sub-component in the top level component.
// see https://github.com/lhorie/mithril.js/issues/694
const allModals      = {};
const defaultOptions = {
  title:   'This is the title',
  body:    () => (<small>This is the body</small>),
  size:    'tiny',
  buttons: [
    {
      text:     'OK',
      onclick:  _.noop(),
      disabled: _.identity(false)
    },
  ],
  onclose: _.noop()
};

const Modal = function (options) {
  const self    = this;
  const modalId = `modal-${  s.uuid()}`;
  options       = _.assign({}, defaultOptions, options);

  this.modalDialog = {
    oninit () {
      this.close = function (onclose, destroy, e) {
        if (onclose) {
          onclose(e);
        }

        if (!e.defaultPrevented) {
          destroy();
        }
      };
    },

    view (vnode) {
      return (
        <div class="reveal-overlay" style={{display: 'block'}}>
          <div class={`reveal ${  options.size ? options.size : ''}`}
               style={{display: 'block'}}>

            <h4 class='modal-title'>{options.title}</h4>

            <div class="modal-body">
              {options.body()}
            </div>

            <button class="close-button"
                    aria-label="Close modal"
                    type="button"
                    onclick={vnode.state.close.bind(vnode, options.onclose, self.destroy)}>
              <span aria-hidden="true">&times;</span>
            </button>

            <f.row class="modal-buttons" collapse>
              {_.map(_.isFunction(options.buttons) ? options.buttons() : options.buttons, (button) => {
                return (
                  <f.button disabled={button.disabled ? button.disabled() : false}
                            onclick={button.onclick}
                            class={button.class}>{button.text}</f.button>
                );
              })}
            </f.row>
          </div>
        </div>
      );
    }
  };

  this.render = function () {
    allModals[modalId] = this;
    m.redraw();
  };

  this.destroy = function () {
    delete allModals[modalId];
    m.redraw();
  };
};

$(() => {
  const $body        = $('body');
  const $modalParent = $('<div class="new-modal-container"/>').appendTo($body);

  // so you can directly access the modals in tests via `$('.modal-parent').data('modal')`
  $modalParent.data('modals', allModals);
  const ModalDialogs = {
    view () {
      const sortedModalKeys = _.keysIn(allModals).sort();
      return (
        <div>
          {_.map(sortedModalKeys, (key) => {
            return m(allModals[key].modalDialog);
          })}
        </div>
      );
    }
  };
  m.mount($modalParent.get(0), ModalDialogs);
});

Modal.destroyAll = function () {
  _.each(_.values(allModals), (modal) => {
    modal.destroy();
  });
};

Modal.count = function () {
  return _.keys(allModals).length;
};

module.exports = Modal;
