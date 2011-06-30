require 'webrick'
require 'net/http'
require 'uri'
require 'thumbslug_common'

include WEBrick

describe 'HTTP proxying' do
  include ThumbslugMethods
  before(:all) do
    @http_proc = create_httpd
    @tslug_pipe =  create_thumbslug({:ssl => 'false', :cdn_ssl => 'false'})
    @tslug_header_pipe = create_thumbslug({:ssl => 'false',
                                          :sendTSHeader => 'true',
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


  it 'validate mocked env' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('http://127.0.0.1:9090/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a file from thumbslug' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('http://127.0.0.1:8088/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a 404 from the cdn' do
    response = get('http://127.0.0.1:8088/this_will_404')
    response.code.should == '404'
  end

  it 'pull a 500 from the cdn' do
    response = get('http://127.0.0.1:8088/this_will_500')
    response.code.should == '500'
  end

  it 'check that headers are being passed through' do
    response = get('http://127.0.0.1:8088/trace', {'captain'=>'sub'})
    header_idx = response.body =~ /sub/
    header_idx.should > 0
  end

  it 'check that thumbslug header is added' do
    #8089 is the thumbslug instance with header injection
    response = get('http://127.0.0.1:8089/trace')
    header_idx = response.body =~ /Thumbslug/
    header_idx.should > 0
  end
end
