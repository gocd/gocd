require 'test/unit' unless defined? $ZENTEST and $ZENTEST
require 'rubyforge'
require 'time'
require 'yaml'
require 'tempfile'

class TestRubyForgeCookieManager < Test::Unit::TestCase
  def setup
    cookie = cookie_string('session_ser', 'zzzzzzzzzzzzzzzzzz')
    @url      = URI.parse('http://rubyforge.org/account/login.php')
    @cookie   = WEBrick::Cookie.parse_set_cookie(cookie)
    @manager  = RubyForge::CookieManager.new
  end

  def test_empty?
    manager = RubyForge::CookieManager.new
    assert(manager.empty?)

    assert_equal(0, manager[URI.parse('http://rubyforge.org/')].length)
    assert(manager.empty?)
  end

  def test_add
    manager = RubyForge::CookieManager.new
    assert(manager.empty?)
    manager.add(@url, @cookie)
    assert(!manager.empty?)

    cookie = manager[@url][@cookie.name]
    assert cookie
    assert_equal(@cookie.object_id, cookie.object_id)
  end

  def test_add_expired_cookie
    cookie = cookie_string('session_ser', 'zzzz',
              :expires => (Time.now - 10).strftime('%A, %d-%b-%Y %H:%M:%S %Z'))
    cookie   = WEBrick::Cookie.parse_set_cookie(cookie)

    manager = RubyForge::CookieManager.new
    assert(manager.empty?)
    manager.add(@url, cookie)
    assert(manager.empty?)
  end

  def test_marshal
    @manager.add(@url, @cookie)
    assert(!@manager.empty?)
    m = YAML.load(YAML.dump(@manager))
    assert(!m.empty?)
  end

  def test_save!
    tmp = Tempfile.new('cookie_jar')
    @manager.cookies_file = tmp.path
    @manager.add(@url, @cookie)
    @manager.save!

    @manager = RubyForge::CookieManager.load(tmp.path)
    assert(! @manager.empty?)
  end

  def test_load_empty_file
    tmp = Tempfile.new('cookie_jar')
    manager = RubyForge::CookieManager.load(tmp.path)
    assert(manager.empty?)
  end

  def test_load_legacy_file
    tmp = Tempfile.new('cookie_jar')
    tmp.write([
      'https://rubyforge.org/account/login.php',
      'session_ser',
      'zzzzzz',
      '123456',
      '.rubyforge.org',
      '/',
      '13'
    ].join("\t"))
    manager = RubyForge::CookieManager.load(tmp.path)
    assert(manager.empty?)
  end

  def test_cookies_file=
    tmp = Tempfile.new('cookie_jar')
    @manager.cookies_file = tmp.path
    assert(@manager.empty?)
  end

  def cookie_string(name, value, options = {})
    options = {
      :expires  => (Time.now + 86400).strftime('%A, %d-%b-%Y %H:%M:%S %Z'),
      :path     => '/',
      :domain   => '.rubyforge.org',
    }.merge(options)
    "#{name}=#{value}; #{options.map { |o| o.join('=') }.join('; ')}"
  end
end
