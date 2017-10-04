unless Proc.method_defined? :lambda?
  module Backports
    def self.alias_method_chain(mod, target, feature)
      mod.class_eval do
        # Strip out punctuation on predicates or bang methods since
        # e.g. target?_without_feature is not a valid method name.
        aliased_target, punctuation = target.to_s.sub(/([?!=])$/, ''), $1
        yield(aliased_target, punctuation) if block_given?

        with_method, without_method = "#{aliased_target}_with_#{feature}#{punctuation}", "#{aliased_target}_without_#{feature}#{punctuation}"

        alias_method without_method, target
        alias_method target, with_method

        case
          when public_method_defined?(without_method)
            public target
          when protected_method_defined?(without_method)
            protected target
          when private_method_defined?(without_method)
            private target
        end
      end
    end
  end

  class Proc
    # Standard in Ruby 1.9. See official documentation[http://ruby-doc.org/core-1.9/classes/Proc.html]
    def lambda?
      !!__is_lambda__
    end

    attr_accessor :__is_lambda__
    private :__is_lambda__
    private :__is_lambda__=
  end

  class Method
    def to_proc_with_lambda_tracking
      proc = to_proc_without_lambda_tracking
      proc.send :__is_lambda__=, true
      proc
    end
    Backports.alias_method_chain self, :to_proc, :lambda_tracking
  end

  module Kernel
    def lambda_with_lambda_tracking(&block)
      l = lambda_without_lambda_tracking(&block)
      l.send :__is_lambda__=, true unless block.send(:__is_lambda__) == false
      l
    end

    def proc_with_lambda_tracking(&block)
      l = proc_without_lambda_tracking(&block)
      l.send :__is_lambda__=, block.send(:__is_lambda__) == true
      l
    end

    Backports.alias_method_chain self, :lambda, :lambda_tracking
    Backports.alias_method_chain self, :proc, :lambda_tracking
  end
end
