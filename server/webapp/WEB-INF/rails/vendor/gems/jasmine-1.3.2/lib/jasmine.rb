jasmine_files = ['base',
                 'dependencies',
                 'core_configuration',
                 'configuration',
                 'config',
                 'application',
                 'server',
                 'selenium_driver',
                 'rspec_formatter',
                 'command_line_tool',
                 'page',
                 'path_mapper',
                 'asset_pipeline_utility',
                 'asset_pipeline_mapper',
                 'asset_expander',
                 'results_processor',
                 'results',
                 'path_expander',
                 'yaml_config_parser',
                 File.join('runners', 'http')]

jasmine_files.each do |file|
  require File.join('jasmine', file)
end
# jasmine_rack_files.each do |file|
  # require File.join('rack', 'jasmine', file)
# end

require File.join('jasmine', "railtie") if Jasmine::Dependencies.rails3?


