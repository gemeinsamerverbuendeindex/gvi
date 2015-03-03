### Collection
* Create the GVI-Collection  
`http://MYSERVER:MYPORT/solr/admin/collections?action=CREATE&name=GVI&collection.configName=GVIconf&numShards=5`
* Delete the GVI-Collection  
`http://MYSERVER:MYPORT/solr/admin/collections?action=DELETE&name=GVI`
* Upload local configuration  
`> java -classpath solr-webapp/webapp/WEB-INF/lib/*:lib/ext/* org.apache.solr.cloud.ZkCLI -z MYZOOKEEPER(s) -cmd upconfig -confname GVIconf -confdir MYCONFDIR`

### SOLR
* Start a solr-node (low level)  
`> cd example_in_solr_package`  
`> java -server -d64 -Xmx5g -Xms5g -XX:+UseG1GC  -Djetty.port=MYPORT -DhostName=MYSERVER -Dsolr.solr.home=/gvi/index/Server_1 -DzkHost=MYZOOKEEPER(s) -Dlog4j.debug=false -Dlog4j.configuration=file:MYCONFDIR/log4j.properties -jar start.jar`

### Zookeeper
* Test status  
` echo srvr | nc ZOOKEEPER ZKsPORTNR`
