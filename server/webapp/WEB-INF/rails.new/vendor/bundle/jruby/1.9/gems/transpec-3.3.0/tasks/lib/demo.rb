# coding: utf-8

require_relative 'project'

class Demo < Project
  DEMO_BRANCH = 'transpec-demo'

  def self.base_dir_path
    File.join('tmp', 'demo')
  end

  def run
    puts " Publishing conversion example of #{name} project ".center(80, '=')

    setup

    in_project_dir do
      transpec '--force', '--convert', 'stub_with_hash'
      sh 'bundle exec rspec'
      sh "git checkout --quiet -b #{DEMO_BRANCH}"
      sh 'git commit -aF .git/COMMIT_EDITMSG'
      sh "git push --force origin #{DEMO_BRANCH}"
    end
  end

  private

  def transpec(*args)
    sh File.join(Transpec.root, 'bin', 'transpec'), *args
  end

  def setup_from_remote
    FileUtils.rm_rf(project_dir) if Dir.exist?(project_dir)
    super
  end

  def shallow_clone?
    false
  end

  def git_local_branch_exist?(branch)
    in_project_dir do
      system('git', 'show-ref', '--verify', '--quiet', "refs/heads/#{branch}")
    end
  end
end
