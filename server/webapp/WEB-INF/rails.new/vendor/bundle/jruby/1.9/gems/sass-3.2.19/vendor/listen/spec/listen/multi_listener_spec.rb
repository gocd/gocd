require 'spec_helper'

describe Listen::MultiListener do
  let(:adapter)             { mock(Listen::Adapter, :start => true).as_null_object }
  let(:watched_directories) { [File.dirname(__FILE__), File.expand_path('../..', __FILE__)] }

  subject { described_class.new(*watched_directories) }

  before do
    Listen::Adapter.stub(:select_and_initialize) { adapter }
    # Don't build a record of the files inside the base directory.
    Listen::DirectoryRecord.any_instance.stub(:build)
  end

  it_should_behave_like 'a listener to changes on a file-system'

  describe '#initialize' do
    context 'with no options' do
      it 'sets the directories' do
        subject.directories.should =~ watched_directories
      end

      it 'converts the passed paths into absolute paths - #21' do
        paths = watched_directories.map { |d| File.join(d, '..') }
        described_class.new(*paths).directories.should =~ watched_directories.map{ |d| File.expand_path('..', d) }
      end
    end

    context 'with custom options' do
      subject do
        args = watched_directories << {:ignore => /\.ssh/, :filter => [/.*\.rb/,/.*\.md/], :latency => 0.5, :force_polling => true}
        described_class.new(*args)
      end

      it 'passes the custom ignored paths to each directory record' do
        subject.directories_records.each do |r|
          r.ignoring_patterns.should include /\.ssh/
        end
      end

      it 'passes the custom filters to each directory record' do
        subject.directories_records.each do |r|
          r.filtering_patterns.should =~  [/.*\.rb/,/.*\.md/]
        end
      end

      it 'sets adapter_options' do
        subject.instance_variable_get(:@adapter_options).should eq(:latency => 0.5, :force_polling => true)
      end
    end
  end

  describe '#start' do
    it 'selects and initializes an adapter' do
      Listen::Adapter.should_receive(:select_and_initialize).with(watched_directories, {}) { adapter }
      subject.start
    end

    it 'builds all directories records' do
      subject.directories_records.each do |r|
        r.should_receive(:build)
      end
      subject.start
    end
  end

  context 'with a started listener' do
    before do
      subject.stub(:initialize_adapter) { adapter }
      subject.start
    end

    describe '#unpause' do
      it 'rebuilds all directories records' do
        subject.directories_records.each do |r|
          r.should_receive(:build)
        end
        subject.unpause
      end
    end
  end

  describe '#ignore' do
    it 'delegates the work to each directory record' do
      subject.directories_records.each do |r|
        r.should_receive(:ignore).with 'some_directory'
      end
      subject.ignore 'some_directory'
    end
  end

  describe '#ignore!' do
    it 'delegates the work to each directory record' do
      subject.directories_records.each do |r|
        r.should_receive(:ignore!).with 'some_directory'
      end
      subject.ignore! 'some_directory'
    end
  end

  describe '#filter' do
    it 'delegates the work to each directory record' do
      subject.directories_records.each do |r|
        r.should_receive(:filter).with /\.txt$/
      end
      subject.filter /\.txt$/
    end
  end

  describe '#filter!' do
    it 'delegates the work to each directory record' do
      subject.directories_records.each do |r|
        r.should_receive(:filter!).with /\.txt$/
      end
      subject.filter! /\.txt$/
    end
  end

  describe '#on_change' do
    let(:directories) { %w{dir1 dir2 dir3} }
    let(:changes)     { {:modified => [], :added => [], :removed => []} }
    let(:callback)    { Proc.new { @called = true } }

    before do
      @called = false
      subject.stub(:fetch_records_changes => changes)
    end

    it 'fetches the changes of all directories records' do
      subject.unstub(:fetch_records_changes)

      subject.directories_records.each do |record|
        record.should_receive(:fetch_changes).with(
          directories, hash_including(:relative_paths => described_class::DEFAULT_TO_RELATIVE_PATHS)
        ).and_return(changes)
      end
      subject.on_change(directories)
    end

    context 'with no changes to report' do
      if RUBY_VERSION[/^1.8/]
        it 'does not run the callback' do
            subject.change(&callback)
            subject.on_change(directories)
            @called.should be_false
        end
      else
        it 'does not run the callback' do
          callback.should_not_receive(:call)
          subject.change(&callback)
          subject.on_change(directories)
        end
      end
    end

    context 'with changes to report' do
      let(:changes)     { {:modified => %w{path1}, :added => [], :removed => %w{path2}} }

      if RUBY_VERSION[/^1.8/]
        it 'runs the callback passing it the changes' do
          subject.change(&callback)
          subject.on_change(directories)
          @called.should be_true
        end
      else
        it 'runs the callback passing it the changes' do
          callback.should_receive(:call).with(changes[:modified], changes[:added], changes[:removed])
          subject.change(&callback)
          subject.on_change(directories)
        end
      end
    end
  end
end
