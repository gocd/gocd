module Representable
  # Allows to implement a pipeline of filters where a value gets passed in and the result gets
  # passed to the next callable object.
  #
  # Note: this is still experimental.
  class Pipeline < Array
    include Uber::Callable
    # include Representable::Cloneable

    def call(context, value, *args)
      inject(value) { |memo, block| block.call(memo, *args) }
    end
  end
end
