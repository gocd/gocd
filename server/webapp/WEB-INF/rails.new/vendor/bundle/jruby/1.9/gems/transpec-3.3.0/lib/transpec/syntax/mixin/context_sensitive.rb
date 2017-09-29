# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/send'

module Transpec
  class Syntax
    module Mixin
      module ContextSensitive
        extend ActiveSupport::Concern
        include Send

        included do
          define_dynamic_analysis do |rewriter|
            code = "is_a?(Class) && ancestors.any? { |a| a.name == 'RSpec::Core::ExampleGroup' }"
            rewriter.register_request(node, :example_group_context?, code, :context)
          end
        end

        def conversion_target?
          return false unless dynamic_analysis_target?

          in_example_group_context = if runtime_data.run?(node)
                                       # If we have runtime data, check with it.
                                       return false unless defined_by_rspec?
                                       runtime_data[node, :example_group_context?]
                                     else
                                       # Otherwise check statically.
                                       static_context_inspector.scopes.last == :example_group
                                     end

          in_example_group_context == should_be_in_example_group_context?
        end

        def should_be_in_example_group_context?
          fail NotImplementedError
        end
      end
    end
  end
end
