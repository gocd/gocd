warn "DL: This is only a partial implementation, and it's likely broken"

require 'ffi'

module DL
  TypeMap = {
    '0' => :void,
    'C' => :char,
    'H' => :short,
    'I' => :int,
    'L' => :long,
    'F' => :float,
    'D' => :double,
    'S' => :string,
    's' => :pointer,
    'p' => :pointer,
    'P' => :pointer,
    'c' => :pointer,
    'h' => :pointer,
    'i' => :pointer,
    'l' => :pointer,
    'f' => :pointer,
    'd' => :pointer,
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

  ALIGN_SHORT = 2
  ALIGN_INT = 4
  ALIGN_LONG = FFI::Platform::ARCH =~ /sparc/ ? 8 : (FFI::Platform::LONG_SIZE / 8)
  ALIGN_VOIDP = FFI::Platform::ARCH =~ /sparc/ ? 8 : (FFI::Platform::ADDRESS_SIZE / 8)
  ALIGN_FLOAT = FFI::Platform::ARCH =~ /sparc/ ? 8 : 4
  ALIGN_DOUBLE = FFI::Platform::ARCH =~ /sparc/ ? 8 : (FFI::Platform::LONG_SIZE / 8)

  class DLError < StandardError

  end

  class DLTypeError < DLError

  end

  def self.find_type(type)
    ffi_type = TypeMap[type]
    raise DLTypeError.new("Unknown type '#{type}'") unless ffi_type
    FFI.find_type(ffi_type)
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
      count = []
      while i < type.length && type[i] =~ /[0123456789]/
        count << type[i]
        i += 1
      end
      n = count.empty? ? 1 : count.join("").to_i
      case t
      when 'I'
        size = align(size, ALIGN_INT) + n * 4
      when 'i'
        size += n * 4
      when 'L'
        size = align(size, ALIGN_LONG) + n * FFI::Platform::LONG_SIZE / 8
      when 'l'
        size += n * FFI::Platform::LONG_SIZE / 8
      when 'F'
        size = align(size, ALIGN_FLOAT) + n * 4
      when 'f'
        size += n * 4
      when 'D'
        size = align(size, ALIGN_DOUBLE) + n * 8
      when 'd'
        size += n * 8
      when 'C', 'c'
        size += n * 1
      when 'H'
        size = align(size, ALIGN_SHORT) + n * 4
      when 'h'
        size += n * 2
      when 'P', 'S'
        size = align(size, ALIGN_VOIDP) + n * FFI::Platform::ADDRESS_SIZE / 8
      when 'p', 's'
        size += n * FFI::Platform::ADDRESS_SIZE / 8
      else
        raise DLTypeError.new("unexpected type '#{t}'")
      end
    end
    size
  end

  class Handle

    def initialize(libname, flags = RTLD_LAZY | RTLD_GLOBAL)
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
      raise "Closing #{self} not allowed" unless @enable_close
      @open = false
    end

    def sym(func, prototype = "0")
      raise "Closed handle" unless @open
      address = @lib.find_function(func)
      Symbol.new(address, prototype, func) if address && !address.null?
    end

    def [](func, ty = nil)
      sym(func, ty || "0")
    end

    def enable_close
      @enable_close = true
    end

    def disable_close
      @enable_close = false
    end
  end

  def self.find_return_type(type)
    # Restrict types to the known-supported ones
    raise "Unsupported type '#{type}" unless type =~ /[CHILFDPS]/
    DL.find_type(type)
  end

  def self.find_param_type(type)
    # Restrict types to the known-supported ones
    raise "Unsupported type '#{type}" unless type =~ /[CHILFDPS]/
    DL.find_type(types)
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

      @invoker = FFI::Invoker.new(address, arg_types, rt, "default")
      
      if rt == FFI::NativeType::POINTER
        def self.call(*args)
          [ PtrData.new(@invoker.call(*args)), args ]
        end
      end
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

  class PtrData
    def initialize(addr, size = nil, sym = nil)
      @ptr = addr
    end

    def self.malloc(size, free = nil)
      self.new(FFI::MemoryPointer.new(size))
    end

    def null?
      @ptr.null?
    end

    def to_ptr
      @ptr
    end

    def struct!(type, *members)
      builder = FFI::StructLayoutBuilder.new
      i = 0
      members.each do |name|
        t = type[i].chr
        i += 1
        if i < type.length && type[i] =~ /[0123456789]/
          raise DLTypeError.new("array fields not supported in struct")
        end
        if t =~ /[CHILFDPS]/
          builder.add_field(name, DL.find_type(t))
        else
          raise DLTypeError.new("Unsupported type '#{t}")
        end
      end
      @layout = builder.build
      self
    end

    def [](name)
      @layout.get(@ptr, name)
    end
    
    def []=(name, value)
      @layout.put(@ptr, name, value)
    end

    def size
      @layout ? @layout.size : @ptr.total
    end
  end

  def self.dlopen(libname)
    Handle.new(libname)
  end

  def self.malloc(size, free = nil)
    PtrData.malloc(size, free)
  end
end
