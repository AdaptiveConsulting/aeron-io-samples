/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples;

import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.infra.AppClusteredService;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.Integer.parseInt;

/**
 * Sample cluster application
 */
public class ClusterApp
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterApp.class);

    /**
     * The main method.
     * @param args command line args
     */
    public static void main(final String[] args)
    {
        final Map<String, String> envMap = System.getenv();

        LOGGER.info("----------------------");
        LOGGER.info("Environment variables:");
        LOGGER.info("----------------------");
        for (final String envName : envMap.keySet())
        {
            LOGGER.info("{} = {}", envName, envMap.get(envName));
        }

        LOGGER.info("----------------------");
        LOGGER.info("System Props:");
        LOGGER.info("----------------------");
        final Properties properties = System.getProperties();
        properties.forEach((k, v) -> LOGGER.info("{}:{}", k, v));

        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
        String portBaseString = System.getenv("CLUSTER_PORT_BASE");
        if (null == portBaseString || portBaseString.isEmpty())
        {
            portBaseString = System.getProperty("port.base", "9000");
        }
        String clusterNode = System.getenv("CLUSTER_NODE");
        if (null == clusterNode || clusterNode.isEmpty())
        {
            clusterNode = System.getProperty("node.id", "0");
        }
        String clusterAddresses = System.getenv("CLUSTER_ADDRESSES");
        if (null == clusterAddresses || clusterAddresses.isEmpty())
        {
            clusterAddresses = System.getProperty("cluster.addresses", "localhost");
        }
        LOGGER.info("CLUSTER_ADDRESSES: {}", clusterAddresses);
        LOGGER.info("CLUSTER_NODE: {}", clusterNode);
        LOGGER.info("CLUSTER_PORT_BASE: {}", portBaseString);

        final int portBase = parseInt(portBaseString);
        final int nodeId = parseInt(clusterNode);
        final String hosts = clusterAddresses;

        LOGGER.info("Starting Cluster Node {} on base port {} with hosts {}...", nodeId, portBase, hosts);

        final ClusterConfig clusterConfig = ClusterConfig.create(
            nodeId, List.of(hosts.split(",")), List.of(hosts.split(",")), portBase,
            new AppClusteredService());
        clusterConfig.consensusModuleContext().ingressChannel("aeron:udp");

        try (
            ClusteredMediaDriver ignored = ClusteredMediaDriver.launch(
                clusterConfig.mediaDriverContext(),
                clusterConfig.archiveContext(),
                clusterConfig.consensusModuleContext());
            ClusteredServiceContainer ignored1 = ClusteredServiceContainer.launch(
                clusterConfig.clusteredServiceContext()))
        {
            LOGGER.info("Started Cluster Node...");
            barrier.await();
            LOGGER.info("Exiting");
        }
    }
}
