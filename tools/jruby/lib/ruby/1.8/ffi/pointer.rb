module FFI
  class Pointer
    # Write +obj+ as a C int at the memory pointed to.
      def write_int(obj)
        put_int32(0, obj)
      end

      # Read a C int from the memory pointed to.
      def read_int
        get_int32(0)
      end

      # Write +obj+ as a C long at the memory pointed to.
      def write_long(obj)
        put_long(0, obj)
      end

      # Read a C long from the memory pointed to.
      def read_long
        get_long(0)
      end

      def write_string(str, len=nil)
        len = str.size unless len
        # Write the string data without NUL termination
        put_bytes(0, str)
      end
      def read_array_of_type(type, reader, length)
        ary = []
        size = FFI.type_size(type)
        tmp = self
        (length - 1).times {
          ary << tmp.send(reader)
          tmp += size
        }
        ary << tmp.send(reader)
        ary
      end

      def write_array_of_type(type, writer, ary)
        size = FFI.type_size(type)
        tmp = self
        (ary.length - 1).times do |i|
          tmp.send(writer, ary[i])
          tmp += size
        end
        tmp.send(writer, ary.last)
        self
      end
      def read_array_of_int(length)
        get_array_of_int32(0, length)
      end

      def write_array_of_int(ary)
        put_array_of_int32(0, ary)
      end

      def read_array_of_long(length)
        get_array_of_long(0, length)
      end

      def write_array_of_long(ary)
        put_array_of_long(0, ary)
      end
      def read_pointer
        get_pointer(0)
      end
      def write_pointer(ptr)
        put_pointer(0, ptr)
      end
      def read_array_of_pointer(length)
        get_array_of_pointer(0, length)
      end
      def write_array_of_pointer(ary)
        put_array_of_pointer(0, ary)
      end
  end
end
