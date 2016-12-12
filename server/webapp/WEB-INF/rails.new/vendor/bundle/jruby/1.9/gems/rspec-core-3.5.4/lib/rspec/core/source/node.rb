RSpec::Support.require_rspec_core "source/location"

module RSpec
  module Core
    class Source
      # @private
      # A wrapper for Ripper AST node which is generated with `Ripper.sexp`.
      class Node
        include Enumerable

        attr_reader :sexp, :parent

        def self.sexp?(array)
          array.is_a?(Array) && array.first.is_a?(Symbol)
        end

        def initialize(ripper_sexp, parent=nil)
          @sexp = ripper_sexp.freeze
          @parent = parent
        end

        def type
          sexp[0]
        end

        def args
          @args ||= raw_args.map do |raw_arg|
            if Node.sexp?(raw_arg)
              Node.new(raw_arg, self)
            elsif Location.location?(raw_arg)
              Location.new(*raw_arg)
            elsif raw_arg.is_a?(Array)
              GroupNode.new(raw_arg, self)
            else
              raw_arg
            end
          end.freeze
        end

        def children
          @children ||= args.select { |arg| arg.is_a?(Node) }.freeze
        end

        def location
          @location ||= args.find { |arg| arg.is_a?(Location) }
        end

        def each(&block)
          return to_enum(__method__) unless block_given?

          yield self

          children.each do |child|
            child.each(&block)
          end
        end

        def each_ancestor
          return to_enum(__method__) unless block_given?

          current_node = self

          while (current_node = current_node.parent)
            yield current_node
          end
        end

        def inspect
          "#<#{self.class} #{type}>"
        end

      private

        def raw_args
          sexp[1..-1] || []
        end
      end

      # @private
      class GroupNode < Node
        def type
          :group
        end

      private

        def raw_args
          sexp
        end
      end
    end
  end
end
