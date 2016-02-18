require 'rails/generators'

module Foundation
  module Generators
    class InstallGenerator < ::Rails::Generators::Base
      desc "Install Foundation within a Rails project"
      source_root File.join(File.dirname(__FILE__), "templates")
      argument :layout_name, :type => :string, :default => "application", :banner => "layout_name"

      class_option :haml, :desc => "Generate Haml layout instead of erb", :type => :boolean
      class_option :slim, :desc => "Generate Slim layout instead of erb", :type => :boolean

      def add_assets
        # rails_ujs breaks, need to incorporate rails-behavior plugin for this to work seamlessly
        # gsub_file "app/assets/javascripts/application#{detect_js_format[0]}", /\/\/= require jquery\n/, ""
        insert_into_file File.join(javascripts_base_dir, "application#{detect_js_format[0]}"), "#{detect_js_format[1]} require foundation\n", :after => "jquery_ujs\n"
        append_to_file File.join(javascripts_base_dir, "application#{detect_js_format[0]}"), "#{detect_js_format[2]}"
        create_app_scss
        insert_into_file File.join(stylesheets_base_dir, "application#{detect_css_format[0]}"), "\n#{detect_css_format[1]} require foundation_and_overrides\n", :after => "require_self"
      end

      def detect_js_format
        %w(.coffee .coffee.erb .js.coffee .js.coffee.erb .js .js.erb).each do |ext|
          if File.exist?(File.join(javascripts_base_dir, "application#{ext}"))
            if ext.include?(".coffee")
              return [ext, "#=", "\n() ->\n  $(document).foundation()\n"]
            else
              return [ext, "//=", "\n$(function(){ $(document).foundation(); });\n"]
            end
          end
        end
      end

      def detect_css_format
        %w(.css .css.sass .sass .css.scss .scss).each do |ext|
          if File.exist?(File.join(stylesheets_base_dir, "application#{ext}"))
            if ext.include?(".sass") || ext.include?(".scss")
              return [ext, "//="]
            else
              return [ext, " *="]
            end
          end
        end
      end

      def create_layout
        if options.haml? || (defined?(Haml) && options.haml?)
          template "application.html.haml", File.join(layouts_base_dir, "#{file_name}.html.haml")
        elsif options.slim? || (defined?(Slim) && options.slim?)
          template "application.html.slim", File.join(layouts_base_dir, "#{file_name}.html.slim")
        else
          template "application.html.erb", File.join(layouts_base_dir, "#{file_name}.html.erb")
        end
      end

      def create_app_scss
        template "foundation_and_overrides.scss", File.join(stylesheets_base_dir, "foundation_and_overrides.scss")
        template "_settings.scss", File.join(stylesheets_base_dir, "_settings.scss")
      end

      private

      def file_name
        layout_name.underscore.downcase
      end

      def javascripts_base_dir
        File.join("app", "assets", "javascripts")
      end

      def stylesheets_base_dir
        File.join("app", "assets", "stylesheets")
      end

      def layouts_base_dir
        File.join("app", "views", "layouts")
      end
    end
  end
end
