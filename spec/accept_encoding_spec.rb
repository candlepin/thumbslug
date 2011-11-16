require 'net/https'
require 'uri'
require 'thumbslug_common'
require 'timeout'
require 'zlib'
require 'stringio'

describe 'Accept Encoding header' do
  include ThumbslugMethods

  around(:each) do |test|
    Timeout::timeout(6) {
      test.run
    }
  end

  before(:all) do
    @http_proc = create_httpd
    @tslug_pipe =  create_thumbslug({:ssl => 'false', :cdn_ssl => 'false'})
    @tslug_header_pipe = create_thumbslug({:ssl => 'false',
                                          :cdn_sendTSHeader => 'true',
                                          :port => '8089',
                                          :cdn_ssl => 'false'})
  end

  after(:all) do
    #clean up what we forked out
    Process.kill('INT', @http_proc)
    Process.kill('INT', @tslug_pipe.pid)
    Process.kill('INT', @tslug_header_pipe.pid)
    puts "Waiting for forked procs to terminate..."
    Process.waitall()
    puts "Done"
  end


  it 'should let the client request gzip encoded responses' do
    # if the client requests a compressed response, that request
    # should go on to the cdn, and we should respect whatever the CDN does.
    filename = Dir.pwd + '/spec/data/lorem.ipsum'
    uri = URI.parse('http://127.0.0.1:8089/lorem.ipsum')
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Get.new(uri.request_uri)
    request.initialize_http_header({"Accept-Encoding" => "gzip;q=1.0"})
    response = http.request(request)
    response.code.should == "200"

    inflater = Zlib::GzipReader.new(StringIO.new(response.body.to_s))
    buf = inflater.read

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(buf)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'should not encode responses if the client doesnt ask for it' do
    # if the client requests a compressed response, that request
    # should go on to the cdn, and we should respect whatever the CDN does.
    filename = Dir.pwd + '/spec/data/lorem.ipsum'
    uri = URI.parse('http://127.0.0.1:8089/lorem.ipsum')
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Get.new(uri.request_uri)
    response = http.request(request)
    response.code.should == "200"

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

end
