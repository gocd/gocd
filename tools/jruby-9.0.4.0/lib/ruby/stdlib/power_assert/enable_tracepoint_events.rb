if defined? RubyVM
  basic_classes = [
    Fixnum, Float, String, Array, Hash, Bignum, Symbol, Time, Regexp
  ]

  basic_operators = [
    :+, :-, :*, :/, :%, :==, :===, :<, :<=, :<<, :[], :[]=,
    :length, :size, :empty?, :succ, :>, :>=, :!, :!=, :=~, :freeze
  ]

  # set redefined flag
  basic_classes.each do |klass|
    basic_operators.each do |bop|
      if klass.public_method_defined?(bop)
        klass.instance_method(bop).owner.module_eval do
          public bop
        end
      end
    end
  end

  # bypass check_cfunc
  verbose = $VERBOSE
  begin
    $VERBOSE = nil
    module PowerAssert
      refine BasicObject do
        def !
        end

        def ==
        end
      end

      refine Module do
        def ==
        end
      end

      refine Symbol do
        def ==
        end
      end
    end
  ensure
    $VERBOSE = verbose
  end

  # disable optimization
  RubyVM::InstructionSequence.compile_option = {
    specialized_instruction: false
  }
end
