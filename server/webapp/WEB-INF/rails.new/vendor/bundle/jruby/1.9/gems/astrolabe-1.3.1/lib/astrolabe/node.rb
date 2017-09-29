# coding: utf-8

require 'parser'

module Astrolabe
  # `Astrolabe::Node` is a subclass of `Parser::AST::Node`. It provides an access to parent node and
  # an object-oriented way to handle AST with the power of `Enumerable`.
  #
  # Though not described in the auto-generated API documentation, it has predicate methods for every
  # node type. These methods would be useful especially when combined with `Enumerable` methods.
  #
  # @example
  #   node.send_type?    # Equivalent to: `node.type == :send`
  #   node.op_asgn_type? # Equivalent to: `node.type == :op_asgn`
  #
  #   # Non-word characters (other than a-zA-Z0-9_) in type names are omitted.
  #   node.defined_type? # Equivalent to: `node.type == :defined?`
  #
  #   # Find the first lvar node under the receiver node.
  #   lvar_node = node.each_descendant.find(&:lvar_type?)
  class Node < Parser::AST::Node
    # @see http://rubydoc.info/gems/ast/AST/Node:initialize
    def initialize(type, children = [], properties = {})
      @mutable_attributes = {}

      # ::AST::Node#initialize freezes itself.
      super

      # #parent= would be invoked multiple times for a node because there are pending nodes while
      # constructing AST and they are replaced later.
      # For example, `lvar` and `send` type nodes are initially created as an `ident` type node and
      # fixed to each type later.
      # So, the #parent attribute needs to be mutable.
      each_child_node do |child_node|
        child_node.parent = self
      end
    end

    Parser::Meta::NODE_TYPES.each do |node_type|
      method_name = "#{node_type.to_s.gsub(/\W/, '')}_type?"
      define_method(method_name) do
        type == node_type
      end
    end

    # Returns the parent node, or `nil` if the receiver is a root node.
    #
    # @return [Node, nil] the parent node or `nil`
    def parent
      @mutable_attributes[:parent]
    end

    def parent=(node)
      @mutable_attributes[:parent] = node
    end

    protected :parent=

    # Returns whether the receiver is a root node or not.
    #
    # @return [Boolean] whether the receiver is a root node or not
    def root?
      parent.nil?
    end

    # Returns the index of the receiver node in its siblings.
    #
    # @return [Integer] the index of the receiver node in its siblings
    def sibling_index
      parent.children.index { |sibling| sibling.equal?(self) }
    end

    # Calls the given block for each ancestor node in the order from parent to root.
    # If no block is given, an `Enumerator` is returned.
    #
    # @overload each_ancestor
    #   Yield all nodes.
    # @overload each_ancestor(type)
    #   Yield only nodes matching the type.
    #   @param [Symbol] type a node type
    # @overload each_ancestor(type_a, type_b, ...)
    #   Yield only nodes matching any of the types.
    #   @param [Symbol] type_a a node type
    #   @param [Symbol] type_b a node type
    # @overload each_ancestor(types)
    #   Yield only nodes matching any of types in the array.
    #   @param [Array<Symbol>] types an array containing node types
    # @yieldparam [Node] node each ancestor node
    # @return [self] if a block is given
    # @return [Enumerator] if no block is given
    def each_ancestor(*types, &block)
      return to_enum(__method__, *types) unless block_given?

      types.flatten!

      if types.empty?
        visit_ancestors(&block)
      else
        visit_ancestors_with_types(types, &block)
      end

      self
    end

    # Returns an array of ancestor nodes.
    # This is a shorthand for `node.each_ancestor.to_a`.
    #
    # @return [Array<Node>] an array of ancestor nodes
    def ancestors
      each_ancestor.to_a
    end

    # Calls the given block for each child node.
    # If no block is given, an `Enumerator` is returned.
    #
    # Note that this is different from `node.children.each { |child| ... }` which yields all
    # children including non-node element.
    #
    # @overload each_child_node
    #   Yield all nodes.
    # @overload each_child_node(type)
    #   Yield only nodes matching the type.
    #   @param [Symbol] type a node type
    # @overload each_child_node(type_a, type_b, ...)
    #   Yield only nodes matching any of the types.
    #   @param [Symbol] type_a a node type
    #   @param [Symbol] type_b a node type
    # @overload each_child_node(types)
    #   Yield only nodes matching any of types in the array.
    #   @param [Array<Symbol>] types an array containing node types
    # @yieldparam [Node] node each child node
    # @return [self] if a block is given
    # @return [Enumerator] if no block is given
    def each_child_node(*types)
      return to_enum(__method__, *types) unless block_given?

      types.flatten!

      children.each do |child|
        next unless child.is_a?(Node)
        yield child if types.empty? || types.include?(child.type)
      end

      self
    end

    # Returns an array of child nodes.
    # This is a shorthand for `node.each_child_node.to_a`.
    #
    # @return [Array<Node>] an array of child nodes
    def child_nodes
      each_child_node.to_a
    end

    # Calls the given block for each descendant node with depth first order.
    # If no block is given, an `Enumerator` is returned.
    #
    # @overload each_descendant
    #   Yield all nodes.
    # @overload each_descendant(type)
    #   Yield only nodes matching the type.
    #   @param [Symbol] type a node type
    # @overload each_descendant(type_a, type_b, ...)
    #   Yield only nodes matching any of the types.
    #   @param [Symbol] type_a a node type
    #   @param [Symbol] type_b a node type
    # @overload each_descendant(types)
    #   Yield only nodes matching any of types in the array.
    #   @param [Array<Symbol>] types an array containing node types
    # @yieldparam [Node] node each descendant node
    # @return [self] if a block is given
    # @return [Enumerator] if no block is given
    def each_descendant(*types, &block)
      return to_enum(__method__, *types) unless block_given?

      types.flatten!

      if types.empty?
        visit_descendants(&block)
      else
        visit_descendants_with_types(types, &block)
      end

      self
    end

    # Returns an array of descendant nodes.
    # This is a shorthand for `node.each_descendant.to_a`.
    #
    # @return [Array<Node>] an array of descendant nodes
    def descendants
      each_descendant.to_a
    end

    # Calls the given block for the receiver and each descendant node with depth first order.
    # If no block is given, an `Enumerator` is returned.
    #
    # This method would be useful when you treat the receiver node as a root of tree and want to
    # iterate all nodes in the tree.
    #
    # @overload each_node
    #   Yield all nodes.
    # @overload each_node(type)
    #   Yield only nodes matching the type.
    #   @param [Symbol] type a node type
    # @overload each_node(type_a, type_b, ...)
    #   Yield only nodes matching any of the types.
    #   @param [Symbol] type_a a node type
    #   @param [Symbol] type_b a node type
    # @overload each_node(types)
    #   Yield only nodes matching any of types in the array.
    #   @param [Array<Symbol>] types an array containing node types
    # @yieldparam [Node] node each node
    # @return [self] if a block is given
    # @return [Enumerator] if no block is given
    def each_node(*types, &block)
      return to_enum(__method__, *types) unless block_given?

      types.flatten!

      yield self if types.empty? || types.include?(type)

      if types.empty?
        visit_descendants(&block)
      else
        visit_descendants_with_types(types, &block)
      end

      self
    end

    protected

    def visit_descendants(&block)
      children.each do |child|
        next unless child.is_a?(Node)
        yield child
        child.visit_descendants(&block)
      end
    end

    def visit_descendants_with_types(types, &block)
      children.each do |child|
        next unless child.is_a?(Node)
        yield child if types.include?(child.type)
        child.visit_descendants_with_types(types, &block)
      end
    end

    private

    def visit_ancestors
      last_node = self

      while (current_node = last_node.parent)
        yield current_node
        last_node = current_node
      end
    end

    def visit_ancestors_with_types(types)
      last_node = self

      while (current_node = last_node.parent)
        yield current_node if types.include?(current_node.type)
        last_node = current_node
      end
    end
  end
end
