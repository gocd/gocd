# coding: utf-8

require 'transpec/syntax'
require 'transpec/syntax/mixin/send'

module Transpec
  class Syntax
    class Double < Syntax
      include Mixin::Send

      def dynamic_analysis_target?
        super && receiver_node.nil? && [:double, :mock, :stub].include?(method_name)
      end

      def convert_to_double!
        return if method_name == :double
        replace(selector_range, 'double')
        add_record
      end

      private

      def add_record
        super("#{method_name}('something')", "double('something')")
      end
    end
  end
end
