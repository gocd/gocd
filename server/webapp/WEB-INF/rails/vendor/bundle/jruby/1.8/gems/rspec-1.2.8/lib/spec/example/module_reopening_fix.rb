module Spec
  module Example
    # When you reopen a module that is included in another module that is included in a class,
    # the new material you define does not make it to the class. This fixes that.
    #
    # == Example
    #
    #   module M1; end
    #
    #   module M2
    #     def foo; "FOO"; end
    #   end
    #
    #   class C
    #     include M1
    #   end
    #
    #   module M1
    #     include M2
    #   end
    #
    #   c = C.new
    #   c.foo
    #   NoMethodError: undefined method `foo' for #<C:0x5e89a4>
    #     from (irb):12
    module ModuleReopeningFix
      def child_modules
        @child_modules ||= []
      end

      def included(mod)
        child_modules << mod
      end

      def include(mod)
        super
        child_modules.each do |child_module|
          child_module.__send__(:include, mod)
        end
      end
    end
  end
end
