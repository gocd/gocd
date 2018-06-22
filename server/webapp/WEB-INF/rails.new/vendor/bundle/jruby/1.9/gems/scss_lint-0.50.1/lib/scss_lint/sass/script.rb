# Ignore documentation lints as these aren't original implementations.
# rubocop:disable Documentation

module Sass::Script
  # Since the Sass library is already loaded at this point.
  # Define the `node_name` and `visit_method` class methods for each Sass Script
  # parse tree node type so that our custom visitor can seamless traverse the
  # tree.
  # Define the `invalid_child_method_name` and `invalid_parent_method_name`
  # class methods to make errors understandable.
  #
  # This would be easier if we could just define an `inherited` callback, but
  # that won't work since the Sass library will have already been loaded before
  # this code gets loaded, so the `inherited` callback won't be fired.
  #
  # Thus we are left to manually define the methods for each type explicitly.
  {
    'Value' => %w[ArgList Bool Color List Map Null Number String],
    'Tree'  => %w[Funcall Interpolation ListLiteral Literal MapLiteral
                  Operation Selector StringInterpolation UnaryOperation Variable],
  }.each do |namespace, types|
    types.each do |type|
      node_name = type.downcase

      eval <<-DECL
        class #{namespace}::#{type}
          def self.node_name
            :script_#{node_name}
          end

          def self.visit_method
            :visit_script_#{node_name}
          end

          def self.invalid_child_method_name
            :"invalid_#{node_name}_child?"
          end

          def self.invalid_parent_method_name
            :"invalid_#{node_name}_parent?"
          end
        end
      DECL
    end
  end

  class Value::Base
    attr_accessor :node_parent

    def children
      []
    end

    def line
      @line || (node_parent && node_parent.line)
    end

    def source_range
      @source_range || (node_parent && node_parent.source_range)
    end
  end

  # Contains extensions of Sass::Script::Tree::Nodes to add support for
  # accessing various parts of the parse tree not provided out-of-the-box.
  module Tree
    class Node
      attr_accessor :node_parent
    end

    class Literal
      # Literals wrap their underlying values. For sake of convenience, consider
      # the wrapped value a child of the Literal.
      def children
        [value]
      end
    end

    # This monkey patch can be removed once scss-lint depends on a minimum
    # version of the sass gem that includes a fix for
    # https://github.com/sass/sass/issues/1799
    class ListLiteral
      def source_range
        return @source_range if @elements.empty?
        @source_range.end_pos = @elements.last.source_range.end_pos
        @source_range
      end
    end

    class MapLiteral
      def source_range
        return @source_range if @pairs.empty?
        @source_range.end_pos = @pairs.last.last.source_range.end_pos
        @source_range
      end
    end
  end
end
