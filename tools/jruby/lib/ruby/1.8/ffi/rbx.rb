
#
# Added for backwards compat until all rubinius code is converted to use 'extend FFI::Library'
#
class Module
  include FFI::Library
  def set_ffi_lib(lib)
    ffi_lib lib
  end
end

# Define MemoryPointer globally for rubinius FFI backward compatibility
MemoryPointer = FFI::MemoryPointer
