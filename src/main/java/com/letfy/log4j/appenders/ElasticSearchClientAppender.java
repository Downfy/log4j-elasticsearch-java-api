/*
 * Copyright 2012 Letfy Team <admin@letfy.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.letfy.log4j.appenders;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.ClientConfig;
import io.searchbox.core.Index;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * Using ElasticSearch store LoggingEvent for insert the document log4j.
 *
 * @author Tran Anh Tuan <tk1cntt@gmail.com>
 */
public class ElasticSearchClientAppender extends AppenderSkeleton {

    private ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private JestClient client;
    private String applicationName = "application";
    private String hostName = "127.0.0.1";
    private String elasticIndex = "logging-index";
    private String elasticType = "logging";
    private String elasticHost = "http://localhost:9200";

    /**
     * Submits LoggingEvent for insert the document if it reaches severity
     * threshold.
     *
     * @param loggingEvent
     */
    @Override
    protected void append(LoggingEvent loggingEvent) {
        if (isAsSevereAsThreshold(loggingEvent.getLevel())) {
            threadPool.submit(new AppenderTask(loggingEvent));
        }
    }

    /**
     * Create ElasticSearch Client.
     *
     * @see AppenderSkeleton
     */
    @Override
    public void activateOptions() {
        // Need to do this if the cluster name is changed, probably need to set this and sniff the cluster
        try {
            // Configuration
            ClientConfig clientConfig = new ClientConfig.Builder(elasticHost).multiThreaded(true).build();

            // Construct a new Jest client according to configuration via factory
            JestClientFactory factory = new JestClientFactory();
            factory.setClientConfig(clientConfig);
            client = factory.getObject();
        } catch (Exception ex) {
        }

        super.activateOptions();
    }

    /**
     * Elastic Search host.
     *
     * @return
     */
    public String getElasticHost() {
        return elasticHost;
    }

    /**
     * Elastic Search host.
     *
     * @param elasticHost
     */
    public void setElasticHost(String elasticHost) {
        this.elasticHost = elasticHost;
    }

    /**
     * Elastic Search index.
     *
     * @return
     */
    public String getElasticIndex() {
        return elasticIndex;
    }

    /**
     * Elastic Search index.
     *
     * @param elasticIndex
     */
    public void setElasticIndex(String elasticIndex) {
        this.elasticIndex = elasticIndex;
    }

    /**
     * Elastic Search type.
     *
     * @return Type
     */
    public String getElasticType() {
        return elasticType;
    }

    /**
     * Elastic Search type.
     *
     * @param elasticType
     */
    public void setElasticType(String elasticType) {
        this.elasticType = elasticType;
    }

    /**
     * Name application using log4j.
     *
     * @return
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Name application using log4j.
     *
     * @param applicationId
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Host name application run.
     *
     * @return
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Host name application run.
     *
     * @param ip
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Close Elastic Search client.
     */
    @Override
    public void close() {
        client.shutdownClient();
    }

    /**
     * Ensures that a Layout property is not required
     *
     * @return
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * Simple Callable class that insert the document into ElasticSearch
     */
    class AppenderTask implements Callable<LoggingEvent> {

        LoggingEvent loggingEvent;

        AppenderTask(LoggingEvent loggingEvent) {
            this.loggingEvent = loggingEvent;
        }

        protected void writeBasic(Map<String, Object> json, LoggingEvent event) {
            json.put("hostName", getHostName());
            json.put("applicationName", getApplicationName());
            json.put("timestamp", event.getTimeStamp());
            json.put("logger", event.getLoggerName());
            json.put("level", event.getLevel().toString());
            json.put("message", event.getMessage());
        }

        protected void writeThrowable(Map<String, Object> json, LoggingEvent event) {
            ThrowableInformation ti = event.getThrowableInformation();
            if (ti != null) {
                Throwable t = ti.getThrowable();
                json.put("className", t.getClass().getCanonicalName());
                json.put("stackTrace", getStackTrace(t));
            }
        }

        protected String getStackTrace(Throwable aThrowable) {
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            aThrowable.printStackTrace(printWriter);
            return result.toString();
        }

        /**
         * Method is called by ExecutorService and insert the document into
         * ElasticSearch
         *
         * @return
         * @throws Exception
         */
        @Override
        public LoggingEvent call() throws Exception {
            try {
                if (client != null) {
                    // Set up the es index response 
                    String uuid = UUID.randomUUID().toString();
                    Map<String, Object> data = new HashMap<String, Object>();

                    writeBasic(data, loggingEvent);
                    writeThrowable(data, loggingEvent);
                    // insert the document into elasticsearch
                    Index index = new Index.Builder(data).index(getElasticIndex()).type(getElasticType()).id(uuid).build();
                    client.execute(index);
                }
            } catch (Exception ex) {
            }
            return loggingEvent;
        }
    }
}
