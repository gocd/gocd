require 'ffi'
require 'dl'

module Fiddle
  TYPE_VOID         = DL::TYPE_VOID
  TYPE_VOIDP        = DL::TYPE_VOIDP
  TYPE_CHAR         = DL::TYPE_CHAR
  TYPE_SHORT        = DL::TYPE_SHORT
  TYPE_INT          = DL::TYPE_INT
  TYPE_LONG         = DL::TYPE_LONG
  TYPE_LONG_LONG    = DL::TYPE_LONG_LONG
  TYPE_FLOAT        = DL::TYPE_FLOAT
  TYPE_DOUBLE       = DL::TYPE_DOUBLE

  WINDOWS = FFI::Platform.windows?

  class Function
    DEFAULT = "default"
    STDCALL = "stdcall"

    def initialize(ptr, args, return_type, abi = DEFAULT)
      @ptr, @args, @return_type, @abi = ptr, args, return_type, abi
      raise TypeError.new "invalid return type" unless return_type.is_a?(Integer)
      raise TypeError.new "invalid return type" unless args.is_a?(Array)
      
      @function = FFI::Function.new(
        DL.__ffi_type__(@return_type),
        @args.map { |t| DL.__ffi_type__(t) },
        ptr.is_a?(DL::CPtr) ? ptr.ffi_ptr : FFI::Pointer.new(ptr.to_i),
        :convention => @abi
      )
      @function.attach(self, "call")
    end

    # stubbed; should be overwritten by initialize's #attach call above
    def call(*args); end
  end

  class Closure
    def initialize(ret, args, abi = Function::DEFAULT)
      @ctype, @args = ret, args
      raise TypeError.new "invalid return type" unless ret.is_a?(Integer)
      raise TypeError.new "invalid return type" unless args.is_a?(Array)

      @function = FFI::Function.new(
        DL.__ffi_type__(@ctype),
        @args.map { |t| DL.__ffi_type__(t) },
        self,
        :convention => abi
      )
    end

    def to_i
      @function.to_i
    end
  end
end