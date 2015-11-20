require "rubysl/socket/version"
require "fcntl"

class SocketError < StandardError
end

# @todo   Socket#accept[_nonblock]
# @todo   UNIXServer#accept[_nonblock]
# @todo   UDPSocket#recvfrom

class BasicSocket < IO
  FFI = Rubinius::FFI

  class << self
    def from_descriptor(fixnum)
      sock = allocate()
      sock.from_descriptor(fixnum)
      return sock
    end

    alias :for_fd :from_descriptor
  end

  def from_descriptor(fixnum)
    IO.setup self, fixnum, nil, true
    return self
  end

  def self.do_not_reverse_lookup=(setting)
    @no_reverse_lookup = setting
  end

  def self.do_not_reverse_lookup
    @no_reverse_lookup = true unless defined? @no_reverse_lookup
    @no_reverse_lookup
  end

  def do_not_reverse_lookup=(setting)
    @no_reverse_lookup = setting
  end

  def do_not_reverse_lookup
    @no_reverse_lookup
  end

  def getsockopt(level, optname)
    data = Socket::Foreign.getsockopt(descriptor, level, optname)

    sockaddr = Socket::Foreign.getsockname(descriptor)
    family, = Socket::Foreign.getnameinfo sockaddr, Socket::Constants::NI_NUMERICHOST | Socket::Constants::NI_NUMERICSERV
    Socket::Option.new(family, level, optname, data)
  end

  def setsockopt(level_or_option, optname=nil, optval=nil)
    level = nil

    case level_or_option
    when Socket::Option
      if !optname.nil?
        raise ArgumentError, "given 2, expected 3"
      end
      level = level_or_option.level
      optname = level_or_option.optname
      optval = level_or_option.data
    else
      if level_or_option.nil? or optname.nil?
        nb_arg = 3 - [level_or_option, optname, optval].count(nil)
        raise ArgumentError, "given #{nb_arg}, expected 3"
      end
      level = level_or_option
    end

    optval = 1 if optval == true
    optval = 0 if optval == false

    error = 0

    sockname = Socket::Foreign.getsockname descriptor
    family = Socket::Foreign.getnameinfo(sockname).first

    level = level_arg(family, level)
    optname = optname_arg(level, optname)

    case optval
    when Fixnum then
      FFI::MemoryPointer.new :socklen_t do |val|
        val.write_int optval
        error = Socket::Foreign.setsockopt(descriptor, level,
                                           optname, val,
                                           val.total)
      end
    when String then
      FFI::MemoryPointer.new optval.bytesize do |val|
        val.write_string optval, optval.bytesize
        error = Socket::Foreign.setsockopt(descriptor, level,
                                           optname, val,
                                           optval.size)
      end
    else
      raise TypeError, "socket option should be a String, a Fixnum, true, or false"
    end

    Errno.handle "Unable to set socket option" unless error == 0

    return 0
  end

  def getsockname()
    return Socket::Foreign.getsockname(descriptor)
  end

  #
  # Obtain peername information for this socket.
  #
  # @see  Socket.getpeername
  #
  def getpeername()
    Socket::Foreign.getpeername @descriptor
  end

  #
  #
  #
  def send(message, flags, to = nil)
    connect to if to

    bytes = message.bytesize
    bytes_sent = 0

    FFI::MemoryPointer.new :char, bytes + 1 do |buffer|
      buffer.write_string message, bytes
      bytes_sent = Socket::Foreign.send(descriptor, buffer, bytes, flags)
      Errno.handle 'send(2)' if bytes_sent < 0
    end

    bytes_sent
  end

  def recvfrom(bytes_to_read, flags = 0)
    # FIXME 0 is knowledge from io.cpp
    return socket_recv(bytes_to_read, flags, 0)
  end

  def recv(bytes_to_read, flags = 0)
    # FIXME 0 is knowledge from io.cpp
    return socket_recv(bytes_to_read, flags, 0)
  end

  def close_read
    ensure_open

    # If we were only in readonly mode, close it all together
    if @mode & ACCMODE == RDONLY
      return close
    end

    # MRI doesn't check if shutdown worked, so we don't.
    Socket::Foreign.shutdown @descriptor, 0

    @mode = WRONLY

    nil
  end

  def close_write
    ensure_open

    # If we were only in writeonly mode, close it all together
    if @mode & ACCMODE == WRONLY
      return close
    end

    Socket::Foreign.shutdown @descriptor, 1

    # Mark it as read only
    @mode = RDONLY

    nil
  end

  #
  # Sets socket nonblocking and reads up to given number of bytes.
  #
  # @todo   Should EWOULDBLOCK be passed unchanged? --rue
  #
  def recv_nonblock(bytes_to_read, flags = 0)
    fcntl Fcntl::F_SETFL, Fcntl::O_NONBLOCK
    socket_recv bytes_to_read, flags, 0
  rescue Errno::EWOULDBLOCK
    raise Errno::EAGAIN
  end

  def shutdown(how = 2)
    err = Socket::Foreign.shutdown @descriptor, how
    Errno.handle "shutdown" unless err == 0
  end

  private

  def level_arg(family, level)
    case level
    when Symbol, String
      if Socket::Constants.const_defined?(level)
        Socket::Constants.const_get(level)
      else
        if is_ip_family?(family)
          ip_level_to_int(level)
        else
          unknown_level_to_int(level)
        end
      end
    else
      level
    end
  end

  def optname_arg(level, optname)
    case optname
    when Symbol, String
      if Socket::Constants.const_defined?(optname)
        Socket::Constants.const_get(optname)
      else
        case(level)
        when Socket::Constants::SOL_SOCKET
          constant("SO", optname)
        when Socket::Constants::IPPROTO_IP
          constant("IP", optname)
        when Socket::Constants::IPPROTO_TCP
          constant("TCP", optname)
        when Socket::Constants::IPPROTO_UDP
          constant("UDP", optname)
        else
          if Socket::Constants.const_defined?(Socket::Constants::IPPROTO_IPV6) &&
            level == Socket::Constants::IPPROTO_IPV6
            constant("IPV6", optname)
          else
            optname
          end
        end
      end
    else
      optname
    end
  end

  def is_ip_family?(family)
    family == "AF_INET" || family == "AF_INET6"
  end

  def ip_level_to_int(level)
    prefixes = ["IPPROTO", "SOL"]
    prefixes.each do |prefix|
      if Socket::Constants.const_defined?("#{prefix}_#{level}")
        return Socket::Constants.const_get("#{prefix}_#{level}")
      end
    end
  end

  def unknown_level_to_int(level)
    constant("SOL", level)
  end

  def constant(prefix, suffix)
    if Socket::Constants.const_defined?("#{prefix}_#{suffix}")
      Socket::Constants.const_get("#{prefix}_#{suffix}")
    end
  end

end

class Socket < BasicSocket
  FFI = Rubinius::FFI

  # @todo   Is omitting empty-value constants reasonable? --rue
  module Constants
    all_valid = FFI.config_hash("socket").reject {|name, value| value.empty? }

    all_valid.each {|name, value| const_set name, Integer(value) }

    # MRI compat. socket is a pretty screwed up API. All the constants in Constants
    # must also be directly accessible on Socket itself. This means it's not enough
    # to include Constants into Socket, because Socket#const_defined? must be able
    # to see constants like AF_INET6 directly on Socket, but #const_defined? doesn't
    # check inherited constants. O_o
    #
    all_valid.each {|name, value| Socket.const_set name, Integer(value) }


    afamilies = all_valid.to_a.select { |name,| name =~ /^AF_/ }
    afamilies.map! {|name, value| [value.to_i, name] }

    pfamilies = all_valid.to_a.select { |name,| name =~ /^PF_/ }
    pfamilies.map! {|name, value| [value.to_i, name] }

    AF_TO_FAMILY = Hash[*afamilies.flatten]
    PF_TO_FAMILY = Hash[*pfamilies.flatten]
  end

  module Foreign
    extend FFI::Library

    class Addrinfo < FFI::Struct
      config("rbx.platform.addrinfo", :ai_flags, :ai_family, :ai_socktype,
             :ai_protocol, :ai_addrlen, :ai_addr, :ai_canonname, :ai_next)
    end

    class Linger < FFI::Struct
      config("rbx.platform.linger", :l_onoff, :l_linger)
    end

    attach_function :_bind,    "bind", [:int, :pointer, :socklen_t], :int
    attach_function :_connect, "connect", [:int, :pointer, :socklen_t], :int

    attach_function :accept,   [:int, :pointer, :pointer], :int
    attach_function :close,    [:int], :int
    attach_function :shutdown, [:int, :int], :int
    attach_function :listen,   [:int, :int], :int
    attach_function :socket,   [:int, :int, :int], :int
    attach_function :send,     [:int, :pointer, :size_t, :int], :ssize_t
    attach_function :recv,     [:int, :pointer, :size_t, :int], :ssize_t
    attach_function :recvfrom, [:int, :pointer, :size_t, :int,
                                :pointer, :pointer], :int

    attach_function :_getsockopt,
                    "getsockopt", [:int, :int, :int, :pointer, :pointer], :int
    attach_function :_getaddrinfo,
                    "getaddrinfo", [:string, :string, :pointer, :pointer], :int

    attach_function :gai_strerror,  [:int], :string
    attach_function :setsockopt,    [:int, :int, :int, :pointer, :socklen_t], :int
    attach_function :freeaddrinfo,  [:pointer], :void
    attach_function :_getpeername,  "getpeername", [:int, :pointer, :pointer], :int
    attach_function :_getsockname,  "getsockname", [:int, :pointer, :pointer], :int

    attach_function :socketpair,    [:int, :int, :int, :pointer], :int

    attach_function :gethostname,   [:pointer, :size_t], :int
    attach_function :getservbyname, [:pointer, :pointer], :pointer

    attach_function :htons,         [:uint16_t], :uint16_t
    attach_function :ntohs,         [:uint16_t], :uint16_t

    attach_function :_getnameinfo,
                    "getnameinfo", [:pointer, :socklen_t, :pointer, :socklen_t,
                                    :pointer, :socklen_t, :int], :int

    def self.bind(descriptor, sockaddr)
      FFI::MemoryPointer.new :char, sockaddr.bytesize do |sockaddr_p|
        sockaddr_p.write_string sockaddr, sockaddr.bytesize

        _bind descriptor, sockaddr_p, sockaddr.bytesize
      end
    end

    def self.connect(descriptor, sockaddr)
      err = 0
      FFI::MemoryPointer.new :char, sockaddr.bytesize do |sockaddr_p|
        sockaddr_p.write_string sockaddr, sockaddr.bytesize

        err = _connect descriptor, sockaddr_p, sockaddr.bytesize
      end

      err
    end

    def self.getsockopt(descriptor, level, optname)
      FFI::MemoryPointer.new 256 do |val| # HACK magic number
        FFI::MemoryPointer.new :socklen_t do |length|
          length.write_int 256 # HACK magic number

          err = _getsockopt descriptor, level, optname, val, length

          Errno.handle "Unable to get socket option" unless err == 0

          return val.read_string(length.read_int)
        end
      end
    end

    def self.getaddrinfo(host, service = nil, family = nil, socktype = nil,  protocol = nil, flags = nil)
      hints = Addrinfo.new
      hints[:ai_family] = family || 0
      hints[:ai_socktype] = socktype || 0
      hints[:ai_protocol] = protocol || 0
      hints[:ai_flags] = flags || 0

      if host && (host.empty? || host == '<any>')
        host = "0.0.0.0"
      elsif host == '<broadcast>'
        host = '255.255.255.255'
      end

      res_p = FFI::MemoryPointer.new :pointer

      err = _getaddrinfo host, service, hints.pointer, res_p

      raise SocketError, gai_strerror(err) unless err == 0

      ptr = res_p.read_pointer

      return [] unless ptr

      res = Addrinfo.new ptr

      addrinfos = []

      while true
        addrinfo = []
        addrinfo << res[:ai_flags]
        addrinfo << res[:ai_family]
        addrinfo << res[:ai_socktype]
        addrinfo << res[:ai_protocol]
        addrinfo << res[:ai_addr].read_string(res[:ai_addrlen])
        addrinfo << res[:ai_canonname]

        addrinfos << addrinfo

        break unless res[:ai_next]

        res = Addrinfo.new res[:ai_next]
      end

      return addrinfos
    ensure
      hints.free if hints

      if res_p
        ptr = res_p.read_pointer

        # Be sure to feed a legit pointer to freeaddrinfo
        if ptr and !ptr.null?
          freeaddrinfo ptr
        end
        res_p.free
      end
    end

    def self.getaddress(host)
      addrinfos = getaddrinfo(host)
      unpack_sockaddr_in(addrinfos.first[4], false).first
    end

    def self.getnameinfo(sockaddr, flags = Socket::Constants::NI_NUMERICHOST | Socket::Constants::NI_NUMERICSERV,
                         reverse_lookup = !BasicSocket.do_not_reverse_lookup)
      name_info = []
      value = nil

      FFI::MemoryPointer.new :char, sockaddr.bytesize do |sockaddr_p|
        FFI::MemoryPointer.new :char, Socket::Constants::NI_MAXHOST do |node|
          FFI::MemoryPointer.new :char, Socket::Constants::NI_MAXSERV do |service|
            sockaddr_p.write_string sockaddr, sockaddr.bytesize

            if reverse_lookup then
              err = _getnameinfo(sockaddr_p, sockaddr.bytesize,
                                 node, Socket::Constants::NI_MAXHOST, nil, 0, 0)

              name_info[2] = node.read_string if err == 0
            end

            err = _getnameinfo(sockaddr_p, sockaddr.bytesize,
                               node, Socket::Constants::NI_MAXHOST,
                               service, Socket::Constants::NI_MAXSERV,
                               flags)

            unless err == 0 then
              raise SocketError, gai_strerror(err)
            end

            sa_family = SockAddr_In.new(sockaddr)[:sin_family]

            name_info[0] = Socket::Constants::AF_TO_FAMILY[sa_family]
            name_info[1] = service.read_string
            name_info[3] = node.read_string
          end
        end
      end

      name_info[2] = name_info[3] if name_info[2].nil?
      name_info
    end

    def self.getpeername(descriptor)
      FFI::MemoryPointer.new :char, 128 do |sockaddr_storage_p|
        FFI::MemoryPointer.new :socklen_t do |len_p|
          len_p.write_int 128

          err = _getpeername descriptor, sockaddr_storage_p, len_p

          Errno.handle 'getpeername(2)' unless err == 0

          sockaddr_storage_p.read_string len_p.read_int
        end
      end
    end

    def self.getsockname(descriptor)
      FFI::MemoryPointer.new :char, 128 do |sockaddr_storage_p|
        FFI::MemoryPointer.new :socklen_t do |len_p|
          len_p.write_int 128

          err = _getsockname descriptor, sockaddr_storage_p, len_p

          Errno.handle 'getsockname(2)' unless err == 0

          sockaddr_storage_p.read_string len_p.read_int
        end
      end
    end

    def self.pack_sockaddr_in(host, port, family, type, flags)
      hints = Addrinfo.new
      hints[:ai_family] = family
      hints[:ai_socktype] = type
      hints[:ai_flags] = flags

      if host && host.empty?
        host = "0.0.0.0"
      end

      res_p = FFI::MemoryPointer.new :pointer

      err = _getaddrinfo host, port.to_s, hints.pointer, res_p

      raise SocketError, gai_strerror(err) unless err == 0

      return [] if res_p.read_pointer.null?

      res = Addrinfo.new res_p.read_pointer

      return res[:ai_addr].read_string(res[:ai_addrlen])

    ensure
      hints.free if hints

      if res_p then
        ptr = res_p.read_pointer

        freeaddrinfo ptr if ptr and not ptr.null?

        res_p.free
      end
    end

    def self.unpack_sockaddr_in(sockaddr, reverse_lookup)
      family, port, host, ip = getnameinfo sockaddr, Socket::Constants::NI_NUMERICHOST | Socket::Constants::NI_NUMERICSERV, reverse_lookup
      # On some systems this doesn't fail for families other than AF_INET(6)
      # so we raise manually here.
      raise ArgumentError, 'not an AF_INET/AF_INET6 sockaddr' unless family =~ /AF_INET/
      return host, ip, port.to_i
    end
  end

  module ListenAndAccept
    include IO::Socketable

    def listen(backlog)
      backlog = Rubinius::Type.coerce_to backlog, Fixnum, :to_int

      err = Socket::Foreign.listen descriptor, backlog

      Errno.handle 'listen(2)' unless err == 0

      err
    end

    def accept
      raise IOError, "closed stream" if closed? # Truffle: comply with MRI

      fd = super

      socket = self.class.superclass.allocate
      IO.setup socket, fd, nil, true
      socket.binmode
      socket
    end

    #
    # Set nonblocking and accept.
    #
    def accept_nonblock
      raise IOError, "closed stream" if closed? # Truffle: comply with MRI

      fcntl(Fcntl::F_SETFL, Fcntl::O_NONBLOCK)

      fd = nil
      sockaddr = nil

      FFI::MemoryPointer.new 1024 do |sockaddr_p| # HACK from MRI
        FFI::MemoryPointer.new :int do |size_p|
          fd = Socket::Foreign.accept descriptor, sockaddr_p, size_p
        end
      end

      Errno.handle 'accept(2)' if fd < 0

      # TCPServer -> TCPSocket etc. *sigh*
      socket = self.class.superclass.allocate
      IO.setup socket, fd, nil, true
      socket
    end

  end

  include Socket::ListenAndAccept

  class SockAddr_In < FFI::Struct
    config("rbx.platform.sockaddr_in", :sin_family, :sin_port, :sin_addr, :sin_zero)

    def initialize(sockaddrin)
      @p = FFI::MemoryPointer.new sockaddrin.bytesize
      @p.write_string(sockaddrin, sockaddrin.bytesize)
      super(@p)
    end

    def to_s
      @p.read_string(@p.total)
    end

  end

  class Option
    attr_reader :family, :level, :optname, :data

    def self.bool(family, level, optname, bool)
      data = [(bool ? 1 : 0)].pack('i')
      new family, level, optname, data
    end

    def self.int(family, level, optname, integer)
      new family, level, optname, [integer].pack('i')
    end

    def self.linger(onoff, secs)
      linger = Socket::Foreign::Linger.new

      case onoff
      when Integer
        linger[:l_onoff] = onoff
      else
        linger[:l_onoff] = onoff ? 1 : 0
      end
      linger[:l_linger] = secs

      p = linger.to_ptr
      data = p.read_string(p.total)

      new :UNSPEC, :SOCKET, :LINGER, data
    end

    def initialize(family, level, optname, data)
      @family = family_arg(family)
      @family_name = family
      @level = level_arg(@family, level)
      @level_name = level
      @optname = optname_arg(@level, optname)
      @opt_name = optname
      @data = data
    end

    def unpack(template)
      @data.unpack template
    end

    def inspect
      "#<#{self.class}: #@family_name #@level_name #@opt_name #{@data.inspect}>"
    end

    def bool
      unless @data.length == Rubinius::FFI.type_size(:int)
        raise TypeError, "size differ. expected as sizeof(int)=" +
          "#{Rubinius::FFI.type_size(:int)} but #{@data.length}"
      end

      i = @data.unpack('i').first
      i == 0 ? false : true
    end

    def int
      unless @data.length == Rubinius::FFI.type_size(:int)
        raise TypeError, "size differ. expected as sizeof(int)=" +
          "#{Rubinius::FFI.type_size(:int)} but #{@data.length}"
      end
      @data.unpack('i').first
    end

    def linger
      if @level != Socket::SOL_SOCKET || @optname != Socket::SO_LINGER
        raise TypeError, "linger socket option expected"
      end
      if @data.bytesize != FFI.config("linger.sizeof")
        raise TypeError, "size differ. expected as sizeof(struct linger)=" +
          "#{FFI.config("linger.sizeof")} but #{@data.length}"
      end

      linger = Socket::Foreign::Linger.new
      linger.to_ptr.write_string @data, @data.bytesize

      onoff = nil
      case linger[:l_onoff]
      when 0 then onoff = false
      when 1 then onoff = true
      else onoff = linger[:l_onoff].to_i
      end

      [onoff, linger[:l_linger].to_i]
    end

    alias :to_s :data


    private

    def family_arg(family)
      case family
      when Symbol, String
        f = family.to_s
        if f[0..2] != 'AF_'
          f = 'AF_' + f
        end
        Socket.const_get f
      when Integer
        family
      else
        raise SocketError, "unknown socket domain: #{family}"
      end
    rescue NameError
      raise SocketError, "unknown socket domain: #{family}"
    end

    def level_arg(family, level)
      case level
      when Symbol, String
        if Socket::Constants.const_defined?(level)
          Socket::Constants.const_get(level)
        else
          if is_ip_family?(family)
            ip_level_to_int(level)
          else
            unknown_level_to_int(level)
          end
        end
      when Integer
        level
      else
        raise SocketError, "unknown protocol level: #{level}"
      end
    rescue NameError
      raise SocketError, "unknown protocol level: #{level}"
    end

    def optname_arg(level, optname)
      case optname
      when Symbol, String
        if Socket::Constants.const_defined?(optname)
          Socket::Constants.const_get(optname)
        else
          case(level)
          when Socket::Constants::SOL_SOCKET
            constant("SO", optname)
          when Socket::Constants::IPPROTO_IP
            constant("IP", optname)
          when Socket::Constants::IPPROTO_TCP
            constant("TCP", optname)
          when Socket::Constants::IPPROTO_UDP
            constant("UDP", optname)
          else
            if Socket::Constants.const_defined?(Socket::Constants::IPPROTO_IPV6) &&
                level == Socket::Constants::IPPROTO_IPV6
              constant("IPV6", optname)
            else
              optname
            end
          end
        end
      else
        optname
      end
    rescue NameError
      raise SocketError, "unknown socket level option name: #{optname}"
    end

    def is_ip_family?(family)
      [Socket::AF_INET, Socket::AF_INET6].include? family
    end

    def ip_level_to_int(level)
      prefixes = ["IPPROTO", "SOL"]
      prefixes.each do |prefix|
        if Socket::Constants.const_defined?("#{prefix}_#{level}")
          return Socket::Constants.const_get("#{prefix}_#{level}")
        end
      end
    end

    def unknown_level_to_int(level)
      constant("SOL", level)
    end

    def constant(prefix, suffix)
      #if Socket::Constants.const_defined?("#{prefix}_#{suffix}")
        Socket::Constants.const_get("#{prefix}_#{suffix}")
      #end
    end
  end

  # If we have the details to support unix sockets, do so.
  if FFI.config("sockaddr_un.sun_family.offset") and Socket::Constants.const_defined?(:AF_UNIX)
    class SockAddr_Un < FFI::Struct
      config("rbx.platform.sockaddr_un", :sun_family, :sun_path)

      def initialize(filename = nil)
        maxfnsize = self.size - (FFI.config("sockaddr_un.sun_family.size") + 1)

        if filename and filename.length > maxfnsize
          raise ArgumentError, "too long unix socket path (max: #{maxfnsize}bytes)"
        end
        @p = FFI::MemoryPointer.new self.size
        if filename
          @p.write_string( [Socket::AF_UNIX].pack("s") + filename )
        end
        super @p
      end

      def to_s
        @p.read_string self.size
      end
    end
  end

  def self.getaddrinfo(host, service, family = 0, socktype = 0,
                       protocol = 0, flags = 0)
    if service
      if service.kind_of? Fixnum
        service = service.to_s
      else
        service = StringValue(service)
      end
    end

    addrinfos = Socket::Foreign.getaddrinfo(host, service, family, socktype,
                                            protocol, flags)

    addrinfos.map do |ai|
      addrinfo = []
      addrinfo << Socket::Constants::AF_TO_FAMILY[ai[1]]

      sockaddr = Foreign.unpack_sockaddr_in ai[4], !BasicSocket.do_not_reverse_lookup

      addrinfo << sockaddr.pop # port
      addrinfo.concat sockaddr # hosts
      addrinfo << ai[1]
      addrinfo << ai[2]
      addrinfo << ai[3]
      addrinfo
    end
  end

  def self.getnameinfo(sockaddr, flags = 0)
    port   = nil
    host   = nil
    family = Socket::AF_UNSPEC
    if sockaddr.is_a?(Array)
      if sockaddr.size == 3
        af = sockaddr[0]
        port = sockaddr[1]
        host = sockaddr[2]
      elsif sockaddr.size == 4
        af = sockaddr[0]
        port = sockaddr[1]
        host = sockaddr[3] || sockaddr[2]
      else
        raise ArgumentError, "array size should be 3 or 4, #{sockaddr.size} given"
      end

      if family == "AF_INET"
        family = Socket::AF_INET
      elsif family == "AF_INET6"
        family = Socket::AF_INET6
      end
      sockaddr = Socket::Foreign.pack_sockaddr_in(host, port, family, Socket::SOCK_DGRAM, 0)
    end

    family, port, host, ip = Socket::Foreign.getnameinfo(sockaddr, flags)
    [host, port]
  end

  def self.gethostname
    FFI::MemoryPointer.new :char, 1024 do |mp|  #magic number 1024 comes from MRI
      Socket::Foreign.gethostname(mp, 1024) # same here
      return mp.read_string
    end
  end

  def self.gethostbyname(hostname)
    addrinfos = Socket.getaddrinfo(hostname, nil)

    hostname     = addrinfos.first[2]
    family       = addrinfos.first[4]
    addresses    = []
    alternatives = []
    addrinfos.each do |a|
      alternatives << a[2] unless a[2] == hostname
      # transform addresses to packed strings
      if a[4] == family
        sockaddr = Socket.sockaddr_in(1, a[3])
        if family == AF_INET
          # IPv4 address
          offset = FFI.config("sockaddr_in.sin_addr.offset")
          size = FFI.config("sockaddr_in.sin_addr.size")
          addresses << sockaddr.byteslice(offset, size)
        elsif family == AF_INET6
          # Ipv6 address
          offset = FFI.config("sockaddr_in6.sin6_addr.offset")
          size = FFI.config("sockaddr_in6.sin6_addr.size")
          addresses << sockaddr.byteslice(offset, size)
        else
          addresses << a[3]
        end
      end
    end

    [hostname, alternatives.uniq, family] + addresses.uniq
  end


  class Servent < FFI::Struct
    config("rbx.platform.servent", :s_name, :s_aliases, :s_port, :s_proto)

    def initialize(data)
      @p = FFI::MemoryPointer.new data.bytesize
      @p.write_string(data, data.bytesize)
      super(@p)
    end

    def to_s
      @p.read_string(size)
    end

  end

  def self.getservbyname(service, proto='tcp')
    FFI::MemoryPointer.new :char, service.length + 1 do |svc|
      FFI::MemoryPointer.new :char, proto.length + 1 do |prot|
        svc.write_string(service + "\0")
        prot.write_string(proto + "\0")
        fn = Socket::Foreign.getservbyname(svc, prot)

        raise SocketError, "no such service #{service}/#{proto}" if fn.nil?

        s = Servent.new(fn.read_string(Servent.size))
        return Socket::Foreign.ntohs(s[:s_port])
      end
    end
  end

  def self.pack_sockaddr_in(port, host, type = Socket::SOCK_DGRAM, flags = 0)
    Socket::Foreign.pack_sockaddr_in host, port, Socket::AF_UNSPEC, type, flags
  end

  def self.unpack_sockaddr_in(sockaddr)
    host, address, port = Socket::Foreign.unpack_sockaddr_in sockaddr, false

    return [port, address]
  rescue SocketError => e
    if e.message =~ /ai_family not supported/ then # HACK platform specific?
      raise ArgumentError, 'not an AF_INET/AF_INET6 sockaddr'
    else
      raise e
    end
  end

  def self.socketpair(domain, type, protocol, klass=self)
    if domain.kind_of? String
      if domain.prefix? "AF_" or domain.prefix? "PF_"
        begin
          domain = Socket::Constants.const_get(domain)
        rescue NameError
          raise SocketError, "unknown socket domain #{domani}"
        end
      else
        raise SocketError, "unknown socket domain #{domani}"
      end
    end

    type = get_socket_type(type)

    FFI::MemoryPointer.new :int, 2 do |mp|
      Socket::Foreign.socketpair(domain, type, protocol, mp)
      fd0, fd1 = mp.read_array_of_int(2)

      [ klass.from_descriptor(fd0), klass.from_descriptor(fd1) ]
    end
  end

  class << self
    alias_method :sockaddr_in, :pack_sockaddr_in
    alias_method :pair, :socketpair
  end

  # Only define these methods if we support unix sockets
  if self.const_defined?(:SockAddr_Un)
    def self.pack_sockaddr_un(file)
      SockAddr_Un.new(file).to_s
    end

    def self.unpack_sockaddr_un(addr)

      if addr.bytesize > FFI.config("sockaddr_un.sizeof")
        raise TypeError, "too long sockaddr_un - #{addr.bytesize} longer than #{FFI.config("sockaddr_un.sizeof")}"
      end

      struct = SockAddr_Un.new
      struct.pointer.write_string(addr, addr.bytesize)

      struct[:sun_path]
    end

    class << self
      alias_method :sockaddr_un, :pack_sockaddr_un
    end
  end

  def initialize(family, socket_type, protocol=0)
    @no_reverse_lookup = self.class.do_not_reverse_lookup
    family = self.class.get_protocol_family(family)
    socket_type = self.class.get_socket_type(socket_type)
    descriptor  = Socket::Foreign.socket family, socket_type, protocol

    Errno.handle 'socket(2)' if descriptor < 0

    IO.setup self, descriptor, nil, true
  end

  def bind(server_sockaddr)
    err = Socket::Foreign.bind(descriptor, server_sockaddr)
    Errno.handle 'bind(2)' unless err == 0
    err
  end

  # @todo  Should this be closing the descriptor? --rue
  def connect(sockaddr, extra=nil)
    if extra
      sockaddr = Socket.pack_sockaddr_in sockaddr, extra
    else
      sockaddr = StringValue(sockaddr)
    end

    status = Socket::Foreign.connect descriptor, sockaddr

    if status < 0
      begin
        Errno.handle "connect(2)"
      rescue Errno::EISCONN
        return 0
      end
    end

    return 0
  end

  def connect_nonblock(sockaddr)
    fcntl(Fcntl::F_SETFL, Fcntl::O_NONBLOCK)

    status = Socket::Foreign.connect descriptor, StringValue(sockaddr)
    if status < 0
      Errno.handle "connect(2)"
    end

    return status
  end

  def self.get_protocol_family(family)
    case family
    when Fixnum
      return family
    when String
      # do nothing
    when Symbol
      family = family.to_s
    else
      family = StringValue(family)
    end

    family = "PF_#{family}" unless family[0, 3] == "PF_"
    Socket::Constants.const_get family
  end

  def self.get_socket_type(type)
    if type.kind_of? String
      if type.prefix? "SOCK_"
        begin
          type = Socket::Constants.const_get(type)
        rescue NameError
          raise SocketError, "unknown socket type #{type}"
        end
      else
        raise SocketError, "unknown socket type #{type}"
      end
    end

    if type.kind_of? Symbol
      begin
        type = Socket::Constants.const_get("SOCK_#{type}")
      rescue NameError
        raise SocketError, "unknown socket type #{type}"
      end
    end

    type
  end
end

class UNIXSocket < BasicSocket
  include IO::TransferIO

  # Coding to the lowest standard here.
  def recvfrom(bytes_read, flags = 0)
    # FIXME 2 is hardcoded knowledge from io.cpp
    socket_recv(bytes_read, flags, 2)
  end

  def initialize(path)
    @no_reverse_lookup = self.class.do_not_reverse_lookup
    @path = path
    unix_setup
    @path = ""  # Client
  end

  def path
    unless @path
      sockaddr = Socket::Foreign.getsockname descriptor
      _, @path = sockaddr.unpack('SZ*')
    end

    return @path
  end

  def from_descriptor(fixnum)
    super
    @path = nil
  end

  def unix_setup(server = false)
    status = nil
    phase = 'socket(2)'
    sock = Socket::Foreign.socket Socket::Constants::AF_UNIX, Socket::Constants::SOCK_STREAM, 0

    Errno.handle phase if sock < 0

    IO.setup self, sock, 'r+', true

    sockaddr = Socket.pack_sockaddr_un(@path)

    if server then
      phase = 'bind(2)'
      status = Socket::Foreign.bind descriptor, sockaddr
    else
      phase = 'connect(2)'
      status = Socket::Foreign.connect descriptor, sockaddr
    end

    if status < 0 then
      close
      Errno.handle phase
    end

    if server then
      phase = 'listen(2)'
      status = Socket::Foreign.listen descriptor, 5
      if status < 0
        close
        Errno.handle phase
      end
    end

    return sock
  end
  private :unix_setup

  def addr
    sockaddr = Socket::Foreign.getsockname descriptor
    _, sock_path = sockaddr.unpack('SZ*')
    ["AF_UNIX", sock_path]
  end

  def peeraddr
    sockaddr = Socket::Foreign.getpeername descriptor
    _, sock_path = sockaddr.unpack('SZ*')
    ["AF_UNIX", sock_path]
  end

  def recv_io(klass=IO, mode=nil)
    begin
      fd = recv_fd
    rescue PrimitiveFailure
      raise SocketError, "file descriptor was not passed"
    end

    return fd unless klass

    if klass < BasicSocket
      klass.for_fd(fd)
    else
      klass.for_fd(fd, mode)
    end
  end

  class << self
    def socketpair(type=Socket::SOCK_STREAM, protocol=0)
      Socket.socketpair(Socket::PF_UNIX, type, protocol, self)
    end

    alias_method :pair, :socketpair
  end

end

class UNIXServer < UNIXSocket

  include Socket::ListenAndAccept

  def initialize(path)
    @no_reverse_lookup = self.class.do_not_reverse_lookup
    @path = path
    unix_setup(true)
  end
end

class IPSocket < BasicSocket

  def self.getaddress(host)
    Socket::Foreign.getaddress host
  end

  def addr(reverse_lookup=nil)
    sockaddr = Socket::Foreign.getsockname descriptor

    reverse_lookup = !do_not_reverse_lookup if reverse_lookup.nil?

    family, port, host, ip = Socket::Foreign.getnameinfo sockaddr, Socket::Constants::NI_NUMERICHOST | Socket::Constants::NI_NUMERICSERV, reverse_lookup
    [family, port.to_i, host, ip]
  end

  def peeraddr(reverse_lookup=nil)
    sockaddr = Socket::Foreign.getpeername descriptor

    reverse_lookup = !do_not_reverse_lookup if reverse_lookup.nil?

    family, port, host, ip = Socket::Foreign.getnameinfo sockaddr, Socket::Constants::NI_NUMERICHOST | Socket::Constants::NI_NUMERICSERV, reverse_lookup
    [family, port.to_i, host, ip]
  end

  def recvfrom(maxlen, flags = 0)
    # FIXME 1 is hardcoded knowledge from io.cpp
    flags = 0 if flags.nil?
    socket_recv maxlen, flags, 1
  end

  def recvfrom_nonblock(maxlen, flags = 0)
    # Set socket to non-blocking, if we can
    # Todo: Ensure this works in Windows!  If not, I claim that's Fcntl's fault.
    fcntl(Fcntl::F_SETFL, Fcntl::O_NONBLOCK)
    flags = 0 if flags.nil?
    flags |= Socket::MSG_DONTWAIT

    # Wait until we have something to read
    # @todo  Why? ^^ --rue
    IO.select([self])
    return recvfrom(maxlen, flags)
  end
end

class UDPSocket < IPSocket
  FFI = Rubinius::FFI

  def initialize(socktype = Socket::AF_INET)
    @no_reverse_lookup = self.class.do_not_reverse_lookup
    @socktype = socktype
    status = Socket::Foreign.socket @socktype,
                                    Socket::SOCK_DGRAM,
                                    Socket::IPPROTO_UDP
    Errno.handle 'socket(2)' if status < 0

    IO.setup self, status, nil, true
  end

  def bind(host, port)
    @host = host.to_s if host
    @port = port.to_s if port

    addrinfos = Socket::Foreign.getaddrinfo(@host,
                                           @port,
                                           @socktype,
                                           Socket::SOCK_DGRAM, 0,
                                           Socket::AI_PASSIVE)

    status = -1

    addrinfos.each do |addrinfo|
      flags, family, socket_type, protocol, sockaddr, canonname = addrinfo

      status = Socket::Foreign.bind descriptor, sockaddr

      break if status >= 0
    end

    if status < 0
      Errno.handle 'bind(2)'
    end

    status
  end

  def connect(host, port)
    sockaddr = Socket::Foreign.pack_sockaddr_in host, port, @socktype, Socket::SOCK_DGRAM, 0

    syscall = 'connect(2)'
    status = Socket::Foreign.connect descriptor, sockaddr

    if status < 0
      Errno.handle syscall
    end

    0
  end

  def send(message, flags, *to)
    connect *to unless to.empty?

    bytes = message.bytesize
    bytes_sent = 0

    FFI::MemoryPointer.new :char, bytes + 1 do |buffer|
      buffer.write_string message, bytes
      bytes_sent = Socket::Foreign.send(descriptor, buffer, bytes, flags)
      Errno.handle 'send(2)' if bytes_sent < 0
    end

    bytes_sent
  end

  def inspect
    "#<#{self.class}:0x#{object_id.to_s(16)} #{@host}:#{@port}>"
  end

end

class TCPSocket < IPSocket
  FFI = Rubinius::FFI

  def self.gethostbyname(hostname)
    addrinfos = Socket.getaddrinfo(hostname, nil)

    hostname     = addrinfos.first[2]
    family       = addrinfos.first[4]
    addresses    = []
    alternatives = []
    addrinfos.each do |a|
      alternatives << a[2] unless a[2] == hostname
      addresses    << a[3] if a[4] == family
    end

    [hostname, alternatives.uniq, family] + addresses.uniq
  end

  #
  # @todo   Is it correct to ignore the to? If not, does
  #         the socket need to be reconnected? --rue
  #
  def send(bytes_to_read, flags, to = nil)
    super(bytes_to_read, flags)
  end


  def initialize(host, port, local_host=nil, local_service=nil)
    @no_reverse_lookup = self.class.do_not_reverse_lookup
    @host = host
    @port = port

    tcp_setup @host, @port, local_host, local_service
  end

  def tcp_setup(remote_host, remote_service, local_host = nil,
                local_service = nil, server = false)
    status = nil
    syscall = nil
    remote_host    = StringValue(remote_host)    if remote_host
    if remote_service
      if remote_service.kind_of? Fixnum
        remote_service = remote_service.to_s
      else
        remote_service = StringValue(remote_service)
      end
    end

    flags = server ? Socket::AI_PASSIVE : 0
    @remote_addrinfo = Socket::Foreign.getaddrinfo(remote_host,
                                                   remote_service,
                                                   Socket::AF_UNSPEC,
                                                   Socket::SOCK_STREAM, 0,
                                                   flags)

    if server == false and (local_host or local_service)
      local_host    = local_host.to_s    if local_host
      local_service = local_service.to_s if local_service
      @local_addrinfo = Socket::Foreign.getaddrinfo(local_host,
                                                    local_service,
                                                    Socket::AF_UNSPEC,
                                                    Socket::SOCK_STREAM, 0, 0)
    end

    sock = nil

    @remote_addrinfo.each do |addrinfo|
      flags, family, socket_type, protocol, sockaddr, canonname = addrinfo

      sock = Socket::Foreign.socket family, socket_type, protocol
      syscall = 'socket(2)'

      next if sock < 0

      if server
        FFI::MemoryPointer.new :socklen_t do |val|
          val.write_int 1
          level = Socket::Constants::SOL_SOCKET
          optname = Socket::Constants::SO_REUSEADDR
          error = Socket::Foreign.setsockopt(sock, level,
                                             optname, val,
                                             val.total)
          # Don't check error because if this fails, we just continue
          # anyway.
        end

        status = Socket::Foreign.bind sock, sockaddr
        syscall = 'bind(2)'
      else
        if @local_addrinfo
          # Pick a local_addrinfo for the family and type of
          # the remote side
          li = @local_addrinfo.find do |i|
            i[1] == family && i[2] == socket_type
          end

          if li
            status = Socket::Foreign.bind sock, li[4]
            syscall = 'bind(2)'
          else
            status = 1
          end
        else
          status = 1
        end

        if status >= 0
          status = Socket::Foreign.connect sock, sockaddr
          syscall = 'connect(2)'
        end
      end

      if status < 0
        Socket::Foreign.close sock
      else
        break
      end
    end

    if status < 0
      Errno.handle syscall
    end

    if server
      err = Socket::Foreign.listen sock, 5
      unless err == 0
        Socket::Foreign.close sock
        Errno.handle syscall
      end
    end

    # Only setup once we have found a socket we can use. Otherwise
    # because we manually close a socket fd, we can create an IO fd
    # alias condition which causes EBADF because when an IO is finalized
    # and it's fd has been closed underneith it, we close someone elses
    # fd!
    IO.setup self, sock, nil, true
  end
  private :tcp_setup

  def from_descriptor(descriptor)
    IO.setup self, descriptor, nil, true

    self
  end
end

class TCPServer < TCPSocket

  include Socket::ListenAndAccept

  def initialize(host, port = nil)
    @no_reverse_lookup = self.class.do_not_reverse_lookup

    if Fixnum === host and port.nil? then
      port = host
      host = nil
    end

    if String === host and port.nil? then
      port = Integer(host)
      host = nil
    end

    port = StringValue port unless port.kind_of? Fixnum

    @host = host
    @port = port

    tcp_setup @host, @port, nil, nil, true
  end

end
