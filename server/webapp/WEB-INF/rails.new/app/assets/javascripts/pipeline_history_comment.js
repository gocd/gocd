function initPipelineHistoryComment(jQuery, Modalbox, dashboardPeriodicalExecuter) {
    var my = {};

    my.showModal = function (pipelineLabel) {
        var div = jQuery('#comment-form-' + pipelineLabel)[0];

        Modalbox.show(div);
    };

    my.submit = function (pipelineName, pipelineLabel) {
        var commentText = jQuery('#comment-input').val();

        jQuery.post(
            '/go/pipelines/' + pipelineName + '/' + pipelineLabel + '/comment',
            { comment: commentText },
            my.onCommentSuccessCloseModalAndRefreshPipelineHistory,
            'json'
        );
    };

    my.onCommentSuccessCloseModalAndRefreshPipelineHistory = function () {
        Modalbox.hide();
        dashboardPeriodicalExecuter.fireNow();
    };

    return my;
}
