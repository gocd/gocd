module Sass
  module Tree
    # A dynamic node representing a function definition.
    #
    # @see Sass::Tree
    class FunctionNode < Node
      # The name of the function.
      # @return [String]
      attr_reader :name

      # The arguments to the function. Each element is a tuple
      # containing the variable for argument and the parse tree for
      # the default value of the argument
      #
      # @return [Array<Script::Tree::Node>]
      attr_accessor :args

      # The splat argument for this function, if one exists.
      #
      # @return [Script::Tree::Node?]
      attr_accessor :splat

      # @param name [String] The function name
      # @param args [Array<(Script::Tree::Node, Script::Tree::Node)>]
      #   The arguments for the function.
      # @param splat [Script::Tree::Node] See \{#splat}
      def initialize(name, args, splat)
        @name = name
        @args = args
        @splat = splat
        super()

        if %w[and or not].include?(name)
          raise Sass::SyntaxError.new("Invalid function name \"#{name}\".")
        end
      end
    end
  end
end
