# Prepares a record for the test and fetches changes
# afterwards.
#
# @param [String] root_path the path to watch
# @param [Hash] options
# @option options [Array<string>] :paths optional paths fetch changes for
# @option options [Boolean] :use_last_record allow the use of an already
#   created record, handy for ordered tests.
#
# @return [Array, Array, Array] the changes
#
def changes(root_path, options = {})
  unless @record || options[:use_last_record]
    @record = Listen::DirectoryRecord.new(root_path)
    @record.filter(options.delete(:filter)) if options[:filter]
    @record.ignore(options.delete(:ignore)) if options[:ignore]

    # Build the record after adding the filtering and ignoring patterns
    @record.build
  end

  yield if block_given?

  paths = options.delete(:paths) || [root_path]
  options[:recursive] = true if options[:recursive].nil?

  changes = @record.fetch_changes(paths, {:relative_paths => true}.merge(options))

  [changes[:modified], changes[:added], changes[:removed]]
end

# Generates a small time difference before performing a time sensitive
# task (like comparing mtimes of files).
#
# @note Modification time for files only includes the milliseconds on Linux with MRI > 1.9.2,
#   that's why we generate a difference that's greater than 1 second.
#
def small_time_difference
  t = Time.now
  diff = t.to_f - t.to_i

  sleep( 1.5 - (diff < 0.5 ? diff : 0.4) )
end

# Ensures that the test runs at almost the same second at which
# changes are being checked.
#
def ensure_same_second
  t = Time.now
  diff = t.to_f - t.to_i

  if diff > 0.1 # We are not at the beginning of a second
    sleep 1.1 - diff # 1.1 is used instead of 1 to account for the processing time (estimated at 0.1 sec)
  end
end
