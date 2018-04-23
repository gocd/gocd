module Versionist
  class NewApiVersionGenerator < Rails::Generators::Base
    include InflectorFixes
    include RspecHelper

    desc "creates the infrastructure for a new API version"

    source_root File.expand_path('../templates', __FILE__)

    argument :version, :type => :string
    argument :module_name, :type => :string
    class_option :default, :type => :boolean
    class_option :header, :type => :hash, :group => :header
    class_option :parameter, :type => :hash, :group => :parameter
    class_option :path, :type => :hash, :group => :path
    class_option :defaults, :type => :hash, :group => :defaults

    def verify_options
      raise "Must specify at least one versioning strategy option" if !['header', 'parameter', 'path'].any? {|strategy| options.has_key?(strategy)}
      if options.has_key?("header")
        raise "Must specify name and value for header versioning strategy" if !options["header"].has_key?("name") || !options["header"].has_key?("value")
      end
      if options.has_key?("parameter")
        raise "Must specify name and value for parameter versioning strategy" if !options["parameter"].has_key?("name") || !options["parameter"].has_key?("value")
      end
      if options.has_key?("path")
        raise "Must specify value for path versioning strategy" if !options["path"].has_key?("value")
      end
    end

    def add_routes
      in_root do
        api_version_block = /api_version.*:module\s*(=>|:)\s*("|')#{module_name_for_route(module_name)}("|')/
        matching_version_blocks = File.readlines("config/routes.rb").grep(api_version_block)
        raise "API version already exists in config/routes.rb" if !matching_version_blocks.empty?
        route_string = "api_version(:module => \"#{module_name_for_route(module_name)}\""
        ['header', 'parameter', 'path'].each do |versioning_strategy|
          if options.has_key?(versioning_strategy)
            options[versioning_strategy].symbolize_keys!
            route_string << ", :#{versioning_strategy} => {#{options[versioning_strategy].to_s.gsub(/[\{\}]/, '').gsub('=>', ' => ')}}"
          end
        end
        if options.has_key?('defaults')
          options['defaults'].symbolize_keys!
          route_string << ", :defaults => {#{options['defaults'].to_s.gsub(/[\{\}]/, '').gsub('=>', ' => ')}}"
        end
        if options.has_key?('default')
          route_string << ", :default => true"
        end
        route_string << ") do\n  end"
        route route_string
      end
    end

    def add_controller_base
      in_root do
        empty_directory "app/controllers/#{module_name_for_path(module_name)}"
        template 'base_controller.rb', File.join("app", "controllers", "#{module_name_for_path(module_name)}", "base_controller.rb")
      end
    end

    # due to the inflector quirks we can't use hook_for :test_framework
    def add_controller_base_tests
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          empty_directory "test/#{Versionist.test_path}/#{module_name_for_path(module_name)}"
          empty_directory "test/integration/#{module_name_for_path(module_name)}"

          if Versionist.older_than_rails_5?
            template 'base_controller_integration_test.rb', File.join("test", "integration", "#{module_name_for_path(module_name)}", "base_controller_test.rb")
            template 'base_controller_functional_test.rb', File.join("test", "#{Versionist.test_path}", "#{module_name_for_path(module_name)}", "base_controller_test.rb")
          else
            template 'base_controller_functional_test_rails_5.rb', File.join("test", "#{Versionist.test_path}", "#{module_name_for_path(module_name)}", "base_controller_test_rails_5.rb")
          end
        when :rspec
          @rspec_require_file = rspec_helper_filename
          empty_directory "spec/controllers/#{module_name_for_path(module_name)}"
          template 'base_controller_spec.rb', File.join("spec", "controllers", "#{module_name_for_path(module_name)}", "base_controller_spec.rb"), :assigns => { :rspec_require_file => @rspec_require_file }
          empty_directory "spec/requests/#{module_name_for_path(module_name)}"
          template 'base_controller_spec.rb', File.join("spec", "requests", "#{module_name_for_path(module_name)}", "base_controller_spec.rb"), :assigns => { :rspec_require_file => @rspec_require_file }
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end

    def add_presenters_base
      in_root do
        empty_directory "app/presenters/#{module_name_for_path(module_name)}"
        template 'base_presenter.rb', File.join("app", "presenters", "#{module_name_for_path(module_name)}", "base_presenter.rb")
      end
    end

    def add_presenter_test
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          empty_directory "test/presenters/#{module_name_for_path(module_name)}"

          if Versionist.older_than_rails_5?
            template 'base_presenter_test.rb', File.join("test", "presenters", "#{module_name_for_path(module_name)}", "base_presenter_test.rb")
          else
            template 'base_presenter_test_rails_5.rb', File.join("test", "presenters", "#{module_name_for_path(module_name)}", "base_presenter_test_rails_5.rb")
          end
        when :rspec
          @rspec_require_file = rspec_helper_filename
          empty_directory "spec/presenters/#{module_name_for_path(module_name)}"
          template 'base_presenter_spec.rb', File.join("spec", "presenters", "#{module_name_for_path(module_name)}", "base_presenter_spec.rb"), :assigns => { :rspec_require_file => @rspec_require_file }
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end

    def add_helpers_dir
      in_root do
        empty_directory "app/helpers/#{module_name_for_path(module_name)}"
      end
    end

    def add_helpers_test_dir
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          empty_directory "test/helpers/#{module_name_for_path(module_name)}"
        when :rspec
          empty_directory "spec/helpers/#{module_name_for_path(module_name)}"
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end

    def add_documentation_base
      in_root do
        empty_directory "public/docs/#{version}"
        template 'docs_index.rb', File.join("public", "docs", "#{version}", "index.html")
        template 'docs_style.rb', File.join("public", "docs", "#{version}", "style.css")
      end
    end
  end
end
