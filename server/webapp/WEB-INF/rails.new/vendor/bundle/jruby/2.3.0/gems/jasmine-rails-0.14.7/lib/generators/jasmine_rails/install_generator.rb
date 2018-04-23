require 'rails/generators/base'

module JasmineRails
  module Generators
    class InstallGenerator < ::Rails::Generators::Base
      source_root File.expand_path("../templates", __FILE__)

      desc "This generator configures a Rails application for running the Jasmine::Rails testsuite"

      def create_jasmine_config
        template "jasmine.yml", "spec/javascripts/support/jasmine.yml"
      end

      def add_routes
        route "mount JasmineRails::Engine => '/specs' if defined?(JasmineRails)"
      end
    end
  end
end
