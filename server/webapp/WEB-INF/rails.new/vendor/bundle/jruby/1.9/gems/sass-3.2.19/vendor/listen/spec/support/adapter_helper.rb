# Adapter watch
#
# @param [Listen::Listener] listener the adapter listener
# @param [String] path the path to watch
#
def watch(listener, expected_changes, *paths)
  callback = lambda { |changed_dirs, options| @called = true; listener.on_change(changed_dirs) }
  @adapter = Listen::Adapter.select_and_initialize(paths, { :report_changes => false, :latency => test_latency }, &callback)

  forced_stop = false
  prevent_deadlock = Proc.new { sleep(10); puts "Forcing stop"; @adapter.stop; forced_stop = true }

  @adapter.start(false)

  yield

  t = Thread.new(&prevent_deadlock)
  @adapter.wait_for_changes(expected_changes)

  unless forced_stop
    Thread.kill(t)
    @adapter.report_changes
  end
ensure
  unless forced_stop
    Thread.kill(t) if t
    @adapter.stop
  end
end

shared_examples_for 'a filesystem adapter' do
  subject { described_class.new(File.dirname(__FILE__), &Proc.new {}) }

  describe '#start' do
    after { subject.stop }

    context 'with the blocking param set to true' do
      it 'blocks the current thread after starting the workers' do
        @called = false
        t = Thread.new { subject.start(true); @called = true }
        sleep(test_latency * 3)
        Thread.kill(t) if t
        @called.should be_false
      end
    end

    context 'with the blocking param set to false' do
      it 'does not block the current thread after starting the workers' do
        @called = false
        t = Thread.new { subject.start(false); @called = true }
        sleep(test_latency * 3)
        Thread.kill(t) if t
        @called.should be_true
      end
    end
  end

  describe '#started?' do
    context 'with a new adapter' do
      it 'returns false' do
        subject.should_not be_started
      end
    end

    context 'with a stopped adapter' do
      before { subject.start(false); subject.stop }

      it 'returns false' do
        subject.should_not be_started
      end
    end

    context 'with a started adapter' do
      before { subject.start(false) }
      after  { subject.stop }

      it 'returns true' do
        subject.should be_started
      end
    end
  end
end

shared_examples_for 'an adapter that call properly listener#on_change' do |*args|
  options = (args.first && args.first.is_a?(Hash)) ? args.first : {}
  let(:listener) { mock(Listen::Listener) }
  before { described_class.stub(:works?) { true } }

  context 'single file operations' do
    context 'when a file is created' do
      it 'detects the added file' do
        fixtures do |path|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path)
          end

          watch(listener, 1, path) do
            touch 'new_file.rb'
          end
        end
      end

      context 'given a symlink', :unless => windows? do
        it 'detects the added file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path)
            end

            touch 'new_file.rb'

            watch(listener, 1, path) do
              ln_s 'new_file.rb', 'new_file_symlink.rb'
            end
          end
        end
      end

      context 'given a new created directory' do
        it 'detects the added file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path, "#{path}/a_directory")
            end

            watch(listener, 2, path) do
              mkdir 'a_directory'
              # Needed for INotify, because of :recursive rb-inotify custom flag?
              sleep 0.05
              touch 'a_directory/new_file.rb'
            end
          end
        end
      end

      context 'given an existing directory' do
        it 'detects the added file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/a_directory")
            end

            mkdir 'a_directory'

            watch(listener, 1, path) do
              touch 'a_directory/new_file.rb'
            end
          end
        end
      end

      context 'given a directory with subdirectories' do
        it 'detects the added file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/a_directory/subdirectory")
            end

            mkdir_p 'a_directory/subdirectory'

            watch(listener, 1, path) do
              touch 'a_directory/subdirectory/new_file.rb'
            end
          end
        end
      end
    end

    context 'when a file is modified' do
      it 'detects the modified file' do
        fixtures do |path|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path)
          end

          touch 'existing_file.txt'

          watch(listener, 1, path) do
            touch 'existing_file.txt'
          end
        end
      end

      context 'given a symlink', :unless => windows? do
        it 'detects the modified file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path)
            end

            touch 'existing_file.rb'
            ln_s  'existing_file.rb', 'existing_file_symlink.rb'

            watch(listener, 1, path) do
              touch 'existing_file.rb'
            end
          end
        end
      end

      context 'given a hidden file' do
        it 'detects the modified file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path)
            end

            touch '.hidden'

            watch(listener, 1, path) do
              touch '.hidden'
            end
          end
        end
      end

      context 'given a file mode change', :unless => windows? do
        it 'does not detect the mode change' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path)
            end

            touch 'run.rb'

            watch(listener, 1, path) do
              chmod 0777, 'run.rb'
            end
          end
        end
      end

      context 'given an existing directory' do
        it 'detects the modified file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/a_directory")
            end

            mkdir 'a_directory'
            touch 'a_directory/existing_file.txt'

            watch(listener, 1, path) do
              touch 'a_directory/existing_file.txt'
            end
          end
        end
      end

      context 'given a directory with subdirectories' do
        it 'detects the modified file' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/a_directory/subdirectory")
            end

            mkdir_p 'a_directory/subdirectory'
            touch   'a_directory/subdirectory/existing_file.txt'

            watch(listener, 1, path) do
              touch 'a_directory/subdirectory/new_file.rb'
            end
          end
        end
      end
    end

    context 'when a file is moved' do
      it 'detects the file move' do
        fixtures do |path|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path)
          end

          touch 'move_me.txt'

          watch(listener, 1, path) do
            mv 'move_me.txt', 'new_name.txt'
          end
        end
      end

      context 'given a symlink', :unless => windows? do
        it 'detects the file move' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path)
            end

            touch 'move_me.rb'
            ln_s  'move_me.rb', 'move_me_symlink.rb'

            watch(listener, 1, path) do
              mv 'move_me_symlink.rb', 'new_symlink.rb'
            end
          end
        end
      end

      context 'given an existing directory' do
        it 'detects the file move into the directory' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path, "#{path}/a_directory")
            end

            mkdir 'a_directory'
            touch 'move_me.txt'

            watch(listener, 2, path) do
              mv 'move_me.txt', 'a_directory/move_me.txt'
            end
          end
        end

        it 'detects a file move out of the directory' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path, "#{path}/a_directory")
            end

            mkdir 'a_directory'
            touch 'a_directory/move_me.txt'

            watch(listener, 2, path) do
              mv 'a_directory/move_me.txt', 'i_am_here.txt'
            end
          end
        end

        it 'detects a file move between two directories' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/from_directory", "#{path}/to_directory")
            end

            mkdir 'from_directory'
            touch 'from_directory/move_me.txt'
            mkdir 'to_directory'

            watch(listener, 2, path) do
              mv 'from_directory/move_me.txt', 'to_directory/move_me.txt'
            end
          end
        end
      end

      context 'given a directory with subdirectories' do
        it 'detects files movements between subdirectories' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/a_directory/subdirectory", "#{path}/b_directory/subdirectory")
            end

            mkdir_p 'a_directory/subdirectory'
            mkdir_p 'b_directory/subdirectory'
            touch   'a_directory/subdirectory/move_me.txt'

            watch(listener, 2, path) do
              mv 'a_directory/subdirectory/move_me.txt', 'b_directory/subdirectory'
            end
          end
        end
      end
    end

    context 'when a file is deleted' do
      it 'detects the file removal' do
        fixtures do |path|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path)
          end

          touch 'unnecessary.txt'

          watch(listener, 1, path) do
            rm 'unnecessary.txt'
          end
        end
      end

      context 'given a symlink', :unless => windows? do
        it 'detects the file removal' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include(path)
            end

            touch 'unnecessary.rb'
            ln_s  'unnecessary.rb', 'unnecessary_symlink.rb'

            watch(listener, 1, path) do
              rm 'unnecessary_symlink.rb'
            end
          end
        end
      end

      context 'given an existing directory' do
        it 'detects the file removal' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/a_directory")
            end

            mkdir 'a_directory'
            touch 'a_directory/do_not_use.rb'

            watch(listener, 1, path) do
              rm 'a_directory/do_not_use.rb'
            end
          end
        end
      end

      context 'given a directory with subdirectories' do
        it 'detects the file removal' do
          fixtures do |path|
            listener.should_receive(:on_change).once.with do |array|
              array.should include("#{path}/a_directory/subdirectory")
            end

            mkdir_p 'a_directory/subdirectory'
            touch   'a_directory/subdirectory/do_not_use.rb'

            watch(listener, 1, path) do
              rm 'a_directory/subdirectory/do_not_use.rb'
            end
          end
        end
      end
    end
  end

  context 'multiple file operations' do
    it 'detects the added files' do
      fixtures do |path|
        listener.should_receive(:on_change).once.with do |array|
          array.should include(path, "#{path}/a_directory")
        end

        watch(listener, 2, path) do
          touch 'a_file.rb'
          touch 'b_file.rb'
          mkdir 'a_directory'
          # Needed for INotify, because of :recursive rb-inotify custom flag?
          # Also needed for the osx adapter
          sleep 0.05
          touch 'a_directory/a_file.rb'
          touch 'a_directory/b_file.rb'
        end
      end
    end

    it 'detects the modified files' do
      fixtures do |path|
        listener.should_receive(:on_change).once.with do |array|
          array.should include(path, "#{path}/a_directory")
        end

        touch 'a_file.rb'
        touch 'b_file.rb'
        mkdir 'a_directory'
        touch 'a_directory/a_file.rb'
        touch 'a_directory/b_file.rb'

        watch(listener, 2, path) do
          touch 'b_file.rb'
          touch 'a_directory/a_file.rb'
        end
      end
    end

    it 'detects the removed files' do
      fixtures do |path|
        listener.should_receive(:on_change).once.with do |array|
          array.should include(path, "#{path}/a_directory")
        end

        touch 'a_file.rb'
        touch 'b_file.rb'
        mkdir 'a_directory'
        touch 'a_directory/a_file.rb'
        touch 'a_directory/b_file.rb'

        watch(listener, 2, path) do
          rm 'b_file.rb'
          rm 'a_directory/a_file.rb'
        end
      end
    end
  end

  context 'single directory operations' do
    it 'detects a moved directory' do
      fixtures do |path|
        listener.should_receive(:on_change).once.with do |array|
          array.should include(path)
        end

        mkdir 'a_directory'
        touch 'a_directory/a_file.rb'
        touch 'a_directory/b_file.rb'

        watch(listener, 1, path) do
          mv 'a_directory', 'renamed'
        end
      end
    end

    it 'detects a removed directory' do
      fixtures do |path|
        listener.should_receive(:on_change).once.with do |array|
          array.should include(path, "#{path}/a_directory")
        end

        mkdir 'a_directory'
        touch 'a_directory/a_file.rb'
        touch 'a_directory/b_file.rb'

        watch(listener, 2, path) do
          rm_rf 'a_directory'
        end
      end
    end
  end

  context "paused adapter" do
    context 'when a file is created' do
      it "doesn't detects the added file" do
        fixtures do |path|
          watch(listener, 1, path) do # The expected changes param is set to one!
            @adapter.paused = true
            touch 'new_file.rb'
          end
          @called.should be_nil
        end
      end
    end
  end

  context "when multiple directories are listened to" do
    context 'when files are added to one of multiple directories' do
      it 'detects added files' do
        fixtures(2) do |path1, path2|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path2)
          end

          watch(listener, 1, path1, path2) do
            touch "#{path2}/new_file.rb"
          end
        end
      end
    end

    context 'when files are added to multiple directories' do
      it 'detects added files' do
        fixtures(2) do |path1, path2|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path1, path2)
          end

          watch(listener, 2, path1, path2) do
            touch "#{path1}/new_file.rb"
            touch "#{path2}/new_file.rb"
          end
        end
      end
    end

    context 'given a new and an existing directory on multiple directories' do
      it 'detects the added file' do
        fixtures(2) do |path1, path2|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path2, "#{path1}/a_directory", "#{path2}/b_directory")
          end

          mkdir "#{path1}/a_directory"

          watch(listener, 3, path1, path2) do
            mkdir "#{path2}/b_directory"
            # Needed for INotify
            sleep 0.05
            touch "#{path1}/a_directory/new_file.rb"
            touch "#{path2}/b_directory/new_file.rb"
          end
        end
      end
    end

    context 'when a file is moved between the multiple watched directories' do
      it 'detects the movements of the file' do
        fixtures(3) do |path1, path2, path3|
          listener.should_receive(:on_change).once.with do |array|
            array.should include("#{path1}/from_directory", path2, "#{path3}/to_directory")
          end

          mkdir "#{path1}/from_directory"
          touch "#{path1}/from_directory/move_me.txt"
          mkdir "#{path3}/to_directory"

          watch(listener, 3, path1, path2, path3) do
            mv "#{path1}/from_directory/move_me.txt", "#{path2}/move_me.txt"
            mv "#{path2}/move_me.txt", "#{path3}/to_directory/move_me.txt"
          end
        end
      end
    end

    context 'when files are deleted from the multiple watched directories' do
      it 'detects the files removal' do
        fixtures(2) do |path1, path2|
          listener.should_receive(:on_change).once.with do |array|
            array.should include(path1, path2)
          end

          touch "#{path1}/unnecessary.txt"
          touch "#{path2}/unnecessary.txt"

          watch(listener, 2, path1, path2) do
            rm "#{path1}/unnecessary.txt"
            rm "#{path2}/unnecessary.txt"
          end
        end
      end
    end
  end
end
