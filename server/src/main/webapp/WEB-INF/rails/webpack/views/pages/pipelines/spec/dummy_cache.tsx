/*
 * Copyright 2024 Thoughtworks, Inc.
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

import {SuggestionCache} from "../non_scm_material_fields";

export class DummyCache implements SuggestionCache {
  ready() {
    return true;
  }

  // tslint:disable-next-line
  prime(onSuccess: () => void, onError?: () => void) {
  }

  contents() {
    return [];
  }

  pipelines() {
    return [];
  }

  stages(pipeline: string) {
    return [];
  }

  failureReason() {
    return undefined;
  }

  failed() {
    return false;
  }

  // tslint:disable-next-line
  invalidate() {
  }
}