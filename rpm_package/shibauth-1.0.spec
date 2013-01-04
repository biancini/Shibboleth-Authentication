Name:		shibauth
Version:	1.0
Release:	1%{?dist}
Summary:	PAM and NSS modules for authenticating with Shibboleth.

Group:		libs
License:	GPL-2
URL:		https://github.com/biancini/Shibboleth-Authentication
Source0:	%{name}-%{version}.tar.gz
BuildArch:	i386
BuildRoot:	%(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)

Requires:	libcurl, libcurl-devel, nss, openssl-devel, pam-devel, libconfig-devel

%description
Implementation of a PAM and a NSS module used to integrate Shibboleth between the
authentication mechanisms for a Linux workstation.

The PAM module permits to authenticate over HTTP Basic Authentication.
It uses libcurl to interact with the webserver and reads user session information
from the web pages served after authentication.

The NSS module permits to obtain user information from a web page behind basic
authentication. The page must expose two different contents depending on query string:
- if ?passwd is provided the page must supply a file with the same structure of
  /etc/passwd and listing all users defined by the server
- if ?group is provided the page must supply a file with the same structure of
  /etc/group and listing all user groups defined by the server

%prep
%setup -q


%build
make %{?_smp_mflags}


%install
rm -rf $RPM_BUILD_ROOT
make install DESTDIR=$RPM_BUILD_ROOT


%clean
rm -rf $RPM_BUILD_ROOT


%post
echo "Installed PAM and NSS module. Proceed with usual configuration."



%files
%defattr(-,root,root)
%doc README.md
%config /etc/*
/lib/security/pam_http.so
/lib/libnss_shib.so.2


%changelog
* Fri Jan 04 2013 Andrea Biancini <andrea.biancini@gmail.com>
- Initial build 

