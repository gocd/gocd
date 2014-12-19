module Sprockets
  module Paths
    # Returns `Environment` root.
    #
    # All relative paths are expanded with root as its base. To be
    # useful set this to your applications root directory. (`Rails.root`)
    def root
      @trail.root.dup
    end

    # Returns an `Array` of path `String`s.
    #
    # These paths will be used for asset logical path lookups.
    #
    # Note that a copy of the `Array` is returned so mutating will
    # have no affect on the environment. See `append_path`,
    # `prepend_path`, and `clear_paths`.
    def paths
      @trail.paths.dup
    end

    # Prepend a `path` to the `paths` list.
    #
    # Paths at the end of the `Array` have the least priority.
    def prepend_path(path)
      @trail.prepend_path(path)
    end

    # Append a `path` to the `paths` list.
    #
    # Paths at the beginning of the `Array` have a higher priority.
    def append_path(path)
      @trail.append_path(path)
    end

    # Clear all paths and start fresh.
    #
    # There is no mechanism for reordering paths, so its best to
    # completely wipe the paths list and reappend them in the order
    # you want.
    def clear_paths
      @trail.paths.dup.each { |path| @trail.remove_path(path) }
    end

    # Returns an `Array` of extensions.
    #
    # These extensions maybe omitted from logical path searches.
    #
    #     # => [".js", ".css", ".coffee", ".sass", ...]
    #
    def extensions
      @trail.extensions.dup
    end

    protected
      attr_reader :trail
  end
end
