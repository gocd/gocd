/*
 * Copyright 2020 ThoughtWorks, Inc.
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
