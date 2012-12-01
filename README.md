log4j-elasticsearch-java-api
============================

Using log4j insert log info into ElasticSearch

The configuration is simple:

### Properties Configuration ###
<pre><code>
# RootLogger
log4j.rootLogger=INFO,stdout,elastic

# Logging Threshold
log4j.threshhold=ALL

#
# stdout
# Add *stdout* to rootlogger above if you want to use this 
#
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n

# ElasticSearch log4j appender for application
log4j.appender.elastic=com.letfy.log4j.appenders.ElasticSearchClientAppender
</code></pre>
