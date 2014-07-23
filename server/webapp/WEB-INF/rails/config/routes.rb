##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

ActionController::Routing::Routes.draw do |map|

  unless defined?(CONSTANTS)
    USER_NAME_FORMAT = ROLE_NAME_FORMAT = GROUP_NAME_FORMAT = TEMPLATE_NAME_FORMAT = PIPELINE_NAME_FORMAT = STAGE_NAME_FORMAT = ENVIRONMENT_NAME_FORMAT = /[\w\-][\w\-.]*/
    JOB_NAME_FORMAT = /[\w\-.]+/
    PIPELINE_COUNTER_FORMAT = STAGE_COUNTER_FORMAT = /-?\d+/
    NON_NEGATIVE_INTEGER = /\d+/
    CONSTANTS = true
    PIPELINE_LOCATOR_CONSTRAINTS = {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}
    STAGE_LOCATOR_CONSTRAINTS = {:stage_name => STAGE_NAME_FORMAT, :stage_counter => STAGE_COUNTER_FORMAT}.merge(PIPELINE_LOCATOR_CONSTRAINTS)
    ENVIRONMENT_NAME_CONSTRAINT = {:name => ENVIRONMENT_NAME_FORMAT}
  end

  map.with_options(:no_layout=>true) do |no_layout|
    no_layout.api_pipeline_action 'api/pipelines/:pipeline_name/:action', :action=> ':action', :controller=>'api/pipelines', :pipeline_name => PIPELINE_NAME_FORMAT, :conditions => {:method => :post}

    no_layout.pause_pipeline 'api/pipelines/:pipeline_name/pause', :controller=>'api/pipelines', :action => 'pause', :conditions => {:method => :post}, :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}
    no_layout.unpause_pipeline 'api/pipelines/:pipeline_name/unpause', :controller=>'api/pipelines', :action => 'unpause', :conditions => {:method => :post}, :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}
    no_layout.cancel_stage "api/stages/:id/cancel", :controller => "api/stages", :action => "cancel"
    no_layout.cancel_stage_using_pipeline_stage_name "api/stages/:pipeline_name/:stage_name/cancel", :controller => "api/stages", :action => "cancel_stage_using_pipeline_stage_name"
    no_layout.material_notify "api/material/notify/:post_commit_hook_material_type", :controller => "api/materials", :action => "notify", :conditions => {:method => :post}

    no_layout.match 'api/plugins/status', :action => 'status', :controller => 'api/plugins'
    no_layout.match 'api/users/:username', :action => 'destroy', :controller => 'api/users', :conditions => {:method => :delete}, :requirements => {:username => USER_NAME_FORMAT}
    no_layout.with_options(:format => 'xml') do |xml_routes|
      xml_routes.with_options(:controller => 'api/pipelines') do |pipeline_api|
        pipeline_api.api_pipeline_stage_feed 'api/pipelines/:name/stages.xml', :action=> 'stage_feed', :conditions => {:method => :get}, :requirements => {:name => PIPELINE_NAME_FORMAT}
        pipeline_api.api_pipeline_instance 'api/pipelines/:name/:id.xml', :action=> 'pipeline_instance', :conditions => {:method => :get}, :requirements => {:name => PIPELINE_NAME_FORMAT}
        pipeline_api.card_activity "api/card_activity/:pipeline_name/:from_pipeline_counter/to/:to_pipeline_counter", :action => 'card_activity', :no_layout => true, :conditions => {:method => :get}, :requirements => {:from_pipeline_counter => PIPELINE_COUNTER_FORMAT, :to_pipeline_counter => PIPELINE_COUNTER_FORMAT, :pipeline_name => PIPELINE_NAME_FORMAT}
        pipeline_api.api_pipelines 'api/pipelines.xml', :action=> 'pipelines', :conditions => {:method => :get}
      end

      xml_routes.job 'api/jobs/scheduled.xml', :action=> 'scheduled', :controller=> 'api/jobs'
      xml_routes.job 'api/jobs/:id.xml', :action=> 'index', :controller=> 'api/jobs'

      xml_routes.stage 'api/stages/:id.xml', :action=> 'index', :controller=> 'api/stages'

      xml_routes.server 'api/server.xml', :action=> 'info', :controller=> 'api/server', :format => 'xml'
      xml_routes.server 'api/support', :action=> 'capture_support_info', :controller=> 'api/server', :format => 'text'
      xml_routes.match 'api/users.xml', :action => 'index', :controller => 'api/users'

      xml_routes.material 'api/materials/:id.xml', :controller => "application", :action => "unresolved"
      xml_routes.modification 'api/materials/:materialId/changeset/:modificationId.xml', :controller => "application", :action => "unresolved"

      xml_routes.server 'api/fanin_trace/:name', :action=> 'fanin_trace', :controller=> 'api/fanin_trace', :format => 'text', :requirements => {:name => PIPELINE_NAME_FORMAT}
      xml_routes.server 'api/fanin/:name', :action=> 'fanin', :controller=> 'api/fanin_trace', :format => 'text', :requirements => {:name => PIPELINE_NAME_FORMAT}

      xml_routes.server 'api/process_list', :action=> 'process_list', :controller=> 'api/process_list', :format => 'text'
    end

    no_layout.agents_information 'api/agents', :action => 'index', :controller => 'api/agents', :format => 'json', :conditions => {:method => :get}
    no_layout.api_disable_agent 'api/agents/edit_agents', :action => 'edit_agents', :controller => 'api/agents', :conditions => {:method => :post}
    no_layout.agent_action "api/agents/:uuid/:action", :controller => 'api/agents', :requirements => {:action => /enable|disable|delete/}, :conditions => {:method => :post}

    no_layout.pipeline_material_search "pipelines/material_search", :controller => 'pipelines', :action => 'material_search', :conditions => {:method => :post}
    no_layout.pipeline_show_with_option "pipelines/show_for_trigger", :controller => 'pipelines', :action => 'show_for_trigger', :conditions => {:method => :post}
    no_layout.environment_new "environments/new", :controller => 'environments', :action => 'new', :conditions => {:method => :get}
    no_layout.environment_create "environments/create", :controller => 'environments', :action => 'create', :conditions => {:method => :post}

    [:pipelines, :agents, :variables].each do |action|
        no_layout.send("environment_edit_#{action}", "environments/:name/edit/#{action}", :controller => 'environments', :action => "edit_#{action}", :conditions => {:method => :get}, :requirements => ENVIRONMENT_NAME_CONSTRAINT)
    end
    no_layout.environment_update "environments/:name", :controller => 'environments', :action => 'update', :conditions => {:method => :put},:requirements => ENVIRONMENT_NAME_CONSTRAINT

    no_layout.backup_api_url "api/admin/start_backup", :controller => "api/admin", :action => "start_backup", :conditions => {:method => :post}

    no_layout.admin_command_cache_reload "api/admin/command-repo-cache/reload", :action => "reload_cache", :controller => 'api/commands', :conditions => {:method => :post}
  end

  map.environment_show "environments/:name/show", :controller => 'environments', :action => 'show', :conditions => {:method => :get}, :requirements => ENVIRONMENT_NAME_CONSTRAINT

  map.edit_agents "agents/edit_agents", :controller => 'agents', :action => 'edit_agents', :conditions => {:method => :post}

  map.with_options(:controller => 'agents', :action => 'index') do |agents| #i should be get only
    agents.agents "agents", :format => "html"
    agents.connect "agents.:format"
  end

  map.agent_filter_autocomplete "agents/filter_autocomplete/:action", :controller => 'agent_autocomplete', :requirements => {:action => /resource|os|ip|name|status|environment/}, :conditions => {:method => :get}
  map.agent_detail "agents/:uuid", :controller => 'agent_details', :action => 'show', :conditions => {:method => :get}
  map.agent_grouping_data "agents/:action", :controller => 'agents', :requirements => {:action => /(resource|environment)_selector/}, :conditions => {:method => :post}
  map.job_run_history_on_agent "agents/:uuid/job_run_history", :controller => 'agent_details', :action => "job_run_history", :conditions => {:method => :get}

  map.pipeline_dashboard "pipelines", :controller => 'pipelines', :action => 'index', :conditions => {:method => :get}
  map.build_cause "pipelines/:pipeline_name/:pipeline_counter/build_cause", :controller => 'pipelines', :action => 'build_cause', :no_layout => true, :conditions => {:method => :get}, :requirements => PIPELINE_LOCATOR_CONSTRAINTS
  map.vsm_show "pipelines/value_stream_map/:pipeline_name/:pipeline_counter.:format", :controller => 'value_stream_map', :action => 'show', :conditions => {:method => :get}, :defaults => {:format => :html}, :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}
  map.welcome_page "home", :controller => 'pipelines', :action => 'index', :conditions => {:method => :get}, :format => "html"
  map.pipeline "pipelines/:action", :controller => 'pipelines', :conditions => {:method => :post}

  map.rerun_jobs "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/rerun-jobs", :controller => :stages, :action => :rerun_jobs,:requirements => STAGE_LOCATOR_CONSTRAINTS, :conditions => {:method => :post}
  map.stage_detail_tab "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/:action", :controller => :stages, :requirements => STAGE_LOCATOR_CONSTRAINTS, :action => :overview
  map.stage_detail "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter.:format", :controller => :stages, :action => :overview, :requirements => STAGE_LOCATOR_CONSTRAINTS

  map.with_options(:controller => 'comparison') do |m|
    m.compare_pipelines "compare/:pipeline_name/:from_counter/with/:to_counter", :action => :show, :conditions => {:method => :get}, :requirements => {:from_counter => PIPELINE_COUNTER_FORMAT, :to_counter => PIPELINE_COUNTER_FORMAT, :pipeline_name => PIPELINE_NAME_FORMAT}
    m.compare_pipelines_page "compare/:pipeline_name/page/:page", :action => :page, :conditions => {:method => :get}, :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}
    m.compare_pipelines_list "compare/:pipeline_name/list/compare_with/:other_pipeline_counter", :format => "json", :action => :list, :conditions => {:method => :get}, :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}
    m.compare_pipelines_timeline "compare/:pipeline_name/timeline/:page", :action => :timeline, :conditions => {:method => :get}, :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}
  end

  map.failure_details_internal "failures/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/:job_name/:suite_name/:test_name",
                               :controller => 'failures', :action => 'show', :no_layout => true,
                               :requirements => STAGE_LOCATOR_CONSTRAINTS,
                               :conditions => {:method => :get}

  map.stage_history "history/stage/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter", :controller => :stages, :action => :history, :requirements => STAGE_LOCATOR_CONSTRAINTS, :conditions => {:method => :get}
  map.config_change "config_change/between/:later_md5/and/:earlier_md5", :controller => :stages, :action => :config_change, :conditions => {:method => :get}

  map.run_stage "/run/:pipeline_name/:pipeline_counter/:stage_name", :pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT, :stage_name => STAGE_NAME_FORMAT, :controller => "null", :action => "null"

  map.global_message "server/messages.json", :controller => "server", :action => "messages", :format => "json"

  map.pipeline_status_gadget "gadgets/pipeline.xml", :controller => "gadgets/pipeline", :action => "index", :format => 'xml', :conditions => {:method => :get}
  map.pipeline_status_gadget_content "gadgets/pipeline/content", :controller => "gadgets/pipeline", :action => "content", :no_layout => true, :conditions => {:method => :get}

  map.with_options(:controller => 'environments', :action => 'index') do |env|
    env.environments "environments", :format => "html"
    env.connect "environments.:format"
  end

  map.with_options(:no_layout=>true) do |no_layout|
    no_layout.users_new "admin/users/new", :controller => 'admin/users', :action => 'new', :conditions => {:method => :get}
    no_layout.users_create "admin/users/create", :controller => 'admin/users', :action => 'create', :conditions => {:method => :post}
    no_layout.users_search"admin/users/search", :controller => 'admin/users', :action => 'search', :conditions => {:method => :post}
    no_layout.user_roles "admin/users/roles", :controller => "admin/users", :action => "roles", :conditions => {:method => :post}
    no_layout.users_delete "admin/users/delete_all", :controller => 'admin/users', :action => 'delete_all', :conditions => {:method => :delete} #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war
  end

  map.user_operate "admin/users/operate", :controller => "admin/users", :action => "operate", :conditions => {:method => :post}

  map.with_options(:controller => "admin/users") do |admin|
    admin.user_listing "admin/users", :action => "users"
  end

  map.with_options(:controller => "admin/pipelines_snippet") do |controller|
    controller.pipelines_snippet "admin/pipelines/snippet", :action => "index", :conditions => {:method => :get}
    controller.pipelines_snippet_show "admin/pipelines/snippet/:group_name", :action => "show", :conditions => {:method => :get}, :requirements => {:group_name => GROUP_NAME_FORMAT }
    controller.pipelines_snippet_edit "admin/pipelines/snippet/:group_name/edit", :action => "edit", :conditions => {:method => :get}, :requirements => {:group_name => GROUP_NAME_FORMAT }
    controller.pipelines_snippet_update "admin/pipelines/snippet/:group_name", :action => "update", :conditions => {:method => :put}, :requirements => {:group_name => GROUP_NAME_FORMAT }
  end

  map.with_options(:controller => "admin/backup") do |controller|
    controller.perform_backup "admin/backup", :action => "perform_backup", :conditions => {:method => :post}
    controller.backup_server "admin/backup",  :action => "index", :conditions => {:method => :get}
    controller.delete_backup_history "admin/backup/delete_all",  :action => "delete_all", :conditions => {:method => :delete} #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war
  end

  map.with_options(:controller => "admin/plugins/plugins") do |plugins|
    plugins.plugins_listing "admin/plugins", :action => "index", :conditions => {:method => :get}
  end

  ["svn", "git", "hg", "p4", "dependency", "tfs", "package"].each do |material_type|
    map.with_options(:controller => "admin/materials/#{material_type}", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT }) do |material|
      material.send("admin_#{material_type}_new", "admin/pipelines/:pipeline_name/materials/#{material_type}/new", :action => "new", :conditions => {:method => :get})
      material.send("admin_#{material_type}_create", "admin/pipelines/:pipeline_name/materials/#{material_type}", :action => "create", :conditions => {:method => :post})
      material.send("admin_#{material_type}_edit", "admin/pipelines/:pipeline_name/materials/#{material_type}/:finger_print/edit", :action => "edit", :conditions => {:method => :get})
      material.send("admin_#{material_type}_update", "admin/pipelines/:pipeline_name/materials/#{material_type}/:finger_print", :action => "update", :conditions => {:method => :put})
    end
  end

  map.with_options(:controller => "admin/materials/dependency", :no_layout => true, :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}) do |dep_material|
    dep_material.admin_dependency_material_pipeline_name_search "admin/pipelines/:pipeline_name/materials/dependency/pipeline_name_search", :action => "pipeline_name_search", :conditions => {:method => :get}
    dep_material.admin_dependency_material_load_stage_names_for "admin/pipelines/:pipeline_name/materials/dependency/load_stage_names_for", :action => "load_stage_names_for", :conditions => {:method => :get}
  end

  map.with_options(:controller => "admin/materials", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT }) do |material|
    material.admin_material_index "admin/:stage_parent/:pipeline_name/materials", :action => "index", :defaults => {:stage_parent => "pipelines"}, :conditions => {:method => :get}
    material.admin_material_delete "admin/:stage_parent/:pipeline_name/materials/:finger_print", :action => "destroy", :conditions => {:method => :delete}
  end

  map.pipeline_new "admin/pipeline/new", :controller => "admin/pipelines", :action => "new", :conditions => {:method => :get}
  map.pipeline_create "admin/pipelines", :controller => "admin/pipelines", :action => "create", :conditions => {:method => :post}
  map.pipeline_clone "admin/pipeline/:pipeline_name/clone", :controller => "admin/pipelines", :action => "clone", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}, :conditions => {:method => :get}
  map.pipeline_save_clone "admin/pipeline/save_clone", :controller => "admin/pipelines", :action => "save_clone", :conditions => {:method => :post}
  map.pause_info_refresh "admin/pipelines/:pipeline_name/pause_info.json", :controller => "admin/pipelines", :action => "pause_info", :format => "json", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}, :conditions => {:method => :get}

  map.with_options(:controller => "admin/pipelines", :requirements => {:stage_parent => "pipelines", :pipeline_name => PIPELINE_NAME_FORMAT, :current_tab => /#{["general", "project_management", "environment_variables", "permissions", "parameters"].join("|")}/}) do |pipeline|
    pipeline.pipeline_edit "admin/pipelines/:pipeline_name/:current_tab", :action => "edit",  :conditions => {:method => :get}
    pipeline.pipeline_update "admin/pipelines/:pipeline_name/:current_tab", :action => "update", :conditions => {:method => :put}
  end

  map.with_options(:controller => "admin/stages", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT, :stage_parent => /(pipelines|templates)/}) do |stages|
    stages.admin_stage_listing "admin/:stage_parent/:pipeline_name/stages", :action => "index", :conditions => {:method => :get}
    stages.admin_stage_use_template "admin/:stage_parent/:pipeline_name/stages", :action => "use_template", :defaults => {:stage_parent => "pipelines"},  :conditions => {:method => :put}
    stages.admin_stage_new "admin/:stage_parent/:pipeline_name/stages/new", :action => "new", :conditions => {:method => :get}
    stages.admin_stage_delete "admin/:stage_parent/:pipeline_name/stages/:stage_name", :action => "destroy", :conditions => { :method => :delete}, :requirements => {:stage_name => STAGE_NAME_FORMAT}
    stages.admin_stage_create "admin/:stage_parent/:pipeline_name/stages", :action => "create", :conditions => {:method => :post}

    stages.admin_stage_increment_index "admin/:stage_parent/:pipeline_name/stages/:stage_name/index/increment", :action => "increment_index", :conditions => {:method => :post}, :requirements => {:stage_name => STAGE_NAME_FORMAT}
    stages.admin_stage_decrement_index "admin/:stage_parent/:pipeline_name/stages/:stage_name/index/decrement", :action => "decrement_index", :conditions => {:method => :post}, :requirements => {:stage_name => STAGE_NAME_FORMAT}

    stages.with_options(:controller => "admin/stages", :requirements => {:current_tab => /(settings|environment_variables|permissions)/}) do |stages_edit|
      stages_edit.admin_stage_edit "admin/:stage_parent/:pipeline_name/stages/:stage_name/:current_tab", :action => "edit", :conditions => {:method => :get}, :requirements => {:stage_name => STAGE_NAME_FORMAT}
      stages_edit.admin_stage_update "admin/:stage_parent/:pipeline_name/stages/:stage_name/:current_tab", :action => "update", :conditions => {:method => :put}, :requirements => {:stage_name => STAGE_NAME_FORMAT}
    end

  end

  map.with_options(:controller => "admin/jobs", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT,  :stage_name => STAGE_NAME_FORMAT, :stage_parent => /(pipelines|templates)/}) do |jobs|
    jobs.admin_job_listing "admin/:stage_parent/:pipeline_name/stages/:stage_name/jobs", :action => "index",  :conditions => {:method => :get}
    jobs.admin_job_new "admin/:stage_parent/:pipeline_name/stages/:stage_name/jobs/new", :action => "new", :conditions => {:method => :get}
    jobs.admin_job_create "admin/:stage_parent/:pipeline_name/stages/:stage_name/jobs", :action => "create", :conditions => {:method => :post}
    jobs.admin_job_update "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/:current_tab", :action => "update", :conditions => {:method => :put}, :requirements => {:job_name => JOB_NAME_FORMAT} #TODO: use job name format, so part splitting doesn't mess us up -JJ
    jobs.admin_job_delete "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name", :action => "destroy", :conditions => {:method => :delete}, :requirements => {:job_name => JOB_NAME_FORMAT}

    jobs.with_options(:controller => "admin/jobs", :requirements => {:current_tab => /#{["settings", "environment_variables", "artifacts", "resources", "tabs"].join("|")}/}) do |job_edit|
      job_edit.admin_job_edit "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/:current_tab", :action => "edit", :conditions => {:method => :get}, :requirements => {:job_name => JOB_NAME_FORMAT}
      job_edit.admin_job_update "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/:current_tab", :action => "update", :conditions => {:method => :put}, :requirements => {:job_name => JOB_NAME_FORMAT}
    end
  end

  map.with_options(:controller => "admin/commands") do |commands|
    commands.admin_commands "admin/commands", :action => "index", :conditions => {:method => :get}
    commands.admin_command_definition "admin/commands/show", :action => "show", :conditions => {:method => :get}
    commands.admin_command_lookup "admin/commands/lookup", :action => "lookup", :conditions => {:method => :get}, :format => "text"
  end

  map.with_options(:controller => "admin/tasks", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT, :stage_name => STAGE_NAME_FORMAT, :job_name => JOB_NAME_FORMAT}, :current_tab => "tasks") do |tasks|
    tasks.admin_tasks_listing "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks", :action => "index", :conditions => {:method => :get}
    tasks.admin_task_new "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/new", :action => "new", :conditions => {:method => :get}
    tasks.admin_task_create "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type", :action => "create", :conditions => {:method => :post}
    tasks.admin_task_increment_index "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/task/:task_index/index/increment", :action => "increment_index", :conditions => {:method => :post}, :requirements => {:stage_name => STAGE_NAME_FORMAT, :job_name => JOB_NAME_FORMAT}
    tasks.admin_task_decrement_index "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/task/:task_index/index/decrement", :action => "decrement_index", :conditions => {:method => :post}, :requirements => {:stage_name => STAGE_NAME_FORMAT, :job_name => JOB_NAME_FORMAT}
    tasks.with_options(:requirements => {:task_index => NON_NEGATIVE_INTEGER}) do |task|
      task.admin_task_edit "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/:task_index/edit", :action => "edit", :conditions => {:method => :get}
      task.admin_task_delete "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:task_index", :action => "destroy", :conditions => {:method => :delete}
      task.admin_task_update "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/:task_index", :action => "update", :conditions => {:method => :put}
    end
  end

  map.with_options(:controller => "admin/configuration") do |config|
    config.config_view "admin/config_xml", :action => "show",  :conditions => {:method => :get}
    config.config_update "admin/config_xml", :action => "update",  :conditions => {:method => :put}
    config.config_edit "admin/config_xml/edit", :action => "edit",  :conditions => {:method => :get}
  end

  map.with_options(:controller => "admin/garage") do |garage|
    garage.garage_index "admin/garage", :action => "index", :conditions => {:method => :get}
    garage.garage_gc "admin/garage/gc", :action => "gc", :conditions => {:method => :post}
  end

  map.edit_server_config "admin/config/server", :controller => "admin/server", :action => "index"
  map.update_server_config "admin/config/server/update", :controller => "admin/server", :action => "update", :conditions => {:method => :post}
  map.validate_server_config_params "admin/config/server/validate", :controller => "admin/server", :action => "validate", :conditions => {:method => :get}
  map.send_test_email "admin/config/server/test_email", :controller => "admin/server", :action => "test_email", :conditions => {:method => :post}
  map.validate_ldap_settings "admin/config/server/validate_ldap", :controller => "admin/server", :action => "validate_ldap", :conditions => {:method => :post}

  map.with_options(:controller => "admin/pipeline_groups") do |groups|
    groups.pipeline_groups "admin/pipelines", :action => "index"
    groups.pipeline_group_new "admin/pipeline_group/new", :action => "new", :conditions => {:method => :get}
    groups.pipeline_group_create "admin/pipeline_group", :action => "create", :conditions => {:method => :post}
    groups.move_pipeline_to_group "admin/pipelines/move/:pipeline_name", :action => "move", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}, :conditions => {:method => :put}
    groups.delete_pipeline "admin/pipelines/:pipeline_name", :action => "destroy", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}, :conditions => {:method => :delete}
    groups.pipeline_group_edit "admin/pipeline_group/:group_name/edit", :action => "edit", :requirements => {:group_name => GROUP_NAME_FORMAT}, :conditions => {:method => :get}
    groups.pipeline_group_show "admin/pipeline_group/:group_name", :action => "show", :requirements => {:group_name => GROUP_NAME_FORMAT}, :conditions => {:method => :get}
    groups.pipeline_group_update "admin/pipeline_group/:group_name", :action => "update", :requirements => {:group_name => GROUP_NAME_FORMAT}, :conditions => {:method => :put}
    groups.pipeline_group_delete "admin/pipeline_group/:group_name", :action => "destroy_group", :requirements => {:group_name => GROUP_NAME_FORMAT}, :conditions => {:method => :delete}
    groups.possible_groups "/admin/pipelines/possible_groups/:pipeline_name/:config_md5", :action => "possible_groups", :requirements => {:pipeline_name => PIPELINE_NAME_FORMAT}, :conditions => {:method => :get}
  end

  map.with_options(:controller => "admin/templates") do |template|
    template.templates "admin/templates", :action => "index"
    template.template_new "admin/templates/new", :action => "new", :conditions => { :method => :get }
    template.template_create "admin/templates/create", :action => "create", :conditions => { :method => :post }
    template.delete_template "admin/templates/:pipeline_name", :action => "destroy", :requirements => { :pipeline_name => PIPELINE_NAME_FORMAT }, :conditions => { :method => :delete }
    template.edit_template_permissions "admin/templates/:template_name/permissions", :action => "edit_permissions", :requirements =>  { :template_name => TEMPLATE_NAME_FORMAT }, :conditions => { :method => :get }
    template.update_template_permissions "admin/templates/:template_name/permissions", :action => "update_permissions", :requirements =>  { :template_name => TEMPLATE_NAME_FORMAT }, :conditions => { :method => :post }

    template.with_options(:controller => "admin/templates", :requirements => {:current_tab => /#{["general"].join("|")}/}) do |template|
      template.template_edit "admin/:stage_parent/:pipeline_name/:current_tab", :action => "edit", :requirements =>  { :pipeline_name => PIPELINE_NAME_FORMAT }, :conditions => { :method => :get }
      template.template_update "admin/:stage_parent/:pipeline_name/:current_tab", :action => "update", :requirements =>  { :pipeline_name => PIPELINE_NAME_FORMAT }, :conditions => { :method => :put }
    end
  end

  map.with_options(:controller => "admin/package_definitions") do |package_definitions|
    package_definitions.package_definitions_new "admin/package_definitions/:repo_id/new", :action => "new", :conditions => { :method => :get }
    package_definitions.package_definitions_new_for_new_pipeline_wizard "admin/package_definitions/:repo_id/new_for_new_pipeline_wizard", :action => "new_for_new_pipeline_wizard", :conditions => { :method => :get }
    package_definitions.pipelines_used_in "admin/package_definitions/:repo_id/:package_id/pipelines_used_in", :action => "pipelines_used_in", :conditions => {:method => :get}
    package_definitions.package_definitions_show "admin/package_definitions/:repo_id/:package_id", :action => "show", :conditions => {:method => :get}
    package_definitions.package_definitions_show_for_new_pipeline_wizard "admin/package_definitions/:repo_id/:package_id/for_new_pipeline_wizard", :action => "show_for_new_pipeline_wizard", :conditions => {:method => :get}
    package_definitions.package_definitions_show_with_repository_list "admin/package_definitions/:repo_id/:package_id/with_repository_list", :action => "show_with_repository_list", :conditions => {:method => :get}
    package_definitions.package_definition_delete "admin/package_definitions/:repo_id/:package_id", :action => "destroy", :conditions => {:method => :delete}
    package_definitions.package_definition_check_connection "admin/package_definitions/check_connection", :action => "check_connection", :conditions => {:method => :get}
  end

  map.with_options(:controller => "admin/package_repositories") do |package_repositories|
    package_repositories.package_repositories "admin/package_repositories", :action => "index", :conditions => { :method => :get }
    package_repositories.package_repositories_new "admin/package_repositories/new", :action => "new", :conditions => { :method => :get }
    package_repositories.package_repositories_check_connection "admin/package_repositories/check_connection", :action => "check_connection", :conditions => { :method => :get }
    package_repositories.package_repositories_list "admin/package_repositories/list", :action => "list", :conditions => { :method => :get }
    package_repositories.package_repositories_edit "admin/package_repositories/:id/edit", :action => "edit", :conditions => { :method => :get }
    package_repositories.package_repositories_create "admin/package_repositories", :action => "create", :conditions => { :method => :post }
    package_repositories.package_repositories_update "admin/package_repositories/:id", :action => "update", :conditions => { :method => :put }
    package_repositories.package_repositories_delete "admin/package_repositories/:id", :action => "destroy", :conditions => { :method => :delete }
    package_repositories.package_repositories_plugin_config "admin/package_repositories/:plugin/config/", :action => "plugin_config", :conditions => { :method => :get }
    package_repositories.package_repositories_plugin_config_for_repo "admin/package_repositories/:id/:plugin/config/", :action => "plugin_config_for_repo", :conditions => { :method => :get }
  end

  map.with_options(:controller => "config_view/templates") do |config_view_templates|
    config_view_templates.config_view_templates_show "/config_view/templates/:name", :action => "show", :requirements =>  { :name => PIPELINE_NAME_FORMAT }, :conditions => { :method => :get }
  end

  map.dismiss_license_expiry_warning "/users/dismiss_license_expiry_warning", :controller => "admin/users", :action => "dismiss_license_expiry_warning", :no_layout => true
  map.user_disabled_cas_error '/cas_errors/user_disabled', :controller => :cas_errors, :action => 'user_disabled'
  map.user_unknown_cas_error '/cas_errors/user_unknown', :controller => :cas_errors, :action => 'user_unknown'

  map.gobbler "*url", :controller => "application", :action => "unresolved"

  map.root :controller => "java", :action => "null"

end


# The priority is based upon order of creation: first created -> highest priority.

# Sample of regular route:
#   map.connect 'products/:id', :controller => 'catalog', :action => 'view'
# Keep in mind you can assign values other than :controller and :action

# Sample of named route:
#   map.purchase 'products/:id/purchase', :controller => 'catalog', :action => 'purchase'
# This route can be invoked with purchase_url(:id => product.id)

# Sample resource route (maps HTTP verbs to controller actions automatically):
#   map.resources :products

# Sample resource route with options:
#   map.resources :products, :member => { :short => :get, :toggle => :post }, :collection => { :sold => :get }

# Sample resource route with sub-resources:
#   map.resources :products, :has_many => [ :comments, :sales ], :has_one => :seller

# Sample resource route with more complex sub-resources
#   map.resources :products do |products|
#     products.resources :comments
#     products.resources :sales, :collection => { :recent => :get }
#   end

# Sample resource route within a namespace:
#   map.namespace :admin do |admin|
#     # Directs /admin/products/* to Admin::ProductsController (app/controllers/admin/products_controller.rb)
#     admin.resources :products
#   end

# You can have the root of your site routed with map.root -- just remember to delete public/index.html.
# map.root :controller => "welcome"

# See how all your routes lay out with "rake routes"

