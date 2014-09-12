/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END**********************************/

FieldStateReplicator = function() {

  function update_state(self, id, originator) {
    var pears = self.id_fields_map[id];
    update_pears_state(self, id, originator, pears);
  }

  function update_pears_state(self, id, originator, pears, prop) {
    var check_type = (originator.type === 'checkbox' || originator.type === 'radiobutton');
    var checked = check_type && originator.checked;
    var value = check_type || originator.value;
    pears.each(function (field) {
      if(check_type) {
        field.checked = checked;
      } else {
        field.value = value;
      }
    });
  }

  function init() {
    this.id_fields_map = {};
  }

  //js is single threaded :-)
  init.prototype.register = function(field, id) {
    var self = this;
    Event.observe(field, 'change', function () {
      update_state(self, id, field);
    });

    Event.observe(field, 'keyup', function () {
      update_state(self, id, field);
    });
    var pears = this.id_fields_map[id];
    if (pears) {
      pears.push(field);
      update_pears_state(this, id, pears[0], pears);
      return;
    }
    this.id_fields_map[id] = [field];
  };

  init.prototype.unregister = function(field, id) {
    var pears = this.id_fields_map[id];
    if(pears) {
      this.id_fields_map[id] = pears.reject(function(pear) {
        if (pear === field) {
          Event.stopObserving(field);
          return true;
        } else {
          return false;
        }
      });
    }
  };

  init.prototype.unregister_all = function() {
      for(var id in this.id_fields_map) {
          var pears = this.id_fields_map[id];
          for(var i = 0; i < pears.length; i++) {
              Event.stopObserving(pears[i]);
          }
          this.id_fields_map[id] = [];
      }
  };

  init.prototype.register_all_matching = function(under, css_selector, id_loader) {
    var self = this;
    under.select(css_selector).each(function (elem) {
        self.register(elem, id_loader(elem));
    });
  };

  return init;
}();
