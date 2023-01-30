/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples;

import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.infra.AppClusteredService;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/***
 * Sample cluster application
 */
public class ClusterApp
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterApp.class);

    /***
     * The main method.
     * @param args command line args
     */
    public static void main(final String[] args)
    {
        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();

        //temp config for initial development - replace with proper config.
        final ClusterConfig clusterConfig = ClusterConfig.create(
            0, List.of("localhost"), List.of("localhost"), 9000,
            new AppClusteredService());
        clusterConfig.consensusModuleContext().ingressChannel("aeron:udp?endpoint=localhost:9010|term-length=64k");
        clusterConfig.mediaDriverContext().errorHandler(errorHandler("Media Driver"));
        clusterConfig.archiveContext().errorHandler(errorHandler("Archive"));
        clusterConfig.aeronArchiveContext().errorHandler(errorHandler("Aeron Archive"));
        clusterConfig.consensusModuleContext().errorHandler(errorHandler("Consensus Module"));
        clusterConfig.clusteredServiceContext().errorHandler(errorHandler("Clustered Service"));

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
     * Logs errors within the given context.
     *
     * @param context the context to log within
     * @return the ErrorHandler to be used
     */
    private static ErrorHandler errorHandler(final String context)
    {
        return (Throwable throwable) -> LOGGER.error(context, throwable);
    }
}
