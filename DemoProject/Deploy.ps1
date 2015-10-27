$source = "C:\TeamCity\buildAgent\work\1d831bc68e6b0d1e\DemoProject\SampleCDRMWebApp\SampleCDRMWebApp"
$destination = "D:\DeploymentTest"
Copy-Item -Recurse $source -Destination $destination