require 'active_support'
require 'active_support/core_ext/module/aliasing'
require 'rspec/matchers/built_in/have'

module RSpec::Rails::Matchers
  module HaveExtensions
    extend ActiveSupport::Concern

    # @api private
    #
    # Enhances the failure message for `should have(n)` matchers
    def failure_message_for_should_with_errors_on_extensions
      return "expected #{relativities[@relativity]}#{@expected} errors on :#{@args[0]}, got #{@actual}" if @collection_name == :errors_on
      return "expected #{relativities[@relativity]}#{@expected} error on :#{@args[0]}, got #{@actual}"  if @collection_name == :error_on
      return failure_message_for_should_without_errors_on_extensions
    end

    # @api private
    #
    # Enhances the description for `should have(n)` matchers
    def description_with_errors_on_extensions
      return "have #{relativities[@relativity]}#{@expected} errors on :#{@args[0]}" if @collection_name == :errors_on
      return "have #{relativities[@relativity]}#{@expected} error on :#{@args[0]}"  if @collection_name == :error_on
      return description_without_errors_on_extensions
    end

    included do
      alias_method_chain :failure_message_for_should, :errors_on_extensions
      alias_method_chain :description, :errors_on_extensions
    end
  end
end

RSpec::Matchers::BuiltIn::Have.class_eval do
  include RSpec::Rails::Matchers::HaveExtensions
end
