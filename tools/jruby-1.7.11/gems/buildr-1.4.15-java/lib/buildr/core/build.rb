# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr #:nodoc:

  class Options

    # Runs the build in parallel when true (defaults to false). You can force a parallel build by
    # setting this option directly, or by running the parallel task ahead of the build task.
    #
    # This option only affects recursive tasks. For example:
    #   buildr parallel package
    # will run all package tasks (from the sub-projects) in parallel, but each sub-project's package
    # task runs its child tasks (prepare, compile, resources, etc) in sequence.
    attr_accessor :parallel

  end

  task('parallel') { Buildr.options.parallel = true }


  module Build

    include Extension

    first_time do
      desc 'Build the project'
      Project.local_task('build') { |name| "Building #{name}" }
      desc 'Clean files generated during a build'
      Project.local_task('clean') { |name| "Cleaning #{name}" }

      desc 'The default task is build'
      task 'default'=>'build'
    end

    before_define(:build => [:compile, :test]) do |project|
      project.recursive_task 'build'
      project.recursive_task 'clean'
      project.clean do
        rm_rf project.path_to(:target)
        rm_rf project.path_to(:reports)
      end
    end

    after_define(:build)

    # *Deprecated:* Use +path_to(:target)+ instead.
    def target
      Buildr.application.deprecated 'Use path_to(:target) instead'
      layout.expand(:target)
    end

    # *Deprecated:* Use Layout instead.
    def target=(dir)
      Buildr.application.deprecated 'Use Layout instead'
      layout[:target] = _(dir)
    end

    # *Deprecated:* Use +path_to(:reports)+ instead.
    def reports()
      Buildr.application.deprecated 'Use path_to(:reports) instead'
      layout.expand(:reports)
    end

    # *Deprecated:* Use Layout instead.
    def reports=(dir)
      Buildr.application.deprecated 'Use Layout instead'
      layout[:reports] = _(dir)
    end

    # :call-seq:
    #    build(*prereqs) => task
    #    build { |task| .. } => task
    #
    # Returns the project's build task. With arguments or block, also enhances that task.
    def build(*prereqs, &block)
      task('build').enhance prereqs, &block
    end

    # :call-seq:
    #    clean(*prereqs) => task
    #    clean { |task| .. } => task
    #
    # Returns the project's clean task. With arguments or block, also enhances that task.
    def clean(*prereqs, &block)
      task('clean').enhance prereqs, &block
    end

  end

  module Hg #:nodoc:
    module_function

    # :call-seq:
    #   hg(*args)
    #
    # Executes a Mercurial (hg) command passing through the args and returns the output. 
    # Throws exception if the exit status is not zero. For example:
    #   hg 'commit'
    #   hg 'update', 'default'
    def hg(*args)
      cmd = "hg #{args.shift} #{args.map { |arg| arg.inspect }.join(' ')}"
      output = `#{cmd}`
      fail "Mercurial command \"#{cmd}\" failed with status #{$?.exitstatus}\n#{output}" unless $?.exitstatus == 0
      return output
    end

    # Return a list of uncommitted / untracked files as reported by hg status
    # The codes used to show the status of files are:
    #   M = modified
    #   A = added
    #   R = removed
    #   C = clean
    #   ! = missing (deleted by non-hg command, but still tracked)
    #   ? = not tracked
    #   I = ignored
    #     = origin of the previous file listed as A (added)
    def uncommitted_files
      `hg status`.scan(/^(A|M|R|!|\?) (\S.*)$/).map{ |match| match.last.split.last }
    end

    # Commit the given file with a message. The file should already be added to the Mercurial index.
    def commit(file, message)
      hg 'commit', '-m', message, file
    end

    # Update the remote branch with the local commits
    # This will push the current remote destination and current branch.
    def push
      hg 'push'
    end

    # Return the name of the current local branch or nil if none.
    def current_branch
      hg('branch').to_s.strip
    end

    # Return the aliases (if any) of any remote repositories which the current local branch tracks
    def remote
      hg('paths').scan(/^(?:default|default-push)\s+=\s+(\S.*)/).map{ |match| match.last }
    end
  end

  module Git  #:nodoc:
    module_function

    # :call-seq:
    #   git(*args)
    #
    # Executes a Git command and returns the output. Throws exception if the exit status
    # is not zero. For example:
    #   git 'commit'
    #   git 'remote', 'show', 'origin'
    def git(*args)
      cmd = "git #{args.shift} #{args.map { |arg| arg.inspect }.join(' ')}"
      output = `#{cmd}`
      fail "GIT command \"#{cmd}\" failed with status #{$?.exitstatus}\n#{output}" unless $?.exitstatus == 0
      return output
    end

    # Returns list of uncommited/untracked files as reported by git status.
    def uncommitted_files
      `git status`.scan(/^#(\t|\s{7})(\S.*)$/).map { |match| match.last.split.last }
    end

    # Commit the given file with a message.
    # The file has to be known to Git meaning that it has either to have been already committed in the past
    # or freshly added to the index. Otherwise it will fail.
    def commit(file, message)
      git 'commit', '-m', message, file
    end

    # Update the remote refs using local refs
    #
    # By default, the "remote" destination of the push is the the remote repo linked to the current branch.
    # The default remote branch is the current local branch.
    def push(remote_repo = remote, remote_branch = current_branch)
      git 'push', remote, current_branch
    end

    # Return the name of the remote repository whose branch the current local branch tracks,
    # or nil if none.
    def remote(branch = current_branch)
      remote = git('config', '--get', "branch.#{branch}.remote").to_s.strip
      remote if !remote.empty? && git('remote').include?(remote)
    end

    # Return the name of the current branch
    def current_branch
      git('branch')[/^\* (.*)$/, 1]
    end
  end


  module Svn #:nodoc:
    module_function

    # :call-seq:
    #   svn(*args)
    #
    # Executes a SVN command and returns the output. Throws exception if the exit status
    # is not zero. For example:
    #   svn 'commit'
    def svn(*args)
      output = `svn #{args.shift} #{args.map { |arg| arg.inspect }.join(' ')}`
      fail "SVN command failed with status #{$?.exitstatus}" unless $?.exitstatus == 0
      return output
    end

    def tag(tag_name)
      url = tag_url repo_url, tag_name
      remove url, 'Removing old copy' rescue nil
      copy Dir.pwd, url, "Release #{tag_name}"
    end

    # Status check reveals modified files, but also SVN externals which we can safely ignore.
    def uncommitted_files
      svn('status', '--ignore-externals').split("\n").reject { |line| line =~ /^X\s/ }
    end

    def commit(file, message)
      svn 'commit', '-m', message, file
    end

    # :call-seq:
    #   tag_url(svn_url, version) => tag_url
    #
    # Returns the SVN url for the tag.
    # Can tag from the trunk or from branches.
    # Can handle the two standard repository layouts.
    #   - http://my.repo/foo/trunk => http://my.repo/foo/tags/1.0.0
    #   - http://my.repo/trunk/foo => http://my.repo/tags/foo/1.0.0
    def tag_url(svn_url, tag)
      trunk_or_branches = Regexp.union(%r{^(.*)/trunk(.*)$}, %r{^(.*)/branches(.*)/([^/]*)$})
      match = trunk_or_branches.match(svn_url)
      prefix = match[1] || match[3]
      suffix = match[2] || match[4]
      prefix + '/tags' + suffix + '/' + tag
    end

    # Return the current SVN URL
    def repo_url
      svn('info', '--xml')[/<url>(.*?)<\/url>/, 1].strip
    end

    def copy(dir, url, message)
      svn 'copy', '--parents', dir, url, '-m', message
    end

    def remove(url, message)
      svn 'remove', url, '-m', message
    end

  end


  class Release #:nodoc:

    THIS_VERSION_PATTERN  = /(THIS_VERSION|VERSION_NUMBER)\s*=\s*(["'])(.*)\2/

    class << self

      # Use this to specify a different tag name for tagging the release in source control.
      # You can set the tag name or a proc that will be called with the version number,
      # for example:
      #   Release.tag_name = lambda { |ver| "foo-#{ver}" }
      attr_accessor :tag_name

      # Use this to specify a different commit message to commit the buildfile with the next version in source control.
      # You can set the commit message or a proc that will be called with the next version number,
      # for example:
      #   Release.commit_message = lambda { |ver| "Changed version number to #{ver}" }
      attr_accessor :commit_message

      # Use this to specify the next version number to replace VERSION_NUMBER with in the buildfile.
      # You can set the next version or a proc that will be called with the current version number.
      # For example, with the following buildfile:
      #   THIS_VERSION = "1.0.0-rc1" 
      #   Release.next_version = lambda { |version| 
      #       version[-1] = version[-1].to_i + 1
      #       version
      #   } 
      # 
      # Release.next_version will return "1.0.0-rc2", so at the end of the release, the buildfile will contain VERSION_NUMBER = "1.0.0-rc2" 
      #
      attr_accessor :next_version

      # :call-seq:
      #     add(MyReleaseClass)
      #
      # Add a Release implementation to the list of available Release classes.
      def add(release)
        @list ||= []
        @list |= [release]
      end
      alias :<< :add

      # The list of supported Release implementations
      def list
        @list ||= []
      end

      # Finds and returns the Release instance for this project.
      def find
        unless @release
          klass = list.detect { |impl| impl.applies_to? }
          @release = klass.new if klass
        end
        @release
      end

    end

    # :call-seq:
    #   make()
    #
    # Make a release.
    def make
      @this_version = extract_version
      check
      with_release_candidate_version do |release_candidate_buildfile|
        args = []
        args << 'buildr' << '--buildfile' << release_candidate_buildfile
        args << '--environment' << Buildr.environment unless Buildr.environment.to_s.empty?
        args << 'clean' << 'upload' << 'DEBUG=no'
        sh *args
      end
      tag_release resolve_tag
      update_version_to_next if this_version != resolve_next_version(this_version)
    end

    def check
      if this_version == resolve_next_version(this_version) && this_version.match(/-SNAPSHOT$/)
        fail "The next version can't be equal to the current version #{this_version}.\nUpdate THIS_VERSION/VERSION_NUMBER, specify Release.next_version or use NEXT_VERSION env var" 
      end
    end

    # :call-seq:
    #   extract_version() => this_version
    #
    # Extract the current version number from the buildfile.
    # Raise an error if not found.
    def extract_version
      buildfile = File.read(Buildr.application.buildfile.to_s)
      buildfile.scan(THIS_VERSION_PATTERN)[0][2]
    rescue
      fail 'Looking for THIS_VERSION = "..." in your Buildfile, none found'
    end

    # Use this to specify a different tag name for tagging the release in source control.
    # You can set the tag name or a proc that will be called with the version number,
    # for example:
    #   Release.find.tag_name = lambda { |ver| "foo-#{ver}" }
    # Deprecated: you should use Release.tag_name instead
    def tag_name=(tag_proc)
      Buildr.application.deprecated "Release.find.tag_name is deprecated. You should use Release.tag_name instead"
      Release.tag_name=(tag_proc)
    end

  protected
 
    # the initial value of THIS_VERSION
    attr_accessor :this_version

    # :call-seq:
    #   with_release_candidate_version() { |filename| ... }
    #
    # Yields to block with release candidate buildfile, before committing to use it.
    #
    # We need a Buildfile with upgraded version numbers to run the build, but we don't want the
    # Buildfile modified unless the build succeeds. So this method updates the version number in
    # a separate (Buildfile.next) file, yields to the block with that filename, and if successful
    # copies the new file over the existing one.
    #
    # The release version is the current version without '-SNAPSHOT'.  So:
    #   THIS_VERSION = 1.1.0-SNAPSHOT
    # becomes:
    #   THIS_VERSION = 1.1.0
    # for the release buildfile.
    def with_release_candidate_version
      release_candidate_buildfile = Buildr.application.buildfile.to_s + '.next'
    
      release_candidate_buildfile_contents = change_version { |version|
        version.gsub(/-SNAPSHOT$/, "")
      }
      File.open(release_candidate_buildfile, 'w') { |file| file.write release_candidate_buildfile_contents }
      begin
        yield release_candidate_buildfile
        mv release_candidate_buildfile, Buildr.application.buildfile.to_s
      ensure
        rm release_candidate_buildfile rescue nil
      end
    end

    # :call-seq:
    #   change_version() { |this_version| ... } => buildfile
    #
    # Change version number in the current Buildfile, but without writing a new file (yet).
    # Returns the contents of the Buildfile with the modified version number.
    #
    # This method yields to the block with the current (this) version number and expects
    # the block to return the updated version.
    def change_version
      current_version = extract_version
      new_version = yield(current_version)
      buildfile = File.read(Buildr.application.buildfile.to_s)
      buildfile.gsub(THIS_VERSION_PATTERN) { |ver| ver.sub(/(["']).*\1/, %Q{"#{new_version}"}) }
    end

    # Return the name of the tag to tag the release with.
    def resolve_tag
      version = extract_version
      tag = Release.tag_name || version
      tag = tag.call(version) if Proc === tag
      tag
    end

    # Return the new value of THIS_VERSION based on the version passed.
    #
    # This method receives the existing value of THIS_VERSION
    def resolve_next_version(current_version)
        next_version = Release.next_version
        next_version ||= lambda { |v|
          snapshot = v.match(/-SNAPSHOT$/)
          version = v.gsub(/-SNAPSHOT$/, "").split(/\./)
          if snapshot 
            version[-1] = sprintf("%0#{version[-1].size}d", version[-1].to_i + 1) + '-SNAPSHOT'
          end 
          version.join('.')
        } 
        next_version = ENV['NEXT_VERSION'] if ENV['NEXT_VERSION'] 
        next_version = ENV['next_version'] if ENV['next_version'] 
        next_version = next_version.call(current_version) if Proc === next_version
        next_version
    end

    # Move the version to next and save the updated buildfile
    def update_buildfile
      buildfile = change_version { |version| # THIS_VERSION minus SNAPSHOT 
        resolve_next_version(this_version) # THIS_VERSION
      }
      File.open(Buildr.application.buildfile.to_s, 'w') { |file| file.write buildfile }
    end

    # Return the message to use to commit the buildfile with the next version
    def message
      version = extract_version
      msg = Release.commit_message || "Changed version number to #{version}"
      msg = msg.call(version) if Proc === msg
      msg
    end

    def update_version_to_next
      update_buildfile
    end
  end


  class HgRelease < Release
    class << self
      def applies_to?
        if File.exist? '.hg/requires'
          true
        else
          curr_pwd = Dir.pwd
          Dir.chdir('..') do
            return false if curr_pwd == Dir.pwd # Means going up one level is not possible.
            applies_to?
          end
        end
      end
    end

    # Fails if one of these 2 conditions are not met:
    #   1. The reository is not 'clean'; no content staged or unstaged
    #   2. The repository is only a local repository and has no remote refs
    def check
      super
      info "Working in branch '#{Hg.current_branch}'"
      uncommitted = Hg.uncommitted_files
      fail "Uncommitted files violate the First Principle Of Release!\n#{uncommitted.join("\n")}" unless uncommitted.empty?
      fail "You are releasing from a local branch that does not track a remote!" if Hg.remote.empty?
    end

    # Tag this release in Mercurial
    def tag_release(tag)
      unless this_version == extract_version
        info "Committing buildfile with version number #{extract_version}"
        Hg.commit File.basename(Buildr.application.buildfile.to_s), message
        Hg.push if Hg.remote
      end
      info "Tagging release #{tag}"
      Hg.hg 'tag', tag, '-m', "[buildr] Cutting release #{tag}"
      Hg.push if Hg.remote
    end

    # Update buildfile with next version number
    def update_version_to_next
      super
      info "Current version is now #{extract_version}"
      Hg.commit File.basename(Buildr.application.buildfile.to_s), message
      Hg.push if Hg.remote
    end     
  end


  class GitRelease < Release
    class << self
      def applies_to?
        if File.exist? '.git/config'
          true
        else
          curr_pwd = Dir.pwd
          Dir.chdir('..') do
            return false if curr_pwd == Dir.pwd # Means going up one level is not possible.
            applies_to?
          end
        end
      end
    end

    # Fails if one of these 2 conditions are not met:
    #    1. the repository is clean: no content staged or unstaged
    #    2. some remote repositories are defined but the current branch does not track any
    def check
      super
      uncommitted = Git.uncommitted_files
      fail "Uncommitted files violate the First Principle Of Release!\n#{uncommitted.join("\n")}" unless uncommitted.empty?
      fail "You are releasing from a local branch that does not track a remote!" unless Git.remote
    end

    # Add a tag reference in .git/refs/tags and push it to the remote if any.
    # If a tag with the same name already exists it will get deleted (in both local and remote repositories).
    def tag_release(tag)
      unless this_version == extract_version
        info "Committing buildfile with version number #{extract_version}"
        Git.commit File.basename(Buildr.application.buildfile.to_s), message
        Git.push if Git.remote
      end
      info "Tagging release #{tag}"
      Git.git 'tag', '-d', tag rescue nil
      Git.git 'push', Git.remote, ":refs/tags/#{tag}" rescue nil if Git.remote
      Git.git 'tag', '-a', tag, '-m', "[buildr] Cutting release #{tag}"
      Git.git 'push', Git.remote, 'tag', tag if Git.remote
    end

    def update_version_to_next
      super
      info "Current version is now #{extract_version}"
      Git.commit File.basename(Buildr.application.buildfile.to_s), message
      Git.push if Git.remote
    end
  end


  class SvnRelease < Release
    class << self
      def applies_to?
        File.exist?('.svn')
      end
    end

    def check
      super
      fail "Uncommitted files violate the First Principle Of Release!\n"+Svn.uncommitted_files.join("\n") unless Svn.uncommitted_files.empty?
      fail "SVN URL must contain 'trunk' or 'branches/...'" unless Svn.repo_url =~ /(trunk)|(branches.*)$/
    end

    def tag_release(tag)
      # Unlike Git, committing the buildfile with the released version is not necessary.
      # svn tag does commit & tag.
      info "Tagging release #{tag}"
      Svn.tag tag
    end

    def update_version_to_next
      super
      info "Current version is now #{extract_version}"
      Svn.commit Buildr.application.buildfile.to_s, message
    end
  end

  Release.add HgRelease
  Release.add SvnRelease
  Release.add GitRelease

  desc 'Make a release'
  task 'release' do |task|
    release = Release.find
    fail 'Unable to detect the Version Control System.' unless release
    release.make
  end

end

class Buildr::Project #:nodoc:
  include Buildr::Build
end
