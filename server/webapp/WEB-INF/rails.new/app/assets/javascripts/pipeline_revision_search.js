function revisionSearch() {
    var val = jQuery("#revision-search-input").val().trim();
    if(val.length == 0)
        return;
    jQuery.get("/go/revisionsearch.json?revision="+val, function (data) {
        if(data.count == 1) {
            var counter = data.groups[0].history[0].counterOrLabel;
            window.location.href = '/go/pipelines/value_stream_map/' + data.pipelineName + '/' + counter;
        } else {
            window.location.href = '/go/tab/pipeline/search/' + val;
        }
    }).fail(function () {
        window.location.href = '/go/tab/pipeline/search/' + val;
    });
}
jQuery( document ).ready(function() {
    jQuery('#revision-search-input').keypress(function (e) {
        //Enter key
        if (e.keyCode == 13)
            revisionSearch();
    });
});