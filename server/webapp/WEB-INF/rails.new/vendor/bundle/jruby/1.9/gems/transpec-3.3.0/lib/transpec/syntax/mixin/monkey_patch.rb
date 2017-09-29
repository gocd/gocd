# coding: utf-8

require 'active_support/concern'
require 'transpec/syntax/mixin/send'

module Transpec
  class Syntax
    module Mixin
      module MonkeyPatch
        extend ActiveSupport::Concern
        include Send

        def register_syntax_availability_analysis_request(rewriter, key, methods)
          code = "self.class.ancestors.any? { |a| a.name.start_with?('RSpec::') }"

          methods.each do |method|
            code << " && respond_to?(#{method.inspect})"
          end

          rewriter.register_request(node, key, code, :context)
        end

        def syntax_available?(key)
          if runtime_data.present?(node, key)
            runtime_data[node, key]
          else
            static_context_inspector.send(key)
          end
        end

        def subject_node
          receiver_node
        end

        def subject_range
          receiver_range
        end
      end
    end
  end
end
