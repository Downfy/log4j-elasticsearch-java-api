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
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

/**
 * Using ElasticSearch store LoggingEvent for insert the document log4j.
 *
 * @author Tran Anh Tuan <tuanta@letfy.com>
 */
public class ElasticSearchClientAppender extends AppenderSkeleton {

    private ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private Client client;
    private String applicationId = "application";
    private String ip = "127.0.0.1";
    private String clusterName = "elasticsearch";
    private String elasticIndex = "logging-index";
    private String elasticType = "logging";
    private String elasticHost = "localhost";
    private int elasticPort = 9300;

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
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", clusterName).build();
        client = new TransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(elasticHost, elasticPort));

        //node = nodeBuilder().client(true).node();
        //client = node.client();

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
     * Elastic Search port.
     *
     * @return
     */
    public int getElasticPort() {
        return elasticPort;
    }

    /**
     * Elastic Search port.
     *
     * @param elasticPort
     */
    public void setElasticPort(int elasticPort) {
        this.elasticPort = elasticPort;
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
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Name application using log4j.
     *
     * @param applicationId
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * IP address application run.
     *
     * @return
     */
    public String getIp() {
        return ip;
    }

    /**
     * IP address application run.
     *
     * @param ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Elastic Search name of cluster.
     *
     * @return
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * Elastic Search name of cluster.
     *
     * @param clusterName
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * Close Elastic Search client.
     */
    @Override
    public void close() {
        client.close();
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
            json.put("applicationIp", getIp());
            json.put("applicationName", getApplicationId());
            json.put("level", event.getLevel().toString());
            json.put("timestamp", event.getTimeStamp());
            json.put("message", event.getMessage());
            json.put("logger", event.getLoggerName());
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
                // Set up the es index response 
                String uuid = UUID.randomUUID().toString();
                IndexRequestBuilder response = client.prepareIndex(getElasticIndex(), getElasticType(), uuid);
                Map<String, Object> data = new HashMap<String, Object>();

                writeBasic(data, loggingEvent);
                writeThrowable(data, loggingEvent);
                // insert the document into elasticsearch
                response.setSource(data);
                response.execute();
            } catch (Exception ex) {
            }
            return loggingEvent;
        }
    }
}
