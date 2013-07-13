mvn clean install
for x in sling-activemq-*
do
mvn sling:install -Dsling.url=http://debian:4502
mvn sling:install -Dsling.url=http://debian:4503
mvn sling:install -Dsling.url=http://debian:4504
done
