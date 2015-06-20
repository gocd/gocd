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

describe("command_auto_complete", function () {
    var originalAjax;
    var originalGet;
    var url = "http://foo.bar/go/autocomplete";

    beforeEach(function () {
        setFixtures("<div class='under_test'>\n" +
            "    <input type=\"text\" class=\"command\" name=\"command\"/>\n" +
            "    <textarea class=\"arguments\"></textarea>\n" +
            "\n" +
            "    <div class=\"gist_based_auto_complete\">\n" +
            "        <input type=\"text\" class=\"lookup_command\" name=\"lookup_command\"/>\n" +
            "    </div>\n" +
            "\n" +
            "    <div class=\"snippet_details hidden\">\n" +
            "        <div class=\"name\">\n" +
            "            <span class=\"value\"></span>\n" +
            "        </div>\n" +
            "        <div class=\"description\">\n" +
            "            <span class=\"value\"></span>\n" +
            "        </div>\n" +
            "        <div class=\"author\">\n" +
            "            <span class=\"key\">Author:</span>\n" +
            "            <span class=\"value\"></span>\n" +
            "            <span class=\"value-with-link\"><a></a></span>\n" +
            "        </div>\n" +
            "        <div class=\"more-info\">\n" +
            "            <span class=\"value-with-link\"><a>more info</a></span>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <div class=\"invalid_snippets\">\n" +
            "    </div>\n" +
            "</div>\n" +
            "\n" +
            "<div class='under_test_for_old_style_args'>\n" +
            "    <input type=\"text\" class=\"command\" name=\"command\"/>\n" +
            "    <input type=\"text\" class=\"arguments-text-box-not-text-area\"/>\n" +
            "\n" +
            "    <div class=\"gist_based_auto_complete\">\n" +
            "        <input type=\"text\" class=\"lookup_command\" name=\"lookup_command\"/>\n" +
            "        <div class=\"error-message-for-old-args hidden\">The lookup feature is only available for the new style of custom commands.</div>\n" +
            "    </div>\n" +
            "\n" +
            "    <div class=\"snippet_details hidden\">\n" +
            "    </div>\n" +
            "</div>");
        originalAjax = jQuery.ajax;
        originalGet = jQuery.get;
    });

    afterEach(function () {
        jQuery.ajax = originalAjax;
        jQuery.get = originalGet;
    });

    it("testShouldSendWhateverIsEnteredInTheTextboxToTheAjaxCallToLookup", function () {
        var dataItWasCalledWith = null;
        jQuery.ajax = function (data) {
            dataItWasCalledWith = data;
        };
        var commandLookup = new CommandSnippetLookup(jQuery(".under_test .lookup_command").get(0), url);
        jQuery(".under_test .lookup_command").val("");
        commandLookup.hookupAutocomplete();

        var autoCompleteText = jQuery(".under_test .lookup_command");
        autoCompleteText.val("ms");
        autoCompleteText.search();
        assertNotNull(dataItWasCalledWith);
        assertEquals("ms", dataItWasCalledWith.data.lookup_prefix);
        assertEquals(url, dataItWasCalledWith.url);
    });

    it("testShouldPopulateCommandAndArgumentsWhenASnippetIsSelected", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n"};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("ls", jQuery(".under_test .command").val());
        assertEquals("-abc\n-def\n", jQuery(".under_test .arguments").val());
    });

    it("testShouldPopulateNameAndDescriptionWhenASnippetIsSelected", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", name: "Command name", description: "Some description"};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("Command name", jQuery(".under_test .snippet_details .name .value").text());
        assertEquals("Some description", jQuery(".under_test .snippet_details .description .value").text());
    });

    it("testShouldPopulateAuthorDataWithoutLinkWhenAuthorInfoIsEmpty", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: "Author - 1", authorinfo: null, moreinfo: "some-link"};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("Author - 1", jQuery(".under_test .snippet_details .author .value").text());
        assertTrue("Author should be visible", jQuery(".under_test .snippet_details .author .value").is(":visible"));
        assertTrue("Author with link should not be visible", jQuery(".under_test .snippet_details .author .value-with-link a").is(":hidden"));
    });

    it("testShouldPopulateAuthorDataWithLinkWhenAuthorInfoIsNotEmpty", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: "Author - 1", authorinfo: "http://author.link", moreinfo: "some-link"};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("Author - 1", jQuery(".under_test .snippet_details .author .value-with-link a").text());
        assertEquals("http://author.link", jQuery(".under_test .snippet_details .author .value-with-link a").attr('href'));
        assertTrue("Author should not be visible", jQuery(".under_test .snippet_details .author .value").is(":hidden"));
        assertTrue("Author with link should be visible", jQuery(".under_test .snippet_details .author .value-with-link a").is(":visible"));
    });

    it("testShouldPopulateMoreInfoIfMoreInfoIsNotEmpty", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: "Author - 1", authorinfo: "http://author.link", moreinfo: "http://some-link"};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("more info", jQuery(".under_test .snippet_details .more-info .value-with-link a").text());
        assertEquals("http://some-link", jQuery(".under_test .snippet_details .more-info .value-with-link a").attr('href'));
        assertTrue("more info should be visible", jQuery(".under_test .snippet_details .more-info").is(':visible'));
    });

    it("testShouldHideMoreInfoIfMoreInfoIsEmpty", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: "Author - 1", authorinfo: "http://author.link", moreinfo: null};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertTrue("more info should be hidden", jQuery(".under_test .snippet_details .more-info .value-with-link a").is(':hidden'));
    });

    it("testShouldHideAuthorInfoIfAllAuthorInfoIsEmpty", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: null, authorinfo: null, moreinfo: null};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertTrue("Author info should be hidden", jQuery(".under_test .snippet_details .author").is(':hidden'));
    });

    it("", function () {
    });

    it("testShouldDefaultAuthorNameIfOnlyAuthorInfoIsAvailable", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: null, authorinfo: "http://author.link", moreinfo: null};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("link", jQuery(".under_test .snippet_details .author .value-with-link a").text());
        assertEquals("http://author.link", jQuery(".under_test .snippet_details .author .value-with-link a").attr('href'));
        assertTrue("Author should not be visible", jQuery(".under_test .snippet_details .author .value").is(":hidden"));
        assertTrue("Author with link should be visible", jQuery(".under_test .snippet_details .author .value-with-link a").is(":visible"));
    });

    it("testShouldDefaultDefaultDescriptionIfItIsNotAvailable", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: null, description: null};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("No description available.", jQuery(".under_test .snippet_details .description .value").text());
    });

    it("testShouldAddHttpSchemePrefixToAuthorInfoLinkIfItDoesNotStartWithASchemeFollowedByColonFollowedByTwoSlashes", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: "author", authorinfo: "author.link", moreinfo: null};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("http://author.link", jQuery(".under_test .snippet_details .author .value-with-link a").attr('href'));
    });

    it("testShouldNotAddHttpSchemePrefixToAuthorInfoLinkIfItStartsWithASchemeFollowedByColonFollowedByTwoSlashes", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: "author", authorinfo: "HTTPS://author.link", moreinfo: null};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("HTTPS://author.link", jQuery(".under_test .snippet_details .author .value-with-link a").attr('href'));
    });

    it("testShouldAddHttpSchemePrefixToMoreInfoInfoLinkIfItDoesNotStartWithASchemeFollowedByColonFollowedByTwoSlashes", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: null, authorinfo: null, moreinfo: "moreinfo.link"};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("http://moreinfo.link", jQuery(".under_test .snippet_details .more-info .value-with-link a").attr('href'));
    });

    it("testShouldNotAddHttpSchemePrefixToMoreInfoLinkIfItStartsWithASchemeFollowedByColonFollowedByTwoSlashes", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: null, authorinfo: null, moreinfo: "ftp://moreinfo.link"};
        setupCommandSnippetData(fakeDataFromServer);

        triggerSelectionFromAutoCompleteOptions();

        assertEquals("ftp://moreinfo.link", jQuery(".under_test .snippet_details .more-info .value-with-link a").attr('href'));
    });

    it("testShouldDisableCommandRepositoryFeatureIfArgumentsAreOldStyleArgs", function () {
        TaskSnippet.attachClickHandlers('.under_test_for_old_style_args', 'some-command-lookup-url', 'some-command-definition-url', ".command", ".arguments-textarea-which-cannot-be-found");

        assertTrue("Lookup command textbox should be disabled", jQuery(".under_test_for_old_style_args .lookup_command").is(":disabled"));
        assertEquals("The lookup feature is only available for the new style of custom commands.", jQuery(".under_test_for_old_style_args .error-message-for-old-args").text());
        assertTrue("Message about old style args for command repo should be visible", jQuery(".under_test_for_old_style_args .error-message-for-old-args").is(':visible'));
    });

    it("testShouldHideInvalidSnippetDetailsOnFirstCommandSnippetSelection", function () {
        var fakeDataFromServer = {command: "ls", arguments: "-abc\n-def\n", author: null, authorinfo: "http://author.link", moreinfo: null};
        setupCommandSnippetData(fakeDataFromServer);

        jQuery(".under_test .invalid_snippets").show();

        triggerSelectionFromAutoCompleteOptions();

        assertTrue("Invalid Snippet Details should be hidden. ", jQuery(".under_test .invalid_snippets").is(":hidden"));
    });

    function setupCommandSnippetData(fakeDataFromServer) {
        var urlItWasCalledWith = null;
        var queryParamsItWasCalledWith = null;
        jQuery.get = function (url, queryParams, callBack) {
            urlItWasCalledWith = url;
            queryParamsItWasCalledWith = queryParams;
            return callBack(fakeDataFromServer);
        };

        TaskSnippet.attachClickHandlers('.under_test', 'some-command-lookup-url', 'some-command-definition-url', ".command", ".arguments");
    }

    function triggerSelectionFromAutoCompleteOptions() {
        jQuery(".under_test .lookup_command").trigger('result', ["msbuild", "/path/to/msbuild.xml"]);
    }
});