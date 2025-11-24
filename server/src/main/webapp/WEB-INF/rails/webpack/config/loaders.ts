/*
 * Copyright Thoughtworks, Inc.
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

import {getBabelLoader} from "./loaders/babel-loader";
import {getCssLoaders} from "./loaders/css-loader";
import {getStaticAssetsLoader} from "./loaders/static-assets-loader";
import {getTypescriptLoader} from "./loaders/ts-loader";
import {ConfigOptions} from "./variables";

export function loaders(configOptions: ConfigOptions) {
  return [
    getTypescriptLoader(configOptions),
    getBabelLoader(configOptions),
    getCssLoaders(configOptions),
    getStaticAssetsLoader(configOptions)
  ];
}
