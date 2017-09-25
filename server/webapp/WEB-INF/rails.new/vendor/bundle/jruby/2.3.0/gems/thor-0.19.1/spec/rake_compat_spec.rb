require "helper"
require "thor/rake_compat"
require "rake/tasklib"

$main = self

class RakeTask < Rake::TaskLib
  def initialize
    define
  end

  def define
    $main.instance_eval do
      desc "Say it's cool"
      task :cool do
        puts "COOL"
      end

      namespace :hiper_mega do
        task :super do
          puts "HIPER MEGA SUPER"
        end
      end
    end
  end
end

class ThorTask < Thor
  include Thor::RakeCompat
  RakeTask.new
end

describe Thor::RakeCompat do
  it "sets the rakefile application" do
    expect(%w[rake_compat_spec.rb Thorfile]).to include(Rake.application.rakefile)
  end

  it "adds rake tasks to thor classes too" do
    task = ThorTask.tasks["cool"]
    expect(task).to be
  end

  it "uses rake tasks descriptions on thor" do
    expect(ThorTask.tasks["cool"].description).to eq("Say it's cool")
  end

  it "gets usage from rake tasks name" do
    expect(ThorTask.tasks["cool"].usage).to eq("cool")
  end

  it "uses non namespaced name as description if non is available" do
    expect(ThorTask::HiperMega.tasks["super"].description).to eq("super")
  end

  it "converts namespaces to classes" do
    expect(ThorTask.const_get(:HiperMega)).to eq(ThorTask::HiperMega)
  end

  it "does not add tasks from higher namespaces in lowers namespaces" do
    expect(ThorTask.tasks["super"]).not_to be
  end

  it "invoking the thor task invokes the rake task" do
    expect(capture(:stdout) do
      ThorTask.start %w[cool]
    end).to eq("COOL\n")

    expect(capture(:stdout) do
      ThorTask::HiperMega.start %w[super]
    end).to eq("HIPER MEGA SUPER\n")
  end
end
