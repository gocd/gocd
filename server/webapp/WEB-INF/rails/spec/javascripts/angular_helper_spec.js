/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END**********************************/

describe("angular_helper", function(){
    beforeEach(function(){
        setFixtures("<form id=\"form_id\" name=\"form_id\">\n" +
                "</form>"
        );
    });

    afterEach(function(){
    });

    it("test_should_define_angular_app", function(){
        new AngularHelper().defineModule("foo");
        assertNotNull(angular.module("foo"))
    });

    it("test_should_check_angular_app_defined", function(){
        new AngularHelper().defineModule("defined-app");
        var definedApp = new AngularHelper().isDefined("defined-app");
        var undefinedApp = new AngularHelper().isDefined("undefined-app");
        assertTrue(definedApp)
        assertFalse(undefinedApp)
    });
});

