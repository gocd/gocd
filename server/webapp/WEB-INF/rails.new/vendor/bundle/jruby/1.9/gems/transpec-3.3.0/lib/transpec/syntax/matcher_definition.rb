# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'

module Transpec
  class Syntax
    class MatcherDefinition < Syntax
      include Mixin::Send

      CONVERSION_CORRESPONDENCE = {
                      match_for_should: :match,
                  match_for_should_not: :match_when_negated,
            failure_message_for_should: :failure_message,
        failure_message_for_should_not: :failure_message_when_negated
      }

      def dynamic_analysis_target?
        super && receiver_node.nil? && CONVERSION_CORRESPONDENCE.keys.include?(method_name)
      end

      def convert_deprecated_method!
        replacement_method_name = CONVERSION_CORRESPONDENCE[method_name].to_s
        replace(selector_range, replacement_method_name)
        add_record("#{method_name} { }", "#{replacement_method_name} { }")
      end
    end
  end
end
