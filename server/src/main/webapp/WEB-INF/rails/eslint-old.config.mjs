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

import globals from "globals";
import js from "@eslint/js";
import {FlatCompat} from "@eslint/eslintrc";

const compat = new FlatCompat({
  recommendedConfig: js.configs.recommended,
});

export const nonWebPackedLegacyConfig = {
  files: ["app/assets/javascripts/**/*.js", "spec/javascripts/**/*.js"],
  ignores: ["spec/javascripts/support/**"],

  extends: compat.extends("eslint:recommended"),

  languageOptions: {
    globals: {
      ...globals.browser,
      ...globals.jasmine,
      ...globals.jquery,
      module: true,

      // Jasmine jquery
      setFixtures: 'readonly',

      // often js
      often: 'writable',
      start: 'writable',
      wait: 'writable',
      done: 'writable',

      // libraries bound globally
      _: 'readonly',
      crel: 'readonly',
      d3: 'readonly',
      moment: 'readonly',
      pako: 'readonly',

      // Our own custom code mounted globally from various places
      context_path: 'readonly',
      contextPath: 'readonly',
      clean_active_css_class_on_element: 'readonly',
      is_result_unknown: 'readonly',
      json_to_css: 'readonly',
      make_collapsable: 'readonly',

      AjaxRefresher: 'readonly',
      AjaxRefreshers: 'readonly',
      AnsiUp: 'readonly',
      BuildDetail: 'writable',
      BuildSummaryObserver: 'readonly',
      ConsoleScroller: 'readonly',
      ConsoleLogObserver: 'readonly',
      ConsoleLogSocket: 'readonly',
      CrelAnsiUp: 'readonly',
      DashboardPeriodicalExecutor: 'readonly',
      DummyNode: 'readonly',
      Emitter: 'readonly',
      FieldStateReplicator: 'writable',
      FoldableSection: 'readonly',
      GoCDLinkSupport: 'readonly',
      Graph_Renderer: 'writable',
      JsonToCss: 'readonly',
      LogOutputTransformer: 'readonly',
      MicroContentPopup: 'writable',
      PeriodicExecutor: 'writable',
      PipelineDependencyNode: 'readonly',
      PluginEndpoint: 'readonly',
      PluginEndpointRequestHandler: 'readonly',
      SCMDependencyNode: 'readonly',
      SubTabs: 'readonly',
      StageDetailAjaxRefresher: 'readonly',
      StageHistory: 'readonly',
      TimerObserver: 'readonly',
      TransMessage: 'readonly',
      TabsManager: 'readonly',
      Util: 'writable',
      VSM: 'writable',
      VSMGraph: 'readonly',
      VSMAnalytics: 'readonly',
      WebSocketWrapper: 'readonly',
    },

    ecmaVersion: 2022,
    sourceType: "module",
  },

  rules: {
    "arrow-parens": ["error"],
    "arrow-spacing": ["error"],
    "template-curly-spacing": ["error", "never"],
    "no-const-assign": ["error"],
    "object-shorthand": ["error"],
    "prefer-arrow-callback": ["off"],
    "prefer-const": ["error"],
    "prefer-template": ["error"],
    "block-scoped-var": ["error"],
    "eol-last": ["error"],
    "indent-legacy": ["error", 2],
    eqeqeq: ["off"],
    camelcase: ["off"],
    "linebreak-style": ["error", "unix"],
    quotes: ["off"],
    semi: ["error", "always"],
    "no-unused-vars": ["off"],
    curly: ["error"],
  },
};