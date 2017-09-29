# coding: utf-8

require 'active_support/concern'

module Transpec
  class Syntax
    module Mixin
      module MatcherOwner
        extend ActiveSupport::Concern

        module ClassMethods
          def add_matcher(matcher_class)
            accessor = "#{matcher_class.snake_case_name}_matcher"
            ivar = "@#{accessor}"

            define_method(accessor) do
              return instance_variable_get(ivar) if instance_variable_defined?(ivar)
              matcher = matcher_class.new(
                matcher_node, self, runtime_data, project, source_rewriter, report
              )
              instance_variable_set(ivar, matcher)
            end

            matcher_accessors << accessor
          end

          def matcher_accessors
            @matcher_accessors ||= []
          end
        end

        def dependent_syntaxes
          super + self.class.matcher_accessors.map { |accessor| send(accessor) }
        end
      end
    end
  end
end
