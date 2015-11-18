module Sass::Script::Tree
  # A parse tree node representing a list literal. When resolved, this returns a
  # {Sass::Tree::Value::List}.
  class ListLiteral < Node
    # The parse nodes for members of this list.
    #
    # @return [Array<Node>]
    attr_reader :elements

    # The operator separating the values of the list. Either `:comma` or
    # `:space`.
    #
    # @return [Symbol]
    attr_reader :separator

    # Creates a new list literal.
    #
    # @param elements [Array<Node>] See \{#elements}
    # @param separator [Symbol] See \{#separator}
    def initialize(elements, separator)
      @elements = elements
      @separator = separator
    end

    # @see Node#children
    def children; elements; end

    # @see Value#to_sass
    def to_sass(opts = {})
      return "()" if elements.empty?
      members = elements.map do |v|
        if element_needs_parens?(v)
          "(#{v.to_sass(opts)})"
        else
          v.to_sass(opts)
        end
      end

      return "(#{members.first},)" if separator == :comma && members.length == 1

      members.join(sep_str(nil))
    end

    # @see Node#deep_copy
    def deep_copy
      node = dup
      node.instance_variable_set('@elements', elements.map {|e| e.deep_copy})
      node
    end

    def inspect
      "(#{elements.map {|e| e.inspect}.join(separator == :space ? ' ' : ', ')})"
    end

    def force_division!
      # Do nothing. Lists prevent division propagation.
    end

    protected

    def _perform(environment)
      list = Sass::Script::Value::List.new(
        elements.map {|e| e.perform(environment)},
        separator)
      list.source_range = source_range
      list.options = options
      list
    end

    private

    # Returns whether an element in the list should be wrapped in parentheses
    # when serialized to Sass.
    def element_needs_parens?(element)
      if element.is_a?(ListLiteral)
        return Sass::Script::Parser.precedence_of(element.separator) <=
               Sass::Script::Parser.precedence_of(separator)
      end

      return false unless separator == :space

      if element.is_a?(UnaryOperation)
        return element.operator == :minus || element.operator == :plus
      end

      return false unless element.is_a?(Operation)
      return true unless element.operator == :div
      !(is_literal_number?(element.operand1) && is_literal_number?(element.operand2))
    end

    # Returns whether a value is a number literal that shouldn't be divided.
    def is_literal_number?(value)
      value.is_a?(Literal) &&
        value.value.is_a?((Sass::Script::Value::Number)) &&
        !value.value.original.nil?
    end

    def sep_str(opts = options)
      return ' ' if separator == :space
      return ',' if opts && opts[:style] == :compressed
      ', '
    end
  end
end
