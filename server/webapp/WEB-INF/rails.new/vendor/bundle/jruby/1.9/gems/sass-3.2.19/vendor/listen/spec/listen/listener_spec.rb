require 'spec_helper'

describe Listen::Listener do
  let(:adapter)           { mock(Listen::Adapter, :start => true).as_null_object }
  let(:watched_directory) { File.dirname(__FILE__) }

  subject { described_class.new(watched_directory) }

  before do
    Listen::Adapter.stub(:select_and_initialize) { adapter }
    # Don't build a record of the files inside the base directory.
    subject.directory_record.stub(:build)
  end

  it_should_behave_like 'a listener to changes on a file-system'

  describe '#initialize' do
    context 'with no options' do
      it 'sets the directory' do
        subject.directory.should eq watched_directory
      end

      it 'converts the passed path into an absolute path - #21' do
        described_class.new(File.join(watched_directory, '..')).directory.should eq File.expand_path('..', watched_directory)
      end

      it 'sets the option for using relative paths in the callback to the default one' do
        subject.instance_variable_get(:@use_relative_paths).should eq described_class::DEFAULT_TO_RELATIVE_PATHS
      end
    end

    context 'with custom options' do
      subject { described_class.new(watched_directory, :ignore => /\.ssh/, :filter => [/.*\.rb/,/.*\.md/],
                                    :latency => 0.5, :force_polling => true, :relative_paths => true) }

      it 'passes the custom ignored paths to the directory record' do
        subject.directory_record.ignoring_patterns.should include /\.ssh/
      end

      it 'passes the custom filters to the directory record' do
        subject.directory_record.filtering_patterns.should =~  [/.*\.rb/,/.*\.md/]
      end

      it 'sets the cutom option for using relative paths in the callback' do
        subject.instance_variable_get(:@use_relative_paths).should be_true
      end

      it 'sets adapter_options' do
        subject.instance_variable_get(:@adapter_options).should eq(:latency => 0.5, :force_polling => true)
      end
    end
  end

  describe '#start' do
    it 'selects and initializes an adapter' do
      Listen::Adapter.should_receive(:select_and_initialize).with(watched_directory, {}) { adapter }
      subject.start
    end

    it 'builds the directory record' do
      subject.directory_record.should_receive(:build)
      subject.start
    end
  end

  context 'with a started listener' do
    before do
      subject.stub(:initialize_adapter) { adapter }
      subject.start
    end

    describe '#unpause' do
      it 'rebuilds the directory record' do
        subject.directory_record.should_receive(:build)
        subject.unpause
      end
    end
  end

  describe '#ignore'do
    it 'delegates the work to the directory record' do
      subject.directory_record.should_receive(:ignore).with 'some_directory'
      subject.ignore 'some_directory'
    end
  end

  describe '#ignore!'do
    it 'delegates the work to the directory record' do
      subject.directory_record.should_receive(:ignore!).with 'some_directory'
      subject.ignore! 'some_directory'
    end
  end

  describe '#filter' do
    it 'delegates the work to the directory record' do
      subject.directory_record.should_receive(:filter).with /\.txt$/
      subject.filter /\.txt$/
    end
  end

  describe '#filter!' do
    it 'delegates the work to the directory record' do
      subject.directory_record.should_receive(:filter!).with /\.txt$/
      subject.filter! /\.txt$/
    end
  end


  describe '#on_change' do
    let(:directories) { %w{dir1 dir2 dir3} }
    let(:changes)     { {:modified => [], :added => [], :removed => []} }
    let(:callback)    { Proc.new { @called = true } }

    before do
      @called = false
      subject.directory_record.stub(:fetch_changes => changes)
    end

    it 'fetches the changes of the directory record' do
      subject.directory_record.should_receive(:fetch_changes).with(
        directories, hash_including(:relative_paths => described_class::DEFAULT_TO_RELATIVE_PATHS)
      )
      subject.on_change(directories)
    end

    context 'with relative paths option set to true' do
      subject { described_class.new(watched_directory, :relative_paths => true) }

      it 'fetches the changes of the directory record' do
        subject.directory_record.should_receive(:fetch_changes).with(directories, hash_including(:relative_paths => true))
        subject.on_change(directories)
      end
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
