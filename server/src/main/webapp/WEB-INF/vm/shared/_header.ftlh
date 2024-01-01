<#--
 * Copyright 2024 Thoughtworks, Inc.
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
 -->
<!DOCTYPE HTML>
<html xmlns="http://www.w3.org/1999/xhtml" lang='en'>

<head>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
  <link href="${req.getContextPath()}/${concatenatedCruiseIconFilePath}" rel="shortcut icon"/>
  <link rel="stylesheet" type="text/css" href="${req.getContextPath()}/${pathResolver.getAssetPath("application.css")}" media="all"/>
  <link rel="stylesheet" type="text/css" href="${req.getContextPath()}/${pathResolver.getAssetPath("css/application.css")}" media="all"/>
  <link rel="stylesheet" type="text/css" href="${req.getContextPath()}/${pathResolver.getAssetPath("vm/application.css")}" media="all"/>

  <link rel="stylesheet" type="text/css" href="${req.getContextPath()}/${pathResolver.getAssetPath("new-theme.css")}" media="all"/>
  <script type="text/javascript" src="${req.getContextPath()}/${pathResolver.getAssetPath("application.js")}"></script>
  <script type="text/javascript"> var contextPath = "${req.getContextPath()}"; </script>

    <#list webpackAssetsService.getJSAssetPathsFor("single_page_apps/polyfill", "single_page_apps/header_footer_shim") as js>
        <script src="${js}"></script>
    </#list>

    <#list webpackAssetsService.getCSSAssetPathsFor("single_page_apps/header_footer_shim") as css>
        <link rel="stylesheet" type="text/css" href="${css}" />
    </#list>

  <title>
      <#if title??>
          ${title}
      </#if>
  </title>
</head>
<body
  data-is-user-admin="${userHasAdministratorRights?c}"
  data-current-gocd-version="${currentGoCDVersion.getGocdDistVersion()}"
  data-can-user-view-admin="${userHasViewAdministratorRights?c}"
  data-is-user-group-admin="${userHasGroupAdministratorRights?c}"
  data-can-user-view-templates="${userHasTemplateViewUserRights?c}"
  data-show-analytics-dashboard="${showAnalyticsDashboard?c}"
  data-user-display-name="${principal}"
  data-user-anonymous="${isAnonymousUser?c}"
  data-is-server-in-maintenance-mode="${maintenanceModeService.isMaintenanceMode()?c}"
  <#if maintenanceModeService.isMaintenanceMode()>
      data-maintenance-mode-updated-on = "${maintenanceModeService.updatedOn()}"
      data-maintenance-mode-updated-by = "${maintenanceModeService.updatedBy()}"
  </#if>
  >
<div class="page-wrap">
  <div id="body_bg">
    <div id="body_bg_highlight">
      <div id="body_wrapper">

        <div id="doc3" class="yui-t7">

          <div id="app-menu">
          </div>

          <div id="bd">
