module Spec
  module Rails
    module Example
      class AssignsHashProxy #:nodoc:
        def initialize(example_group, &block)
          @target = block.call
          @example_group = example_group
        end

        def [](key)
          return false if false == assigns[key] || false == assigns[key.to_s]
          assigns[key] || assigns[key.to_s] || @target.instance_variable_get("@#{key}")
        end

        def []=(key, val)
          @target.instance_variable_set("@#{key}", val)
        end

        def delete(key)
          assigns.delete(key.to_s)
          @target.instance_variable_set("@#{key}", nil)
        end

        def each(&block)
          assigns.each &block
        end

        def has_key?(key)
          assigns.key?(key.to_s)
        end

        protected
        def assigns
          @example_group.orig_assigns
        end
      end
    end
  end
end
