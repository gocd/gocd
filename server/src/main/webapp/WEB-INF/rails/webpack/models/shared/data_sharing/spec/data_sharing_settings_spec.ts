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
import {timeFormatter} from "helpers/time_formatter";
import {DataSharingSettings} from "models/shared/data_sharing/data_sharing_settings";

describe("Data Sharing Settings Model", () => {
  const dataSharingSettingsURL = "/go/api/data_sharing/settings";

  const dataSharingSettingsJSON = {
    _embedded: {
      allow: true,
      updated_by: "Admin",
      updated_on: "2018-06-14T05:45:30Z"
    }
  };

  it("should deserialize data sharing settings from JSON", () => {
    const settings = DataSharingSettings.fromJSON(dataSharingSettingsJSON);

    expect(settings.allowed()).toBe(dataSharingSettingsJSON._embedded.allow);
    expect(settings.updatedBy()).toBe(dataSharingSettingsJSON._embedded.updated_by);
  });

  it("should deserialize updated on time into date format", () => {
    const settings = DataSharingSettings.fromJSON(dataSharingSettingsJSON);

    expect(settings.updatedOn()).toBe(timeFormatter.formatInDate(dataSharingSettingsJSON._embedded.updated_on));
  });

  it("should tell whether data sharing settings has ever been changed by admin", () => {
    let settings = DataSharingSettings.fromJSON(dataSharingSettingsJSON);
    expect(settings.hasEverChangedByAdmin()).toBe(true);

    dataSharingSettingsJSON._embedded.updated_by = "Default";

    settings = DataSharingSettings.fromJSON(dataSharingSettingsJSON);
    expect(settings.hasEverChangedByAdmin()).toBe(false);
  });

  it("should toggle consent value", () => {
    const settings = DataSharingSettings.fromJSON(dataSharingSettingsJSON);

    expect(settings.allowed()).toBe(true);
    settings.toggleConsent();
    expect(settings.allowed()).toBe(false);
  });

  it("should reset consent value", () => {
    const settings = DataSharingSettings.fromJSON(dataSharingSettingsJSON);

    expect(settings.allowed()).toBe(true);
    settings.toggleConsent();
    expect(settings.allowed()).toBe(false);
    settings.resetConsent();
    expect(settings.allowed()).toBe(true);
    settings.resetConsent();
    expect(settings.allowed()).toBe(true);
  });

  it("should fetch data sharing settings", () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(dataSharingSettingsURL).andReturn({
                                                                   responseText: JSON.stringify(dataSharingSettingsJSON),
                                                                   status: 200,
                                                                   responseHeaders: {
                                                                     "Content-Type": "application/vnd.go.cd.v1+json"
                                                                   }
                                                                 });

      const successCallback = jasmine.createSpy().and.callFake((metricsSettings: any) => {
        expect(metricsSettings.allowed()).toBe(dataSharingSettingsJSON._embedded.allow);
        expect(metricsSettings.updatedBy()).toBe(dataSharingSettingsJSON._embedded.updated_by);
      });

      DataSharingSettings.get().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it("should patch data sharing settings", () => {
    jasmine.Ajax.withMock(() => {
      const updatedMetricsSettings = {
        _embedded: {
          allow: false,
          updated_by: "Bob"
        }
      };

      jasmine.Ajax.stubRequest(dataSharingSettingsURL, undefined, "PATCH").andReturn({
                                                                                       responseText: JSON.stringify(
                                                                                         updatedMetricsSettings),
                                                                                       status: 200,
                                                                                       responseHeaders: {
                                                                                         "Content-Type": "application/vnd.go.cd.v1+json",
                                                                                       }
                                                                                     });

      const metricsSettings = DataSharingSettings.fromJSON(dataSharingSettingsJSON);

      const successCallback = jasmine.createSpy().and.callFake(() => {
        expect(metricsSettings.allowed()).toBe(updatedMetricsSettings._embedded.allow);
        expect(metricsSettings.updatedBy()).toBe(updatedMetricsSettings._embedded.updated_by);
      });

      expect(metricsSettings.allowed()).toBe(true);
      metricsSettings.toggleConsent();
      expect(metricsSettings.allowed()).toBe(false);

      metricsSettings.save().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });
});
