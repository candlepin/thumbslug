%global _binary_filedigest_algorithm 1
%global _source_filedigest_algorithm 1
%global _binary_payload w9.gzdio
%global _source_payload w9.gzdio

Name: thumbslug
Summary: Thumbslug CDN proxy
Group: Internet/Applications
License: GPLv2
Version: 0.0.18
Release: 1%{?dist}
URL: http://fedorahosted.org/thumbslug
Source: %{name}-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-buildroot
Vendor: Red Hat, Inc
BuildArch: noarch

Requires(pre): shadow-utils

BuildRequires: ant >= 1.7.0
BuildRequires: thumbslug-deps >= 0.0.8

%define __jar_repack %{nil}

%description
Thumbslug is a content and entitlement proxy for on premesis Candlepin installs.

%prep
%setup -q 

%build
ant -Dlibdir=/usr/share/thumbslug/lib/ clean package

%install
install -d -m 755 $RPM_BUILD_ROOT/%{_datadir}/%{name}/
install -m 644 target/%{name}.jar $RPM_BUILD_ROOT/%{_datadir}/%{name}

install -d -m 755 $RPM_BUILD_ROOT/%{_initddir}
install -m 755 thumbslug.init $RPM_BUILD_ROOT/%{_initddir}/%{name}

install -d -m 755 $RPM_BUILD_ROOT/%{_sysconfdir}/thumbslug
install -m 640 thumbslug.conf \
            $RPM_BUILD_ROOT/%{_sysconfdir}/thumbslug/thumbslug.conf

install -d -m 775 $RPM_BUILD_ROOT/%{_var}/log/thumbslug


%clean
rm -rf $RPM_BUILD_ROOT


%pre
getent group thumbslug >/dev/null || groupadd -r thumbslug
getent passwd thumbslug >/dev/null || \
    useradd -r -g thumbslug -d %{_datadir}/%{name} -s /sbin/nologin \
    -c "thumbslug content and entitlement proxy" thumbslug 
exit 0


%post
/sbin/chkconfig --add %{name}


%postun
if [ "$1" -ge "1" ] ; then
    /sbin/service %{name} condrestart >/dev/null 2>&1 || :
fi


%preun
if [ $1 -eq 0 ] ; then
    /sbin/service %{name} stop >/dev/null 2>&1
    /sbin/chkconfig --del %{name}
fi



%files
%defattr(-, root, thumbslug)
%doc README
%{_initddir}/%{name}

%dir %{_sysconfdir}/thumbslug
%config(noreplace) %{_sysconfdir}/%{name}/%{name}.conf

%dir %{_datadir}/%{name}
%{_datadir}/%{name}/thumbslug.jar

%dir %{_var}/log/thumbslug
%ghost %attr(660, thumbslug, thumbslug) %{_var}/run/thumbslug.pid
%ghost %attr(660, thumbslug, thumbslug) %{_var}/lock/subsys/thumbslug


%changelog
* Mon Nov 28 2011 James Bowes <jbowes@redhat.com> 0.0.18-1
- Add chkconfig initscript lines (jbowes@redhat.com)

* Wed Nov 23 2011 James Bowes <jbowes@redhat.com> 0.0.17-1
- add rpm scriptlets for init script (jbowes@redhat.com)
- add a note about client configuration in the readme (jbowes@redhat.com)
- update to latest akuma (jbowes@redhat.com)
- change lock file (jbowes@redhat.com)
- print ssl error to log file (jbowes@redhat.com)
- fix ant packaging (jbowes@redhat.com)
- add restart and status init targets (jbowes@redhat.com)
- Add config file for etc (jbowes@redhat.com)
- Set up thumbslug user (jbowes@redhat.com)
- update spec to install init file (jbowes@redhat.com)
- Allow overriding config values from thumbslug.conf (jbowes@redhat.com)
- fill in spec description (jbowes@redhat.com)
- update config documentation (jbowes@redhat.com)

* Wed Nov 16 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.16-1
- ibiblio moved. (jesusr@redhat.com)

* Wed Nov 16 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.15-1
- Support downloading entitlement certificates from candlepin (jbowes@redhat.com)
- Switch to PEM format files for thumbslug to CDN connection (jbowes@redhat.com)

* Wed Oct 26 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.14-1
- respin thumbslug (jesusr@redhat.com)

* Tue Oct 25 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.13-1
- WIP ts -> candlepin code for chris to pick up (jbowes@redhat.com)
- remove TODO about getSink (jbowes@redhat.com)
- add testing servlets for spec tests (cduryee@redhat.com)
- use correct dist-cvs branch (jesusr@redhat.com)
- Don't bother encoding/decoding the request/response bodies (jbowes@redhat.com)

* Tue Oct 11 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.12-1
- adding proper branch (jesusr@redhat.com)

* Tue Oct 11 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.11-1
- allow dist-cvs building (jesusr@redhat.com)

* Mon Oct 10 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.10-1
- update gitignore (jbowes@redhat.com)
- checkstyle fixup (jbowes@redhat.com)

* Wed Jul 13 2011 Chris Duryee (beav) <cduryee@redhat.com>
- ant jar stuff (cduryee@redhat.com)
- Merge commit 'ed410d78475813f9bd8e28530408c60280c7a000' (cduryee@redhat.com)
- first cut at a thumbslug init file (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- pull the jar into the rpm (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- make the jar file have a consistent name (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- dont generate schema (we dont have any) (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- include ant (cduryee@redhat.com)

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- try to make an rpm (cduryee@redhat.com)

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- bump the version 

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- new package built with tito

* Mon Jul 11 2011 Chris Duryee <cduryee@redhat.com> 0.0.1-1
- first cut (cduryee@redhat.com)
