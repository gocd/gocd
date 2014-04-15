module Spec
  module Matchers
    module DSL
      # See Spec::Matchers
      def define(name, &declarations)
        define_method name do |*expected|
          Spec::Matchers::Matcher.new name, *expected, &declarations
        end
      end
      
      # Deprecated - use define
      def create(name, &declarations)
        Spec.deprecate("Spec::Matchers.create","Spec::Matchers.define")
        define(name, &declarations)
      end
    end
  end
end

Spec::Matchers.extend Spec::Matchers::DSL
