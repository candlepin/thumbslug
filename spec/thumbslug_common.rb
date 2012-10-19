require 'openssl'
require 'webrick/https'

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

  def create_httpd(secure=false)
    privkey = OpenSSL::PKey::RSA.new(File.read("spec/data/cdn-key.pem"))
    server_cert = OpenSSL::X509::Certificate.new(File.read("spec/data/cdn.pem"))
    ca_cert = "spec/data/CA/cdn-ca.pem" 
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
        #comment out these two lines to enable webrick logging
        :Logger => WEBrick::Log.new('/dev/null'),
        :AccessLog => [nil, nil],
      }
    else
      config = {
        :Port => 9090,
        :BindAddress => '127.0.0.1',
        :DocumentRoot => Dir.pwd + '/spec/data/',
        #comment out these two lines to enable webrick logging
        :Logger => WEBrick::Log.new("/dev/null"),
        :AccessLog => [nil, nil],
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

  def create_thumbslug(params={})

    jar = " -jar " + Dir.pwd + "/target/thumbslug-1.0.0.jar"

    #these need to all be strings
    config = {
     :port => '8088',
     :ssl => 'true',
     :ssl_keystore => 'spec/data/keystore-spec.p12',
     :ssl_keystore_password => 'pass',
     :ssl_ca_keystore => 'spec/data/CA/cpin-cacert.pem',
     :ssl_client_keystore => 'spec/data/cdn-client.pem',
     :ssl_client_dynamic_ssl => 'false',
     :cdn_port => '9090',
     :cdn_host => 'localhost',
     :cdn_ssl => 'true',
     :cdn_ssl_ca_keystore => 'spec/data/CA/cdn-ca.pem',
     :cdn_sendTSHeader => 'false',
     :candlepin_host => 'localhost',
     :candlepin_port => '9898',
     :candlepin_oauth_key => 'thumbslug',
     :candlepin_oauth_secret => 'shhhhh',
    }

    params.each_pair do |key, value|
      config[key] = value
    end

    tslug_exec_string = "java " +
                 " -Ddaemonize=false" +
                 " -Dport=" + config[:port] +
                 " -Dssl=" + config[:ssl] +
                 " -Dssl.client.keystore=" + config[:ssl_client_keystore] +
                 " -Dssl.client.dynamicSsl=" + config[:ssl_client_dynamic_ssl] +
                 " -Dssl.keystore=" + config[:ssl_keystore] +
                 " -Dssl.keystore.password=" + config[:ssl_keystore_password] +
                 " -Dssl.ca.keystore=" + config[:ssl_ca_keystore] +
                 " -Dcdn.port=" + config[:cdn_port] +
                 " -Dcdn.host=" + config[:cdn_host] +
                 " -Dcdn.ssl=" + config[:cdn_ssl] +
                 " -Dcdn.ssl.ca.keystore=" + config[:cdn_ssl_ca_keystore] +
                 " -Dcandlepin.host=" + config[:candlepin_host] +
                 " -Dcandlepin.port=" + config[:candlepin_port] +
                 " -Dcdn.sendTSheader=" + config[:cdn_sendTSHeader] +
                 " -Dcandlepin.oauth.key=" + config[:candlepin_oauth_key] +
                 " -Dcandlepin.oauth.secret=" + config[:candlepin_oauth_secret] +
                 " -Dcdn.proxy=false" +
                 " -Dlog.error=error.log" +
                 " -Dlog.access=access.log" +
                 #" -Dcdn.proxy.host=proxy-ip-here" +
                 #" -Dcdn.proxy.port=proxy-port-here" +
                 jar
    pipe = IO.popen(tslug_exec_string, "w+")
    #this is perlesque
    while pipe.gets()
      break if $_ =~ /Running Thumbslug/
    end 
    return pipe
  end
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
