require 'rails/generators'

module Foundation
  module Generators
    class InstallGenerator < ::Rails::Generators::Base
      source_root File.join(File.dirname(__FILE__), 'templates')
      argument :layout_name, :type => :string, :default => 'application', :banner => 'layout_name'

      class_option :haml, :desc => 'Generate HAML layout instead of erb', :type => :boolean
      class_option :slim, :desc => 'Generate Slim layout instead of erb', :type => :boolean

      def add_assets
        # rails_ujs breaks, need to incorporate rails-behavior plugin for this to work seamlessly
        # gsub_file "app/assets/javascripts/application#{detect_js_format[0]}", /\/\/= require jquery\n/, ""
        insert_into_file File.join(javascripts_base_dir, "application#{detect_js_format[0]}"), "#{detect_js_format[1]} require foundation\n", :after => "jquery_ujs\n"
        append_to_file File.join(javascripts_base_dir, "application#{detect_js_format[0]}"), "#{detect_js_format[2]}"
        settings_file = File.join(File.dirname(__FILE__),"..", "..", "..", "vendor", "assets", "stylesheets", "foundation", "_settings.scss")
        create_file File.join(stylesheets_base_dir, 'foundation_and_overrides.scss'), File.read(settings_file)
        append_to_file File.join(stylesheets_base_dir, 'foundation_and_overrides.scss'), "\n@import 'foundation';\n"
        insert_into_file File.join(stylesheets_base_dir, "application#{detect_css_format[0]}"), "\n#{detect_css_format[1]} require foundation_and_overrides\n", :after => "require_self"
      end

      def detect_js_format
        return ['.coffee', '#=', "\n() ->\n  $(document).foundation()\n"] if File.exist?(File.join(javascripts_base_dir, 'application.coffee'))
        return ['.coffee.erb', '#=', "\n() ->\n  $(document).foundation()\n"] if File.exist?(File.join(javascripts_base_dir, 'application.coffee.erb'))
        return ['.js.coffee', '#=', "\n() ->\n  $(document).foundation()\n"] if File.exist?(File.join(javascripts_base_dir, 'application.js.coffee'))
        return ['.js.coffee.erb', '#=', "\n() ->\n  $(document).foundation()\n"] if File.exist?(File.join(javascripts_base_dir, 'application.js.coffee.erb'))
        return ['.js', '//=', "\n$(function(){ $(document).foundation(); });\n"] if File.exist?(File.join(javascripts_base_dir, 'application.js'))
        return ['.js.erb', '//=', "\n$(function(){ $(document).foundation(); });\n"] if File.exist?(File.join(javascripts_base_dir, 'application.js.erb'))
      end

      def detect_css_format
        return ['.css', ' *='] if File.exist?(File.join(stylesheets_base_dir, 'application.css'))
        return ['.css.sass', ' //='] if File.exist?(File.join(stylesheets_base_dir, 'application.css.sass'))
        return ['.sass', ' //='] if File.exist?(File.join(stylesheets_base_dir, 'application.sass'))
        return ['.css.scss', ' //='] if File.exist?(File.join(stylesheets_base_dir, 'application.css.scss'))
        return ['.scss', ' //='] if File.exist?(File.join(stylesheets_base_dir, 'application.scss'))
      end

      def create_layout
        if options.haml?||(defined?(Haml) && options.haml?)
          template 'application.html.haml', File.join(layouts_base_dir, "#{file_name}.html.haml")
        elsif options.slim?||(defined?(Slim) && options.slim?)
          template 'application.html.slim', File.join(layouts_base_dir, "#{file_name}.html.slim")
        else
          template 'application.html.erb', File.join(layouts_base_dir, "#{file_name}.html.erb")
        end
      end

      private

      def file_name
        layout_name.underscore.downcase
      end

      def javascripts_base_dir
        File.join('app', 'assets', 'javascripts')
      end

      def stylesheets_base_dir
        File.join('app', 'assets', 'stylesheets')
      end

      def layouts_base_dir
        File.join('app', 'views', 'layouts')
      end
    end
  end
end
