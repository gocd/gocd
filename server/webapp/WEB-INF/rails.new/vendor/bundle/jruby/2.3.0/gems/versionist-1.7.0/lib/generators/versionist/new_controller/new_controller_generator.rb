module Versionist
  class NewControllerGenerator < Rails::Generators::NamedBase
    include InflectorFixes
    include RspecHelper

    desc "creates a new controller for an existing API version"
    source_root File.expand_path('../templates', __FILE__)

    argument :module_name, :type => :string

    def new_controller
      in_root do
        raise "API module namespace #{module_name} doesn't exist. Please run \'rails generate versionist:new_api_version\' generator first" if !File.exists?("app/controllers/#{module_name_for_path(module_name)}")
        template 'new_controller.rb', File.join("app", "controllers", "#{module_name_for_path(module_name)}", "#{file_name}_controller.rb")

        api_version_block = /api_version.*:?module\s*(=>|:)\s*("|')#{module_name_for_route(module_name)}("|')/
        new_route = "    resources :#{file_name}\n"
        matching_version_blocks = File.readlines("config/routes.rb").grep(api_version_block)
        if matching_version_blocks.empty?
          raise "API version doesn't exist in config/routes.rb. Please run \'rails generate versionist:new_api_version\' generator first"
        elsif matching_version_blocks.size > 1
          raise "API version is duplicated in config/routes.rb"
        else
          version_block = matching_version_blocks.first
          inject_into_file "config/routes.rb", "#{new_route}", {:after => version_block, :verbose => false}
        end
      end
    end

    # due to the inflector quirks we can't use hook_for :test_framework
    def new_controller_tests
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          if Versionist.older_than_rails_5?
            template 'new_controller_integration_test.rb', File.join("test", "integration", "#{module_name_for_path(module_name)}", "#{file_name}_controller_test.rb")
            template 'new_controller_functional_test.rb', File.join("test", "#{Versionist.test_path}", "#{module_name_for_path(module_name)}", "#{file_name}_controller_test.rb")
          else
            template 'new_controller_functional_test_rails_5.rb', File.join("test", "#{Versionist.test_path}", "#{module_name_for_path(module_name)}", "#{file_name}_controller_test_rails_5.rb")
          end
        when :rspec
          @rspec_require_file = rspec_helper_filename
          template 'new_controller_spec.rb', File.join("spec", "controllers", "#{module_name_for_path(module_name)}", "#{file_name}_controller_spec.rb"), :assigns => { :rspec_require_file => @rspec_require_file }
          template 'new_controller_spec.rb', File.join("spec", "requests", "#{module_name_for_path(module_name)}", "#{file_name}_controller_spec.rb"), :assigns => { :rspec_require_file => @rspec_require_file }
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end
  end
end
