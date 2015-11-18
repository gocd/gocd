require 'ffi'

module PTY
  private
  module LibUtil
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    # forkpty(3) is in libutil on linux and BSD, libc on MacOS
    if FFI::Platform.linux? || (FFI::Platform.bsd? && !FFI::Platform.mac?)
      ffi_lib 'libutil'
    end
    attach_function :forkpty, [ :buffer_out, :buffer_out, :buffer_in, :buffer_in ], :pid_t
  end
  module LibC
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    attach_function :close, [ :int ], :int
    attach_function :strerror, [ :int ], :string
    attach_function :execv, [ :string, :buffer_in ], :int
    attach_function :execvp, [ :string, :buffer_in ], :int
    attach_function :dup2, [ :int, :int ], :int
    attach_function :dup, [ :int ], :int
    attach_function :_exit, [ :int ], :void
  end
  Buffer = FFI::Buffer
  def self.build_args(args)
    cmd = args.shift
    cmd_args = args.map do |arg|
      FFI::MemoryPointer.from_string(arg)
    end
    exec_args = FFI::MemoryPointer.new(:pointer, 1 + cmd_args.length + 1)
    exec_cmd = FFI::MemoryPointer.from_string(cmd)
    exec_args[0].put_pointer(0, exec_cmd)
    cmd_args.each_with_index do |arg, i|
      exec_args[i + 1].put_pointer(0, arg)
    end
    [ cmd, exec_args ]
  end
  public
  def self.getpty(*args)
    mfdp = Buffer.alloc_out :int
    name = Buffer.alloc_out 1024
    exec_cmd, exec_args = build_args(args)
    pid = LibUtil.forkpty(mfdp, name, nil, nil)
    #
    # We want to do as little as possible in the child process, since we're running
    # without any GC thread now, so test for the child case first
    #
    if pid == 0
      LibC.execvp(exec_cmd, exec_args)
      LibC._exit(1)
    end
    raise "forkpty failed: #{LibC.strerror(FFI.errno)}" if pid < 0
    masterfd = mfdp.get_int(0)
    rfp = FFI::IO.for_fd(masterfd, "r")
    wfp = FFI::IO.for_fd(LibC.dup(masterfd), "w")
    if block_given?
      retval = yield rfp, wfp, pid
      begin; rfp.close; rescue Exception; end
      begin; wfp.close; rescue Exception; end
      retval
    else
      [ rfp, wfp, pid ]
    end
  end
  def self.spawn(*args, &block)
    self.getpty("/bin/sh", "-c", args[0], &block)
  end
end