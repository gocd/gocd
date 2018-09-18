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

/* utf-8 encoding */
var caution = true;

var default_cookie_expires = new Date();
//default expires = 1 year
default_cookie_expires.setTime(default_cookie_expires.getTime() + 12 * 30 * 24 * 60 * 60 * 1000);

/* name - name of the cookie
 * value - value of the cookie
 * [expires] - expiration date of the cookie
 * (defaults to end of current session)
 * [path] - path for which the cookie is valid
 * (defaults to path of calling document)
 * [domain] - domain for which the cookie is valid
 * (defaults to domain of calling document)
 * [secure] - Boolean value indicating if
 * the cookie transmission requires a secure transmission
 * an argument defaults when it is assigned null as a placeholder
 * a null placeholder is not required for trailing omitted arguments
 */
function setCookie(name, value, expires, path, domain, secure) {
    var curCookie = name + "=" + escape(value) +
                    ((expires) ? "; expires=" + expires.toGMTString() : "; expires=" + default_cookie_expires.toGMTString()) +
                    ((path) ? "; path=" + path : "") +
                    ((domain) ? "; domain=" + domain : ";") +
                    ((secure) ? "; secure" : "");
    if (!caution || (name + "=" + escape(value)).length <= 4000) {
        document.cookie = curCookie;
    }
    else {
        if (confirm("Cookie size exceed 4KB，please empty your cookie.")){
            document.cookie = curCookie;
        }
    }
}

/*s name - name of the cookie
 * return string containing value
 * of specified cookie or null if cookie
 * does not exist
 */
function getCookie(name) {
    var prefix = name + "=";
    var cookieStartIndex = document.cookie.indexOf(prefix);
    if (cookieStartIndex == -1){
        return null;
    }
    var cookieEndIndex = document.cookie.indexOf(";", cookieStartIndex +
                                                      prefix.length);
    if (cookieEndIndex == -1){
        cookieEndIndex = document.cookie.length;
    }
    return unescape(document.cookie.substring(cookieStartIndex +
                                              prefix.length,cookieEndIndex));
}

/* name - name of the cookie
 * [path] - path of the cookie
 * (must be same as path used to create cookie)
 * [domain] - domain of the cookie
 * (must be same as domain used to create cookie)
 * path and domain default if assigned
 * null or omitted if no explicit argument proceeds
 */
function deleteCookie(name, path, domain) {
    if (getCookie(name)) {
        document.cookie = name + "=" +
                          ((path) ? "; path=" + path : "") +
                          ((domain) ? "; domain=" + domain : "") +
                          "; expires=Thu, 01-Jan-70 00:00:01 GMT";
    }
}
