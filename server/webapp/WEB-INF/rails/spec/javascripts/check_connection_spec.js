/*
 * Copyright 2015 ThoughtWorks, Inc.
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

describe("check_connection", function () {
    var orig = WizardPage.checkConnection;
    var actualPipeline, actualMaterialUrl, actualMaterialType, actualUsername, actualPassword, actualIsEncrypted;

    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <button id=\"check_connection\"></button>\n" +
            "    <input class=\"url\" value=\"url\"/>\n" +
            "    <input class=\"username\" value=\"user\"/>\n" +
            "    <input class=\"password\" value=\"Password\"/>\n" +
            "    <input class=\"encryptedPassword\" value=\"Encrypted Password\"/>\n" +
            "    <input type=\"checkbox\" class=\"passwordNotChanged\" value=\"0\"/>\n" +
            "    <input type=\"checkbox\" class=\"passwordChanged\" checked=\"checked\" value=\"0\"/>\n" +
            "</div>");
        WizardPage.checkConnection = function (pipeline, username, password, materialUrl, materialType, isEncrypted) {
            actualPipeline = pipeline;
            actualMaterialUrl = materialUrl;
            actualMaterialType = materialType;
            actualUsername = username;
            actualPassword = password;
            actualIsEncrypted = isEncrypted;
        };
    });

    afterEach(function () {
        WizardPage.checkConnection = orig;
    });

    it("testShouldUseTheEncryptedPasswordIfSet", function () {
        new CheckConnection().hookupCheckConnection("#check_connection", "foo-pipeline", "svn", ".url", ".username", ".password", ".encryptedPassword", ".passwordNotChanged");

        fire_event($("check_connection"), "click");

        assertEquals("foo-pipeline", actualPipeline);
        assertEquals("svn", actualMaterialType);
        assertEquals("user", actualUsername);
        assertEquals("Encrypted Password", actualPassword);
        assertEquals(true, actualIsEncrypted);
        assertEquals("url", actualMaterialUrl);
    });

    it("testShouldUseThePasswordIfItIsChangedSet", function () {
        new CheckConnection().hookupCheckConnection("#check_connection", "foo-pipeline", "svn", ".url", ".username", ".password", ".encryptedPassword", ".passwordChanged");

        fire_event($("check_connection"), "click");

        assertEquals("foo-pipeline", actualPipeline);
        assertEquals("svn", actualMaterialType);
        assertEquals("user", actualUsername);
        assertEquals("Password", actualPassword);
        assertEquals(false, actualIsEncrypted);
        assertEquals("url", actualMaterialUrl);
    });

    it("testShouldUseThePasswordIfEncryptedIsFalse", function () {
        new CheckConnection().hookupCheckConnection("#check_connection", "foo-pipeline", "svn", ".url", ".username", ".password", "false", ".passwordChanged");

        fire_event($("check_connection"), "click");

        assertEquals("foo-pipeline", actualPipeline);
        assertEquals("svn", actualMaterialType);
        assertEquals("user", actualUsername);
        assertEquals("Password", actualPassword);
        assertEquals(false, actualIsEncrypted);
        assertEquals("url", actualMaterialUrl);
    });

    it("testShouldNotSetPasswordIfNothingIsPassed", function () {
        new CheckConnection().hookupCheckConnection("#check_connection", "foo-pipeline", "svn", ".url", ".username", "", "false", ".passwordChanged");

        fire_event($("check_connection"), "click");

        assertEquals("foo-pipeline", actualPipeline);
        assertEquals("svn", actualMaterialType);
        assertEquals("user", actualUsername);
        assertEquals('', actualPassword);
        assertEquals(false, actualIsEncrypted);
        assertEquals("url", actualMaterialUrl);
    });
});
