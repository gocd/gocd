#
# Copyright (C) 2008-2010 Wayne Meissner
# Copyright (C) 2008 Luc Heinrich <luc@honk-honk.com>
#
# Version: EPL 1.0/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Common Public
# License Version 1.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.eclipse.org/legal/cpl-v10.html
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# Alternatively, the contents of this file may be used under the terms of
# either of the GNU General Public License Version 2 or later (the "GPL"),
# or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the EPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the EPL, the GPL or the LGPL.
#
#
# This file contains code that was originally under the following license:
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of the Evan Phoenix nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
module FFI
  CURRENT_PROCESS = USE_THIS_PROCESS_AS_LIBRARY = Object.new

  module Library
    CURRENT_PROCESS = FFI::CURRENT_PROCESS
    LIBC = FFI::Platform::LIBC

    def ffi_lib(*names)
      raise LoadError.new("library names list must not be empty") if names.empty?
      lib_flags = defined?(@ffi_lib_flags) ? @ffi_lib_flags : FFI::DynamicLibrary::RTLD_LAZY | FFI::DynamicLibrary::RTLD_LOCAL
      ffi_libs = names.map do |name|
        if name == FFI::CURRENT_PROCESS
          FFI::DynamicLibrary.open(nil, FFI::DynamicLibrary::RTLD_LAZY | FFI::DynamicLibrary::RTLD_LOCAL)
        else
          libnames = (name.is_a?(::Array) ? name : [ name ]).map { |n| [ n, FFI.map_library_name(n) ].uniq }.flatten.compact
          lib = nil
          errors = {}

          libnames.each do |libname|
            begin
              lib = FFI::DynamicLibrary.open(libname, lib_flags)
              break if lib
            rescue Exception => ex
              ldscript = false
              if ex.message =~ /(([^ \t()])+\.so([^ \t:()])*):([ \t])*invalid ELF header/
                if File.read($1) =~ /GROUP *\( *([^ \)]+) *\)/
                  libname = $1
                  ldscript = true
                end
              end

              if ldscript
                retry
              else
                errors[libname] = ex
              end
            end
          end

          if lib.nil?
            raise LoadError.new(errors.values.join('. '))
          end

          # return the found lib
          lib
        end
      end

      @ffi_libs = ffi_libs
    end


    def ffi_convention(convention = nil)
      @ffi_convention ||= :default
      @ffi_convention = convention if convention
      @ffi_convention
    end

    def ffi_libraries
      raise LoadError.new("no library specified") if !defined?(@ffi_libs) || @ffi_libs.empty?
      @ffi_libs
          end

    FlagsMap = {
      :global => DynamicLibrary::RTLD_GLOBAL,
      :local => DynamicLibrary::RTLD_LOCAL,
      :lazy => DynamicLibrary::RTLD_LAZY,
      :now => DynamicLibrary::RTLD_NOW
    }

    def ffi_lib_flags(*flags)
      lib_flags = flags.inject(0) { |result, f| result | FlagsMap[f] }
      if (lib_flags & (DynamicLibrary::RTLD_LAZY | DynamicLibrary::RTLD_NOW)) == 0
        lib_flags |= DynamicLibrary::RTLD_LAZY
      end

      if (lib_flags & (DynamicLibrary::RTLD_GLOBAL | DynamicLibrary::RTLD_LOCAL) == 0)
        lib_flags |= DynamicLibrary::RTLD_LOCAL
      end

      @ffi_lib_flags = lib_flags
    end

    
    ##
    # Attach C function +name+ to this module.
    #
    # If you want to provide an alternate name for the module function, supply
    # it after the +name+, otherwise the C function name will be used.#
    #
    # After the +name+, the C function argument types are provided as an Array.
    #
    # The C function return type is provided last.

    def attach_function(mname, a2, a3, a4=nil, a5 = nil)
      cname, arg_types, ret_type, opts = (a4 && (a2.is_a?(String) || a2.is_a?(Symbol))) ? [ a2, a3, a4, a5 ] : [ mname.to_s, a2, a3, a4 ]


      # Convert :foo to the native type
      arg_types = arg_types.map { |e| find_type(e) }
      options = Hash.new
      options[:convention] = defined?(@ffi_convention) ? @ffi_convention : :default
      options[:type_map] = defined?(@ffi_typedefs) ? @ffi_typedefs : nil
      options[:enums] = defined?(@ffi_enum_map) ? @ffi_enum_map : nil
      options.merge!(opts) if opts.is_a?(Hash)

      # Try to locate the function in any of the libraries
      invokers = []
      load_error = nil
      ffi_libraries.each do |lib|
        if invokers.empty?
          begin
            function = lib.find_function(cname.to_s)
            raise FFI::NotFoundError.new(cname.to_s, *ffi_libraries.map { |clib| clib.name }) unless function
            invokers << if arg_types.length > 0 && arg_types[arg_types.length - 1] == FFI::NativeType::VARARGS
              FFI::VariadicInvoker.new(arg_types, find_type(ret_type), function, options)
            else
              FFI::Function.new(find_type(ret_type), arg_types, function, options)
            end
   
          rescue LoadError => ex
            load_error = ex

          end
        end
      end
      invoker = invokers.compact.shift
      raise load_error if load_error && invoker.nil?

      invoker.attach(self, mname.to_s)
      invoker # Return a version that can be called via #call
    end

    def attach_variable(mname, a1, a2 = nil)
      cname, type = a2 ? [ a1, a2 ] : [ mname.to_s, a1 ]
      address = nil
      ffi_libraries.each do |lib|
        begin
          address = lib.find_variable(cname.to_s)
          break unless address.nil?
        rescue LoadError
        end
      end

      raise FFI::NotFoundError.new(cname, *ffi_libraries) if address.nil? || address.null?
      
      if type.is_a?(Class) && type < FFI::Struct
        # If it is a global struct, just attach directly to the pointer
        s = type.new(address)
        self.module_eval <<-code, __FILE__, __LINE__
          @@ffi_gvar_#{mname} = s
          def self.#{mname}
            @@ffi_gvar_#{mname}
          end
        code

      else
        sc = Class.new(FFI::Struct)
        sc.layout :gvar, find_type(type)
        s = sc.new(address)
        #
        # Attach to this module as mname/mname=
        #
        self.module_eval <<-code, __FILE__, __LINE__
          @@ffi_gvar_#{mname} = s
          def self.#{mname}
            @@ffi_gvar_#{mname}[:gvar]
          end
          def self.#{mname}=(value)
            @@ffi_gvar_#{mname}[:gvar] = value
          end
        code

      end
      
      address
    end

    def callback(*args)
      raise ArgumentError, "wrong number of arguments" if args.length < 2 || args.length > 3
      name, params, ret = if args.length == 3
        args
      else
        [ nil, args[0], args[1] ]
      end

      native_params = params.map { |e| find_type(e) }
      raise ArgumentError, "callbacks cannot have variadic parameters" if native_params.include?(FFI::Type::VARARGS)
      options = Hash.new
      options[:convention] = defined?(@ffi_convention) ? @ffi_convention : :default
      options[:enums] = defined?(@ffi_enum_map) ? @ffi_enum_map : nil

      cb = FFI::CallbackInfo.new(find_type(ret), native_params, options)

      # Add to the symbol -> type map (unless there was no name)
      unless name.nil?
        __cb_map[name] = cb

        # Also put in the type map, so it can be used for typedefs
        __type_map[name] = cb
      end

      cb
    end

    def __type_map
      defined?(@ffi_typedefs) ? @ffi_typedefs : (@ffi_typedefs = Hash.new)
    end

    def __cb_map
      defined?(@ffi_callbacks) ? @ffi_callbacks: (@ffi_callbacks = Hash.new)
    end

    def typedef(old, add, info=nil)
      @ffi_typedefs = Hash.new unless defined?(@ffi_typedefs)

      @ffi_typedefs[add] = if old.kind_of?(Type)
        old

      elsif @ffi_typedefs.has_key?(old)
        @ffi_typedefs[old]

      elsif old.is_a?(DataConverter)
        Type::Mapped.new(old)

      elsif old == :enum
        if add.kind_of?(Array)
          self.enum(add)
        else
          self.enum(info, add)
        end

      else
        FFI.find_type(old)
      end
    end

    def enum(*args)
      #
      # enum can be called as:
      # enum :zero, :one, :two  # unnamed enum
      # enum [ :zero, :one, :two ] # equivalent to above
      # enum :foo, [ :zero, :one, :two ] create an enum named :foo
      #
      name, values = if args[0].kind_of?(Symbol) && args[1].kind_of?(Array)
        [ args[0], args[1] ]
      elsif args[0].kind_of?(Array)
        [ nil, args[0] ]
      else
        [ nil, args ]
      end

      e = Enum.new(values, name)
      if name
        @ffi_tagged_enums = Hash.new unless defined?(@ffi_tagged_enums)
        @ffi_tagged_enums[name] = e
        # If called as enum :foo, [ :zero, :one, :two ], add a typedef alias
        typedef(e, name)
      end

      @ffi_enum_map = Hash.new unless defined?(@ffi_enum_map)

      # append all the enum values to a global :name => value map
      @ffi_enum_map.merge!(e.symbol_map)

      e
    end

    def enum_type(name)
      @ffi_tagged_enums[name] if defined?(@ffi_tagged_enums)
    end

    def enum_value(symbol)
      @ffi_enum_map[symbol]
    end

    def find_type(t)
      if t.kind_of?(FFI::Type)
        t
      
      elsif defined?(@ffi_typedefs) && @ffi_typedefs.has_key?(t)
        @ffi_typedefs[t]

      elsif defined?(@ffi_callbacks) && @ffi_callbacks.has_key?(t)
        @ffi_callbacks[t]

      elsif t.is_a?(DataConverter)
        # Add a typedef so next time the converter is used, it hits the cache
        typedef Type::Mapped.new(t), t

      elsif t.is_a?(Class) && t < FFI::Struct
        FFI::NativeType::POINTER

      end || FFI.find_type(t)
    end
  end
end
