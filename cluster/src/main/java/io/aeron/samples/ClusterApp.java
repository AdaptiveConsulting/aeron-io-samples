/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples;

import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.infra.AppClusteredService;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.agrona.concurrent.SystemEpochClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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
        final String[] hostArray = hosts.split(",");
        final ClusterConfig clusterConfig = ClusterConfig.create(
            nodeId, List.of(hostArray), List.of(hostArray), portBase,
            new AppClusteredService());
        clusterConfig.consensusModuleContext().ingressChannel("aeron:udp");

        //loop until cluster addresses can resolve self, or timeout at 1 minute
        final long endTime = SystemEpochClock.INSTANCE.time() + 60000;
        java.security.Security.setProperty("networkaddress.cache.ttl", "0");
        boolean resolved = false;
        while (!resolved)
        {
            if (SystemEpochClock.INSTANCE.time() > endTime)
            {
                LOGGER.error("cannot resolve name {}, exiting", hostArray[nodeId]);
                System.exit(-1);
            }

            try
            {
                final InetAddress byName = Inet4Address.getByName(hostArray[nodeId]);
                LOGGER.info("resolved name {} to {}", hostArray[nodeId], byName.getHostAddress());
                resolved = true;
            }
            catch (final UnknownHostException e)
            {
                LOGGER.warn("cannot yet resolve name {}, retrying in 5 seconds", hostArray[nodeId]);
                quietSleep(5000);
            }
        }

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

    /**
     * Sleeps for the given number of milliseconds, ignoring any interrupts.
     *
     * @param millis the number of milliseconds to sleep.
     */
    private static void quietSleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (final InterruptedException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
