# coding: utf-8

require 'active_support/concern'

module Transpec
  class Syntax
    module Mixin
      module Send
        extend ActiveSupport::Concern

        module TargetDetection
          def dynamic_analysis_target?
            node && node.send_type?
          end

          def conversion_target?
            return false unless dynamic_analysis_target?
            return true unless runtime_data.run?(send_analysis_target_node)
            defined_by_rspec?
          end

          private

          def defined_by_rspec?
            defined_in_rspec_source? && !example_method_defined_by_user?
          end

          def defined_in_rspec_source?
            source_location = runtime_data[send_analysis_target_node, source_location_key]
            return true unless source_location
            source_path = source_location.first
            return false unless source_path
            source_path.match(%r{/rspec\-[^/]+/lib/rspec/})
          end

          def example_method_defined_by_user?
            runtime_data[send_analysis_target_node, example_method_defined_by_user_key]
          end

          def send_analysis_target_node
            receiver_node || node
          end

          def source_location_key
            "#{method_name}_source_location".to_sym
          end

          def example_method_defined_by_user_key
            "#{method_name}_example_method_defined_by_user?".to_sym
          end
        end

        include TargetDetection

        included do
          define_dynamic_analysis do |rewriter|
            target_type = receiver_node ? :object : :context

            key = source_location_key
            code = "method(#{method_name.inspect}).source_location"
            rewriter.register_request(send_analysis_target_node, key, code, target_type)

            key = example_method_defined_by_user_key
            code = <<-END.gsub(/^\s+\|/, '').chomp
              |owner = method(#{method_name.inspect}).owner
              |owner != RSpec::Core::ExampleGroup &&
              |  owner.ancestors.include?(RSpec::Core::ExampleGroup)
            END
            rewriter.register_request(send_analysis_target_node, key, code, target_type)
          end
        end

        def receiver_node
          node.children[0]
        end

        def method_name
          node.children[1]
        end

        def arg_node
          node.children[2]
        end

        def arg_nodes
          node.children[2..-1]
        end

        def selector_range
          node.loc.selector
        end

        def receiver_range
          receiver_node.loc.expression
        end

        def arg_range
          arg_node.loc.expression
        end

        def args_range
          arg_nodes.first.loc.expression.begin.join(arg_nodes.last.loc.expression.end)
        end

        def parentheses_range
          selector_range.end.join(expression_range.end)
        end

        def range_in_between_receiver_and_selector
          receiver_range.end.join(selector_range.begin)
        end

        def range_in_between_selector_and_arg
          selector_range.end.join(arg_range.begin)
        end

        def range_after_arg
          arg_range.end.join(expression_range.end)
        end
      end
    end
  end
end
