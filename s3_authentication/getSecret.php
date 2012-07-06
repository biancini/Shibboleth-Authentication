<?php
header("Content-type: text/plain");

$debug = strtolower($_SERVER['QUERY_STRING']) == "debug";

$urlIdP = $_SERVER["Shib-Identity-Provider"];
$urlIdP = str_replace("http://", "https://", $urlIdP);
$urlIdP = str_replace("/idp/shibboleth", "/idp/retrieveS3", $urlIdP);

if ($debug) print $urlIdP . "\n";

$eduPersonTargetedId = explode("!", $_SERVER["eduPersonTargetedID"]);
$spEntityId = $eduPersonTargetedId[1];

if ($debug) print $spEntityId . "\n";

$curl_handle = curl_init();
curl_setopt($curl_handle, CURLOPT_URL, $urlIdP);
curl_setopt($curl_handle, CURLOPT_SSL_VERIFYPEER, 0);
curl_setopt($curl_handle, CURLOPT_SSL_VERIFYHOST, 0);
curl_setopt($curl_handle, CURLOPT_HTTPHEADER, array("SP_EntityId: " . $spEntityId, "SessionIndex: " . $_SERVER["Shib-Session-Index"]));
if ($debug) curl_setopt($curl_handle, CURLOPT_HEADER, true);

print "Chiamata";
$result = curl_exec($curl_handle);
print "---> $result";
curl_close($curl_handle);

if ($result['curl_error'] != "") {
        if ($debug) print "ERROR: " + $result['curl_error'];
}
else {
        if ($debug) print $result['body'];
}
?>
