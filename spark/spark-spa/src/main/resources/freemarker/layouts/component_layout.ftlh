<#--
 Copyright 2020 ThoughtWorks, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
  -->
<!DOCTYPE html>
<html class="no-js">
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=Edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="gocd-params"
        data-timezone="${serverTimezoneUTCOffset}"
        data-page-refresh-interval="${spaRefreshInterval}"
        data-page-timeout="${spaTimeout}">
  <title>${viewTitle} - Go</title>

  <link rel="shortcut icon" href="/go/${railsAssetsService.getAssetPath('cruise.ico')}"/>
  <#list webpackAssetsService.getCSSAssetPathsFor("single_page_apps/global", "single_page_apps/${controllerName}") as css>
      <link href="${css}" media="screen" rel="stylesheet"/>
  </#list>

  <#list webpackAssetsService.getJSAssetPathsFor("single_page_apps/global", "single_page_apps/polyfill", "single_page_apps/${controllerName}") as js>
      <script src="${js}"></script>
  </#list>

</head>

<body id="${controllerName}-page"
      data-controller-name="${controllerName}"
      data-current-gocd-version="${currentGoCDVersion}"
      data-is-user-admin="${securityService.isUserAdmin(currentUser)?c}"
      data-can-user-view-admin="${securityService.canViewAdminPage(currentUser)?c}"
      data-is-user-group-admin="${securityService.isUserGroupAdmin(currentUser)?c}"
      data-can-user-view-templates="${securityService.isAuthorizedToViewTemplates(currentUser)?c}"
      data-show-analytics-dashboard="${showAnalyticsDashboard?c}"
      data-user-display-name="${currentUser.displayName}"
      data-user-anonymous="${currentUser.anonymous?c}"
      data-is-server-in-maintenance-mode="${maintenanceModeService.isMaintenanceMode()?c}"
      <#if maintenanceModeService.isMaintenanceMode() >
      data-maintenance-mode-updated-on="${maintenanceModeService.updatedOn()}"
      data-maintenance-mode-updated-by="${maintenanceModeService.updatedBy()}"
      </#if>
      data-show-materials-spa="${show_materials_spa?c}"
      data-meta="${meta}"
      >
    <#if devMode>
    <script>
      var oldManifestValue = null;

      function checkUpdates(url) {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', url + '?' + new Date().getTime());
        xhr.onreadystatechange = function () {
          if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
              var json = JSON.parse(xhr.responseText)
              if (oldManifestValue == null) {
                oldManifestValue = json.hash;
                return;
              } else if (oldManifestValue != json.hash) {
                window.location.reload(true)
              }
            }
          }
        };
        xhr.send();
      }

      if (window.location.search.indexOf('no_hot_reload') === -1) {
        setInterval(function () {
          checkUpdates('/go/assets/webpack/manifest.json')
        }, 1000);
      }
    </script>
    </#if>
</body>
</html>
