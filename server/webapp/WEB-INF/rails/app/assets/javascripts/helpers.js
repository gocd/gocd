function spinny(element) {
    $(element).update('&nbsp;');
    $(element).addClassName('spinny');
}

function removeSpinny(element) {
    $(element).removeClassName('spinny');
}

function showElement(ele,show){
    if(show) ele.show();
    else ele.hide();
}

function goToUrl(url) {
    window.location = window.location.protocol + '//' + window.location.host + url;
}
function redirectToLoginPage(url) {
    goToUrl(url);
}
