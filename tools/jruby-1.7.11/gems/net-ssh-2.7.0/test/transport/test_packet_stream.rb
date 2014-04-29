# encoding: ASCII-8BIT

require 'common'
require 'net/ssh/transport/packet_stream'

module Transport

  class TestPacketStream < Test::Unit::TestCase
    include Net::SSH::Transport::Constants

    def test_client_name_when_getnameinfo_works
      stream.expects(:getsockname).returns(:sockaddr)
      Socket.expects(:getnameinfo).with(:sockaddr, Socket::NI_NAMEREQD).returns(["net.ssh.test"])
      assert_equal "net.ssh.test", stream.client_name
    end

    def test_client_name_when_getnameinfo_fails_first_and_then_works
      stream.expects(:getsockname).returns(:sockaddr)
      Socket.expects(:getnameinfo).with(:sockaddr, Socket::NI_NAMEREQD).raises(SocketError)
      Socket.expects(:getnameinfo).with(:sockaddr).returns(["1.2.3.4"])
      assert_equal "1.2.3.4", stream.client_name
    end

    def test_client_name_when_getnameinfo_fails_but_gethostbyname_works
      stream.expects(:getsockname).returns(:sockaddr)
      Socket.expects(:getnameinfo).with(:sockaddr, Socket::NI_NAMEREQD).raises(SocketError)
      Socket.expects(:getnameinfo).with(:sockaddr).raises(SocketError)
      Socket.expects(:gethostname).returns(:hostname)
      Socket.expects(:gethostbyname).with(:hostname).returns(["net.ssh.test"])
      assert_equal "net.ssh.test", stream.client_name
    end

    def test_client_name_when_getnameinfo_and_gethostbyname_all_fail
      stream.expects(:getsockname).returns(:sockaddr)
      Socket.expects(:getnameinfo).with(:sockaddr, Socket::NI_NAMEREQD).raises(SocketError)
      Socket.expects(:getnameinfo).with(:sockaddr).raises(SocketError)
      Socket.expects(:gethostname).returns(:hostname)
      Socket.expects(:gethostbyname).with(:hostname).raises(SocketError)
      assert_equal "unknown", stream.client_name
    end

    def test_peer_ip_should_query_socket_for_info_about_peer
      stream.expects(:getpeername).returns(:sockaddr)
      Socket.expects(:getnameinfo).with(:sockaddr, Socket::NI_NUMERICHOST | Socket::NI_NUMERICSERV).returns(["1.2.3.4"])
      assert_equal "1.2.3.4", stream.peer_ip
    end

    def test_available_for_read_should_return_nontrue_when_select_fails
      IO.expects(:select).returns(nil)
      assert !stream.available_for_read?
    end

    def test_available_for_read_should_return_nontrue_when_self_is_not_ready
      IO.expects(:select).with([stream], nil, nil, 0).returns([[],[],[]])
      assert !stream.available_for_read?
    end

    def test_available_for_read_should_return_true_when_self_is_ready
      IO.expects(:select).with([stream], nil, nil, 0).returns([[self],[],[]])
      assert stream.available_for_read?
    end

    def test_cleanup_should_delegate_cleanup_to_client_and_server_states
      stream.client.expects(:cleanup)
      stream.server.expects(:cleanup)
      stream.cleanup
    end

    def test_if_needs_rekey_should_not_yield_if_neither_client_nor_server_states_need_rekey
      stream.if_needs_rekey? { flunk "shouldn't need rekey" }
      assert(true)
    end

    def test_if_needs_rekey_should_yield_and_cleanup_if_client_needs_rekey
      stream.client.stubs(:needs_rekey?).returns(true)
      stream.client.expects(:reset!)
      stream.server.expects(:reset!).never
      rekeyed = false
      stream.if_needs_rekey? { rekeyed = true }
      assert(rekeyed)
    end

    def test_if_needs_rekey_should_yield_and_cleanup_if_server_needs_rekey
      stream.server.stubs(:needs_rekey?).returns(true)
      stream.server.expects(:reset!)
      stream.client.expects(:reset!).never
      rekeyed = false
      stream.if_needs_rekey? { rekeyed = true }
      assert(rekeyed)
    end

    def test_if_needs_rekey_should_yield_and_cleanup_if_both_need_rekey
      stream.server.stubs(:needs_rekey?).returns(true)
      stream.client.stubs(:needs_rekey?).returns(true)
      stream.server.expects(:reset!)
      stream.client.expects(:reset!)
      rekeyed = false
      stream.if_needs_rekey? { rekeyed = true }
      assert(rekeyed)
    end

    def test_next_packet_should_not_block_by_default
      IO.expects(:select).returns(nil)
      assert_nothing_raised do
        timeout(1) { stream.next_packet }
      end
    end

    def test_next_packet_should_return_nil_when_non_blocking_and_not_ready
      IO.expects(:select).returns(nil)
      assert_nil stream.next_packet(:nonblock)
    end

    def test_next_packet_should_return_nil_when_non_blocking_and_partial_read
      IO.expects(:select).returns([[stream]])
      stream.expects(:recv).returns([8].pack("N"))
      assert_nil stream.next_packet(:nonblock)
      assert !stream.read_buffer.empty?
    end

    def test_next_packet_should_return_packet_when_non_blocking_and_full_read
      IO.expects(:select).returns([[stream]])
      stream.expects(:recv).returns(packet)
      packet = stream.next_packet(:nonblock)
      assert_not_nil packet
      assert_equal DEBUG, packet.type
    end

    def test_next_packet_should_eventually_return_packet_when_non_blocking_and_partial_read
      IO.stubs(:select).returns([[stream]])
      stream.stubs(:recv).returns(packet[0,10], packet[10..-1])
      assert_nil stream.next_packet(:nonblock)
      packet = stream.next_packet(:nonblock)
      assert_not_nil packet
      assert_equal DEBUG, packet.type
    end

    def test_next_packet_should_block_when_requested_until_entire_packet_is_available
      IO.stubs(:select).returns([[stream]])
      stream.stubs(:recv).returns(packet[0,10], packet[10,20], packet[20..-1])
      packet = stream.next_packet(:block)
      assert_not_nil packet
      assert_equal DEBUG, packet.type
    end

    def test_next_packet_when_blocking_should_fail_when_fill_could_not_read_any_data
      IO.stubs(:select).returns([[stream]])
      stream.stubs(:recv).returns("")
      assert_raises(Net::SSH::Disconnect) { stream.next_packet(:block) }
    end

    def test_next_packet_fails_with_invalid_argument
      assert_raises(ArgumentError) { stream.next_packet("invalid") }
    end

    def test_send_packet_should_enqueue_and_send_data_immediately
      stream.expects(:send).times(3).with { |a,b| a == stream.write_buffer && b == 0 }.returns(15)
      IO.expects(:select).times(2).returns([[], [stream]])
      stream.send_packet(ssh_packet)
      assert !stream.pending_write?
    end

    def test_enqueue_short_packet_should_ensure_packet_is_at_least_16_bytes_long
      packet = Net::SSH::Buffer.from(:byte, 0)
      stream.enqueue_packet(packet)
      # 12 originally, plus the block-size (8), plus the 4-byte length field
      assert_equal 24, stream.write_buffer.length
    end

    def test_enqueue_utf_8_packet_should_ensure_packet_length_is_in_bytes_and_multiple_of_block_length
      packet = Net::SSH::Buffer.from(:string, "\u2603") # Snowman is 3 bytes
      stream.enqueue_packet(packet)
      # When bytesize is measured wrong using length, the result is off by 2.
      # With length instead of bytesize, you get 26 length buffer.
      assert_equal 0, stream.write_buffer.length % 8
    end

    PACKETS = {
      "3des-cbc" => {
        "hmac-md5" => {
          false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, \000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, \000\032w\312\t\306\374\271\345p\215\224",
          :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, \004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, \004\a\200\n\004\202z\270\236\261\330m",
          :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, ",
          :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200",
        },
      },
      "aes128-cbc" => {
        "hmac-md5" => {
          false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251-\345\b\025\242#\336P8\343\361\263\\\241\326\311",
        },
        "hmac-md5-96" => {
          false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251-\345\b\025\242#\336P8\343\361\263",
        },
        "hmac-sha1" => {
          false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251yC\272\314@\301\n\346$\223\367\r\026\366\375(i'\212\351",
        },
        "hmac-sha1-96" => {
          false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251yC\272\314@\301\n\346$\223\367\r",
        },
        "hmac-ripemd160" => {
          false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NYF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NYF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "none" => {
          false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY",
          :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251",
        },
      },
      "aes192-cbc" => {
        "hmac-md5" => {
          false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320D-\345\b\025\242#\336P8\343\361\263\\\241\326\311",
        },
        "hmac-md5-96" => {
          false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320D-\345\b\025\242#\336P8\343\361\263",
        },
        "hmac-sha1" => {
          false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320DyC\272\314@\301\n\346$\223\367\r\026\366\375(i'\212\351",
        },
        "hmac-sha1-96" => {
          false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320DyC\272\314@\301\n\346$\223\367\r",
        },
        "hmac-ripemd160" => {
          false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320D\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320D\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "none" => {
          false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003",
          :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320D",
        },
      },
      "aes256-cbc" => {
        "hmac-md5" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365-\345\b\025\242#\336P8\343\361\263\\\241\326\311",
        },
        "hmac-md5-96" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365-\345\b\025\242#\336P8\343\361\263",
        },
        "hmac-sha1" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365yC\272\314@\301\n\346$\223\367\r\026\366\375(i'\212\351",
        },
        "hmac-sha1-96" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365yC\272\314@\301\n\346$\223\367\r",
        },
        "hmac-ripemd160" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "none" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365",
        },
      },
      "blowfish-cbc" => {
        "hmac-md5" => {
          false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)",
          :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006",
        },
      },
      "cast128-cbc" => {
        "hmac-md5" => {
          false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376",
          :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325",
        },
      },
      "idea-cbc" => {
        "hmac-md5" => {
          false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 HF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 HF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H",
          :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317",
        },
      },
      "arcfour128" => {
        "hmac-md5" => {
          false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255",
          :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224",
        },
      },
      "arcfour256" => {
        "hmac-md5" => {
          false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004frF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004frF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr",
          :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221",
        },
      },
      "arcfour512" => {
        "hmac-md5" => {
          false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330DF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330DF\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D",
          :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313",
        },
      },
      "camellia128-cbc@openssh.org" => {
        "hmac-md5" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9DF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9DF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13",
        },
      },
      "camellia192-cbc@openssh.org" => {
        "hmac-md5" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83EF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83EF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12",
        },
      },
      "camellia256-cbc@openssh.org" => {
        "hmac-md5" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi",
        },
      },
      "camellia128-cbc" => {
        "hmac-md5" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9DF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9DF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D",
          :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13",
        },
      },
      "camellia192-cbc" => {
        "hmac-md5" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83EF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83EF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E",
          :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12",
        },
      },
      "camellia256-cbc" => {
        "hmac-md5" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|",
          :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi",
        },
      },
      "3des-ctr" => {
        "hmac-md5" => {
          false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B\xEC=g\xF1\xB9[E\xB5\x8F\xCE\xCC\b\x82\x9D\x96\xDC",
        },
        "hmac-md5-96" => {
          false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B\xEC=g\xF1\xB9[E\xB5\x8F\xCE\xCC\b",
        },
        "hmac-sha1" => {
          false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B\x897U\xB6\xE44(\x9D\x1C\x13\xFF\xFE\xDD\xC1\xABrI\x8DW\xC9",
        },
        "hmac-sha1-96" => {
          false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B\x897U\xB6\xE44(\x9D\x1C\x13\xFF\xFE",
        },
        "hmac-ripemd160" => {
          false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B)U\xBD\x03U\xDB\x95\x91Y)\xCF\xAE\xA0\xA6\x000\xE9\x1A\xF3Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B)U\xBD\x03U\xDB\x95\x91Y)\xCF\xAE\xA0\xA6\x000\xE9\x1A\xF3Y",
        },
        "none" => {
          false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1",
          :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B",
        },
      },
      "blowfish-ctr" => {
        "hmac-md5" => {
          false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9\xEC=g\xF1\xB9[E\xB5\x8F\xCE\xCC\b\x82\x9D\x96\xDC",
        },
        "hmac-md5-96" => {
          false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9\xEC=g\xF1\xB9[E\xB5\x8F\xCE\xCC\b",
        },
        "hmac-sha1" => {
          false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9\x897U\xB6\xE44(\x9D\x1C\x13\xFF\xFE\xDD\xC1\xABrI\x8DW\xC9",
        },
        "hmac-sha1-96" => {
          false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9\x897U\xB6\xE44(\x9D\x1C\x13\xFF\xFE",
        },
        "hmac-ripemd160" => {
          false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9)U\xBD\x03U\xDB\x95\x91Y)\xCF\xAE\xA0\xA6\x000\xE9\x1A\xF3Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9)U\xBD\x03U\xDB\x95\x91Y)\xCF\xAE\xA0\xA6\x000\xE9\x1A\xF3Y",
        },
        "none" => {
          false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0",
          :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9",
        },
      },
      "aes128-ctr" => {
        "hmac-md5" => {
          false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFDF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFDF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD",
          :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15",
        },
      },
      "aes192-ctr" => {
        "hmac-md5" => {
          false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91",
          :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1",
        },
      },
      "aes256-ctr" => {
        "hmac-md5" => {
          false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\n-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\n-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\nyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\nyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\rF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\n\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\rF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\n\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r",
          :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\n",
        },
      },
      "cast128-ctr" => {
        "hmac-md5" => {
          false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA\xEC=g\xF1\xB9[E\xB5\x8F\xCE\xCC\b\x82\x9D\x96\xDC",
        },
        "hmac-md5-96" => {
          false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA\xEC=g\xF1\xB9[E\xB5\x8F\xCE\xCC\b",
        },
        "hmac-sha1" => {
          false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA\x897U\xB6\xE44(\x9D\x1C\x13\xFF\xFE\xDD\xC1\xABrI\x8DW\xC9",
        },
        "hmac-sha1-96" => {
          false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA\x897U\xB6\xE44(\x9D\x1C\x13\xFF\xFE",
        },
        "hmac-ripemd160" => {
          false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA)U\xBD\x03U\xDB\x95\x91Y)\xCF\xAE\xA0\xA6\x000\xE9\x1A\xF3Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7F\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA)U\xBD\x03U\xDB\x95\x91Y)\xCF\xAE\xA0\xA6\x000\xE9\x1A\xF3Y",
        },
        "none" => {
          false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7",
          :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA",
        },
      },
      "camellia128-ctr@openssh.org" => {
        "hmac-md5" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1FF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1FF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9",
        },
      },
      "camellia192-ctr@openssh.org" => {
        "hmac-md5" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBEF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBEF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE",
        },
      },
      "camellia256-ctr@openssh.org" => {
        "hmac-md5" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xECF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xECF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93",
        },
      },
      "camellia128-ctr" => {
        "hmac-md5" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1FF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1FF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F",
          :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9",
        },
      },
      "camellia192-ctr" => {
        "hmac-md5" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEyC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBEF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBEF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE",
          :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE",
        },
      },
      "camellia256-ctr" => {
        "hmac-md5" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94\xFB\xF3\v\xB1",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3\\\xA1\xD6\xC9",
        },
        "hmac-md5-96" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x00\x1Aw\xCA\t\xC6\xFC\xB9\xE5p\x8D\x94",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93-\xE5\b\x15\xA2#\xDEP8\xE3\xF1\xB3",
        },
        "hmac-sha1" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m\xBD\x05\f\x82g\xB0g\xFE",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r\x16\xF6\xFD(i'\x8A\xE9",
        },
        "hmac-sha1-96" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC\x04\a\x80\n\x04\x82z\xB8\x9E\xB1\xD8m",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93yC\xBA\xCC@\xC1\n\xE6$\x93\xF7\r",
        },
        "hmac-ripemd160" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xECF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xECF\xC3\xC7\x87\xA5\x86\xD5~\xCD(\xF8\xD9\xCB\xC5\vHI\xCAL\x8E",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xC44\x14\xE3q\xEE\x13\x1A\xB2\x81\e9\x8Bd\xB5>^{\xC0\xD0",
        },
        "none" => {
          false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC",
          :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93",
        },
      },
      "none" => {
        "hmac-md5" => {
          false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1-96" => {
          false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-sha1" => {
          false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-ripemd160" => {
          false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212",
          :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^",
        },
      },
      "rijndael-cbc@lysator.liu.se" => {
        "hmac-md5" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365-\345\b\025\242#\336P8\343\361\263\\\241\326\311",
        },
        "hmac-md5-96" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365-\345\b\025\242#\336P8\343\361\263",
        },
        "hmac-sha1" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365yC\272\314@\301\n\346$\223\367\r\026\366\375(i'\212\351",
        },
        "hmac-sha1-96" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365yC\272\314@\301\n\346$\223\367\r",
        },
        "hmac-ripemd160" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\3044\024\343q\356\023\032\262\201\e9\213d\265>^{\300\320",
        },
        "none" => {
          false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340",
          :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365",
        },
      },
      "arcfour" => {
        "hmac-md5" => {
          false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`\000\032w\312\t\306\374\271\345p\215\224\373\363\v\261",
          :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017\354=g\361\271[E\265\217\316\314\b\202\235\226\334",
        },
        "hmac-md5-96" => {
          false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`\000\032w\312\t\306\374\271\345p\215\224",
          :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017\354=g\361\271[E\265\217\316\314\b",
        },
        "hmac-sha1" => {
          false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`\004\a\200\n\004\202z\270\236\261\330m\275\005\f\202g\260g\376",
          :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017\2117U\266\3444(\235\034\023\377\376\335\301\253rI\215W\311",
        },
        "hmac-sha1-96" => {
          false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`\004\a\200\n\004\202z\270\236\261\330m",
          :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017\2117U\266\3444(\235\034\023\377\376",
        },
        "hmac-ripemd160" => {
          false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "hmac-ripemd160@openssh.com" => {
          false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`F\303\307\207\245\206\325~\315(\370\331\313\305\vHI\312L\216",
          :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017)U\275\003U\333\225\221Y)\317\256\240\246\0000\351\032\363Y",
        },
        "none" => {
          false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`",
          :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017",
        },
      },
    }

    if defined?(OpenSSL::Digest::SHA256)
      sha2_packets = {
        "3des-cbc" => {
          "hmac-sha2-256" => {
            false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, 7{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-256-96" => {
            false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, 7{\320\316\365Wy\"c\036y\260",
            :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512" => {
            false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, #/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-512-96" => {
            false => "\003\352\031\261k\243\200\204\301\203]!\a\306\217\201\a[^\304\317\322\264\265~\361\017\n\205\272, #/\317\000\340I\274\363_\225U*",
            :standard => "\317\222v\316\234<\310\377\310\034\346\351\020:\025{\372PDS\246\344\312J\364\301\n\262\r<\037\231Mu\031\240\255\026\362\200Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "aes128-cbc" => {
          "hmac-sha2-256" => {
            false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY7{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251\373\035\334\340M\032B\307\324\232\211m'\347k\253\371\341\326\254\356\263[\2412\302R\320\274\365\255\003",
          },
          "hmac-sha2-256-96" => {
            false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY7{\320\316\365Wy\"c\036y\260",
            :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251\373\035\334\340M\032B\307\324\232\211m",
          },
          "hmac-sha2-512" => {
            false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251N\005f\275u\230\344xF\354+RSTS\360\235\004\311$cW\357o\"fy\031\321yX\tYK\347\363kd\a\022\307r\177[ \274\0164\222\300 \037\330<\264\001^\246\337\004\365\233\202\310",
          },
          "hmac-sha2-512-96" => {
            false => "\240\016\243k]0\330\253\030\320\334\261(\034E\211\230#\326\374\267\311O\211E(\234\325n\306NY#/\317\000\340I\274\363_\225U*",
            :standard => "\273\367\324\032\3762\334\026\r\246\342\022\016\325\024\270.\273\005\314\036\312\211\261\037A\361\362:W\316\352K\204\216b\2124>A\265g\331\177\233dK\251N\005f\275u\230\344xF\354+R",
          },
        },
        "aes192-cbc" => {
          "hmac-sha2-256" => {
            false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\0037{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320D\373\035\334\340M\032B\307\324\232\211m'\347k\253\371\341\326\254\356\263[\2412\302R\320\274\365\255\003",
          },
          "hmac-sha2-512" => {
            false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320DN\005f\275u\230\344xF\354+RSTS\360\235\004\311$cW\357o\"fy\031\321yX\tYK\347\363kd\a\022\307r\177[ \274\0164\222\300 \037\330<\264\001^\246\337\004\365\233\202\310",
          },
          "hmac-sha2-256-96" => {
            false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\0037{\320\316\365Wy\"c\036y\260",
            :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320D\373\035\334\340M\032B\307\324\232\211m",
          },
          "hmac-sha2-512-96" => {
            false => "P$\377\302\326\262\276\215\206\343&\257#\315>Mp\232P\345o\215\330\213\t\027\300\360\300\037\267\003#/\317\000\340I\274\363_\225U*",
            :standard => "se\347\230\026\311\212\250yH\241\302n\364:\276\270M=H1\317\222^\362\237D\225N\354:\343\205M\006[\313$U/yZ\330\235\032\307\320DN\005f\275u\230\344xF\354+R",
          },
        },
        "aes256-cbc" => {
          "hmac-sha2-256" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\3407{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\373\035\334\340M\032B\307\324\232\211m'\347k\253\371\341\326\254\356\263[\2412\302R\320\274\365\255\003",
          },
          "hmac-sha2-256-96" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\3407{\320\316\365Wy\"c\036y\260",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\373\035\334\340M\032B\307\324\232\211m",
          },
          "hmac-sha2-512" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365N\005f\275u\230\344xF\354+RSTS\360\235\004\311$cW\357o\"fy\031\321yX\tYK\347\363kd\a\022\307r\177[ \274\0164\222\300 \037\330<\264\001^\246\337\004\365\233\202\310",
          },
          "hmac-sha2-512-96" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340#/\317\000\340I\274\363_\225U*",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365N\005f\275u\230\344xF\354+R",
          },
        },
        "blowfish-cbc" => {
          "hmac-sha2-256" => {
            false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)7{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-256-96" => {
            false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)7{\320\316\365Wy\"c\036y\260",
            :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512" => {
            false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-512-96" => {
            false => "vT\353\203\247\206L\255e\371\001 6B/\234g\332\371\224l\227\257\346\373E\237C2\212u)#/\317\000\340I\274\363_\225U*",
            :standard => "U\257\231e\347\274\bh\016X\232h\334\v\005\316e1G$-\367##\256$rW\000\210\335_\360\f\000\205#\370\201\006Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "cast128-cbc" => {
          "hmac-sha2-256" => {
            false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\3767{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-256-96" => {
            false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\3767{\320\316\365Wy\"c\036y\260",
            :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512" => {
            false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-512-96" => {
            false => "\361\026\313!\31235|w~\n\261\257\277\e\277b\246b\342\333\eE\021N\345\343m\314\272\315\376#/\317\000\340I\274\363_\225U*",
            :standard => "\375i\253\004\311E\2011)\220$\251A\245\f(\371\263\314\242\353\260\272\367\276\"\031\224$\244\311W\307Oe\224\0017\336\325Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "idea-cbc" => {
          "hmac-sha2-256" => {
            false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H7{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-512" => {
            false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-256-96" => {
            false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H7{\320\316\365Wy\"c\036y\260",
            :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512-96" => {
            false => "\342\255\202$\273\201\025#\245\2341F\263\005@{\000<\266&s\016\251NH=J\322/\220 H#/\317\000\340I\274\363_\225U*",
            :standard => "F\3048\360\357\265\215I\021)\a\254/\315%\354M\004\330\006\356\vFr\250K\225\223x\277+Q)\022\327\311K\025\322\317Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "arcfour128" => {
          "hmac-sha2-256" => {
            false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\2557{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-512" => {
            false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-256-96" => {
            false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\2557{\320\316\365Wy\"c\036y\260",
            :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512-96" => {
            false => "e_\204\037\366\363>\024\263q\025\334\354AO.\026t\231nvD\030\226\234\263\257\335:\001\300\255#/\317\000\340I\274\363_\225U*",
            :standard => "e_\204'\367\217\243v\322\025|\330ios\004[P\270\306\272\017\037\344\214\253\354\272m\261\217/jW'V\277\341U\224Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "arcfour256" => {
          "hmac-sha2-256" => {
            false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr7{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-512" => {
            false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-256-96" => {
            false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr7{\320\316\365Wy\"c\036y\260",
            :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512-96" => {
            false => "B\374\256V\035b\337\215\305h\031bE\271\312\361\017T+\302\024x\3016\315g%\032\331\004fr#/\317\000\340I\274\363_\225U*",
            :standard => "B\374\256n\034\036B\357\244\fpf\300\227\366\333Bp\nj\3303\306D\335\177f}\216\264)\360\325jU^M\357$\221Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "arcfour512" => {
          "hmac-sha2-256" => {
            false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D7{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-512" => {
            false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-256-96" => {
            false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D7{\320\316\365Wy\"c\036y\260",
            :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512-96" => {
            false => "\n{\275\177Yw\307\f\277\221\247'\0318\237\223cR\340\361\356\017\357\235\342\374\005wL\267\330D#/\317\000\340I\274\363_\225U*",
            :standard => "\n{\275GX\vZn\336\365\316#\234\026\243\271.v\301Y\"D\350\357\362\344F\020\e\a\227\306\366\025:\246\2349\233\313Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "arcfour" => {
          "hmac-sha2-256" => {
            false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`7{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-512" => {
            false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-256-96" => {
            false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`7{\320\316\365Wy\"c\036y\260",
            :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512-96" => {
            false => "O\"\200JH\004\200\020Y\323\334\334\273B^\226DL\370\205V\350[>\257\361\223\270\262%\331`#/\317\000\340I\274\363_\225U*",
            :standard => "O\"\200rIx\035r8\267\265\330>lb\274\th\331-\232\243\\L\277\351\320\337\345\225\226\342>b\350\215\253\n\002\017Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "camellia128-cbc@openssh.org" => {
          "hmac-sha2-256" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia192-cbc@openssh.org" => {
          "hmac-sha2-256" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia256-cbc@openssh.org" => {
          "hmac-sha2-256" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiN\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiN\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia128-cbc" => {
          "hmac-sha2-256" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "vO\xD4Mst\xD2 } _\xE3e\xC4\x8A\xAA\xCD\x9E*\xE2\xA5\xC0\xED\xBB\xD5\x99\x12 ^2\xC3\x9D#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\x1Du9\xC7\x12\xA4\x9B\r\b\x19e&\x04e\xCE\rp\xE8=\x87h\xBE2\xE0\xAE\x90\xFF\xB22az\x17\xA4IO7}\xE3h2Q\xB8S\x18+&\xFE\x13N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia192-cbc" => {
          "hmac-sha2-256" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "Nnl\x00\xD2\xBA\x89j-(\xDD\xF4\\\x19\xF4\xB7\x16,\x90\xEA,\xE26\x00I\xF9\xB5Z\x060\x83E#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\x8Cm\x02.\x18\xFA\x87\x7F\x18a\xA8\xAC \x82u\xC7]\xE6rs/\xB3\xF5.>Aw\x96\xEF\xADLO\xDE[\x02\x14k\xCEn\x06\xF6\xBD^\"4';\x12N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia256-cbc" => {
          "hmac-sha2-256" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFi\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiN\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xE9\xAB&\x85*\x8B\x9C\xFF\xC9\xD2\x91\xE7\e\xE7P]\xD7\t\xA0\x99\a\xCD\x83K\x161\xA4\xBD\xCE\x82y|#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\x9D\x87\e\x99\x80mG\ex-\xA1\xEFA\xBB\xBD+!\xF9s\xC1\xBA_\xA8\xE0\x82\xBEX\xA6\xE8\x85\x1E\xBA\xAFY\x0E\xAC\xCB\xE1\xBF\xD1\xFD\xC3X\x8A\xF1qFiN\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "3des-ctr" => {
          "hmac-sha2-256" => {
            false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB17{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B\xF7F\x99v\xB5o\f9$\x94\x81\e\xF4+\x96H\xFC\xFF=\ts\x82`\x16\e,\xE7\t\x8F\x86t\xC7",
          },
          "hmac-sha2-256-96" => {
            false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB17{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91B\xF7F\x99v\xB5o\f9$\x94\x81\e",
          },
          "hmac-sha2-512" => {
            false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91BQ\xC92O\x93\xF1\x8E\x9D\x12\x8E\x0E2\xAE\xE3\x8C\xD0\v\xD1\xF6/$\x0F]2\xC2\xE35\x8F\xD4\xA5\x1F\xC1\x95p\xB8\x91c\xC7\xC2u\x8Bb 4#\x82PFI\xF9\xB7l\xFC\xC9\x01\xB2z(\xDD|\xDC\xA46\x96",
          },
          "hmac-sha2-512-96" => {
            false => "\xED#\x86\xD5\xE1mP\v\f\xB9\xC1\xE6\xFD\xA0~,\xD3\x13\x12\x8Cp\xD4F\x92\xCB\xB6R>\xFA]\x9B\xB1#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xED#\x86\xED\xE0\x11\xCDim\xDD\xA8\xE2x\x8EB\x06\x9E73$\xBC\x9FA\xE0\xDB\xAE\x11Y\xAD\xED\xD43\x86N\x89\xFE\x14V\x91BQ\xC92O\x93\xF1\x8E\x9D\x12\x8E\x0E2",
          },
        },
        "blowfish-ctr" => {
          "hmac-sha2-256" => {
            false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC07{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9\xF7F\x99v\xB5o\f9$\x94\x81\e\xF4+\x96H\xFC\xFF=\ts\x82`\x16\e,\xE7\t\x8F\x86t\xC7",
          },
          "hmac-sha2-256-96" => {
            false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC07{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9\xF7F\x99v\xB5o\f9$\x94\x81\e",
          },
          "hmac-sha2-512" => {
            false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9Q\xC92O\x93\xF1\x8E\x9D\x12\x8E\x0E2\xAE\xE3\x8C\xD0\v\xD1\xF6/$\x0F]2\xC2\xE35\x8F\xD4\xA5\x1F\xC1\x95p\xB8\x91c\xC7\xC2u\x8Bb 4#\x82PFI\xF9\xB7l\xFC\xC9\x01\xB2z(\xDD|\xDC\xA46\x96",
          },
          "hmac-sha2-512-96" => {
            false => "\xF7gk6\xB8\xACK\x1D\xC4Ls\xB0{\x0F\xC7\xC4M\xC5>\xF6G8\xD4\xBCu\x152FoJ\xB0\xC0#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xF7gk\x0E\xB9\xD0\xD6\x7F\xA5(\x1A\xB4\xFE!\xFB\xEE\x00\xE1\x1F^\x8Bs\xD3\xCEe\rq!8\xFA\xFFB\r\xE9\xFC\xF6\xCA\xBC\x03\xA9Q\xC92O\x93\xF1\x8E\x9D\x12\x8E\x0E2",
          },
        },
        "aes128-ctr" => {
          "hmac-sha2-256" => {
            false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xD6\x98\xC1n+6\xCA`s2\x06\xAA\x80\xFA\xF3\xF6\xCA\xF9\xC8[BB\xDC\x9F\xDC$\x88*\xA7\x00\x8E\xFD#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xD6\x98\xC1^2JW\x02\x12Vo\xAE\x05\xD4\xCF\xDC\x87\xDD\xE9\xF3\x8E\t\xDB\xED\xCC<\xCBM\xF0\xB0\xC1\x7F\xD7\x17\x931\xBC~\r\xF2\x87\xB89\x9B\x8B\xB3\x8E\x15N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "aes192-ctr" => {
          "hmac-sha2-256" => {
            false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x917{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x917{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xA8\x02\xB4-\xFBYo4F\"\xCF\xB8\x92\xF08\xAC\xE8\xECk\xECO\xE7\xF8\x01\xF8\xB0\x9E\x05\xFB\xA7\xA7\x91#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xA8\x02\xB4\x1D\xE2%\xF2V'F\xA6\xBC\x17\xDE\x04\x86\xA5\xC8JD\x83\xAC\xFFs\xE8\xA8\xDDb\xAC\x17\xE8\x13\x92V\x9E\x00!\x1F\xD4\x00\x92T\x15\xDE\xA4\xCA\xE9\xC1N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "aes256-ctr" => {
          "hmac-sha2-256" => {
            false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\n\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\n\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\nN\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "M\x1DcA\r]\\\x95?&\xE3D[\xCC1\x9B\xE0\xAF\x96\xA8\x86Y\xBD\x16\xE5xR%u\xC9(\r#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "M\x1Dcq\x14!\xC1\xF7^B\x8A@\xDE\xE2\r\xB1\xAD\x8B\xB7\x00J\x12\xBAd\xF5`\x11B\"yg\x8F\x9F\xAB\xC8 d\xB4\xE7^w\xC4\x89\a\x17\x15\x82\nN\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "cast128-ctr" => {
          "hmac-sha2-256" => {
            false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC77{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA\xF7F\x99v\xB5o\f9$\x94\x81\e\xF4+\x96H\xFC\xFF=\ts\x82`\x16\e,\xE7\t\x8F\x86t\xC7",
          },
          "hmac-sha2-256-96" => {
            false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC77{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEA\xF7F\x99v\xB5o\f9$\x94\x81\e",
          },
          "hmac-sha2-512" => {
            false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEAQ\xC92O\x93\xF1\x8E\x9D\x12\x8E\x0E2\xAE\xE3\x8C\xD0\v\xD1\xF6/$\x0F]2\xC2\xE35\x8F\xD4\xA5\x1F\xC1\x95p\xB8\x91c\xC7\xC2u\x8Bb 4#\x82PFI\xF9\xB7l\xFC\xC9\x01\xB2z(\xDD|\xDC\xA46\x96",
          },
          "hmac-sha2-512-96" => {
            false => "\x10\xA0cJ6W\xC9\xC7\x02\xF8\xCD\xE31\xF9\xE7n\x0Fj\x7F\x99\x8A\f\x84\x80\x80\xE8p\x9C\x14\x83\x1C\xC7#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\x10\xA0cr7+T\xA5c\x9C\xA4\xE7\xB4\xD7\xDBDBN^1FG\x83\xF2\x90\xF03\xFBC3SE\xF7x;q\x89\xA80\xEAQ\xC92O\x93\xF1\x8E\x9D\x12\x8E\x0E2",
          },
        },
        "camellia128-ctr@openssh.org" => {
          "hmac-sha2-256" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia192-ctr@openssh.org" => {
          "hmac-sha2-256" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEN\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEN\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia256-ctr@openssh.org" => {
          "hmac-sha2-256" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia128-ctr" => {
          "hmac-sha2-256" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xE4>\xD9'`\xA5W\x9A\xB7\x19\xA9\x98\xB0\x87f2}\x0F\xBE\xBDS\xA8\xA5\x17\x10\x80\x10<Ww~\x1F#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xE4>\xD9\x17y\xD9\xCA\xF8\xD6}\xC0\x9C5\xA9Z\x180+\x9F\x15\x9F\xE3\xA2e\x00\x98S[\x00\xC71\x9D\xAEx\x19\x17m\x9E\xD6\xC5\x90\xE2d\xFA#\xEB\x94\xA9N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia192-ctr" => {
          "hmac-sha2-256" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EE\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEN\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xEE8:\xB5\x0E\xED\xF4?yh\x8A\xB2{\xF5\x8DH\x95\xA4\xFA\xDF\x01\xAC\xC4\xD5Xb\xBB\xC1\x8B\xD7\xBC\xBE#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xEE8:\x85\x17\x91i]\x18\f\xE3\xB6\xFE\xDB\xB1b\xD8\x80\xDBw\xCD\xE7\xC3\xA7Hz\xF8\xA6\xDCg\xF3<N\xAB\xF7\xE5\xAF\xC5\xE6\x92\xFD\x85,\xF5\x8F\a\x8EEN\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "camellia256-ctr" => {
          "hmac-sha2-256" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0-\xBD\xCA~\x8F\x10U\xED\x01\xFF\x95F\xE5\x86\xAD\xC7\x13N\xE8J",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m'\xE7k\xAB\xF9\xE1\xD6\xAC\xEE\xB3[\xA12\xC2R\xD0\xBC\xF5\xAD\x03",
          },
          "hmac-sha2-256-96" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC7{\xD0\xCE\xF5Wy\"c\x1Ey\xB0",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93\xFB\x1D\xDC\xE0M\x1AB\xC7\xD4\x9A\x89m",
          },
          "hmac-sha2-512" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC#/\xCF\x00\xE0I\xBC\xF3_\x95U*\xD7z\x81\xCEc\xC3\xBDA\xF2\xD8^J\xBF\xC05oI\xBA\xF2\xEA\x86\xF8h\x8B\xB2\xC89\xC8v\x1F\x04\x12\x80]&\xF5\xC8\xC0\x90D[\xE8\x1E\x95\x89\xEB\xF1\xF6\x9F\xB7\x84\xD5",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93N\x05f\xBDu\x98\xE4xF\xEC+RSTS\xF0\x9D\x04\xC9$cW\xEFo\"fy\x19\xD1yX\tYK\xE7\xF3kd\a\x12\xC7r\x7F[ \xBC\x0E4\x92\xC0 \x1F\xD8<\xB4\x01^\xA6\xDF\x04\xF5\x9B\x82\xC8",
          },
          "hmac-sha2-512-96" => {
            false => "\xE3-1\x8E\xA1\xB7\x95\x9E`\x1E\xFB:[\xFD\x15\x8Ee\xD6|\xB6q\xF98\xFF\t\xB3\xD4F\x03\xB3\xFA\xEC#/\xCF\x00\xE0I\xBC\xF3_\x95U*",
            :standard => "\xE3-1\xBE\xB8\xCB\b\xFC\x01z\x92>\xDE\xD3)\xA4(\xF2]\x1E\xBD\xB2?\x8D\x19\xAB\x97!T\x03\xB5n\xC0)\xBE.]\x92\xF5\x05~\t\x04\x99\xFB\xDC\xD6\x93N\x05f\xBDu\x98\xE4xF\xEC+R",
          },
        },
        "none" => {
          "hmac-sha2-256" => {
            false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\2127{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^\367F\231v\265o\f9$\224\201\e\364+\226H\374\377=\ts\202`\026\e,\347\t\217\206t\307",
          },
          "hmac-sha2-256-96" => {
            false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\2127{\320\316\365Wy\"c\036y\260",
            :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^\367F\231v\265o\f9$\224\201\e",
          },
          "hmac-sha2-512" => {
            false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^Q\3112O\223\361\216\235\022\216\0162\256\343\214\320\v\321\366/$\017]2\302\3435\217\324\245\037\301\225p\270\221c\307\302u\213b 4#\202PFI\371\267l\374\311\001\262z(\335|\334\2446\226",
          },
          "hmac-sha2-512-96" => {
            false => "\000\000\000\034\b\004\001\000\000\000\tdebugging\000\000\000\000\b\030CgWO\260\212#/\317\000\340I\274\363_\225U*",
            :standard => "\000\000\000$\tx\234bad``\340LIM*MO\317\314K\ar\030\000\000\000\000\377\377\b\030CgWO\260\212^Q\3112O\223\361\216\235\022\216\0162",
          },
        },
        "rijndael-cbc@lysator.liu.se" => {
          "hmac-sha2-256" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\3407{\320\316\365Wy\"c\036y\260-\275\312~\217\020U\355\001\377\225F\345\206\255\307\023N\350J",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\373\035\334\340M\032B\307\324\232\211m'\347k\253\371\341\326\254\356\263[\2412\302R\320\274\365\255\003",
          },
          "hmac-sha2-256-96" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\3407{\320\316\365Wy\"c\036y\260",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365\373\035\334\340M\032B\307\324\232\211m",
          },
          "hmac-sha2-512" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340#/\317\000\340I\274\363_\225U*\327z\201\316c\303\275A\362\330^J\277\3005oI\272\362\352\206\370h\213\262\3109\310v\037\004\022\200]&\365\310\300\220D[\350\036\225\211\353\361\366\237\267\204\325",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365N\005f\275u\230\344xF\354+RSTS\360\235\004\311$cW\357o\"fy\031\321yX\tYK\347\363kd\a\022\307r\177[ \274\0164\222\300 \037\330<\264\001^\246\337\004\365\233\202\310",
          },
          "hmac-sha2-512-96" => {
            false => "\266\001oG(\201s\255[\202j\031-\354\353]\022\374\367j2\257\b#\273r\275\341\232\264\255\340#/\317\000\340I\274\363_\225U*",
            :standard => "\251!O/_\253\321\217e\225\202\202W\261p\r\357\357\375\231\264Y,nZ/\366\225G\256\3000\036\223\237\353\265vG\231\215cvY\236%\315\365N\005f\275u\230\344xF\354+R",
          },
        },
      }
      sha2_packets.each do |key, val|
        PACKETS[key].merge!(val)
      end
    end

    ciphers = Net::SSH::Transport::CipherFactory::SSH_TO_OSSL.keys
    hmacs = Net::SSH::Transport::HMAC::MAP.keys

    ciphers.each do |cipher_name|
      unless Net::SSH::Transport::CipherFactory.supported?(cipher_name)
        puts "Skipping packet stream test for #{cipher_name}"
        next
      end

      # JRuby Zlib implementation (1.4 & 1.5) does not have byte-to-byte compatibility with MRI's.
      # skip these 80 or more tests under JRuby.
      if defined?(JRUBY_VERSION)
        puts "Skipping zlib tests for JRuby"
        next
      end

      hmacs.each do |hmac_name|
        [false, :standard].each do |compress|
          cipher_method_name = cipher_name.gsub(/\W/, "_")
          hmac_method_name   = hmac_name.gsub(/\W/, "_")
          
          define_method("test_next_packet_with_#{cipher_method_name}_and_#{hmac_method_name}_and_#{compress}_compression") do
            opts = { :shared => "123", :hash => "^&*", :digester => OpenSSL::Digest::SHA1  }
            cipher = Net::SSH::Transport::CipherFactory.get(cipher_name, opts.merge(:key => "ABC", :decrypt => true, :iv => "abc"))
            hmac  = Net::SSH::Transport::HMAC.get(hmac_name, "{}|", opts)

            stream.server.set :cipher => cipher, :hmac => hmac, :compression => compress
            stream.stubs(:recv).returns(PACKETS[cipher_name][hmac_name][compress])
            IO.stubs(:select).returns([[stream]])
            packet = stream.next_packet(:nonblock)
            assert_not_nil packet
            assert_equal DEBUG, packet.type
            assert packet[:always_display]
            assert_equal "debugging", packet[:message]
            assert_equal "", packet[:language]
            stream.cleanup
          end

          define_method("test_enqueue_packet_with_#{cipher_method_name}_and_#{hmac_method_name}_and_#{compress}_compression") do
            opts = { :shared => "123", :digester => OpenSSL::Digest::SHA1, :hash => "^&*" }
            cipher = Net::SSH::Transport::CipherFactory.get(cipher_name, opts.merge(:key => "ABC", :iv => "abc", :encrypt => true))
            hmac  = Net::SSH::Transport::HMAC.get(hmac_name, "{}|", opts)

            srand(100)
            stream.client.set :cipher => cipher, :hmac => hmac, :compression => compress
            stream.enqueue_packet(ssh_packet)
            assert_equal PACKETS[cipher_name][hmac_name][compress], stream.write_buffer
            stream.cleanup
          end
        end
      end
    end

    private

      def stream
        @stream ||= begin
          stream = mock("packet_stream")
          stream.extend(Net::SSH::Transport::PacketStream)
          stream
        end
      end

      def ssh_packet
        Net::SSH::Buffer.from(:byte, DEBUG, :bool, true, :string, "debugging", :string, "")
      end

      def packet
        @packet ||= begin
          data = ssh_packet
          length = data.length + 4 + 1 # length + padding length
          padding = stream.server.cipher.block_size - (length % stream.server.cipher.block_size)
          padding += stream.server.cipher.block_size if padding < 4
          Net::SSH::Buffer.from(:long, length + padding - 4, :byte, padding, :raw, data, :raw, "\0" * padding).to_s
        end
      end
  end

end
