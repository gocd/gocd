# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/monkey_patch'
require 'ast'

module Transpec
  class Syntax
    module Mixin
      module MonkeyPatchAnyInstance
        extend ActiveSupport::Concern
        include MonkeyPatch, ::AST::Sexp

        included do
          define_dynamic_analysis do |rewriter|
            code = <<-END.gsub(/^\s+\|/, '').chomp
              |any_instance_classes = [
              |  'RSpec::Mocks::AnyInstance::Recorder',
              |  'RSpec::Mocks::AnyInstance::Proxy'
              |]
              |
              |if any_instance_classes.include?(self.class.name)
              |  if respond_to?(:klass)
              |    klass.name
              |  elsif instance_variable_defined?(:@klass)
              |    instance_variable_get(:@klass).name
              |  else
              |    nil
              |  end
              |else
              |  nil
              |end
            END
            rewriter.register_request(subject_node, :any_instance_target_class_name, code)
          end
        end

        def any_instance?
          return true unless any_instance_target_node.nil?
          runtime_data[subject_node, :any_instance_target_class_name]
        end

        private

        def any_instance_target_class_source
          return nil unless any_instance?

          if any_instance_target_node
            any_instance_target_node.loc.expression.source
          else
            runtime_data[subject_node, :any_instance_target_class_name]
          end
        end

        def any_instance_target_node
          return nil unless subject_node.send_type?
          return nil unless subject_node.children.count == 2
          receiver_node, method_name = *subject_node
          return nil unless receiver_node
          return nil unless method_name == :any_instance
          receiver_node
        end
      end
    end
  end
end
