module RSpec
  module Core
    class DeprecatedMutableArrayProxy

      def initialize(array)
        @array = array
      end

      mutated_methods =
        [
          :<<, :[]=, :clear, :collect!, :compact!, :concat, :delete,
          :delete_at, :delete_if, :fill, :flatten!, :keep_if, :map!,
          :pop, :push, :reject!, :replace, :reverse!, :rotate!,
          :select!, :shift, :shuffle!, :slice!, :sort!, :sort_by!,
          :uniq!, :unshift
        ]
      array_methods = Array.instance_methods.map(&:to_sym)

      (array_methods & mutated_methods).each do |name|
        define_method(name) do |*args, &block|
          RSpec.deprecate "Mutating the `RSpec.configuration.formatters` array"
          @array.__send__ name, *args, &block
        end
      end

      (array_methods - mutated_methods).each do |name|
        define_method(name) { |*args, &block| @array.__send__ name, *args, &block }
      end

    end
  end
end
