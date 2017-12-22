require 'yard'
require 'fileutils'

module Versionist
  class CopyApiVersionGenerator < Rails::Generators::Base
    include InflectorFixes

    desc "copies an existing API version a new API version"

    source_root File.expand_path('../templates', __FILE__)

    argument :old_version, :type => :string
    argument :old_module_name, :type => :string
    argument :new_version, :type => :string
    argument :new_module_name, :type => :string

    def copy_routes
      in_root do
        if RUBY_VERSION =~ /1.8/ || !defined?(RUBY_ENGINE) || RUBY_ENGINE != "ruby"
          log "ERROR: Cannot copy routes as this feature relies on the Ripper library, which is only available in MRI 1.9."
          return
        end
        parser = YARD::Parser::SourceParser.parse_string(File.read("config/routes.rb"))
        existing_routes = nil
        parser.enumerator.first.traverse do |node|
          existing_routes = node.source if node.type == :fcall && node.source =~ /api_version.*:?module\s*(=>|:)\s*("|')#{module_name_for_route(old_module_name)}("|')/
        end
        if !existing_routes.nil?
          copied_routes = String.new(existing_routes)
          copied_routes.gsub!(/"#{module_name_for_route(old_module_name)}"/, "\"#{module_name_for_route(new_module_name)}\"")
          copied_routes.gsub!(/#{old_version}/, new_version)
          route copied_routes
        else
          say "No routes found in config/routes.rb for #{old_version}"
        end
      end
    end

    def copy_controllers
      in_root do
        if File.exists? "app/controllers/#{module_name_for_path(old_module_name)}"
          log "Copying all files from app/controllers/#{module_name_for_path(old_module_name)} to app/controllers/#{module_name_for_path(new_module_name)}"
          FileUtils.cp_r "app/controllers/#{module_name_for_path(old_module_name)}", "app/controllers/#{module_name_for_path(new_module_name)}"
          Dir.glob("app/controllers/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
            text = File.read(f)
            File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
          end
        else
          say "No controllers found in app/controllers for #{old_version}"
        end
      end
    end

    # due to the inflector quirks we can't use hook_for :test_framework
    def copy_controller_tests
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          if File.exists? "#{Versionist.test_path}/#{module_name_for_path(old_module_name)}"
            log "Copying all files from #{Versionist.test_path}/#{module_name_for_path(old_module_name)} to #{Versionist.test_path}/#{module_name_for_path(new_module_name)}"
            FileUtils.cp_r "#{Versionist.test_path}/#{module_name_for_path(old_module_name)}", "#{Versionist.test_path}/#{module_name_for_path(new_module_name)}"
            Dir.glob("#{Versionist.test_path}/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
              text = File.read(f)
              File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
            end
          else
            say "No tests found in #{Versionist.test_path} for #{old_version}"
          end

          if Versionist.older_than_rails_5?
            if File.exists? "test/integration/#{module_name_for_path(old_module_name)}"
              log "Copying all files from test/integration/#{module_name_for_path(old_module_name)} to test/integration/#{module_name_for_path(new_module_name)}"
              FileUtils.cp_r "test/integration/#{module_name_for_path(old_module_name)}", "test/integration/#{module_name_for_path(new_module_name)}"
              Dir.glob("test/integration/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
                text = File.read(f)
                File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
              end
            else
              say "No integration tests found in test/integration for #{old_version}"
            end
          end
        when :rspec
          if File.exists? "spec/controllers/#{module_name_for_path(old_module_name)}"
            log "Copying all files from spec/controllers/#{module_name_for_path(old_module_name)} to spec/controllers/#{module_name_for_path(new_module_name)}"
            FileUtils.cp_r "spec/controllers/#{module_name_for_path(old_module_name)}", "spec/controllers/#{module_name_for_path(new_module_name)}"
            Dir.glob("spec/controllers/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
              text = File.read(f)
              File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
            end
          else
            say "No controller specs found in spec/controllers for #{old_version}"
          end

          if File.exists? "spec/requests/#{module_name_for_path(old_module_name)}"
            log "Copying all files from spec/requests/#{module_name_for_path(old_module_name)} to spec/requests/#{module_name_for_path(new_module_name)}"
            FileUtils.cp_r "spec/requests/#{module_name_for_path(old_module_name)}", "spec/requests/#{module_name_for_path(new_module_name)}"
            Dir.glob("spec/requests/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
              text = File.read(f)
              File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
            end
          else
            say "No request specs found in spec/requests for #{old_version}"
          end
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end

    def copy_presenters
      in_root do
        if File.exists? "app/presenters/#{module_name_for_path(old_module_name)}"
          log "Copying all files from app/presenters/#{module_name_for_path(old_module_name)} to app/presenters/#{module_name_for_path(new_module_name)}"
          FileUtils.cp_r "app/presenters/#{module_name_for_path(old_module_name)}", "app/presenters/#{module_name_for_path(new_module_name)}"
          Dir.glob("app/presenters/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
            text = File.read(f)
            File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
          end
        else
          say "No presenters found in app/presenters for #{old_version}"
        end
      end
    end

    def copy_presenter_tests
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          if File.exists? "test/presenters/#{module_name_for_path(old_module_name)}"
            log "Copying all files from test/presenters/#{module_name_for_path(old_module_name)} to test/presenters/#{module_name_for_path(new_module_name)}"
            FileUtils.cp_r "test/presenters/#{module_name_for_path(old_module_name)}", "test/presenters/#{module_name_for_path(new_module_name)}"
            Dir.glob("test/presenters/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
              text = File.read(f)
              File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
            end
          else
            say "No presenter tests found in test/presenters for #{old_version}"
          end
        when :rspec
          if File.exists? "spec/presenters/#{module_name_for_path(old_module_name)}"
            log "Copying all files from spec/presenters/#{module_name_for_path(old_module_name)} to spec/presenters/#{module_name_for_path(new_module_name)}"
            FileUtils.cp_r "spec/presenters/#{module_name_for_path(old_module_name)}", "spec/presenters/#{module_name_for_path(new_module_name)}"
            Dir.glob("spec/presenters/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
              text = File.read(f)
              File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
            end
          else
            say "No presenter specs found in spec/presenters for #{old_version}"
          end
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end

    def copy_helpers
      in_root do
        if File.exists? "app/helpers/#{module_name_for_path(old_module_name)}"
          log "Copying all files from app/helpers/#{module_name_for_path(old_module_name)} to app/helpers/#{module_name_for_path(new_module_name)}"
          FileUtils.cp_r "app/helpers/#{module_name_for_path(old_module_name)}", "app/helpers/#{module_name_for_path(new_module_name)}"
          Dir.glob("app/helpers/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
            text = File.read(f)
            File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
          end
        else
          say "No helpers found in app/helpers for #{old_version}"
        end
      end
    end

    def copy_helper_tests
      in_root do
        case Versionist.configuration.configured_test_framework
        when :test_unit
          if File.exists? "test/helpers/#{module_name_for_path(old_module_name)}"
            log "Copying all files from test/helpers/#{module_name_for_path(old_module_name)} to test/helpers/#{module_name_for_path(new_module_name)}"
            FileUtils.cp_r "test/helpers/#{module_name_for_path(old_module_name)}", "test/helpers/#{module_name_for_path(new_module_name)}"
            Dir.glob("test/helpers/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
              text = File.read(f)
              File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
            end
          else
            say "No helper tests found in test/helpers for #{old_version}"
          end
        when :rspec
          if File.exists? "spec/helpers/#{module_name_for_path(old_module_name)}"
            log "Copying all files from spec/helpers/#{module_name_for_path(old_module_name)} to spec/helpers/#{module_name_for_path(new_module_name)}"
            FileUtils.cp_r "spec/helpers/#{module_name_for_path(old_module_name)}", "spec/helpers/#{module_name_for_path(new_module_name)}"
            Dir.glob("spec/helpers/#{module_name_for_path(new_module_name)}/*.rb").each do |f|
              text = File.read(f)
              File.open(f, 'w') {|f| f << text.gsub(/#{old_module_name}/, new_module_name)}
            end
          else
            say "No helper specs found in spec/helpers for #{old_version}"
          end
        else
          say "Unsupported test_framework: #{Versionist.configuration.configured_test_framework}"
        end
      end
    end

    def copy_documentation
      in_root do
        if File.exists? "public/docs/#{old_version}"
          log "Copying all files from public/docs/#{old_version} to public/docs/#{new_version}"
          FileUtils.cp_r "public/docs/#{old_version}/.", "public/docs/#{new_version}"
        else
          say "No documentation found in public/docs for #{old_version}"
        end
      end
    end
  end
end
