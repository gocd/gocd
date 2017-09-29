# coding: utf-8

module Transpec
  module Util
    LITERAL_TYPES = %w(
      true false nil
      int float
      str sym regexp
    ).map(&:to_sym).freeze

    WHITESPACES = [' ', "\t"].freeze

    module_function

    def proc_literal?(node)
      return false unless node.block_type?

      send_node = node.children.first
      receiver_node, method_name, = *send_node

      if receiver_node.nil? || const_name(receiver_node) == 'Kernel'
        [:lambda, :proc].include?(method_name)
      elsif const_name(receiver_node) == 'Proc'
        method_name == :new
      else
        false
      end
    end

    def const_name(node)
      return nil if node.nil? || !node.const_type?

      const_names = []
      const_node = node

      loop do
        namespace_node, name = *const_node
        const_names << name
        break unless namespace_node
        break unless namespace_node.is_a?(Parser::AST::Node)
        break if namespace_node.cbase_type?
        const_node = namespace_node
      end

      const_names.reverse.join('::')
    end

    def here_document?(node)
      return false unless [:str, :dstr].include?(node.type)
      node.loc.respond_to?(:heredoc_end)
    end

    def contain_here_document?(node)
      node.each_node.any? { |n| here_document?(n) }
    end

    def in_explicit_parentheses?(node)
      return false unless node.begin_type?
      source = node.loc.expression.source
      source[0] == '(' && source[-1] == ')'
    end

    def first_block_arg_name(block_node)
      args_node = block_node.children[1]
      first_arg_node = args_node.children.first
      first_arg_node.children.first
    end

    def block_node_taken_by_method(node)
      parent_node = node.parent
      return nil unless parent_node
      return nil unless parent_node.block_type?
      return nil unless parent_node.children.first.equal?(node)
      parent_node
    end

    def each_forward_chained_node(origin_node, mode = nil)
      return to_enum(__method__, origin_node, mode) unless block_given?

      yield origin_node if mode == :include_origin

      parent_node = origin_node

      loop do
        child_node = parent_node.children.first

        return if !child_node || !child_node.is_a?(AST::Node)
        return unless [:send, :block].include?(child_node.type)

        if mode == :parent_as_second_arg
          yield child_node, parent_node
        else
          yield child_node
        end

        parent_node = child_node
      end

      nil
    end

    def each_backward_chained_node(origin_node, mode = nil)
      return to_enum(__method__, origin_node, mode) unless block_given?

      yield origin_node if mode == :include_origin

      origin_node.each_ancestor(:send, :block).reduce(origin_node) do |child_node, parent_node|
        break unless parent_node.children.first.equal?(child_node)

        if mode == :child_as_second_arg
          yield parent_node, child_node
        else
          yield parent_node
        end

        parent_node
      end

      nil
    end

    def indentation_of_line(arg)
      line = case arg
             when AST::Node             then arg.loc.expression.source_line
             when Parser::Source::Range then arg.source_line
             when String                then arg
             else fail ArgumentError, "Invalid argument #{arg}"
             end

      /^(?<indentation>\s*)\S/ =~ line
      indentation
    end

    def beginning_of_line_range(arg)
      range = range_from_arg(arg)
      begin_pos = range.begin_pos - range.column
      Parser::Source::Range.new(range.source_buffer, begin_pos, begin_pos)
    end

    def line_range(arg)
      range = range_from_arg(arg)
      beginning_of_line_range(range).resize(range.source_line.size + 1)
    end

    def each_line_range(arg)
      multiline_range = range_from_arg(arg)
      range = line_range(multiline_range)

      while range.line <= multiline_range.end.line
        yield range
        range = line_range(range.end)
      end
    end

    def range_from_arg(arg)
      case arg
      when AST::Node             then arg.loc.expression
      when Parser::Source::Range then arg
      else fail ArgumentError, "Invalid argument #{arg}"
      end
    end

    def literal?(node)
      case node.type
      when :array, :irange, :erange
        node.children.all? { |n| literal?(n) }
      when :hash
        node.children.all? do |pair_node|
          pair_node.children.all? { |n| literal?(n) }
        end
      when *LITERAL_TYPES
        true
      else
        false
      end
    end

    def expand_range_to_adjacent_whitespaces(range, direction = :both)
      source = range.source_buffer.source
      begin_pos = if [:both, :begin].include?(direction)
                    find_consecutive_whitespace_position(source, range.begin_pos, :downto)
                  else
                    range.begin_pos
                  end

      end_pos = if [:both, :end].include?(direction)
                  find_consecutive_whitespace_position(source, range.end_pos - 1, :upto) + 1
                else
                  range.end_pos
                end

      Parser::Source::Range.new(range.source_buffer, begin_pos, end_pos)
    end

    def find_consecutive_whitespace_position(source, origin, method)
      from, to = case method
                 when :upto
                   [origin + 1, source.length - 1]
                 when :downto
                   [origin - 1, 0]
                 else
                   fail "Invalid method #{method}"
                 end

      from.send(method, to).reduce(origin) do |previous_position, position|
        character = source[position]
        if WHITESPACES.include?(character)
          position
        else
          return previous_position
        end
      end
    end

    def chainable_source(node)
      fail "Invalid argument #{node}" unless node.send_type?

      map = node.loc
      source = map.expression.source

      return source if map.selector.source.start_with?('[')

      arg_node = node.children[2]
      return source unless arg_node

      left_of_arg_range = map.selector.end.join(arg_node.loc.expression.begin)
      return source if left_of_arg_range.source.include?('(')

      if map.selector.source.match(/^\w/)
        relative_index = left_of_arg_range.begin_pos - map.expression.begin_pos
        source[relative_index, left_of_arg_range.length] = '('
        source << ')'
      else
        "(#{source})"
      end
    end
  end
end
