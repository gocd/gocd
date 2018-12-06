(function () {
  "use strict";

  // This file is shared between new and old pages, so we can't use ES6 syntax as the file
  // isn't guaranteed to be compiled

  /* eslint-disable no-var,prefer-template,object-shorthand,prefer-arrow-callback */
  function XhrPromise(settings) {
    // Uses a native XMLHttpRequest object because jQuery XHR does not support
    // "blob" as a responseType (and doesn't provide a clean way to access the native
    // xhr object)
    return new Promise(function req(resolve, reject) {
      var options = Object.assign({
        type: "GET",
        responseType: "text",
        headers: {}
      }, settings);

      if ("undefined" === typeof options.url) {
        throw new Error("Please specify a `url` in your XhrPromise settings");
      }

      var xhr = new XMLHttpRequest();

      xhr.onreadystatechange = function onreadystatechange() {
        if (4 === xhr.readyState) { // request complete
          if (xhr.status < 400 && xhr.status > 199) {
            resolve({data: xhr.response, xhr: xhr});
          } else {
            reject({error: xhr.response, xhr: xhr});
          }
        }
      };

      xhr.open(options.type, options.url);
      xhr.responseType = options.responseType;

      for (var key in options.headers) {
        xhr.setRequestHeader(key, options.headers[key]);
      }

      if ("function" === typeof options.beforeSend) {
        options.beforeSend(xhr);
      }

      xhr.send();
    });
  }

  if ("undefined" !== typeof module) {
    module.exports = XhrPromise;
  } else {
    window.XhrPromise = XhrPromise;
  }
})();
