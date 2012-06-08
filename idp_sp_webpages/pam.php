<?php

/**********************************************************************************************

PHP Page returning values to create the user session and to obtain a trusthworthy
authentication.

This page must returns a list of items like:
  key1=value1
  key2=value2
  key3=value3
  ...
containing all the relevant information about the user session.

The example provided below, lists from the $_SERVER object a bunch of attributes obtained
via Shibboleth after the user authentication and session creation.

***********************************************************************************************/
header("Content-type: text/plain");

// At first put a value in session called autheticated, this value will contain true or false
// depending on if the user should be granted access or not.
print "authenticated=".(eval_authenticateduser() ? "true" : "false")."\n";

// Then all the specified attributes in the $_SERVER variable are put into user session.
// The $headers array contains a list of variable names to be added to the session
$headers = array('Shib-Application-ID', 'Shib-Session-ID', 'Shib-Identity-Provider', 'Shib-Authentication-Instant',
                 'Shib-Authentication-Method', 'Shib-AuthnContext-Class', 'Shib-Session-Index',
                 'eduPersonEntitlement', 'eduPersonPrincipalName', 'eduPersonScopedAffiliation',  'eduPersonTargetedID',
                 'email', 'givenName', 'mail', 'name', 'sn', 'surname', 'uid');


foreach($_SERVER as $key => $value) {
        if(in_array($key, $headers)) {
                print $key."=".$value."\n";
        }
}

// At last the unique key of the cookie whose name starts with "_shibsession_" is added to the user session.
// This key is necessary to retrieve the session from the SP in further HTTP calls.
$cookies = $_SERVER["HTTP_COOKIE"];
foreach (explode(";", $cookies) as $curcookie) {
        if (strpos($curcookie, "=") !== false) {
                list($cookiename, $cookieval) = explode("=", trim($curcookie));
                if (strpos($cookiename, "_shibsession_") !== false) {
                        print "Shib-Session-Unique=".trim(str_replace("_shibsession_", "", $cookiename)."\n");
                }
        }
}

// Function implementing the logic of user authentication.
// Returns true if the user should be granted access and false otherwise.
function eval_authenticateduser() {
        return true;
}
?>

