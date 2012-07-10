<?php

header("Content-type: text/plain");

$debug = strtolower($_SERVER['QUERY_STRING']) == "debug";

$urlIdP = $_SERVER["Shib-Identity-Provider"];
$urlIdP = str_replace("http://", "https://", $urlIdP);
$urlIdP = str_replace("/idp/shibboleth", "/idp/retrieveS3", $urlIdP);

if ($debug) print $urlIdP . "\n";

$eduPersonTargetedId = explode("!", $_SERVER["eduPersonTargetedID"]);
$accessKey = $_SERVER["amazonS3AccessKey"];
$spEntityId = $eduPersonTargetedId[1];

if ($debug) print $spEntityId . "\n";

$headers = array("SP_EntityId: " . $spEntityId,
                 "SessionIndex: " . $_SERVER["Shib-Session-Index"],
                 "AccessKey: " . $accessKey);

$curl_handle = curl_init();
curl_setopt($curl_handle, CURLOPT_URL, $urlIdP);
curl_setopt($curl_handle, CURLOPT_SSL_VERIFYPEER, 0);
curl_setopt($curl_handle, CURLOPT_SSL_VERIFYHOST, 0);
curl_setopt($curl_handle, CURLOPT_HTTPHEADER, $headers);
if ($debug) curl_setopt($curl_handle, CURLOPT_HEADER, true);

$result = curl_exec($curl_handle);
curl_close($curl_handle);

if ($result['curl_error'] != "") {
	if ($debug) print "ERROR: " + $result['curl_error'];
}
else {
	if ($debug) print $result['body'];
}
?>

