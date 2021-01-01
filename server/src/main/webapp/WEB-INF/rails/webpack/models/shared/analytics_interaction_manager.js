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

import Requests from "rails-shared/plugin-endpoint-request-handler";
import {Frame} from "models/shared/frame";
import Routes from "gen/js-routes";

function Namespace(models, name) {
  const prefix = `${encodeURIComponent(name)}:`;

  function withPrefix(u) {
    return prefix + u;
  }

  function withoutPrefix(u) {
    return u.split(":").pop();
  }

  function encodeUID(i, p, t, id) {
    return withPrefix(btoa(JSON.stringify({plugin: p, type: t, id, ordinal: i})));
  }

  function decodeUID(uid) {
    return JSON.parse(atob(withoutPrefix(uid)));
  }

  this.uid    = encodeUID;
  this.unpack = decodeUID;

  this.group = function group() {
    return name;
  };

  this.all = function allModelsInNamespace() {
    const result = {};
    for (const k in models) {
      if (k.startsWith(prefix)) {
        result[k] = models[k];
      }
    }
    return result;
  };

  function toUrl(uid, params = {}) {
    const c = decodeUID(uid);
    return Routes.showAnalyticsPath(c.plugin, c.type, c.id, params);
  }

  this.toUrl = toUrl;

  this.modelFor = function modelFor(uid, extraParams = {}) {
    let model = models[uid];

    if (!model) {
      model = models[uid] = new Frame();
      model.url(toUrl(uid, extraParams));
    }

    return model;
  };
}

/**
 * AnalyticsInteractionManager should be a singleton that handles
 * the bootstrapping of parent page communication channel, which
 * includings things like listener definitions and Frame model
 * management.
 */
function AnalyticsInteractionManager() {
  const models = {};

  this.purge = function destroyAll() {
    for (const k in models) {
      delete models[k];
    }
  };

  this.all = function allModels() {
    // returns the whole world.
    //
    // shallow copy prevents unauthorized writes to
    // `models` internal structure
    return Object.assign({}, models);
  };

  this.ns = function namespace(name) {
    return new Namespace(models, name);
  };

  this.ensure = function ensure() {
    Requests.defineLinkHandler();
    Requests.defineFetchAnalyticsHandler(models);
    return this;
  };
}

export default new AnalyticsInteractionManager();
