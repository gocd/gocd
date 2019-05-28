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
 describe("PipelineHistoryComment", function() {
    var fakeModalBox = jasmine.createSpyObj('Modalbox', ['hide', 'show']);
    var fakeDashboardPeriodicalExecutor = jasmine.createSpyObj('DashboardPeriodicalExecutor', [ 'fireNow' ]);

    var pipelineHistoryComment = initPipelineHistoryComment(jQuery, fakeModalBox, fakeDashboardPeriodicalExecutor);

    describe("#showModal", function() {
        beforeEach(function() {
            setFixtures('<div id="comment-form-1"></div>');
        });

        it("shows the modal with the correct build comment form", function () {
            pipelineHistoryComment.showModal('test-pipeline', 'label', '1');
            var args = fakeModalBox.show.calls.argsFor(0);
            expect(args[0]).toEqual(jQuery('#comment-form-1')[0]);
            expect(args[1]).toEqual({ title: 'Comment on pipeline: test-pipeline label: label' });
        });
    });

    describe("#submit", function() {
        beforeEach(function() {
            setFixtures('<input type="text" id="comment-input" value="This is the comment."/>');
        });

        it("submits a request with the new modal text", function() {
            spyOn(jQuery, "ajax").and.callFake(function (options) {
                options.success();
            });

            pipelineHistoryComment.submit('test-pipeline', '1');

            expect(jQuery.ajax).toHaveBeenCalled();
            expect(jQuery.ajax.calls.mostRecent().args[0]['url']).toBe('/go/pipelines/test-pipeline/1/comment');
            expect(jQuery.ajax.calls.mostRecent().args[0]['type']).toBe('POST');
            expect(jQuery.ajax.calls.mostRecent().args[0]['dataType']).toBe('json');
            expect(jQuery.ajax.calls.mostRecent().args[0]['headers'].Confirm).toBe("true");
            expect(jQuery.ajax.calls.mostRecent().args[0]['data'].comment).toBe("This is the comment.");
        });

    });

    describe("#onCommentSuccessCloseModalAndRefreshPipelineHistory", function() {
        it("closes the modal box", function() {
            pipelineHistoryComment.onCommentSuccessCloseModalAndRefreshPipelineHistory();
            expect(fakeModalBox.hide).toHaveBeenCalled();
        });

        it("refreshes the pipeline history", function() {
            pipelineHistoryComment.onCommentSuccessCloseModalAndRefreshPipelineHistory();
            expect(fakeDashboardPeriodicalExecutor.fireNow).toHaveBeenCalled();
        });

    });
});
