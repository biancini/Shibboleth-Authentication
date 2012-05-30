<?php

/**********************************************************************************************

PHP Page returning virutal passwd and group files to be used by NSS module to map uid/gid with
valid users on Shibboleth IdP.

This page reads the querystring and returns:
- the passwd virtual file if ?passwd is passed as querystring
- the group virtual file if ?group is passed as querystring

The example provided below is intended to access the IdP LDAP with valid administrative
credentials.

***********************************************************************************************/

  // Specificiation of parameters to connect with the LDAP of the IdP
  $LDAPHost = "127.0.0.1";
  $LDAPPort = "389";
  $dn = "ou=people,dc=example,dc=com";
  $LDAPUser = "cn=admin,dc=example,dc=com";
  $LDAPUserPassword = "password";
  $LDAPFieldsToFind = array("uid", "uidnumber", "gidnumber", "displayname", "homedirectory", "loginshell");

  // Connection with LDAP and query for users
  $cnx = ldap_connect($LDAPHost, $LDAPPort) or die("Could not connect to LDAP");
  ldap_set_option($cnx, LDAP_OPT_PROTOCOL_VERSION, 3);
  ldap_bind($cnx, $LDAPUser, $LDAPUserPassword) or die("Could not bind to LDAP");
  error_reporting (E_ALL ^ E_NOTICE);
  $filter = "(&(objectClass=inetOrgPerson)(uid=*))";
  $sr = ldap_search($cnx, $dn, $filter, $LDAPFieldsToFind);
  $info = ldap_get_entries($cnx, $sr);

  // Production of output file
  if ($_SERVER['QUERY_STRING'] == "group") {
    // Production group file

    for ($x=0; $x<$info["count"]; $x++) {
      $gid = $info[$x]['uid'][0];
      $uidNumber = $info[$x]['uidnumber'][0];
      $members = "";

      printf("%s:x:%s:%s\n", $gid, $uidNumber, $members);
    }
  }
  else {
    // Production passwd file
  
    for ($x=0; $x<$info["count"]; $x++) {
      $uid = $info[$x]['uid'][0];
      $uidNumber = $info[$x]['uidnumber'][0];
      $gidNumber = $info[$x]['gidnumber'][0];
      $displayName = $info[$x]['displayname'][0];
      $homeDirectory = $info[$x]['homedirectory'][0];
      $loginShell = $info[$x]['loginshell'][0];

      printf("%s:x:%s:%s:%s:%s:%s\n", $uid, $uidNumber, $gidNumber, $displayName, $homeDirectory, $loginShell);
    }
  }
?>

