/*
 * Copyright 2016 ThoughtWorks, Inc.
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

describe("simple-pipeline", function () {
    it("should pass along all properties with password if the password is not encrypted for check connection", function () {
        spyOn(jQuery, "ajax").and.callFake(function (options) {
            options.success({message: "yay"});
        });

        WizardPage.checkConnection("pipelineName", "username", "password", "url", "tfs", false, "projectPath", "domain", "view")
        var requestBody = {
            type: "tfs",
            pipeline_name: "pipelineName",
            attributes: {
                username: "username",
                url: "url",
                domain: "domain",
                project_path: "projectPath",
                view: "view",
                password: "password"
            }
        };

        var ajax_call_arguments = jQuery.ajax.calls.mostRecent().args[0];
        expect(ajax_call_arguments['url']).toBe("/go/api/admin/material_test")
        expect(ajax_call_arguments['data']).toBe(JSON.stringify(requestBody));
        expect(ajax_call_arguments['headers']['Accept']).toBe("application/vnd.go.cd.v1+json");
        expect(ajax_call_arguments['headers']['Content-Type']).toBe("application/json");
        expect(ajax_call_arguments['type']).toBe("POST")
        expect(jQuery.ajax).toHaveBeenCalled();
    });

    it("should pass along all properties with encrypted password if the password is encrypted for check connection", function () {
        spyOn(jQuery, "ajax").and.callFake(function (options) {
            options.success({message: "yay"});
        });

        WizardPage.checkConnection("pipelineName", "username", "encrypted_password", "url", "tfs", true, "projectPath", "domain", "view")
        var requestBody = {
            type: "tfs",
            pipeline_name: "pipelineName",
            attributes: {
                username: "username",
                url: "url",
                domain: "domain",
                project_path: "projectPath",
                view: "view",
                encrypted_password: "encrypted_password"
            }
        };

        var ajax_call_arguments = jQuery.ajax.calls.mostRecent().args[0];
        expect(ajax_call_arguments['url']).toBe("/go/api/admin/material_test")
        expect(ajax_call_arguments['data']).toBe(JSON.stringify(requestBody));
        expect(jQuery.ajax).toHaveBeenCalled();
    });
});
