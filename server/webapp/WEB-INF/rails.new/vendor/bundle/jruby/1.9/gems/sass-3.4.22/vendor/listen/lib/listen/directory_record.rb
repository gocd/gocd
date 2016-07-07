require 'set'
require 'find'
require 'digest/sha1'

module Listen

  # The directory record stores information about
  # a directory and keeps track of changes to
  # the structure of its childs.
  #
  class DirectoryRecord
    attr_reader :directory, :paths, :sha1_checksums

    # The default list of directories that get ignored by the listener.
    DEFAULT_IGNORED_DIRECTORIES = %w[.rbx .bundle .git .svn bundle log tmp vendor]

    # The default list of files that get ignored by the listener.
    DEFAULT_IGNORED_EXTENSIONS  = %w[.DS_Store]

    # Defines the used precision based on the type of mtime returned by the
    # system (whether its in milliseconds or just seconds)
    #
    begin
      HIGH_PRECISION_SUPPORTED = File.mtime(__FILE__).to_f.to_s[-2..-1] != '.0'
    rescue
      HIGH_PRECISION_SUPPORTED = false
    end

    # Data structure used to save meta data about a path
    #
    MetaData = Struct.new(:type, :mtime)

    # Class methods
    #
    class << self

      # Creates the ignoring patterns from the default ignored
      # directories and extensions. It memoizes the generated patterns
      # to avoid unnecessary computation.
      #
      def generate_default_ignoring_patterns
        @@default_ignoring_patterns ||= Array.new.tap do |default_patterns|
          # Add directories
          ignored_directories = DEFAULT_IGNORED_DIRECTORIES.map { |d| Regexp.escape(d) }
          default_patterns << %r{^(?:#{ignored_directories.join('|')})/}

          # Add extensions
          ignored_extensions = DEFAULT_IGNORED_EXTENSIONS.map { |e| Regexp.escape(e) }
          default_patterns << %r{(?:#{ignored_extensions.join('|')})$}
        end
      end
    end

    # Initializes a directory record.
    #
    # @option [String] directory the directory to keep track of
    #
    def initialize(directory)
      raise ArgumentError, "The path '#{directory}' is not a directory!" unless File.directory?(directory)

      @directory, @sha1_checksums = File.expand_path(directory), Hash.new
      @ignoring_patterns, @filtering_patterns = Set.new, Set.new

      @ignoring_patterns.merge(DirectoryRecord.generate_default_ignoring_patterns)
    end

    # Returns the ignoring patterns in the record to know
    # which paths should be ignored.
    #
    # @return [Array<Regexp>] the ignoring patterns
    #
    def ignoring_patterns
      @ignoring_patterns.to_a
    end

    # Returns the filtering patterns in the record to know
    # which paths should be stored.
    #
    # @return [Array<Regexp>] the filtering patterns
    #
    def filtering_patterns
      @filtering_patterns.to_a
    end

    # Adds ignoring patterns to the record.
    #
    # @example Ignore some paths
    #   ignore %r{^ignored/path/}, /man/
    #
    # @param [Regexp] regexps a list of patterns for ignoring paths
    #
    def ignore(*regexps)
      @ignoring_patterns.merge(regexps).reject! { |r| r.nil? }
    end

    # Replaces ignoring patterns in the record.
    #
    # @example Ignore only these paths
    #   ignore! %r{^ignored/path/}, /man/
    #
    # @param [Regexp] regexps a list of patterns for ignoring paths
    #
    def ignore!(*regexps)
      @ignoring_patterns.replace(regexps).reject! { |r| r.nil? }
    end

    # Adds filtering patterns to the record.
    #
    # @example Filter some files
    #   filter /\.txt$/, /.*\.zip/
    #
    # @param [Regexp] regexps a list of patterns for filtering files
    #
    def filter(*regexps)
      @filtering_patterns.merge(regexps).reject! { |r| r.nil? }
    end

    # Replaces filtering patterns in the record.
    #
    # @example Filter only these files
    #   filter! /\.txt$/, /.*\.zip/
    #
    # @param [Regexp] regexps a list of patterns for filtering files
    #
    def filter!(*regexps)
      @filtering_patterns.replace(regexps).reject! { |r| r.nil? }
    end

    # Returns whether a path should be ignored or not.
    #
    # @param [String] path the path to test
    #
    # @return [Boolean]
    #
    def ignored?(path)
      path = relative_to_base(path)
      @ignoring_patterns.any? { |pattern| pattern =~ path }
    end

    # Returns whether a path should be filtered or not.
    #
    # @param [String] path the path to test
    #
    # @return [Boolean]
    #
    def filtered?(path)
      # When no filtering patterns are set, ALL files are stored.
      return true if @filtering_patterns.empty?

      path = relative_to_base(path)
      @filtering_patterns.any? { |pattern| pattern =~ path }
    end

    # Finds the paths that should be stored and adds them
    # to the paths' hash.
    #
    def build
      @paths = Hash.new { |h, k| h[k] = Hash.new }
      important_paths { |path| insert_path(path) }
    end

    # Detects changes in the passed directories, updates
    # the record with the new changes and returns the changes.
    #
    # @param [Array] directories the list of directories to scan for changes
    # @param [Hash] options
    # @option options [Boolean] recursive scan all sub-directories recursively
    # @option options [Boolean] relative_paths whether or not to use relative paths for changes
    #
    # @return [Hash<Array>] the changes
    #
    def fetch_changes(directories, options = {})
      @changes    = { :modified => [], :added => [], :removed => [] }
      directories = directories.sort_by { |el| el.length }.reverse # diff sub-dir first

      directories.each do |directory|
        next unless directory[@directory] # Path is or inside directory

        detect_modifications_and_removals(directory, options)
        detect_additions(directory, options)
      end

      @changes
    end

    # Converts an absolute path to a path that's relative to the base directory.
    #
    # @param [String] path the path to convert
    #
    # @return [String] the relative path
    #
    def relative_to_base(path)
      path = path.dup
      regexp = "\\A#{Regexp.quote directory}(#{File::SEPARATOR}|\\z)"
      if path.respond_to?(:force_encoding)
        path.force_encoding("BINARY")
        regexp.force_encoding("BINARY")
      end
      if path.sub!(Regexp.new(regexp), '')
        path
      end
    end

    private

    # Detects modifications and removals recursively in a directory.
    #
    # @note Modifications detection begins by checking the modification time (mtime)
    #   of files and then by checking content changes (using SHA1-checksum)
    #   when the mtime of files is not changed.
    #
    # @param [String] directory the path to analyze
    # @param [Hash] options
    # @option options [Boolean] recursive scan all sub-directories recursively
    # @option options [Boolean] relative_paths whether or not to use relative paths for changes
    #
    def detect_modifications_and_removals(directory, options = {})
      paths[directory].each do |basename, meta_data|
        path = File.join(directory, basename)
        case meta_data.type
        when 'Dir'
          detect_modification_or_removal_for_dir(path, options)
        when 'File'
          detect_modification_or_removal_for_file(path, meta_data, options)
        end
      end
    end

    def detect_modification_or_removal_for_dir(path, options)

      # Directory still exists
      if File.directory?(path)
        detect_modifications_and_removals(path, options) if options[:recursive]

      # Directory has been removed
      else
        detect_modifications_and_removals(path, options)
        @paths[File.dirname(path)].delete(File.basename(path))
        @paths.delete("#{File.dirname(path)}/#{File.basename(path)}")
      end
    end

    def detect_modification_or_removal_for_file(path, meta_data, options)
      # File still exists
      if File.exist?(path)
        detect_modification(path, meta_data, options)

      # File has been removed
      else
        removal_detected(path, meta_data, options)
      end
    end

    def detect_modification(path, meta_data, options)
      new_mtime = mtime_of(path)

      # First check if we are in the same second (to update checksums)
      # before checking the time difference
      if (meta_data.mtime.to_i == new_mtime.to_i && content_modified?(path)) || meta_data.mtime < new_mtime
        modification_detected(path, meta_data, new_mtime, options)
      end
    end

    def modification_detected(path, meta_data, new_mtime, options)
      # Update the sha1 checksum of the file
      update_sha1_checksum(path)

      # Update the meta data of the file
      meta_data.mtime = new_mtime
      @paths[File.dirname(path)][File.basename(path)] = meta_data

      @changes[:modified] << (options[:relative_paths] ? relative_to_base(path) : path)
    end

    def removal_detected(path, meta_data, options)
      @paths[File.dirname(path)].delete(File.basename(path))
      @sha1_checksums.delete(path)
      @changes[:removed] << (options[:relative_paths] ? relative_to_base(path) : path)
    end

    # Detects additions in a directory.
    #
    # @param [String] directory the path to analyze
    # @param [Hash] options
    # @option options [Boolean] recursive scan all sub-directories recursively
    # @option options [Boolean] relative_paths whether or not to use relative paths for changes
    #
    def detect_additions(directory, options = {})
      # Don't process removed directories
      return unless File.exist?(directory)

      Find.find(directory) do |path|
        next if path == @directory

        if File.directory?(path)
          # Add a trailing slash to directories when checking if a directory is
          # ignored to optimize finding them as Find.find doesn't.
          if ignored?(path + File::SEPARATOR) || (directory != path && (!options[:recursive] && existing_path?(path)))
            Find.prune # Don't look any further into this directory.
          else
            insert_path(path)
          end
        elsif !ignored?(path) && filtered?(path) && !existing_path?(path)
          if File.file?(path)
            @changes[:added] << (options[:relative_paths] ? relative_to_base(path) : path)
            insert_path(path)
          end
        end
      end
    end

    # Returns whether or not a file's content has been modified by
    # comparing the SHA1-checksum to a stored one.
    # Ensure that the SHA1-checksum is inserted to the sha1_checksums
    # array for later comparaison if false.
    #
    # @param [String] path the file path
    #
    def content_modified?(path)
      return false unless File.ftype(path) == 'file'
      @sha1_checksum = sha1_checksum(path)
      if sha1_checksums[path] == @sha1_checksum || !sha1_checksums.key?(path)
        update_sha1_checksum(path)
        false
      else
        true
      end
    end

    # Inserts a SHA1-checksum path in @SHA1-checksums hash.
    #
    # @param [String] path the SHA1-checksum path to insert in @sha1_checksums.
    #
    def update_sha1_checksum(path)
      if @sha1_checksum ||= sha1_checksum(path)
        @sha1_checksums[path] = @sha1_checksum
        @sha1_checksum = nil
      end
    end

    # Returns the SHA1-checksum for the file path.
    #
    # @param [String] path the file path
    #
    def sha1_checksum(path)
      Digest::SHA1.file(path).to_s
    rescue
      nil
    end

    # Traverses the base directory looking for paths that should
    # be stored; thus paths that are filtered or not ignored.
    #
    # @yield [path] an important path
    #
    def important_paths
      Find.find(directory) do |path|
        next if path == directory

        if File.directory?(path)
          # Add a trailing slash to directories when checking if a directory is
          # ignored to optimize finding them as Find.find doesn't.
          if ignored?(path + File::SEPARATOR)
            Find.prune # Don't look any further into this directory.
          else
            yield(path)
          end
        elsif !ignored?(path) && filtered?(path)
          yield(path)
        end
      end
    end

    # Inserts a path with its type (Dir or File) in paths hash.
    #
    # @param [String] path the path to insert in @paths.
    #
    def insert_path(path)
      meta_data = MetaData.new
      meta_data.type = File.directory?(path) ? 'Dir' : 'File'
      meta_data.mtime = mtime_of(path) unless meta_data.type == 'Dir' # mtimes of dirs are not used yet
      @paths[File.dirname(path)][File.basename(path)] = meta_data
    rescue Errno::ENOENT
    end

    # Returns whether or not a path exists in the paths hash.
    #
    # @param [String] path the path to check
    #
    # @return [Boolean]
    #
    def existing_path?(path)
      paths[File.dirname(path)][File.basename(path)] != nil
    end

    # Returns the modification time of a file based on the precision defined by the system
    #
    # @param [String] file the file for which the mtime must be returned
    #
    # @return [Fixnum, Float] the mtime of the file
    #
    def mtime_of(file)
      File.lstat(file).mtime.send(HIGH_PRECISION_SUPPORTED ? :to_f : :to_i)
    end
  end
end
