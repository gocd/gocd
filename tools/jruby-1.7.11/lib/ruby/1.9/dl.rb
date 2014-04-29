warn "DL: This is only a partial implementation, and it's likely broken" if $VERBOSE

require 'ffi'

module DL
  def self.fiddle?
    true
  end

  class CPtr
    attr_reader :ffi_ptr
    extend FFI::DataConverter
    native_type FFI::Type::Builtin::POINTER

    def self.to_native(value, ctx)
      if value.is_a?(CPtr)
        value.ffi_ptr

      elsif value.is_a?(Integer)
        FFI::Pointer.new(value)

      elsif value.is_a?(String)
        value
      end
    end

    def self.from_native(value, ctx)
      self.new(value)
    end

    def self.to_ptr(value)
      if value.is_a?(String)
        cptr = CPtr.malloc(value.bytesize + 1)
        size = value.bytesize + 1
        cptr.ffi_ptr.put_string(0, value)
        cptr

      elsif value.respond_to?(:to_ptr)
        ptr = value.to_ptr
        ptr.is_a?(CPtr) ? ptr : CPtr.new(ptr)

      else
        CPtr.new(value)
      end
    end

    class << self
      alias [] to_ptr
    end

    def initialize(addr, size = nil, free = nil)
      ptr = if addr.is_a?(FFI::Pointer)
        addr

      elsif addr.is_a?(Integer)
        FFI::Pointer.new(addr)
      end

      @size = size ? size : ptr.size
      @free = free
      @ffi_ptr = free.nil? ? ptr : FFI::AutoPointer.new(ptr, self.class.__freefunc__(free))
    end

    def self.__freefunc__(free)
      if free.is_a?(FFI::Function)
        free

      elsif free.is_a?(FFI::Pointer)
        free.null? ? Proc.new { |ptr| } : FFI::Function.new(:void, [ :pointer ], free)

      elsif free.is_a?(Integer)
        free == 0 ? Proc.new { |ptr| } : FFI::Function.new(:void, [ :pointer ], FFI::Pointer.new(free))

      elsif free.respond_to?(:call)
        free

      else
        raise ArgumentError.new("invalid free func")
      end
    end

    def self.malloc(size, free = nil)
      self.new(LibC.malloc(size), size, free ? free : LibC::FREE)
    end

    def null?
      @ffi_ptr.null?
    end

    def to_ptr
      @ffi_ptr
    end

    def size
      defined?(@layout) ? @layout.size : @size
    end

    def size=(size)
      @size = size
    end

    def [](index, length = nil)
      if length
        ffi_ptr.get_string(index, length)
      else
        ffi_ptr.get_int(index)
      end
    end

    def to_i
      ffi_ptr.to_i
    end
    alias to_int to_i

    def to_str(len = nil)
      if len
        ffi_ptr.get_string(0, len)
      else
        ffi_ptr.get_string(0)
      end
    end
    alias to_s to_str

    def inspect
      "#<#{self.class.name} ptr=#{ffi_ptr.address.to_s(16)} size=#{@size} free=#{@free.inspect}>"
    end

    def +(delta)
      self.class.new(ffi_ptr + delta, @size - delta)
    end

    def -(delta)
      self.class.new(ffi_ptr - delta, @size + delta)
    end

    def ptr
      CPtr.new(ffi_ptr.get_pointer(0))
    end

    def ref
      cptr = CPtr.malloc(FFI::Type::POINTER.size)
      cptr.ffi_ptr.put_pointer(0, ffi_ptr)
      cptr
    end
  end

  NULL = CPtr.new(FFI::Pointer::NULL, 0)

  TYPE_VOID         = 0
  TYPE_VOIDP        = 1
  TYPE_CHAR         = 2
  TYPE_SHORT        = 3
  TYPE_INT          = 4
  TYPE_LONG         = 5
  TYPE_LONG_LONG    = 6
  TYPE_FLOAT        = 7
  TYPE_DOUBLE       = 8

  FFITypes = {
    'c' => FFI::Type::INT8,
    'h' => FFI::Type::INT16,
    'i' => FFI::Type::INT32,
    'l' => FFI::Type::LONG,
    'f' => FFI::Type::FLOAT32,
    'd' => FFI::Type::FLOAT64,
    'p' => FFI::Type::Mapped.new(CPtr),
    's' => FFI::Type::STRING,

    TYPE_VOID => FFI::Type::Builtin::VOID,
    TYPE_VOIDP => FFI::Type::Mapped.new(CPtr),
    TYPE_CHAR => FFI::Type::Builtin::CHAR,
    TYPE_SHORT => FFI::Type::Builtin::SHORT,
    TYPE_INT => FFI::Type::Builtin::INT,
    TYPE_LONG => FFI::Type::Builtin::LONG,
    TYPE_LONG_LONG => FFI::Type::Builtin::LONG_LONG,
    TYPE_FLOAT => FFI::Type::Builtin::FLOAT,
    TYPE_DOUBLE => FFI::Type::Builtin::DOUBLE,
  }

  def self.__ffi_type__(dl_type)
    ffi_type = FFITypes[dl_type]
    ffi_type = FFITypes[-dl_type] if ffi_type.nil? && dl_type.is_a?(Integer) && dl_type < 0
    raise TypeError.new("cannot convert #{dl_type} to ffi") unless ffi_type
    ffi_type
  end

  ALIGN_VOIDP       = FFITypes[TYPE_VOIDP].alignment
  ALIGN_CHAR        = FFITypes[TYPE_CHAR].alignment
  ALIGN_SHORT       = FFITypes[TYPE_SHORT].alignment
  ALIGN_INT         = FFITypes[TYPE_INT].alignment
  ALIGN_LONG        = FFITypes[TYPE_LONG].alignment
  ALIGN_LONG_LONG   = FFITypes[TYPE_LONG_LONG].alignment
  ALIGN_FLOAT       = FFITypes[TYPE_FLOAT].alignment
  ALIGN_DOUBLE      = FFITypes[TYPE_DOUBLE].alignment

  SIZEOF_VOIDP       = FFITypes[TYPE_VOIDP].size
  SIZEOF_CHAR        = FFITypes[TYPE_CHAR].size
  SIZEOF_SHORT       = FFITypes[TYPE_SHORT].size
  SIZEOF_INT         = FFITypes[TYPE_INT].size
  SIZEOF_LONG        = FFITypes[TYPE_LONG].size
  SIZEOF_LONG_LONG   = FFITypes[TYPE_LONG_LONG].size
  SIZEOF_FLOAT       = FFITypes[TYPE_FLOAT].size
  SIZEOF_DOUBLE      = FFITypes[TYPE_DOUBLE].size

  TypeMap = {
    '0' => TYPE_VOID,
    'C' => TYPE_CHAR,
    'H' => TYPE_SHORT,
    'I' => TYPE_INT,
    'L' => TYPE_LONG,
    'F' => TYPE_FLOAT,
    'D' => TYPE_DOUBLE,
    'S' => TYPE_VOIDP,
    's' => TYPE_VOIDP,
    'p' => TYPE_VOIDP,
    'P' => TYPE_VOIDP,
    'c' => TYPE_VOIDP,
    'h' => TYPE_VOIDP,
    'i' => TYPE_VOIDP,
    'l' => TYPE_VOIDP,
    'f' => TYPE_VOIDP,
    'd' => TYPE_VOIDP,
  }
  
  Char2TypeName = {
    '0' => 'void',
    'C' => 'char',
    'H' => 'short',
    'I' => 'int',
    'L' => 'long',
    'F' => 'float',
    'D' => 'double',
    'S' => 'const char *',
    's' => 'char *',
    'p' => 'void *',
    'P' => 'void *',
    'c' => 'char *',
    'h' => 'short *',
    'i' => 'int *',
    'l' => 'long *',
    'f' => 'float *',
    'd' => 'double *',
    'A' => '[]',
    'a' => '[]',
  }

  

  RTLD_LAZY = FFI::DynamicLibrary::RTLD_LAZY
  RTLD_GLOBAL = FFI::DynamicLibrary::RTLD_GLOBAL
  RTLD_NOW = FFI::DynamicLibrary::RTLD_NOW

  class DLError < StandardError

  end

  class DLTypeError < DLError

  end

  def self.find_type(type)
    ffi_type = __ffi_type__(TypeMap[type])
    raise DLTypeError.new("Unknown type '#{type}'") unless ffi_type
    ffi_type
  end

  def self.align(offset, align)
    mask = align - 1;
    off = offset;
    ((off & mask) != 0) ? (off & ~mask) + align : off
  end

  def self.sizeof(type)
    type = type.split(//)
    i = 0
    size = 0
    while i < type.length
      t = type[i]
      i += 1
      count = String.new
      while i < type.length && type[i] =~ /[0123456789]/
        count << type[i]
        i += 1
      end
      n = count.empty? ? 1 : count.to_i
      ffi_type = FFITypes[t.downcase]
      raise DLTypeError.new("unexpected type '#{t}'") unless ffi_type
      if t.upcase == t
        size = align(size, ffi_type.alignment) + n * ffi_type.size
      else
        size += n * ffi_type.size
      end
    end
    size
  end

  class Handle
    def initialize(libname = nil, flags = RTLD_LAZY | RTLD_GLOBAL)
      @lib = FFI::DynamicLibrary.open(libname, flags)
      raise RuntimeError, "Could not open #{libname}" unless @lib

      @open = true

      begin
        yield(self)
      ensure
        self.close
      end if block_given?
    end

    def close
      raise DLError.new("closed handle") unless @open
      @open = false
      0
    end

    def self.sym(func)
      DEFAULT.sym(func)
    end

    def sym(func)
      raise TypeError.new("invalid function name") unless func.is_a?(String)
      raise DLError.new("closed handle") unless @open
      address = @lib.find_function(func)
      raise DLError.new("unknown symbol #{func}") if address.nil? || address.null?
      address.to_i
    end

    def self.[](func)
      self.sym(func)
    end

    def [](func)
      sym(func)
    end

    def enable_close
      @enable_close = true
    end

    def close_enabled?
      @enable_close
    end

    def disable_close
      @enable_close = false
    end

    def to_i
      0
    end

    DEFAULT = Handle.new
  end

  def self.find_return_type(type)
    # Restrict types to the known-supported ones
    raise "Unsupported return type '#{type}'" unless type =~ /[0CHILFDPS]/
    DL.find_type(type)
  end

  def self.find_param_type(type)
    # Restrict types to the known-supported ones
    raise "Unsupported parameter type '#{type}'" unless type =~ /[CHILFDPS]/
    DL.find_type(type)
  end

  class Symbol

    attr_reader :name, :proto

    def initialize(address, type = nil, name = nil)
      @address = address
      @name = name
      @proto = type
      
      rt = DL.find_return_type(type[0].chr)
      arg_types = []
      type[1..-1].each_byte { |t| arg_types << DL.find_param_type(t.chr) } if type.length > 1

      @invoker = FFI::Function.new(rt, arg_types, FFI::Pointer.new(address), :convention => :default)
    end

    def call(*args)
      [ @invoker.call(*args), args ]
    end

    def cproto
      cproto = @proto[1..-1].split(//).map { |t| Symbol.char2type(t) }.join(', ')
      "#{Symbol.char2type(@proto[0].chr)} #{@name}(#{cproto})"
    end

    def inspect
      "#<DL::Symbol func=0x#{@address.address.to_s(16)} '#{cproto}'>"
    end

    def to_s
      cproto
    end

    def to_i
      @address.address.to_i
    end

    def self.char2type(ch)
      Char2TypeName[ch]
    end

  end


  def self.dlopen(libname)
    Handle.new(libname)
  end

  def dlopen(libname)
    DL.dlopen libname
  end

  module LibC
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    MALLOC = attach_function :malloc, [ :size_t ], :pointer
    REALLOC = attach_function :realloc, [ :pointer, :size_t ], :pointer
    FREE = attach_function :free, [ :pointer ], :void
  end

  def self.malloc(size)
    LibC.malloc(size).address
  end

  def self.realloc(ptr, size)
    LibC.realloc(FFI::Pointer.new(ptr), size).address
  end

  def self.free(ptr)
    LibC.free(FFI::Pointer.new(ptr))
  end

  class CFunc
    attr_reader :ctype, :name
    attr_accessor :calltype

    def initialize(addr, type = TYPE_VOID, name = nil, calltype = :cdecl)
      @ptr = CFunc.__cptr__(addr)
      @name = name ? name.dup.taint : nil
      @ffi_rtype = DL.__ffi_type__(type)
      @ctype = type
      @calltype = calltype
    end

    def call(args)
      raise NotImplementedError.new("#{self.class}#call is dangerous and should not be used")

      # This is a half-hearted attempt to get a fubar API to work
      dl_arg_types = args.map { |arg|
        if arg.is_a?(Integer)
          TYPE_LONG
          
        elsif arg.is_a?(DL::CPtr) || arg.is_a?(String)
          TYPE_VOIDP

        else
          raise TypeError.new "unsupported type: #{arg.class}"
        end
      }
      FFI::Function.new(@ffi_rtype,
        dl_arg_types.map { |t| DL.__ffi_type__(t) },
        @ptr.ffi_ptr, @calltype == :stdcall ? :stdcall : :default).call(*args)
    end
    alias [] call

    def self.__cptr__(ptr)
      if ptr.is_a?(CPtr)
        ptr
      elsif ptr.is_a?(Integer)
        CPtr.new(ptr)
      else
        raise TypeError.new "invalid ptr #{@ptr}"
      end
    end

    def self.last_error
      error = FFI.errno
      error == 0 ? nil : error
    end

    def self.last_error=(error)
      FFI.errno = error
    end

    def ctype=(type)
      @ffi_rtype = DL.__ffi_type__(type)
      @ctype = type
    end

    def calltype=(calltype)
      @calltype = calltype
    end

    def ptr
      @ptr.to_i
    end

    def ptr=(ptr)
      @ptr = CFunc.__cptr__(ptr)
    end

    def to_i
      @ptr.to_i
    end

    def inspect
      "#<DL::CFunc:0 ptr=0x#{@ptr.to_i.to_s(16)} type=#{@ctype} name='#{@name}'>"
    end
    alias to_s inspect
  end
end

require 'fiddle/jruby'
