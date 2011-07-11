# Read in user defined variables
if [ -f $HOME/.thumbslugrc ] ; then
    source $HOME/.thumbslugrc
fi

# verify our configs are setup correctly
if [ -z $BASEDIR ] ; then
    echo "BASEDIR not set, please tell me where to build"
    exit 1
fi

if [ -z $DEPSDIR ] ; then
    echo "DEPSDIR not set, where is thumbslug-deps src tree?"
    exit 1
fi

if [ -z $TSDIR ] ; then
    echo "TSDIR not set, where is thumbslug src tree?"
    exit 1
fi

DEPSVERSION=`rpm -q --qf '%{version}-%{release}\n' --specfile $DEPSDIR/thumbslug-deps.spec | head -1`
VERSION=`rpm -q --qf '%{version}-%{release}\n' --specfile $TSDIR/thumbslug.spec | head -1`

pushd $DEPSDIR
tito build --rpm
popd
pushd $TSDIR
tito build --srpm
popd

for i in fedora-15-x86_64
do
    rm -rf /tmp/ts-$i/
    mock -r $i --init
    mock -r $i --install $BASEDIR/noarch/thumbslug-deps-$DEPSVERSION.noarch.rpm
    mock -r $i --installdeps $BASEDIR/thumbslug-$VERSION.src.rpm
    mock -r $i --copyin $BASEDIR/thumbslug-$VERSION.src.rpm  /tmp
    mock -r $i --chroot "cd; rpmbuild --rebuild /tmp/thumbslug-$VERSION.src.rpm"
    mock -r $i --copyout /builddir/build/RPMS/ /tmp/ts-$i/
    mock -r $i --copyout /tmp/thumbslug-$VERSION.src.rpm /tmp/ts-$i/
done
