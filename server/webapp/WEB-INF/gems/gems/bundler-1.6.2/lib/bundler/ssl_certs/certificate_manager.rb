require 'fileutils'

module Bundler
  module SSLCerts
    class CertificateManager
      attr_reader :bundler_cert_path, :bundler_certs, :rubygems_certs

      def self.update_from!(rubygems_path)
        new(rubygems_path).update!
      end

      def initialize(rubygems_path)
        rubygems_certs = File.join(rubygems_path, 'lib/rubygems/ssl_certs')
        @rubygems_certs = certificates_in(rubygems_certs)

        @bundler_cert_path = File.expand_path("..", __FILE__)
        @bundler_certs = certificates_in(bundler_cert_path)
      end

      def up_to_date?
        bundler_certs.zip(rubygems_certs).all? do |bc, rc|
          File.basename(bc) == File.basename(rc) && FileUtils.compare_file(bc, rc)
        end
      end

      def update!
        return if up_to_date?

        FileUtils.rm bundler_certs
        FileUtils.cp rubygems_certs, bundler_cert_path
      end

    private

      def certificates_in(path)
        Dir[File.join(path, "*.pem")].sort
      end

    end
  end
end
