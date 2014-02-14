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
    @tslug_header_pipe = create_thumbslug({'ssl' => 'false',
                                          'cdn.sendTSHeader' => 'true',
                                          'cdn.ssl' => 'false'})
  end

  after(:all) do
    #clean up what we forked out
    Process.kill('INT', @http_proc)
    Process.kill('INT', @tslug_header_pipe.pid)
    puts "Waiting for forked procs to terminate..."
    Process.waitall()
    puts "Done"
  end


  it 'should let the client request gzip encoded responses' do
    # if the client requests a compressed response, that request
    # should go on to the cdn, and we should respect whatever the CDN does.
    filename = Dir.pwd + '/spec/data/lorem.ipsum'
    response = get('http://127.0.0.1:8088/lorem.ipsum', {'Accept-Encoding' => 'gzip;q=1.0'})
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
    response = get('http://127.0.0.1:8088/lorem.ipsum')
    response.code.should == "200"

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

end
