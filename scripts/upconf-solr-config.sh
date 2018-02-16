#!/bin/sh

BINDIR=$(dirname $(readlink -f $0))

ZKCLI=/opt/solr/server/scripts/cloud-scripts/zkcli.sh
CONFDIR=/opt/gvi/solr_config
ZKHOST=localhost:2181

. ${BINDIR}/$1 

echo ${ZKCLI} -z ${ZKHOST}${ZKPATH} -cmd upconfig -confdir ${CONFDIR} -c ${COLLECTION} -confname ${CONFNAME}
${ZKCLI} -z ${ZKHOST}${ZKPATH} -cmd clear /configs/${CONFNAME}
${ZKCLI} -z ${ZKHOST}${ZKPATH} -cmd upconfig -confdir ${CONFDIR} -c ${COLLECTION} -confname ${CONFNAME}
curl "http://localhost:${SOLRPORT}/solr/admin/collections?action=RELOAD&name=${COLLECTION}"
