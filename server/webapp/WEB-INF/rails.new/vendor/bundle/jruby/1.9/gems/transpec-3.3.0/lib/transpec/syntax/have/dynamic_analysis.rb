# coding: utf-8

require 'transpec/syntax/have'

module Transpec
  class Syntax
    class Have
      module DynamicAnalysis
        extend ActiveSupport::Concern

        included do
          define_dynamic_analysis do |rewriter|
            target_node = explicit_subject? ? expectation.subject_node : expectation.node
            target_type = explicit_subject? ? :object : :context

            key = :collection_accessor
            code = collection_accessor_inspection_code
            rewriter.register_request(target_node, key, code, target_type)

            # Give up inspecting query methods of collection accessor with arguments
            # (e.g. have(2).errors_on(variable)) since this is a context of #instance_eval.
            unless items_method_has_arguments?
              key = :available_query_methods
              code = available_query_methods_inspection_code
              rewriter.register_request(target_node, key, code, target_type)
            end

            key = :collection_accessor_is_private?
            code = "#{subject_code}.private_methods.include?(#{items_name.inspect})"
            rewriter.register_request(target_node, key, code, target_type)

            key = :subject_includes_active_model_validations?
            code = "#{subject_code}.is_a?(ActiveModel::Validations)"
            rewriter.register_request(target_node, key, code, target_type)

            key = :project_requires_collection_matcher?
            code = 'defined?(RSpec::CollectionMatchers)'
            rewriter.register_request(target_node, key, code, :context)
          end
        end

        def subject_code
          explicit_subject? ? 'self' : 'subject'
        end

        def collection_accessor_inspection_code
          # `expect(owner).to have(n).things` invokes private owner#things with Object#__send__
          # if the owner does not respond to any of #size, #count and #length.
          #
          # https://github.com/rspec/rspec-expectations/blob/v2.14.3/lib/rspec/matchers/built_in/have.rb#L48-L58
          @collection_accessor_inspection_code ||= <<-END.gsub(/^\s+\|/, '').chomp
            |begin
            |  exact_name = #{items_name.inspect}
            |
            |  inflector = if defined?(ActiveSupport::Inflector) &&
            |                   ActiveSupport::Inflector.respond_to?(:pluralize)
            |                ActiveSupport::Inflector
            |              elsif defined?(Inflector)
            |                Inflector
            |              else
            |                nil
            |              end
            |
            |  if inflector
            |    pluralized_name = inflector.pluralize(exact_name).to_sym
            |    respond_to_pluralized_name = #{subject_code}.respond_to?(pluralized_name)
            |  end
            |
            |  respond_to_query_methods =
            |    !(#{subject_code}.methods & #{QUERY_METHOD_PRIORITIES.inspect}).empty?
            |
            |  if #{subject_code}.respond_to?(exact_name)
            |    exact_name
            |  elsif respond_to_pluralized_name
            |    pluralized_name
            |  elsif respond_to_query_methods
            |    nil
            |  else
            |    exact_name
            |  end
            |end
          END
        end

        def available_query_methods_inspection_code
          <<-END.gsub(/^\s+\|/, '').chomp
            |collection_accessor = #{collection_accessor_inspection_code}
            |target = if collection_accessor
            |           #{subject_code}.__send__(collection_accessor)
            |         else
            |           #{subject_code}
            |         end
            |target.methods & #{QUERY_METHOD_PRIORITIES.inspect}
          END
        end
      end
    end
  end
end
