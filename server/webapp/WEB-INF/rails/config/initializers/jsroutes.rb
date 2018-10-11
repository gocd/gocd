JsRoutes.setup do |config|
  config.prefix = com.thoughtworks.go.util.SystemEnvironment.new.getWebappContextPath
  config.camel_case = true
  config.include = [
    /analytics/,
    /^api_internal/,
    /^apiv\d/,
    /^admin_elastic_profile/,
    /^admin_status_report/,
    /^pipeline_groups/,
    /^environments/,
    /^environment/,
    /^pipeline_group/,
    /^pipeline_edit/,
    /^edit_admin_pipeline_config/,
    /stage_detail_tab/
  ]
end
