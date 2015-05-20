require 'common'

class TestKnownHosts < Test::Unit::TestCase

  def test_key_for_when_all_hosts_are_recognized
    source = File.join(File.dirname(__FILE__),"known_hosts/github")
    kh = Net::SSH::KnownHosts.new(source)
    keys = kh.keys_for("github.com")
    assert_equal(1, keys.count)
    assert_equal("ssh-rsa", keys[0].ssh_type)
  end

end