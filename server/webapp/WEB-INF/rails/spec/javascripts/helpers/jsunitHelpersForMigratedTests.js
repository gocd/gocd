jQuery.browser.msie = false;

function fail(failureMessage) {
    throw "Call to fail()" + failureMessage;
}

function fire_event(element, event, before) {
    if( document.createEvent ) {
        var evt = document.createEvent("HTMLEvents");
        evt.initEvent(event, true, true);
        before && before(evt, element);
        return !$(element).dispatchEvent(evt);
    } else if ( document.createEventObject ) {
        document.createEventObject();
        before && before(event, element);
        return element.fireEvent('on'+event);
    }
}

function passed_json(planName) {
    return construct_new_json(planName, "Waiting", "Passed");
}

function building_json(planName, type) {
    return construct_new_json(planName, "Building", "Passed");
}

function failed_json(planName) {
    return construct_new_json(planName, "Waiting", "Failed");
}

function inactive_json(planName) {
    return construct_new_json(planName, "Waiting", "Unknown");
}

function discontinued_json(planName) {
    return construct_new_json(planName, "Discontinued", "Failed");
}

function paused_json(planName) {
    json = construct_new_json(planName, "Paused", "Passed");
    json.building_info.paused_class_name = "paused";
    return json;
}

function agent_resources(uuid) {
    return {resources : ["jdk1.5", "jdk1.4"], hostname : "machine1", status:'Idle',ipAddress:'192.168.0.45', agentId:uuid};
}

function construct_new_json(projectname, current_status, result) {
    return {building_info :
    {name : projectname,
        build_completed_date : "1 day ago",
        current_status : current_status,
        result : result}}
}

function assertEquals() {
    var actual = null;
    if(arguments.length == 2){
        expected = arguments[0];
        actual = arguments[1];
    }else{
        expected = arguments[1];
        actual = arguments[2];
    }
    expect(actual).toBe(expected);
}

function assertNotEquals() {
    var actual = null;
    if(arguments.length == 2){
        expected = arguments[0];
        actual = arguments[1];
    }else{
        expected = arguments[1];
        actual = arguments[2];
    }
    expect(actual).not.toBe(expected);
}

function assertTrue() {
    var actual = null;
    if(arguments.length >1){
        actual = arguments[1];
    }else{
        actual = arguments[0];
    }
    expect(actual).toBe(true);
}
function assert() {
    var actual = null;
    if(arguments.length >1){
        actual = arguments[1];
    }else{
        actual = arguments[0];
    }
    expect(actual).toBe(true);
}

function assertFalse() {
    var actual = null;
    if(arguments.length >1){
        actual = arguments[1];
    }else{
        actual = arguments[0];
    }
    expect(actual).toBe(false);
}

function assertNotNull() {
    var actual = null;
    if(arguments.length >1){
        actual = arguments[1];
    }else{
        actual = arguments[0];
    }
    expect(actual).not.toBeNull();
}

function assertNull() {
    var actual = null;
    if(arguments.length >1){
        actual = arguments[1];
    }else{
        actual = arguments[0];
    }
    expect(actual).toBeNull();
}

function assertUndefined() {
    var actual = null;
    if(arguments.length >1){
        actual = arguments[1];
    }else{
        actual = arguments[0];
    }
    expect(actual).toBe(undefined);
}

function assertNotUndefined() {
    var actual = null;
    if(arguments.length >1){
        actual = arguments[1];
    }else{
        actual = arguments[0];
    }
    expect(actual).not.toBe(undefined);
}

function assertContains() {
    var textToBeSearched = null;
    var maintext = null;

    if(arguments.length > 2){
        textToBeSearched = arguments[1];
        maintext = arguments[2];
    }else{
        textToBeSearched = arguments[0];
        maintext = arguments[1];
    }
    expect(maintext).toContain(textToBeSearched);
}
