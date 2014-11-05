function initPipelineHistoryComment(jQuery, Modalbox, dashboardPeriodicalExecuter) {
    var my = {};

    my.showModal = function (pipelineName, pipelineLabel) {
        var div = jQuery('#comment-form-' + pipelineLabel)[0];
        var title = 'Comment on ' + pipelineName + ' build ' + pipelineLabel;

        Modalbox.show(div, { title: title });
    };

    my.submit = function (pipelineName, pipelineLabel) {
        var commentText = jQuery('#comment-input').val();
        var url = '/go/pipelines/' + pipelineName + '/' + pipelineLabel + '/comment';

        jQuery.post(
            url,
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
