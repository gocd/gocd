require "active_support/core_ext/marshal"
require "active_support/core_ext/file/atomic"
require "active_support/core_ext/string/conversions"
require "uri/common"

module ActiveSupport
  module Cache
    # A cache store implementation which stores everything on the filesystem.
    #
    # FileStore implements the Strategy::LocalCache strategy which implements
    # an in-memory cache inside of a block.
    class FileStore < Store
      prepend Strategy::LocalCache
      attr_reader :cache_path

      DIR_FORMATTER = "%03X"
      FILENAME_MAX_SIZE = 228 # max filename size on file system is 255, minus room for timestamp and random characters appended by Tempfile (used by atomic write)
      FILEPATH_MAX_SIZE = 900 # max is 1024, plus some room
      EXCLUDED_DIRS = [".", ".."].freeze
      GITKEEP_FILES = [".gitkeep", ".keep"].freeze

      def initialize(cache_path, options = nil)
        super(options)
        @cache_path = cache_path.to_s
      end

      # Deletes all items from the cache. In this case it deletes all the entries in the specified
      # file store directory except for .keep or .gitkeep. Be careful which directory is specified in your
      # config file when using +FileStore+ because everything in that directory will be deleted.
      def clear(options = nil)
        root_dirs = exclude_from(cache_path, EXCLUDED_DIRS + GITKEEP_FILES)
        FileUtils.rm_r(root_dirs.collect { |f| File.join(cache_path, f) })
      rescue Errno::ENOENT
      end

      # Preemptively iterates through all stored keys and removes the ones which have expired.
      def cleanup(options = nil)
        options = merged_options(options)
        search_dir(cache_path) do |fname|
          key = file_path_key(fname)
          entry = read_entry(key, options)
          delete_entry(key, options) if entry && entry.expired?
        end
      end

      # Increments an already existing integer value that is stored in the cache.
      # If the key is not found nothing is done.
      def increment(name, amount = 1, options = nil)
        modify_value(name, amount, options)
      end

      # Decrements an already existing integer value that is stored in the cache.
      # If the key is not found nothing is done.
      def decrement(name, amount = 1, options = nil)
        modify_value(name, -amount, options)
      end

      def delete_matched(matcher, options = nil)
        options = merged_options(options)
        instrument(:delete_matched, matcher.inspect) do
          matcher = key_matcher(matcher, options)
          search_dir(cache_path) do |path|
            key = file_path_key(path)
            delete_entry(path, options) if key.match(matcher)
          end
        end
      end

      private

        def read_entry(key, options)
          if File.exist?(key)
            File.open(key) { |f| Marshal.load(f) }
          end
        rescue => e
          logger.error("FileStoreError (#{e}): #{e.message}") if logger
          nil
        end

        def write_entry(key, entry, options)
          return false if options[:unless_exist] && File.exist?(key)
          ensure_cache_path(File.dirname(key))
          File.atomic_write(key, cache_path) { |f| Marshal.dump(entry, f) }
          true
        end

        def delete_entry(key, options)
          if File.exist?(key)
            begin
              File.delete(key)
              delete_empty_directories(File.dirname(key))
              true
            rescue => e
              # Just in case the error was caused by another process deleting the file first.
              raise e if File.exist?(key)
              false
            end
          end
        end

        # Lock a file for a block so only one process can modify it at a time.
        def lock_file(file_name, &block)
          if File.exist?(file_name)
            File.open(file_name, "r+") do |f|
              begin
                f.flock File::LOCK_EX
                yield
              ensure
                f.flock File::LOCK_UN
              end
            end
          else
            yield
          end
        end

        # Translate a key into a file path.
        def normalize_key(key, options)
          key = super
          fname = URI.encode_www_form_component(key)

          if fname.size > FILEPATH_MAX_SIZE
            fname = Digest::MD5.hexdigest(key)
          end

          hash = Zlib.adler32(fname)
          hash, dir_1 = hash.divmod(0x1000)
          dir_2 = hash.modulo(0x1000)
          fname_paths = []

          # Make sure file name doesn't exceed file system limits.
          begin
            fname_paths << fname[0, FILENAME_MAX_SIZE]
            fname = fname[FILENAME_MAX_SIZE..-1]
          end until fname.blank?

          File.join(cache_path, DIR_FORMATTER % dir_1, DIR_FORMATTER % dir_2, *fname_paths)
        end

        # Translate a file path into a key.
        def file_path_key(path)
          fname = path[cache_path.to_s.size..-1].split(File::SEPARATOR, 4).last
          URI.decode_www_form_component(fname, Encoding::UTF_8)
        end

        # Delete empty directories in the cache.
        def delete_empty_directories(dir)
          return if File.realpath(dir) == File.realpath(cache_path)
          if exclude_from(dir, EXCLUDED_DIRS).empty?
            Dir.delete(dir) rescue nil
            delete_empty_directories(File.dirname(dir))
          end
        end

        # Make sure a file path's directories exist.
        def ensure_cache_path(path)
          FileUtils.makedirs(path) unless File.exist?(path)
        end

        def search_dir(dir, &callback)
          return if !File.exist?(dir)
          Dir.foreach(dir) do |d|
            next if EXCLUDED_DIRS.include?(d)
            name = File.join(dir, d)
            if File.directory?(name)
              search_dir(name, &callback)
            else
              callback.call name
            end
          end
        end

        # Modifies the amount of an already existing integer value that is stored in the cache.
        # If the key is not found nothing is done.
        def modify_value(name, amount, options)
          file_name = normalize_key(name, options)

          lock_file(file_name) do
            options = merged_options(options)

            if num = read(name, options)
              num = num.to_i + amount
              write(name, num, options)
              num
            end
          end
        end

        # Exclude entries from source directory
        def exclude_from(source, excludes)
          Dir.entries(source).reject { |f| excludes.include?(f) }
        end
    end
  end
end
