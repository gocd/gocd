# coding: utf-8

require 'transpec/rspec_dsl'
require 'transpec/util'

module Transpec
  class StaticContextInspector
    include RSpecDSL, Util

    SCOPE_TYPES = [:module, :class, :sclass, :def, :defs, :block].freeze
    TWISTED_SCOPE_TYPES = (SCOPE_TYPES - [:def, :defs]).freeze

    EXAMPLE_CONTEXTS = [
      [:example_group, :example],
      [:example_group, :each_before_after],
      [:example_group, :helper],
      [:example_group, :def],
      [:rspec_configure, :each_before_after],
      [:rspec_configure, :def],
      [:module, :def]
    ].freeze

    EXAMPLE_GROUP_CONTEXTS = [
      [:example_group, :all_before_after],
      [:example_group, :around],
      [:rspec_configure, :all_before_after],
      [:rspec_configure, :around]
    ].freeze

    attr_reader :node, :rspec_version

    def initialize(node, rspec_version)
      @node = node
      @rspec_version = rspec_version
    end

    def scopes
      @scopes ||= begin
        scopes = valid_ancestor_nodes.reverse_each.map { |node| scope_type(node) }
        scopes.compact!
        scopes.extend(ArrayExtension)
      end
    end

    def non_monkey_patch_expectation_available?
      return @expectation_available if instance_variable_defined?(:@expectation_available)
      contexts = EXAMPLE_CONTEXTS + EXAMPLE_GROUP_CONTEXTS
      @expectation_available = match_context?(contexts)
    end

    alias_method :expect_available?, :non_monkey_patch_expectation_available?

    def non_monkey_patch_mock_available?
      return @mock_available if instance_variable_defined?(:@mock_available)
      contexts = EXAMPLE_CONTEXTS
      contexts += EXAMPLE_GROUP_CONTEXTS if rspec_version.rspec_3?
      @mock_available = match_context?(contexts)
    end

    alias_method :expect_to_receive_available?, :non_monkey_patch_mock_available?
    alias_method :allow_to_receive_available?, :non_monkey_patch_mock_available?

    private

    def valid_ancestor_nodes
      valid_nodes = []

      self_and_ancestor_nodes = [node] + node.ancestors

      self_and_ancestor_nodes.each_cons(2) do |child, parent|
        valid_nodes << parent unless belong_to_direct_outer_scope?(child)
      end

      valid_nodes
    end

    def belong_to_direct_outer_scope?(node)
      return false unless TWISTED_SCOPE_TYPES.include?(node.parent.type)
      scope_node = node.parent
      return true if node.equal?(scope_node.children[0])
      scope_node.class_type? && node.equal?(scope_node.children[1])
    end

    def scope_type(node)
      return nil unless SCOPE_TYPES.include?(node.type)

      case node.type
      when :block
        special_block_type(node)
      when :defs
        if node.children.first.self_type?
          nil
        else
          node.type
        end
      else
        node.type
      end
    end

    def special_block_type(block_node) # rubocop:disable MethodLength, CyclomaticComplexity
      send_node = block_node.children.first
      receiver_node, method_name, = *send_node

      if const_name(receiver_node) == 'RSpec'
        case method_name
        when :configure
          :rspec_configure
        when *EXAMPLE_GROUP_METHODS
          :example_group
        else
          nil
        end
      elsif HOOK_METHODS.include?(method_name)
        hook_type(send_node)
      elsif receiver_node
        nil
      elsif EXAMPLE_GROUP_METHODS.include?(method_name)
        :example_group
      elsif EXAMPLE_METHODS.include?(method_name)
        :example
      elsif HELPER_METHODS.include?(method_name)
        :helper
      else
        nil
      end
    end

    def hook_type(send_node)
      _, method_name, arg_node, = *send_node

      return :around if method_name == :around

      if arg_node && [:sym, :str].include?(arg_node.type)
        hook_arg = arg_node.children.first.to_sym
        return :all_before_after if [:all, :context].include?(hook_arg)
      end

      :each_before_after
    end

    def match_context?(scope_trailing_patterns)
      return true if scopes == [:def]

      scope_trailing_patterns.any? do |pattern|
        scopes.end_with?(pattern)
      end
    end

    module ArrayExtension
      def end_with?(*args)
        tail = args.flatten
        self[-(tail.size)..-1] == tail
      end
    end
  end
end
