import {defineConfig, globalIgnores} from "eslint/config";
import react from "eslint-plugin-react";
import globals from "globals";
import babelParser from "@babel/eslint-parser";
import path from "node:path";
import {fileURLToPath} from "node:url";
import js from "@eslint/js";
import {FlatCompat} from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all
});

export default defineConfig(
  [
    globalIgnores([
      "gems",                       // Gems
      "tmp",                        // Temporary typescript output dir
      "target",                     // Build output dir
      "public",                     // Webpack/sprockets output dir

      "node-vendor",                // Vendored webpacked JS
      "vendor/assets/javascripts",  // Vendored raw JS

      "app/assets/javascripts",    // Non-webpacked JS
      "spec/javascripts",          // Non-webpacked JS

      "**/*.ts",                   // Using tslint for these, for now
      "**/*.tsx",                  // Using tslint for these, for now
    ]),
    {
      extends: compat.extends("eslint:recommended", "plugin:react/recommended"),

      plugins: {
        react,
      },

      languageOptions: {
        globals: {
          ...globals.browser,
          ...globals.jasmine,
          ...globals.jquery,
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
    }]);
