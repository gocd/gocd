#!/usr/bin/env ruby
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


# This script helps buildr developers to obtain their own git clone from
# github, and also provides GitFlow commands to keep the git mirror in sync
# with Apache SVN.
#
# If you already have a buildr clone, just do the following:
#
#    git config alias.apache '!'"ruby $PWD/doc/scripts/buildr-git.rb"
#
# After this, you have a 'git apache' command and you can try (be sure to read the help)
#
#    git apache help
#    git apache setup svn --help
#    git apache sync --help
#
# To configure your local repo for svn synchronization,
#
#    git apache update-authors
#    git remote add upstream git@github.com:buildr/buildr.git
#    git apache setup svn --username apacheLogin --apache-git upstream
#    git apache sync
#
# This script can also be run without having a local buildr clone:
#
#   ruby -ropen-uri -e 'eval(open("http://svn.apache.org/viewvc/buildr/trunk/doc/scripts/buildr-git.rb?view=co").read)' help



require 'yaml'
require 'open-uri'

if $0 == '-e' # invoked from open-uri
  gitflow = "http://svn.apache.org/viewvc/buildr/trunk/doc/scripts/gitflow.rb?view=co"
  eval(open(gitflow).read)
else
  require File.expand_path('gitflow', File.dirname(__FILE__))
end

GitFlow.program = 'buildr-git'

module BuildrGit

  class UpdateUsersCommand < GitFlow/'update-users'

    @help = "Update list of Apache SVN committers from Jukka's list."
    @@url = 'http://people.apache.org/~jukka/authors.txt'

    def self.authors_file
      File.expand_path('.git/authors.txt', Dir.pwd)
    end

    def self.user_email(apache_login, authors_file = nil)
      authors_file ||= self.authors_file
      authors = YAML.load(File.read(authors_file).gsub!(/\s+=\s+/, ': '))
      contact = authors[apache_login]
      fail "You are not listed as apache commiter on #{authors_file}" unless contact
      fail "Not a valid contact line: #{contact}" unless contact =~ /\s+<(.*)>/
      [$`, $1]
    end

    def options(opts)
      opts.url = @@url
      opts.file = self.class.authors_file
      [['-u', '--url URL',
        "From URL. defaults to: #{opts.url}", lambda { |url|
          opts.url = url
       }],
       ['-f', '--file FILE',
        "Write to FILE, defaults to #{opts.file}", lambda { |path|
          opts.file = path
        }]
      ]
    end

    def execute(opts, argv)
      FileUtils.mkdir_p(File.dirname(opts.file))
      content = open(opts.url).read
      File.open(opts.file, "w") { |f| f.print content }
    end
  end

  class CloneCommand < GitFlow/:clone
    @help = "Create a clone from github.com/buildr repository."

    def options(opts)
      opts.origin = 'git://github.com/buildr/buildr.git'
      opts.svn_prefix = 'apache'
      opts.project = 'buildr'
      opts.local = expand_path(opts.project)
      [['--prefix SVN_PREFIX', opts.svn_prefix, lambda { |p|
          opts.svn_prefix = p }],
       ['--origin GIT_ORIGIN', opts.origin, lambda { |o|
          opts.origin = o }],
       ['-d', '--dir DIR', opts.local, lambda { |d| opts.local = d }]
      ]
    end

    def execute(opts, argv)
      git 'clone', opts.origin, opts.local
      Dir.chdir(opts.local) do
        run 'update-users'
        run 'setup'
      end
    end
  end

  class SetupCommand < GitFlow/:setup
    @help = "Setup your buildr clone to be used with git mirror."
    def options(opt)
      []
    end

    def execute(opt, argv)
      run 'setup', 'alias'
      run 'setup', 'svn'
    end
  end

  class SetupAliasCommand < SetupCommand/:alias
    def execute(opt, argv)
      me = expand_path('doc/scripts/buildr-git.rb')
      git 'config', 'alias.apache', "!ruby #{me}"
    end
  end

  class SetupSvnCommand < SetupCommand/:svn
    @help = "Setup for getting changes from Apache SVN."

    def options(opt)
      opt.svn_prefix = 'apache'
      opt.svn_path = 'buildr'
      opt.townhall = 'origin'
      [['--username SVN_USER', 'Use Apache svn username for this svn remote',
        lambda { |e| opt.apache_login = e  }],
       ['--svn-prefix PREFIX', 'The name of svn remote to use for project.',
        "Defaults to #{opt.svn_prefix}",
        lambda{|p| opt.svn_prefix = p }],
       ['--svn-uri URI', lambda {|p| opt.svn_uri = p  }],
       ['--svn-rev REVISION', lambda {|p| opt.svn_rev = p  }],
       ['--svn-path PATH', 'The path to append to svn-uri.',
        "Defaults to #{opt.svn_path}", lambda {|p| opt.svn_path = p  }],
       ['--apache-git REMOTE', 'The name of remote you are using as town-hall git repo.',
        "Defaults to #{opt.townhall}",
        lambda {|p| opt.townhall = p }]
      ]
    end

    def execute(opt, argv)
      authors_file = UpdateUsersCommand.authors_file
      git 'config', 'svn.authorsfile', authors_file
      git 'config', 'apache.svn', opt.svn_prefix
      git 'config', 'apache.git', opt.townhall

      if opt.apache_login
        user, email = UpdateUsersCommand.user_email(opt.apache_login, authors_file)
        puts "You claim to be #{user} <#{email}> with apache login: #{opt.apache_login}"
        git('config', 'user.name', user)
        git('config', 'user.email', email)
      end

      if opt.svn_rev
        revision = opt.svn_rev
      else
        location, revision = svn_loc_rev
        revision = opt.svn_rev || revision
      end

      if opt.svn_uri
        repo = opt.svn_uri
      else
        fail "No #{opt.svn_path} directory on #{location}" unless
          location =~ /\/#{opt.svn_path}/
        repo = $`
      end

      # Tell git where the svn repository is
      git('config', "svn-remote.#{opt.svn_prefix}.url", repo)
      git('config', "svn-remote.#{opt.svn_prefix}.fetch",
          "#{opt.svn_path}/trunk:refs/remotes/#{opt.svn_prefix}/trunk")
      git('config', "svn-remote.#{opt.svn_prefix}.branches",
          "#{opt.svn_path}/branches/*:refs/remotes/#{opt.svn_prefix}/*")
      git('config', "svn-remote.#{opt.svn_prefix}.tags",
          "#{opt.svn_path}/tags/*:refs/remotes/#{opt.svn_prefix}/tags/*")

      # Store the user for svn dcommit
      if opt.apache_login
        git('config', "svn-remote.#{opt.svn_prefix}.username", opt.apache_login)
      end

      # Create the svn branch, do this instead of pulling the full svn history
      git('update-ref', "refs/remotes/#{opt.svn_prefix}/trunk",
          'refs/remotes/origin/master')
      # create tags from git
      git('tag').split.each do |tag|
        git('update-ref', "refs/remotes/#{opt.svn_prefix}/tags/#{tag}",
            "refs/tags/#{tag}")
      end
      # update svn metadata
      mkdir_p(expand_path('.git/svn'))
      svn_meta = expand_path('.git/svn/.metadata')
      git('config', '--file', svn_meta,
          "svn-remote.#{opt.svn_prefix}.branches-maxRev", revision)
      git('config', '--file', svn_meta,
          "svn-remote.#{opt.svn_prefix}.tags-maxRev", revision)
    end

    def svn_loc_rev
      meta = sh('git log -n 10 | grep git-svn-id | head -n 1').chomp
      fail "No svn metadata on last 10 commits" if meta.empty?
      meta.split[1].split('@')
    end
  end

  class FetchCommand < GitFlow/:fetch
    @help = "Get changes from svn, creating tags, branches on townhall"
    @documentation = <<-DOC
This command can be used to fetch changes from Apache\'s SVN repo.

GIT CONFIG VALUES:

apache.svn  - The svn remote using to get changes from Apache SVN.
              Set by setup-svn --svn-prefix.
    DOC

    def options(opt)
      opt.apache_svn = git('config', '--get', 'apache.svn').chomp rescue nil
      [['--apache-svn SVN_REMOTE', 'The SVN remote used to get changes from Apache',
        "Current value: #{opt.apache_svn}",
        lambda { |r| opt.apache_svn = r }]
       ]
    end

    def execute(opt, argv)
      fail "Missing apache.svn config value" unless opt.apache_svn
      git('svn', 'fetch', opt.apache_svn)
    end
  end

  class SyncCommand < GitFlow/:sync
    @help = "Synchronizes between Apache svn and git townhall."
    @documentation = <<-DOC
This command will perform the following actions:
  * fetch changes from apache svn.
  * rebase them on the current branch or on the one specified with --onto
  * dcommit (this will push your changes to Apache trunk)

GIT CONFIG VALUES:

apache.svn
  The svn remote using to get changes from Apache SVN.
  Set by setup-svn --svn-prefix.

apache.git
  The git remote used as townhall repository.
  Set by setup-svn --townhall.

svn-remote.APACHE_GIT.username
  If configured, sync will use this svn username while dcommiting.
DOC

    def options(opt)
      git('branch').split.tap { |n| opt.current = n[n.index('*')+1] }
      opt.branch = opt.current
      opt.svn_branch = 'trunk'
      opt.git_branch = 'master'
      opt.apache_git = git('config', '--get', 'apache.git').chomp rescue nil
      opt.apache_svn = git('config', '--get', 'apache.svn').chomp rescue nil
      opt.svn_username = git('config', '--get',
                             "svn-remote.#{opt.apache_svn}.username").chomp rescue nil
      [['--apache-svn SVN_REMOTE', 'The SVN remote used to get changes from Apache',
        "Current value: #{opt.apache_svn}",
        lambda { |r| opt.apache_svn = r }],
       ['--apache-git REMOTE', 'The git remote used as town-hall repository.',
        "Current value: #{opt.apache_git}",
        lambda { |r| opt.apache_git = r }],
       ['--username SVN_USER',
        'Specify the SVN username for dcommit',
        "Defaults to: #{opt.svn_username}",
        lambda { |b| opt.svn_username = b }],
       ['--svn-branch SVN_BRANCH',
        'Specify the SVN branch to rebase changes from, and where to dcommit',
        "Defaults to: #{opt.svn_branch}",
        lambda { |b| opt.svn_branch = b }],
       ['--git-branch REMOTE_BRANCH',
        'Specify the remote town-hall branch (on apache.git) to update',
        "Defaults to: #{opt.git_branch}",
        lambda { |b| opt.git_branch = b }],
       ['--branch BRANCH', 'Specify the local branch to take changes from',
        "Current branch: #{opt.branch}",
        lambda { |b| opt.branch = b }]
      ]
    end

    def execute(opt, argv)
      # obtain the svn url
      url = git('config', '--get', "svn-remote.#{opt.apache_svn}.url").chomp
      # obtain the path for project
      path = git('config', '--get', "svn-remote.#{opt.apache_svn}.branches").
        chomp.split('/branches').first
      commit_url = "#{url}/#{path}/#{opt.svn_branch}"

      # obtain latest changes from svn
      git('svn', 'fetch', '--svn-remote', opt.apache_svn)
      # obtain latest changes from git
      git('fetch', opt.apache_git,
          "#{opt.git_branch}:refs/remotes/#{opt.apache_git}/#{opt.git_branch}")

      # rebase svn changes in the desired branch
      git('rebase', "#{opt.apache_svn}/#{opt.svn_branch}", opt.branch)
      git('rebase', "#{opt.apache_git}/#{opt.git_branch}", opt.branch)

      # dcommit to the specific svn branch
      ['svn', 'dcommit',
       '--svn-remote', opt.apache_svn, '--commit-url', commit_url].tap do |cmd|
        if opt.svn_username
          cmd << '--username' << opt.svn_username
        end
        git(*cmd)
      end

      # update townhall remote ref
      git('update-ref',
          "refs/remotes/#{opt.apache_git}/#{opt.git_branch}",
          "refs/remotes/#{opt.apache_svn}/#{opt.svn_branch}")

      # forward the remote townhall/master to apache/trunk
      git('push', opt.apache_git,
          "refs/remotes/#{opt.apache_git}/#{opt.git_branch}:#{opt.git_branch}")

      # get back to the original branch
      git('checkout', opt.current)
    end
  end


  # This one is displayed when the user executes this script using
  # open-uri -e
  HEADER = <<HEADER

Buildr official commit channel is Apache's svn repository, however some
developers may prefer to use git while working on several features and
merging other's changes.

This script will configure a gitflow copy on so you can commit to svn.

Enter <-h> to see options, <-H> to see notes about configured aliases
and recommended workflow, or any other option.

Ctrl+D or an invalid option to abort
HEADER

  # When fork is completed, we display the following notice on a
  # pager, giving the user a brief overview of git aliases used
  # to keep the mirror in sync.
  NOTICE = <<NOTICE
ALIASES:

  Some git aliases have been created for developer convenience:

    git apache fetch    # get changes from apache/trunk without merging them
                        # you can inspect what's happening on trunk without
                        # having to worry about merging conflicts.
                        # Inspect the remote branch with `git log apache/trunk`
                        # Or if you have a git-ui like `tig` you can use that.

    git apache merge    # Merge already fetched changes on the current branch
                        # Use this command to get up to date with trunk changes
                        # you can always cherry-pick from the apache/trunk
                        # branch.

    git apache pull     # get apache-fetch && git apache-merge

    git apache push     # Push to Apache's SVN. Only staged changes (those
                        # recorded using `git commit`) will be sent to SVN.
                        # You need not to be on the master branch.
                        # Actually you can work on a tiny-feature branch and
                        # commit directly from it.
                        #
                        # VERY IMPORTANT:
                        #
                        # Missing commits on Apache's SVN will be sent using
                        # your apache svn account. This means that you can
                        # make some commits on behalf of others (like patches
                        # comming from JIRA issues or casual contributors)
                        # Review the apache-push alias on .git/config if you
                        # want to change login-name used for commit to SVN.
                        #
                        # See the recommended workflow to avoid commiting
                        # other developers' changes and the following section.

THE GITHUB MIRROR:

   Buildr has an unofficial git mirror on github, maintained by Apache committers:

     http://github.com/buildr/buildr

   This mirror DOES NOT replace Apache's SVN repository. We really care about
   using Apache infrastructure and following Apache project guidelines for
   contributions. This git mirror is provided only for developers convenience,
   allowing them to easily create experimental branches or review code from
   other committers.

   All code that wants to make it to the official Apache Buildr repository needs
   to be committed to the Apache SVN repository by using the command:

      git synchronize

   This command will synchronize both ways svn<->git to keep trunk upto date.
   You need to be an Apache committer and have permissions on the SVN repo.

   It's VERY IMPORTANT for Buildr committers to remember that contributions from
   external entities wanting to be accepted will require them to sign the Apache ICLA.
   We provide the git mirror to make it easier for people to experiment and
   contribute back to Buildr, before merging their code in, please remember they
   have to create create a JIRA issue granting ASF permission to include their code,
   just like any other contribution following Apache's guidelines.

   So, it's very important - if you care about meritocracy - to follow or at
   least that you get an idea of the recommended workflow.

RECOMMENDED WORKFLOW:

   So now that you have your local buildr copy you can create topic branches
   to work on independent features, and still merge easily with head changes.

   They may seem lots of things to consider, but it's all for Buildr's healt.
   As all things git, you can always follow your own workflow and even create
   aliases on you .git/config file to avoid typing much. So, here they are:

   1) get your gitflow configured
     (you have already do so, this was the most difficult part)

   2) create a topic branch to work on, say.. you want to add cool-feature:

        git checkout -b cool-feature master
        # now on branch cool-feature

   3) hack hack hack.. use the source luke.
      every time you feel you have something important like added failing
      spec, added part of feature, or resolved some conflict from merges,
      you can commit your current progress. If you want to be selective, use:

        git commit --interactive

   3) review your changes, get ALL specs passing, repeat step 3 as needed

   4) let's see what are they doing on trunk

        git apache-fetch
        # You can inspect the upstream changes without having to merge them
        git log apache/trunk # what are they doing!!

   5) integrate mainstream changes to your cool-feature branch, you can always
      use `git cherry-pick` to select only some commits.

        git merge apache/trunk cool-feature

   6) Go to 3 unless ALL specs are passing.

   7.a) (Skip to 7.b you have commit bit on Apache's SVN)
      Create a patch using `git format-patch`
      Promote your changes, create a JIRA issue and upload it granting Apache
      license to include your code:

        https://issues.apache.org/jira/browse/BUILDR
        dev@buildr.apache.org

   7.b) Now you have everyhing on staging area and merged important changes
      from apache/trunk, it's time to commit them to Apache's SVN.

        git apache-push

   8) Optional. If you are a buildr committer you may want to synchronize
     the github mirror for helping others to get changes without having to
     wait on Victor's cronjob to run every hour (useful for urgent changes).

        git synchronize

   9) Pull changes from origin frequently.

        git fetch origin
        git rebase --onto origin/master master master

   10) Unconditionally, Go to step 2 ;)
       Share your gitflow workflow, git tips, etc.

RESOURCES:

   http://github.com/buildr/buildr/tree/master
   http://git.or.cz/gitwiki/GitCheatSheet
   http://groups.google.com/group/git-users/web/git-references

NOTICE
  #' for emacs

end
