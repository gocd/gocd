module CacheStoreForTest
  unless $has_loaded_one_time_enhancements
    GoCacheStore.class_eval do
      def write_with_recording(name, value, options = nil)
        writes[key(name, options)] = value
        write_without_recording(name, value, options)
      end

      alias_method_chain :write, :recording

      def read_with_recording(name, options = nil)
        value = read_without_recording(name, options)
        reads[key(name, options)] = value
      end

      alias_method_chain :read, :recording

      def clear_with_recording
        clear_without_recording
        writes.clear
        reads.clear
      end

      alias_method_chain :clear, :recording

      def writes
        @writes ||= {}
      end

      def reads
        @reads ||= {}
      end
    end

    $has_loaded_one_time_enhancements = true
  end
end