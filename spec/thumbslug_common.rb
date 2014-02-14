require 'openssl'
require 'webrick'
require 'webrick/https'
require 'net/http'
require 'zlib'

module ThumbslugMethods

  include WEBrick

  def get(url_str, headers = nil, pem = nil)
    uri = URI.parse(url_str)
    client = Net::HTTP.new uri.host, uri.port
    if uri.scheme == 'https'
      client.use_ssl = true
      client.verify_mode = OpenSSL::SSL::VERIFY_NONE #TODO: verify server
      client.ca_file = "CA/cacert.pem"

      if pem.nil?
        pem = "spec/data/spec/test-entitlement.pem"
      end

      client.key = OpenSSL::PKey::RSA.new(File.read(pem))
      client.cert = OpenSSL::X509::Certificate.new(File.read(pem))
    end
    return client.request(Net::HTTP::Get.new(uri.path, headers))
  end
  module_function :get

  def create_httpd(secure=false)
    privkey = OpenSSL::PKey::RSA.new(File.read("spec/data/cdn-key.pem"))
    server_cert = OpenSSL::X509::Certificate.new(File.read("spec/data/cdn.pem"))
    ca_cert = "spec/data/CA/cdn-ca.pem"
    log_file = 'webrick.log'
    access_log_stream = File.open(log_file, 'w')
    # See http://www.ruby-doc.org/stdlib-2.0/libdoc/webrick/rdoc/WEBrick/AccessLog.html
    access_log = [
      [access_log_stream, WEBrick::AccessLog::COMMON_LOG_FORMAT],
    ]
    if secure
      config = {
        :Port => 9090,
        :BindAddress => '127.0.0.1',
        :DocumentRoot => Dir.pwd + '/spec/data/',
        :Debugger => true,
        :Verbose => true,
        :SSLEnable => true,
        :SSLVerifyClient => OpenSSL::SSL::VERIFY_PEER | OpenSSL::SSL::VERIFY_FAIL_IF_NO_PEER_CERT,
        :SSLPrivateKey => privkey,
        :SSLCertificate => server_cert,
        :SSLCACertificateFile => ca_cert,
        :Logger => WEBrick::Log.new(log_file),
        :AccessLog => access_log,
      }
    else
      config = {
        :Port => 9090,
        :BindAddress => '127.0.0.1',
        :DocumentRoot => Dir.pwd + '/spec/data/',
        :Logger => WEBrick::Log.new(log_file),
        :AccessLog => access_log,
      }
    end
    rd, wr = IO.pipe
    pid = fork {
      rd.close #no need for this side in here
      $stderr = File.open('/dev/null', 'w')
      begin
        server = HTTPServer.new(config)
        trap('INT') { server.stop }
        server.mount "/this_will_500", FiveHundred
        server.mount "/this_will_400", FourHundred
        server.mount "/subscription-cert", GetCert
        server.mount "/trace", Trace
        server.mount "/lorem.ipsum", GzipServlet
        wr.write "Started webrick"
      rescue
        wr.write "Error starting webrick"
      ensure
        wr.close  #this makes the parent process unblock and continue
      end
      server.start
    }
    wr.close #no need for this side in here
    puts "#{rd.read}" #this will wait for the forked process to send an EOF
    rd.close
    return pid
  end
  module_function :create_httpd

  def create_thumbslug(params={}, jvm_debug=false)
    jar = " -cp ./target/thumbslug-1.0.0.jar org.candlepin.thumbslug.Main /dev/null"

    #these need to all be strings
    config = {
      'daemonize' => 'false',
      'port' => '8088',
      'ssl' => 'true',
      'ssl.keystore' => 'spec/data/keystore-spec.p12',
      'ssl.keystore.password' => 'pass',
      'ssl.ca.keystore' => 'spec/data/CA/candlepin-ca.crt',
      'ssl.client.keystore' => 'spec/data/cdn-client.pem',
      'ssl.client.dynamicSsl' => 'false',
      'cdn.port' => '9090',
      'cdn.host' => 'localhost',
      'cdn.ssl' => 'true',
      'cdn.ssl.ca.keystore' => 'spec/data/CA/cdn-ca.pem',
      'cdn.sendTSHeader' => 'false',
      'candlepin.host' => 'localhost',
      'candlepin.port' => '9898',
      'candlepin.oauth.key' => 'thumbslug',
      'candlepin.oauth.secret' => 'shhhhh',
      'cdn.proxy' => 'false',
      'log.error' => 'error.log',
      'log.access' => 'access.log',
    }

    params.each_pair do |key, value|
      config[key] = value
    end

    jvm_args = config.map do |key, value|
      "-D#{key}=#{value}"
    end
    debug_args = "-agentlib:jdwp=transport=dt_socket,address=8123,server=y,suspend=n"
    jvm_args << debug_args if jvm_debug
    tslug_exec_string = "java #{jvm_args.join(' ')} #{jar}"
    pipe = IO.popen(tslug_exec_string, "w+")
    #this is perlesque
    while pipe.gets()
      break if $_ =~ /Running Thumbslug/
    end 
    return pipe
  end
  module_function :create_thumbslug
end


class FiveHundred < WEBrick::HTTPServlet::AbstractServlet

  def do_GET(request, response)
    response.status = 500 
    response['Content-Type'] = "text/plain"
    response.body = 'Error! a 500 error'
  end

end


class FourHundred < WEBrick::HTTPServlet::AbstractServlet

  def do_GET(request, response)
    response.status = 400
    response['Content-Type'] = "text/plain"
    response.body = 'Error! a 400 error'
  end

end

class Trace < WEBrick::HTTPServlet::AbstractServlet
  #ersatz trace that implements GET, not TRACE verb

  def do_GET(request, response)
    response.status = 200
    response['Content-Type'] = "text/plain"
    headers = ""
    request.raw_header.each {|r| headers += r }
    response.body = headers
  end

end

class GetCert < WEBrick::HTTPServlet::AbstractServlet
  #pretend to return a cert

  def do_GET(request, response)
    response.status = 200
    response['Content-Type'] = "text/plain"
    body = ""
    File.open("spec/data/example-subscription-cert.json", 'r') do |file|
        body = file.read
    end
    response.body = body
  end
end


class GzipServlet < WEBrick::HTTPServlet::AbstractServlet

  def do_GET(request, response)
    response.status = 200
    response['Content-Type'] = "text/plain"

    body = ""
    if request['Accept-Encoding'] =~ /gzip/
      File.open("spec/data/lorem.ipsum", 'r') do |file|
        zipped = StringIO.new(body, 'w')
        gz = Zlib::GzipWriter.new(zipped)
        gz.write(file.read)
        gz.close
        response['Content-Encoding'] = "gzip"
      end
    else
      File.open("spec/data/lorem.ipsum", 'r') do |file|
        body = file.read
      end
    end

    response.body = body
  end

end
