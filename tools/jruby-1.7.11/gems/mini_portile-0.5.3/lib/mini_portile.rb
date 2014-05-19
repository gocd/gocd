require 'rbconfig'
require 'net/http'
require 'net/ftp'
require 'fileutils'
require 'tempfile'
require 'digest/md5'

class MiniPortile
  attr_reader :name, :version, :original_host
  attr_writer :configure_options
  attr_accessor :host, :files, :patch_files, :target, :logger

  def initialize(name, version)
    @name = name
    @version = version
    @target = 'ports'
    @files = []
    @patch_files = []
    @logger = STDOUT

    @original_host = @host = detect_host
  end

  def download
    @files.each do |url|
      filename = File.basename(url)
      download_file(url, File.join(archives_path, filename))
    end
  end

  def extract
    @files.each do |url|
      filename = File.basename(url)
      extract_file(File.join(archives_path, filename), tmp_path)
    end
  end

  def patch
    # Set GIT_DIR while appying patches to work around
    # git-apply doing nothing when started within another
    # git directory.
    ENV['GIT_DIR'], old_git = '.', ENV['GIT_DIR']
    begin
      @patch_files.each do |full_path|
        next unless File.exists?(full_path)
        output "Running git apply with #{full_path}..."
        execute('patch', %Q(git apply #{full_path}))
      end
    ensure
      ENV['GIT_DIR'] = old_git
    end
  end

  def configure_options
    @configure_options ||= configure_defaults
  end

  def configure
    return if configured?

    md5_file = File.join(tmp_path, 'configure.md5')
    digest   = Digest::MD5.hexdigest(computed_options)
    File.open(md5_file, "w") { |f| f.write digest }

    execute('configure', %Q(sh configure #{computed_options}))
  end

  def compile
    execute('compile', make_cmd)
  end

  def install
    return if installed?
    execute('install', %Q(#{make_cmd} install))
  end

  def downloaded?
    missing = @files.detect do |url|
      filename = File.basename(url)
      !File.exist?(File.join(archives_path, filename))
    end

    missing ? false : true
  end

  def configured?
    configure = File.join(work_path, 'configure')
    makefile  = File.join(work_path, 'Makefile')
    md5_file  = File.join(tmp_path, 'configure.md5')

    stored_md5  = File.exist?(md5_file) ? File.read(md5_file) : ""
    current_md5 = Digest::MD5.hexdigest(computed_options)

    (current_md5 == stored_md5) && newer?(makefile, configure)
  end

  def installed?
    makefile  = File.join(work_path, 'Makefile')
    target_dir = Dir.glob("#{port_path}/*").find { |d| File.directory?(d) }

    newer?(target_dir, makefile)
  end

  def cook
    download unless downloaded?
    extract
    patch
    configure unless configured?
    compile
    install unless installed?

    return true
  end

  def activate
    lib_path = File.join(port_path, "lib")
    vars = {
      'PATH'          => File.join(port_path, 'bin'),
      'CPATH'         => File.join(port_path, 'include'),
      'LIBRARY_PATH'  => lib_path
    }.reject { |env, path| !File.directory?(path) }

    output "Activating #{@name} #{@version} (from #{port_path})..."
    vars.each do |var, path|
      full_path = File.expand_path(path)

      # turn into a valid Windows path (if required)
      full_path.gsub!(File::SEPARATOR, File::ALT_SEPARATOR) if File::ALT_SEPARATOR

      # save current variable value
      old_value = ENV[var] || ''

      unless old_value.include?(full_path)
        ENV[var] = "#{full_path}#{File::PATH_SEPARATOR}#{old_value}"
      end
    end

    # rely on LDFLAGS when cross-compiling
    if File.exist?(lib_path) && (@host != @original_host)
      full_path = File.expand_path(lib_path)

      old_value = ENV.fetch("LDFLAGS", "")

      unless old_value.include?(full_path)
        ENV["LDFLAGS"] = "-L#{full_path} #{old_value}".strip
      end
    end
  end

  def path
    File.expand_path(port_path)
  end

private

  def tmp_path
    "tmp/#{@host}/ports/#{@name}/#{@version}"
  end

  def port_path
    "#{@target}/#{@host}/#{@name}/#{@version}"
  end

  def archives_path
    "#{@target}/archives"
  end

  def work_path
    Dir.glob("#{tmp_path}/*").find { |d| File.directory?(d) }
  end

  def configure_defaults
    [
      "--host=#{@host}",    # build for specific target (host)
      "--enable-static",    # build static library
      "--disable-shared"    # disable generation of shared object
    ]
  end

  def configure_prefix
    "--prefix=#{File.expand_path(port_path)}"
  end

  def computed_options
    [
      configure_options,     # customized or default options
      configure_prefix,      # installation target
    ].flatten.join(' ')
  end

  def log_file(action)
    File.join(tmp_path, "#{action}.log")
  end

  def tar_exe
    @@tar_exe ||= begin
      %w[gtar bsdtar tar basic-bsdtar].find { |c|
        which(c)
      }
    end
  end

  def tar_compression_switch(filename)
    case File.extname(filename)
      when '.gz', '.tgz'
        'z'
      when '.bz2', '.tbz2'
        'j'
      when '.Z'
        'Z'
      else
        ''
    end
  end

  # From: http://stackoverflow.com/a/5471032/7672
  # Thanks, Mislav!
  #
  # Cross-platform way of finding an executable in the $PATH.
  #
  #   which('ruby') #=> /usr/bin/ruby
  def which(cmd)
    exts = ENV['PATHEXT'] ? ENV['PATHEXT'].split(';') : ['']
    ENV['PATH'].split(File::PATH_SEPARATOR).each do |path|
      exts.each { |ext|
        exe = File.join(path, "#{cmd}#{ext}")
        return exe if File.executable? exe
      }
    end
    return nil
  end

  def detect_host
    return @detect_host if defined?(@detect_host)

    begin
      ENV["LC_ALL"], old_lc_all = "C", ENV["LC_ALL"]

      output = `#{gcc_cmd} -v 2>&1`
      if m = output.match(/^Target\: (.*)$/)
        @detect_host = m[1]
      end

      @detect_host
    ensure
      ENV["LC_ALL"] = old_lc_all
    end
  end

  def extract_file(file, target)
    filename = File.basename(file)
    FileUtils.mkdir_p target

    message "Extracting #{filename} into #{target}... "
    result = `#{tar_exe} #{tar_compression_switch(filename)}xf "#{file}" -C "#{target}" 2>&1`
    if $?.success?
      output "OK"
    else
      output "ERROR"
      output result
      raise "Failed to complete extract task"
    end
  end

  def execute(action, command)
    log        = log_file(action)
    log_out    = File.expand_path(log)
    redirected = command << " >#{log_out} 2>&1"

    Dir.chdir work_path do
      message "Running '#{action}' for #{@name} #{@version}... "
      system redirected
      if $?.success?
        output "OK"
        return true
      else
        output "ERROR, review '#{log}' to see what happened."
        raise "Failed to complete #{action} task"
      end
    end
  end

  def newer?(target, checkpoint)
    if (target && File.exist?(target)) && (checkpoint && File.exist?(checkpoint))
      File.mtime(target) > File.mtime(checkpoint)
    else
      false
    end
  end

  # print out a message with the logger
  def message(text)
    @logger.print text
    @logger.flush
  end

  # print out a message using the logger but return to a new line
  def output(text = "")
    @logger.puts text
    @logger.flush
  end

  # Slighly modified from RubyInstaller uri_ext, Rubinius configure
  # and adaptations of Wayne's RailsInstaller
  def download_file(url, full_path, count = 3)
    return if File.exist?(full_path)
    uri = URI.parse(url)
    begin
      case uri.scheme.downcase
      when /ftp/
        download_file_ftp(uri, full_path)
      when /http|https/
        download_file_http(url, full_path, count)
      end
    rescue Exception => e
      File.unlink full_path if File.exists?(full_path)
      output "ERROR: #{e.message}"
      raise "Failed to complete download task"
    end
  end

  def download_file_http(url, full_path, count = 3)
    filename = File.basename(full_path)

    if ENV['http_proxy']
      protocol, userinfo, host, port  = URI::split(ENV['http_proxy'])
      proxy_user, proxy_pass = userinfo.split(/:/) if userinfo
      http = Net::HTTP::Proxy(host, port, proxy_user, proxy_pass)
    else
      http = Net::HTTP
    end

    message "Downloading #{filename} "
    http.get_response(URI.parse(url)) do |response|
      case response
      when Net::HTTPNotFound
        output "404 - Not Found"
        return false

      when Net::HTTPClientError
        output "Error: Client Error: #{response.inspect}"
        return false

      when Net::HTTPRedirection
        raise "Too many redirections for the original URL, halting." if count <= 0
        url = response["location"]
        return download_file(url, full_path, count - 1)

      when Net::HTTPOK
        with_tempfile(filename, full_path) do |temp_file|
          size = 0
          progress = 0
          total = response.header["Content-Length"].to_i
          response.read_body do |chunk|
            temp_file << chunk
            size += chunk.size
            new_progress = (size * 100) / total
            unless new_progress == progress
              message "\rDownloading %s (%3d%%) " % [filename, new_progress]
            end
            progress = new_progress
          end
          output
        end
      end
    end
  end

  def download_file_ftp(uri, full_path)
    filename = File.basename(uri.path)
    with_tempfile(filename, full_path) do |temp_file|
      size = 0
      progress = 0
      Net::FTP.open(uri.host, uri.user, uri.password) do |ftp|
        ftp.passive = true
        ftp.login
        remote_dir = File.dirname(uri.path)
        ftp.chdir(remote_dir) unless remote_dir == '.'
        total = ftp.size(filename)
        ftp.getbinaryfile(filename, temp_file.path, 8192) do |chunk|
          # Ruby 1.8.7 already wrote the chunk into the file
          unless RUBY_VERSION < "1.9"
            temp_file << chunk
          end

          size += chunk.size
          new_progress = (size * 100) / total
          unless new_progress == progress
            message "\rDownloading %s (%3d%%) " % [filename, new_progress]
          end
          progress = new_progress
        end
      end
      output
    end
  end

  def with_tempfile(filename, full_path)
    temp_file = Tempfile.new("download-#{filename}")
    temp_file.binmode
    yield temp_file
    temp_file.close
    File.unlink full_path if File.exists?(full_path)
    FileUtils.mkdir_p File.dirname(full_path)
    FileUtils.mv temp_file.path, full_path, :force => true
  end

  def gcc_cmd
    cc = ENV["CC"] || RbConfig::CONFIG["CC"] || "gcc"
    return cc.dup
  end

  def make_cmd
    m = ENV['MAKE'] || ENV['make'] || 'make'
    return m.dup
  end
end
