function context_path(path_info) {
    return contextPath + "/" + path_info
}

var ACTIVE_STATUS = $A(['passed', 'failed', 'inactive', 'discontinued', 'paused', 'queued', 'scheduled', 'assigned',
    'preparing', 'building', 'completing', 'building_passed',
    'building_failed', 'building_unknown', 'unknown',
    'level_0', 'level_1', 'level_2', 'level_3', 'level_4', 'level_5', 'level_6', 'level_7', 'level_8']);

function clean_active_css_class_on_element(element) {
    ACTIVE_STATUS.each(function(status) {
        Element.removeClassName($(element), status);
        Element.removeClassName($(element), 'build_profile_' + status);
    });
}

function is_inactive(json) {
    return json.building_info.current_status.toLowerCase() != 'building'
            && json.building_info.result.toLowerCase() == 'unknown';
}

function is_result_unknown(json) {
    return json.building_info.result.toLowerCase() == 'unknown';
}

function is_discontinued(json) {
    return json.building_info.current_status.toLowerCase() == 'discontinued';
}

function is_paused(json) {
    return json.building_info.current_status.toLowerCase() == 'paused';
}

function is_building(json) {
    var status = json.building_info.current_status.toLowerCase();
    return status == 'preparing' || status == 'building' || status == 'completing';
}

function is_queued(json) {
    var status = json.building_info.current_status.toLowerCase();
    return status == 'scheduled' || status == 'assigned';
}

function is_started(json) {
    var status = json.building_info.current_status.toLowerCase();
    return status == 'scheduled';
}

function isEstimatable(status) {
    if (!status) return false;
    var buildStatus = status.toLowerCase();
    return buildStatus == 'building' || buildStatus == 'completing';
}

function is_stage_completed(json){
    var status = json.stage.current_status.toLowerCase();
    return status == 'passed' || status == 'failed' || status == 'cancelled';
}

function is_stage_building(json){
    var status = json.stage.current_status.toLowerCase();
    return status == 'building' || status == 'failing';
}

function should_forcebuild_be_disabled(json) {
    return is_discontinued(json) || is_paused(json) || is_building(json) || is_queued(json);
}
