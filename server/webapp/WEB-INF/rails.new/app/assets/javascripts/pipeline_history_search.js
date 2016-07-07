<!-- Code for searching through labels on pipeline history page -->
function applyLabelFilter() {
    var inputVal = jQuery("#labelFilterField").val().trim();
    if (inputVal.length == 0) {
        removeLabelFilter();
        return;
    }
    jQuery('#search-message').text("");
    jQuery('#labelFilterClear').show();
    jQuery.get("/go/pipelineHistory.json?pipelineName="+pipelineName+"&start=0&perPage=25&labelFilter="+inputVal, function (data) {
        filterHistories(data, inputVal);
    });
}

// Filters histories that match the search text. Rerenders the view with histories that match the text
function filterHistories(pipelineHistory, filter) {
    //Need to stop periodic executor to show only the pipelines matching the filter
    dashboard_periodical_executer.stop();
    $('page_links').innerHTML = "";

    var histories = pipelineHistory.groups[0].history;
    var count = histories.length;
    if(count == 0) {
        jQuery('.pipeline-history-group').html("");
        jQuery('#search-message').text("No instances found matching \"" + filter + "\"");
        return;
    }

    pipelineHistory.showForceBuildButton = false;
    $('pipeline-history').innerHTML = pipelineHistoryObserver.getTemplate().process(
        { data : pipelineHistory, _MODIFIERS: {
            escapeQuotes: function(str) {
                return str.replace(/"/g, '&quot;');
            },
        }});

}

// Removes the applied filter (if there is one) and resumes the periodic executer
function removeLabelFilter() {
    jQuery("#labelFilterField").val("");
    jQuery('#search-message').text("");
    jQuery('#labelFilterClear').hide();

    if(!dashboard_periodical_executer.is_execution_start)
        dashboard_periodical_executer.start();
}

jQuery( document ).ready(function() {
    //Trigger filter when the input field changes
    jQuery('#labelFilterField').on('input', function () {
        applyLabelFilter();
    });
    jQuery('#labelFilterClear').hide();
    jQuery('#labelFilterClear').click(function(){
        removeLabelFilter();
    });
});