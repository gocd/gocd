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

import react from "eslint-plugin-react";
import globals from "globals";
import js from "@eslint/js";
import {FlatCompat} from "@eslint/eslintrc";
import babelParser from "@babel/eslint-parser";

const compat = new FlatCompat({
  recommendedConfig: js.configs.recommended,
});

export const webpackedConfig = {
  files: ["webpack/**/*.{js,mjs,msx}", "spec/webpack/**/*.{js,mjs,msx}"],

  extends: compat.extends("eslint:recommended", "plugin:react/recommended"),

  plugins: {
    react,
  },

  languageOptions: {
    globals: {
      ...globals.browser,
      ...globals.jasmine,
      angular: true,
      module: true,
    },

    parser: babelParser,
    ecmaVersion: "latest",
    sourceType: "module",

    parserOptions: {
      ecmaFeatures: {
        jsx: true,
      },
    },
  },

  settings: {
    react: {
      "createClass": "createClass",
      "pragma": "m",
      "version": "15",
    }
  },

  rules: {
    "arrow-parens": ["error"],
    "arrow-spacing": ["error"],
    "template-curly-spacing": ["error", "never"],
    "no-const-assign": ["error"],
    "no-var": ["error"],
    "object-shorthand": ["error"],
    "prefer-arrow-callback": ["error"],
    "prefer-const": ["error"],
    "prefer-template": ["error"],
    "block-scoped-var": ["error"],
    "eol-last": ["error"],
    "indent-legacy": ["error", 2],
    eqeqeq: ["error", "always"],
    camelcase: ["error"],
    "linebreak-style": ["error", "unix"],
    quotes: ["off"],
    semi: ["error", "always"],

    "no-unused-vars": ["error", {
      args: "all",
      vars: "all",
      argsIgnorePattern: "^_.",
    }],

    "no-empty": ["error"],
    curly: ["error"],
    "react/react-in-jsx-scope": ["off"],
    "react/display-name": ["off"],
    "react/no-unknown-property": ["off"],
    "react/jsx-key": ["off"],
    "react/jsx-no-target-blank": ["off"],
    "react/no-unescaped-entities": ["off"],
  },
};