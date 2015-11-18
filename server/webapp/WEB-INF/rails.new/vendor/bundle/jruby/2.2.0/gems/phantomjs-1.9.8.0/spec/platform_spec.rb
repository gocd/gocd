require 'spec_helper'

describe Phantomjs::Platform do
  before(:each) { Phantomjs.reset! }
  describe "with a system install present" do
    describe "#system_phantomjs_installed?" do
      it "is true when the system version matches Phantomjs.version" do
        Phantomjs::Platform.should_receive(:system_phantomjs_version).and_return(Phantomjs.version)
        expect(Phantomjs::Platform.system_phantomjs_installed?).to be_true
      end

      it "is false when the system version does not match Phantomjs.version" do
        Phantomjs::Platform.should_receive(:system_phantomjs_version).and_return('1.2.3')
        expect(Phantomjs::Platform.system_phantomjs_installed?).to be_false
      end

      it "is false when there's no system version" do
        Phantomjs::Platform.should_receive(:system_phantomjs_version).and_return(nil)
        expect(Phantomjs::Platform.system_phantomjs_installed?).to be_false
      end
    end
  end

  describe "on a 64 bit linux" do
    before do
      Phantomjs::Platform.stub(:host_os).and_return('linux-gnu')
      Phantomjs::Platform.stub(:architecture).and_return('x86_64')
    end

    it "reports the Linux64 Platform as useable" do
      Phantomjs::Platform::Linux64.should be_useable
    end

    describe "without system install" do
      before(:each) { Phantomjs::Platform.stub(:system_phantomjs_version).and_return(nil) }

      it "returns the correct phantom js executable path for the platform" do
        Phantomjs.path.should =~ /x86_64-linux\/bin\/phantomjs$/
      end
    end

    describe "with system install" do
      before(:each) do
        Phantomjs::Platform.stub(:system_phantomjs_version).and_return(Phantomjs.version)
        Phantomjs::Platform.stub(:system_phantomjs_path).and_return('/tmp/path')
      end

      it "returns the correct phantom js executable path for the platform" do
        expect(Phantomjs.path).to be == '/tmp/path'
      end
    end

    it "reports the Linux32 platform as unuseable" do
      Phantomjs::Platform::Linux32.should_not be_useable
    end

    it "reports the Darwin platform as unuseable" do
      Phantomjs::Platform::OsX.should_not be_useable
    end

    it "reports the Win32 Platform as unuseable" do
      Phantomjs::Platform::Win32.should_not be_useable
    end
  end

  describe "on a 32 bit linux" do
    before do
      Phantomjs::Platform.stub(:host_os).and_return('linux-gnu')
      Phantomjs::Platform.stub(:architecture).and_return('x86_32')
    end

    it "reports the Linux32 Platform as useable" do
      Phantomjs::Platform::Linux32.should be_useable
    end

    it "reports another Linux32 Platform as useable" do
      Phantomjs::Platform.stub(:host_os).and_return('linux-gnu')
      Phantomjs::Platform.stub(:architecture).and_return('i686')
      Phantomjs::Platform::Linux32.should be_useable
    end

    describe "without system install" do
      before(:each) { Phantomjs::Platform.stub(:system_phantomjs_version).and_return(nil) }

      it "returns the correct phantom js executable path for the platform" do
        Phantomjs.path.should =~ /x86_32-linux\/bin\/phantomjs$/
      end
    end

    describe "with system install" do
      before(:each) do
        Phantomjs::Platform.stub(:system_phantomjs_version).and_return(Phantomjs.version)
        Phantomjs::Platform.stub(:system_phantomjs_path).and_return('/tmp/path')
      end

      it "returns the correct phantom js executable path for the platform" do
        expect(Phantomjs.path).to be == '/tmp/path'
      end
    end

    it "reports the Linux64 platform as unuseable" do
      Phantomjs::Platform::Linux64.should_not be_useable
    end

    it "reports the Darwin platform as unuseable" do
      Phantomjs::Platform::OsX.should_not be_useable
    end

    it "reports the Win32 Platform as unuseable" do
      Phantomjs::Platform::Win32.should_not be_useable
    end
  end

  describe "on OS X" do
    before do
      Phantomjs::Platform.stub(:host_os).and_return('darwin')
      Phantomjs::Platform.stub(:architecture).and_return('x86_64')
    end

    it "reports the Darwin platform as useable" do
      Phantomjs::Platform::OsX.should be_useable
    end

    describe "without system install" do
      before(:each) { Phantomjs::Platform.stub(:system_phantomjs_version).and_return(nil) }

      it "returns the correct phantom js executable path for the platform" do
        Phantomjs.path.should =~ /darwin\/bin\/phantomjs$/
      end
    end

    describe "with system install" do
      before(:each) do
        Phantomjs::Platform.stub(:system_phantomjs_version).and_return(Phantomjs.version)
        Phantomjs::Platform.stub(:system_phantomjs_path).and_return('/tmp/path')
      end

      it "returns the correct phantom js executable path for the platform" do
        expect(Phantomjs.path).to be == '/tmp/path'
      end
    end

    it "reports the Linux32 Platform as unuseable" do
      Phantomjs::Platform::Linux32.should_not be_useable
    end

    it "reports the Linux64 platform as unuseable" do
      Phantomjs::Platform::Linux64.should_not be_useable
    end

    it "reports the Win32 Platform as unuseable" do
      Phantomjs::Platform::Win32.should_not be_useable
    end
  end

  describe "on Windows" do
    before do
      Phantomjs::Platform.stub(:host_os).and_return('mingw32')
      Phantomjs::Platform.stub(:architecture).and_return('i686')
    end

    describe "without system install" do
      before(:each) { Phantomjs::Platform.stub(:system_phantomjs_version).and_return(nil) }

      it "returns the correct phantom js executable path for the platform" do
        Phantomjs.path.should =~ /win32\/phantomjs.exe$/
      end
    end

    describe "with system install" do
      before(:each) do
        Phantomjs::Platform.stub(:system_phantomjs_version).and_return(Phantomjs.version)
        Phantomjs::Platform.stub(:system_phantomjs_path).and_return("#{ENV['TEMP']}/path")
      end

      it "returns the correct phantom js executable path for the platform" do
        expect(Phantomjs.path).to be == "#{ENV['TEMP']}/path"
      end
    end

    it "reports the Darwin platform as unuseable" do
      Phantomjs::Platform::OsX.should_not be_useable
    end

    it "reports the Linux32 Platform as unuseable" do
      Phantomjs::Platform::Linux32.should_not be_useable
    end

    it "reports the Linux64 platform as unuseable" do
      Phantomjs::Platform::Linux64.should_not be_useable
    end
  end

  describe 'on an unknown platform' do
    before do
      Phantomjs::Platform.stub(:host_os).and_return('foobar')
    end

    it "raises an UnknownPlatform error" do
      -> { Phantomjs.platform }.should raise_error(Phantomjs::UnknownPlatform)
    end
  end
end
