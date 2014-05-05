module Bundler
  class Source
    class Git < Path

      class GitNotInstalledError < GitError
        def initialize
          msg =  "You need to install git to be able to use gems from git repositories. "
          msg << "For help installing git, please refer to GitHub's tutorial at https://help.github.com/articles/set-up-git"
          super msg
        end
      end

      class GitNotAllowedError < GitError
        def initialize(command)
          msg =  "Bundler is trying to run a `git #{command}` at runtime. You probably need to run `bundle install`. However, "
          msg << "this error message could probably be more useful. Please submit a ticket at http://github.com/bundler/bundler/issues "
          msg << "with steps to reproduce as well as the following\n\nCALLER: #{caller.join("\n")}"
          super msg
        end
      end

      class GitCommandError < GitError
        def initialize(command, path = nil)
          msg =  "Git error: command `git #{command}` in directory #{Dir.pwd} has failed."
          msg << "\nIf this error persists you could try removing the cache directory '#{path}'" if path && path.exist?
          super msg
        end
      end

      # The GitProxy is responsible to interact with git repositories.
      # All actions required by the Git source is encapsulated in this
      # object.
      class GitProxy
        attr_accessor :path, :uri, :ref
        attr_writer :revision

        def initialize(path, uri, ref, revision = nil, git = nil)
          @path     = path
          @uri      = uri
          @ref      = ref
          @revision = revision
          @git      = git
          raise GitNotInstalledError.new if allow? && !Bundler.git_present?
        end

        def revision
          @revision ||= allowed_in_path { git("rev-parse #{ref}").strip }
        end

        def branch
          @branch ||= allowed_in_path do
            git("branch") =~ /^\* (.*)$/ && $1.strip
          end
        end

        def contains?(commit)
          allowed_in_path do
            result = git_null("branch --contains #{commit}")
            $? == 0 && result =~ /^\* (.*)$/
          end
        end

        def checkout
          if path.exist?
            return if has_revision_cached?
            Bundler.ui.confirm "Updating #{uri}"
            in_path do
              git_retry %|fetch --force --quiet --tags #{uri_escaped} "refs/heads/*:refs/heads/*"|
            end
          else
            Bundler.ui.info "Fetching #{uri}"
            FileUtils.mkdir_p(path.dirname)
            git_retry %|clone #{uri_escaped} "#{path}" --bare --no-hardlinks --quiet|
          end
        end

        def copy_to(destination, submodules=false)
          unless File.exist?(destination.join(".git"))
            FileUtils.mkdir_p(destination.dirname)
            FileUtils.rm_rf(destination)
            git_retry %|clone --no-checkout --quiet "#{path}" "#{destination}"|
            File.chmod((0777 & ~File.umask), destination)
          end

          SharedHelpers.chdir(destination) do
            git_retry %|fetch --force --quiet --tags "#{path}"|
            git "reset --hard #{@revision}"

            if submodules
              git_retry "submodule update --init --recursive"
            end
          end
        end

      private

        # TODO: Do not rely on /dev/null.
        # Given that open3 is not cross platform until Ruby 1.9.3,
        # the best solution is to pipe to /dev/null if it exists.
        # If it doesn't, everything will work fine, but the user
        # will get the $stderr messages as well.
        def git_null(command)
          git("#{command} 2>#{Bundler::NULL}", false)
        end

        def git_retry(command)
          Bundler::Retry.new("git #{command}", GitNotAllowedError).attempts do
            git(command)
          end
        end

        def git(command, check_errors=true)
          raise GitNotAllowedError.new(command) unless allow?
          out = SharedHelpers.with_clean_git_env { %x{git #{command}} }
          raise GitCommandError.new(command, path) if check_errors && !$?.success?
          out
        end

        def has_revision_cached?
          return unless @revision
          in_path { git("cat-file -e #{@revision}") }
          true
        rescue GitError
          false
        end

        # Escape the URI for git commands
        def uri_escaped
          if Bundler::WINDOWS
            # Windows quoting requires double quotes only, with double quotes
            # inside the string escaped by being doubled.
            '"' + uri.gsub('"') {|s| '""'} + '"'
          else
            # Bash requires single quoted strings, with the single quotes escaped
            # by ending the string, escaping the quote, and restarting the string.
            "'" + uri.gsub("'") {|s| "'\\''"} + "'"
          end
        end

        def allow?
          @git ? @git.allow_git_ops? : true
        end

        def in_path(&blk)
          checkout unless path.exist?
          SharedHelpers.chdir(path, &blk)
        end

        def allowed_in_path
          return in_path { yield } if allow?
          raise GitError, "The git source #{uri} is not yet checked out. Please run `bundle install` before trying to start your application"
        end

      end

    end
  end
end
