/*
 * Copyright 2019 ThoughtWorks, Inc.
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
/* global process, console */

const _              = require('lodash');
const fs             = require('fs');
const fsExtra        = require('fs-extra');
const licenseChecker = require('license-checker');
const path           = require('path');
const upath          = require('upath');

const LicensePlugin = function (options) {
  this.apply = function (compiler) {
    licenseChecker.init({json: true, production: false, start: process.cwd()}, (err, licenseData) => {
      if (err) {
        console.error('Found error'); //eslint-disable-line no-console
        console.error(err); //eslint-disable-line no-console
        process.exit(1);
      }

      const filenames = [];
      compiler.plugin("emit", (compilation, callback) => {
        compilation.chunks.forEach((chunk) => {
          chunk.modulesIterable.forEach((chunkModule) => {
            filenames.push(chunkModule.resource || (chunkModule.rootModule && chunkModule.rootModule.resource));
            if (Array.isArray(chunkModule.fileDependencies)) {
              chunkModule.fileDependencies.forEach((e) => {
                filenames.push(e);
              });
            }
          });
        });
        callback();

        const licenseReport = _.chain(filenames)
          .uniq()
          .filter((fileName) => fileName && fileName.indexOf('node_modules') >= 0)
          .map((fileName) => {
            const file = upath.normalize(fileName).replace(upath.join(process.cwd(), '/node_modules/'), '');
            return file.startsWith("@") ? file.split("/").slice(0, 2).join("/") : file.split("/")[0];
          })
          .uniq()
          .sort()
          .reduce((accumulator, moduleName) => {
            const moduleNameWithVersion = _(licenseData).findKey((_moduleLicenseInfo, moduleNameWithVersion) => {
              return moduleNameWithVersion.startsWith(`${moduleName}@`);
            });

            const licenseDataForModule = licenseData[moduleNameWithVersion];

            if (licenseDataForModule) {
              const moduleVersion = moduleNameWithVersion.split('@')[1];

              if (licenseDataForModule.licenseFile) {
                const licenseReportDirForModule = path.join(path.dirname(options.licenseReportFile), `${moduleName}-${moduleVersion}`);
                fsExtra.removeSync(licenseReportDirForModule);
                fsExtra.mkdirsSync(licenseReportDirForModule);
                fsExtra.copySync(licenseDataForModule.licenseFile, path.join(licenseReportDirForModule, path.basename(licenseDataForModule.licenseFile)));
              }

              accumulator[moduleName] = {
                moduleName,
                moduleVersion,
                moduleUrls:     [licenseDataForModule.repository],
                moduleLicenses: [
                  {
                    moduleLicense:    licenseDataForModule.licenses,
                    moduleLicenseUrl: `https://spdx.org/licenses/${licenseDataForModule.licenses}.html`
                  }
                ]
              };
            } else {
              console.error(`Unable to find license data for ${moduleName}`); //eslint-disable-line no-console
              process.exit(1);
            }
            return accumulator;
          }, {}).value();

        fs.writeFileSync(options.licenseReportFile, `${JSON.stringify(licenseReport, null, 2)}\n`);
      });

    });
  };
};

module.exports = LicensePlugin;
