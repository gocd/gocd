/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import m from "mithril";
import {AbstractObjCache, ObjectCache, rejectAsString} from "models/base/cache";
import {Materials, MaterialWithModification} from "models/materials/materials";
import {EventAware} from "models/mixins/event_aware";
import {MaterialUsagesVM} from "./material_usages_view_model";

interface MaterialUsagesJSON {
  usages: MaterialUsageJSON[];
}

export interface MaterialUsageJSON {
  group: string;
  pipelines: string[];
}

class MaterialUsageCache extends AbstractObjCache<MaterialUsagesVM> {
  fingerprint: string;

  constructor(fingerprint: string) {
    super();
    this.fingerprint = fingerprint;
  }

  doFetch(resolve: (data: MaterialUsagesVM) => void, reject: (reason: string) => void): void {
    ApiRequestBuilder.GET(SparkRoutes.getMaterialUsages(this.fingerprint), ApiVersion.latest)
                     .then((result) => {
                       if (304 === result.getStatusCode()) {
                         resolve(this.contents()); // no change
                         return;
                       }

                       result.do((successResponse) => {
                         const data = JSON.parse(successResponse.body) as MaterialUsagesJSON;
                         resolve(MaterialUsagesVM.fromJSON(data.usages));
                       }, (error) => {
                         reject(error.message);
                       });
                     }).catch(rejectAsString(reject));
  }
}

export class MaterialVM {
  material: MaterialWithModification;
  results: ObjectCache<MaterialUsagesVM>;

  constructor(material: MaterialWithModification, results?: ObjectCache<MaterialUsagesVM>) {
    this.material = material;
    const cache   = results || new MaterialUsageCache(material.config.fingerprint());

    Object.assign(MaterialVM.prototype, EventAware.prototype);
    EventAware.call(this);

    this.results = cache;

    this.on("expand", () => !cache.failed() && cache.prime(m.redraw));
  }

  matches(query: string) {
    if (!query) {
      return true;
    }
    const searchableStrings = [
      this.material.config.type(),
      this.material.config.name(),
      this.material.config.materialUrl()
    ];
    const modification      = this.material.modification;
    if (modification !== null) {
      searchableStrings.push(modification.username, modification.revision, modification.comment);
    }
    return searchableStrings.some((value) => value ? value.toLowerCase().includes(query.trim().toLowerCase()) : false);
  }

  type() {
    return this.material.config.type();
  }
}

export class MaterialVMs extends Array<MaterialVM> {
  constructor(...vals: MaterialVM[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(MaterialVMs.prototype));
  }

  static fromMaterials(materials: Materials): MaterialVMs {
    const models = _.map(materials, (mat) => new MaterialVM(mat));
    return new MaterialVMs(...models);
  }

  sortOnType() {
    this.sort((m1, m2) => m1.type()!.localeCompare(m2.type()!));
  }
}

// tslint:disable-next-line
export interface MaterialVM extends EventAware {}
