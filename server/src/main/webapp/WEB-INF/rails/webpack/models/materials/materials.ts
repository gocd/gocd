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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import Stream from "mithril/stream";
import {humanizedMaterialAttributeName} from "models/config_repos/types";
import {Filter} from "models/maintenance_mode/material";
import {MaterialJSON} from "./serialization";
import {Material, MaterialAttributes} from "./types";

export interface MaterialWithFingerprintJSON extends MaterialJSON {
  fingerprint: string;
}

interface MaterialWithFingerprintsJSON {
  _embedded: {
    materials: MaterialWithFingerprintJSON[];
  };
}

export class MaterialWithFingerprint extends Material {
  fingerprint: Stream<string>;

  constructor(type: string, fingerprint: string, attributes: MaterialAttributes) {
    super(type, attributes);
    this.fingerprint = Stream(fingerprint);
  }

  static fromJSON(data: MaterialWithFingerprintJSON): MaterialWithFingerprint {
    return new MaterialWithFingerprint(data.type, data.fingerprint, MaterialAttributes.deserialize(data));
  }

  attributesAsMap(): Map<string, any> {
    const map = new Map();
    return _.reduce(this.attributes(), MaterialWithFingerprint.resolveKeyValueForAttribute, map);
  }

  matches(textToMatch: string): boolean {
    if (!textToMatch) {
      return true;
    }
    const searchableStrings = [
      this.type(),
      this.name(),
      this.materialUrl()
    ];
    return searchableStrings.some((value) => value ? value.toLowerCase().includes(textToMatch.trim().toLowerCase()) : false);
  }

  private static resolveKeyValueForAttribute(accumulator: Map<string, string>, value: any, key: string) {
    if (key.startsWith("__") || ["name"].includes(key)) {
      return accumulator;
    }

    let renderedValue = value;
    const renderedKey = humanizedMaterialAttributeName(key);

    // test for value being a stream
    if (_.isFunction(value)) {
      value = value();
    }

    // test for value being an EncryptedPassword
    if (value && value.valueForDisplay) {
      renderedValue = value.valueForDisplay();
    }

    renderedValue = _.isFunction(renderedValue) ? renderedValue() : renderedValue;
    if (key === "filter" && renderedValue) {
      renderedValue = (renderedValue as Filter).ignore();
    }
    accumulator.set(renderedKey, renderedValue);
    return accumulator;
  }
}

export class MaterialWithFingerprints extends Array<MaterialWithFingerprint> {
  constructor(...vals: MaterialWithFingerprint[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(MaterialWithFingerprints.prototype));
  }

  static fromJSON(data: MaterialWithFingerprintJSON[]): MaterialWithFingerprints {
    return new MaterialWithFingerprints(...data.map((a) => MaterialWithFingerprint.fromJSON(a)));
  }

  sortOnType() {
    this.sort((m1, m2) => m1.type()!.localeCompare(m2.type()!));
  }
}

export class MaterialAPIs {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.getAllMaterials(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body) as MaterialWithFingerprintsJSON;
                              return MaterialWithFingerprints.fromJSON(data._embedded.materials);
                            }));
  }
}
