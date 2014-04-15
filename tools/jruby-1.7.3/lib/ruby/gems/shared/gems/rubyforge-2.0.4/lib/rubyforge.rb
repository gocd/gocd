#! /usr/bin/env ruby -w

require 'json'
require 'enumerator'
require 'fileutils'
require 'yaml'
require 'open-uri'
require 'rubyforge/client'

$TESTING = false unless defined? $TESTING

class RubyForge

  # :stopdoc:
  VERSION     = '2.0.4'
  HOME        = ENV["HOME"] || ENV["HOMEPATH"] || File::expand_path("~")
  RUBYFORGE_D = File::join HOME, ".rubyforge"
  CONFIG_F    = File::join RUBYFORGE_D, "user-config.yml"

  # We must use __FILE__ instead of DATA because this is now a library
  # and DATA is relative to $0, not __FILE__.
  config = File.read(__FILE__).split(/__END__/).last.gsub(/#\{(.*)\}/) {eval $1}
  CONFIG = YAML.load(config)
  # :startdoc:

  # TODO: add an autoconfig method that is self-repairing, removing key checks 
  attr_reader :userconfig, :autoconfig

  def initialize(userconfig=nil, autoconfig=nil, opts=nil)
    # def initialize(userconfig=CONFIG_F, opts={})
    @userconfig, @autoconfig = userconfig, autoconfig

    @autoconfig ||= CONFIG["rubyforge"].dup
    @userconfig.merge! opts if opts

    @client = nil
    @uri = nil
  end
  
  # These are no-ops now, but we'll keep them here for backwards compatibility
  def login ; end
  def logout ; end

  def configure opts = {}
    user_path        = CONFIG_F
    dir, file        = File.split(user_path)

    @userconfig      = if test(?e, user_path) then
                         YAML.load_file(user_path)
                       else
                         CONFIG
                       end.merge(opts)
    @autoconfig_path = File.join(dir, file.sub(/^user/, 'auto'))
    @autoconfig      = if test(?e, @autoconfig_path) then
                         YAML.load_file(@autoconfig_path)
                       else
                         CONFIG["rubyforge"].dup
                       end
    @autoconfig["type_ids"] = CONFIG['rubyforge']['type_ids'].dup

    raise "no <username>"   unless @userconfig["username"]
    raise "no <password>"   unless @userconfig["password"]

    self
  end

  def force
    @userconfig['force']
  end

  def uri
    uri = @userconfig['uri']
    abort "Using new REST api, but uri isn't api.rubyforge.org.
run `rubyforge setup` and fix please" if
      uri =~ /rubyforge.org/ and uri !~ /api.rubyforge.org/

    @uri ||= URI.parse uri
  end

  def setup
    FileUtils::mkdir_p RUBYFORGE_D, :mode => 0700 unless test ?d, RUBYFORGE_D
    test ?e, CONFIG_F and FileUtils::mv CONFIG_F, "#{CONFIG_F}.bak"
    config = CONFIG.dup
    config.delete "rubyforge"

    open(CONFIG_F, "w") { |f|
      f.write YAML.dump(config)
    }
    edit = (ENV["EDITOR"] || ENV["EDIT"] || "vi") + " '#{CONFIG_F}'"
    system edit or puts "edit '#{CONFIG_F}'"
  end

  def save_autoconfig
    File.open(@autoconfig_path, "w") do |file|
      YAML.dump @autoconfig, file
    end
  end

  def scrape_config
    username = @userconfig['username']

    %w(group package processor release).each do |type|
      @autoconfig["#{type}_ids"].clear if @autoconfig["#{type}_ids"]
    end
  
    json = get_via_rest_api "/users/#{username}/groups.js"

    projects = json.collect {|group| group['group']['unix_group_name'] }
    puts "Fetching #{projects.size} projects"
    projects.each do |project|
      scrape_project(project)
    end
  end
  
  def get_via_rest_api(path)
    url = "#{self.uri}#{path}"
    puts "Hitting REST API: #{url}" if $DEBUG
    JSON.parse(client.get_content(url, {}, {}, @userconfig))
  end

  def scrape_project(project)
    data = {
      "group_ids"     => {},
      "package_ids"   => {},
      "processor_ids" => Hash.new { |h,k| h[k] = {} },
      "release_ids"   => Hash.new { |h,k| h[k] = {} },
    }

    unless data["group_ids"].has_key? project then
      json = get_via_rest_api "/groups/#{project}.js"
      group_id = json["group"]["group_id"].to_i
      data["group_ids"][project] = group_id
    end

    # Get project's packages 
    json = get_via_rest_api "/groups/#{project}/packages.js"
    json.each do |package|
      data["package_ids"][package["package"]["name"]] = package["package"]["package_id"]
      # Get releases for this package
      json = get_via_rest_api "/packages/#{package["package"]["package_id"]}/releases.js"
      json.each do |release|
        data["release_ids"][package["package"]["name"]][release["name"]] = release["release_id"]
      end
    end

    # Get processor ids
    if @autoconfig['processor_ids'].nil? || @autoconfig['processor_ids'].empty?
      puts "Fetching processor ids" if $DEBUG
      json = get_via_rest_api "/processors.js"
      json.each do |processor|
        data["processor_ids"][processor["processor"]["name"]] = processor["processor"]["processor_id"]
      end
    end

    data.each do |key, val|
      @autoconfig[key] ||= {}
      @autoconfig[key].merge! val
    end

    save_autoconfig
  end

  def create_package(group_id, package_name)
    page = "/groups/#{group_id}/packages"

    group_id = lookup "group", group_id
    is_private = @userconfig["is_private"]
    is_public = is_private ? 0 : 1

    form = {
      "package[name]" => package_name,
      "package[is_public]"    => is_public
    }

    run page, form

    group_name = @autoconfig["group_ids"].invert[group_id]
    scrape_project(group_name)
  end

  ##
  # Posts news item to +group_id+ (can be name) with +subject+ and +body+

  def post_news(group_id, subject, body)
    # TODO - what was the post_changes parameter for?
    form = {
      "news_byte[summary]"      => subject,
      "news_byte[details]"      => body
    }
    group_id = lookup "group", group_id
    url = "/groups/#{group_id}/news_bytes"
    run url, form
  end

  def delete_package(group_id, package_id)
    group_id = lookup "group", group_id
    package_id = lookup "package", package_id
    package_name = @autoconfig["package_ids"].invert[package_id]
    @autoconfig["package_ids"].delete package_name
    @autoconfig["release_ids"].delete package_name
    save_autoconfig
    url = "/packages/#{package_id}"
    run url, {"_method" => "delete"}
  end

  def add_release(group_id, package_id, release_name, *files)
    group_id        = lookup "group", group_id
    package_id      = lookup "package", package_id
    release_date    = @userconfig["release_date"]
    release_notes   = @userconfig["release_notes"]
    release_changes = @userconfig["release_changes"]
    preformatted    = @userconfig["preformatted"]
    release_date ||= Time.now.strftime("%Y-%m-%d %H:%M")
    release_notes = IO::read(release_notes) if
      test(?e, release_notes) if release_notes
    release_changes = IO::read(release_changes) if
      test(?e, release_changes) if release_changes
    preformatted = preformatted ? 1 : 0

    form = {
        "release[name]"    => release_name,
        "release[release_date]"     => release_date,
        "release[notes]"   => release_notes,
        "release[changes]" => release_changes,
        "release[preformatted]"    => preformatted,
    }

    url = "/packages/#{package_id}/releases"
    json = run url, form
    
    release_id = JSON.parse(json)["release_id"].to_i rescue nil
    unless release_id then
      puts json if $DEBUG
      raise "Couldn't get release_id, upload failed?"
    end

    # FIXME
    #raise "Invalid package_id #{package_id}" if html[/Invalid package_id/]
    #raise "You have already released this version." if html[/That filename already exists in this project/]

    files.each do |file|
      add_file(group_id, package_id, release_id, file)
    end

    package_name = @autoconfig["package_ids"].invert[package_id]
    raise "unknown package name for #{package_id}" if package_name.nil?
    @autoconfig["release_ids"][package_name] ||= {}
    @autoconfig["release_ids"][package_name][release_name] = release_id
    save_autoconfig

    release_id
  end

  ##
  # add a file to an existing release under the specified group_id,
  # package_id, and release_id
  #
  # example :
  #   add_file("codeforpeople", "traits", "0.8.0", "traits-0.8.0.gem")
  #   add_file("codeforpeople", "traits", "0.8.0", "traits-0.8.0.tgz")
  #   add_file(1024, 1242, "0.8.0", "traits-0.8.0.gem")

  def add_file(group_name, package_name, release_name, userfile)
    type_id      = @userconfig["type_id"]
    group_id     = lookup "group", group_name
    package_id   = lookup "package", package_name
    release_id   = (Integer === release_name) ? release_name : lookup("release", package_name)[release_name]
    url = "/releases/#{release_id}/files.js"

    userfile = open userfile, 'rb'

    type_id ||= userfile.path[%r|\.[^\./]+$|]
    type_id = (lookup "type", type_id rescue lookup "type", ".oth")

    processor_id = @userconfig["processor_id"]
    processor_id ||= "Any"
    processor_id = lookup "processor", processor_id

    form = {
      "file[filename]"      => File.basename(userfile.path),
      "file[processor_id]"  => processor_id,
      "file[type_id]"       => type_id,
      "contents"            => userfile.read
    }

    run url, form
  end

  def client
    return @client if @client

    @client = RubyForge::Client::new ENV["HTTP_PROXY"]
    @client.debug_dev = STDERR if ENV["RUBYFORGE_DEBUG"] || ENV["DEBUG"] || $DEBUG

    @client
  end

  def run(page, form, extheader={}) # :nodoc:
    uri = self.uri + page
    puts "client.post_content #{uri.inspect}, #{form.inspect}, #{extheader.inspect}" if $DEBUG
    response = client.post_content uri, form, extheader, @userconfig
    puts response if $DEBUG
    response
  end

  def lookup(type, val) # :nodoc:
    unless Fixnum === val then
      key = val.to_s
      val = @autoconfig["#{type}_ids"][key]
      raise "no <#{type}_id> configured for <#{ key }>" unless val
    end
    val
  end
end

__END__
#
# base rubyforge uri - store in #{ CONFIG_F }
#
  uri        : http://api.rubyforge.org
#
# this must be your username
#
  username   : tom
#
# this must be your password
#
  password   : password
#
# defaults for some values
#
  is_private : false
# AUTOCONFIG:
  rubyforge :
  #
  # map your group names to their rubyforge ids
  #
    group_ids :
      codeforpeople : 1024
      support : 5
  #
  # map your package names to their rubyforge ids
  #
    package_ids :
      traits : 1241
  #
  # map your package names to their rubyforge ids
  #
    release_ids :
      traits :
        1.2.3 : 666
  #
  # mapping file exts to rubyforge ids
  #
    type_ids :
      .deb         : 1000
      .rpm         : 2000
      .zip         : 3000
      .bz2         : 3100
      .gz          : 3110
      .src.zip     : 5000
      .src.bz2     : 5010
      .src.tar.bz2 : 5010
      .src.gz      : 5020
      .src.tar.gz  : 5020
      .src.rpm     : 5100
      .src         : 5900
      .jpg         : 8000
      .txt         : 8100
      .text        : 8100
      .htm         : 8200
      .html        : 8200
      .pdf         : 8300
      .oth         : 9999
      .ebuild      : 1300
      .exe         : 1100
      .dmg         : 1200
      .tar.gz      : 5000
      .tgz         : 5000
      .gem         : 1400
      .pgp         : 8150
      .sig         : 8150
      .pem         : 1500

  #
  # map processor names to rubyforge ids
  #
    processor_ids :
      Other      : 9999
