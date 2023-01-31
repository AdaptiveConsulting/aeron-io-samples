/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin;

import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.samples.cluster.ClusterConfig;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;

/**
 * Admin client for the cluster main class, working on a direct connection to the cluster
 */
public class Admin
{
    private static final int PORT_BASE = 9000;

    /**
     * Main method
     * @param args command line arguments
     */
    public static void main(final String[] args)
    {
        final AddParticipantTask addParticipant = new AddParticipantTask();
        final AdminClientEgressListener adminClient = new AdminClientEgressListener();
        final String ingressEndpoints = ingressEndpoints(Arrays.asList("localhost")); //todo accept config

        try (
            MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
            AeronCluster aeronCluster = AeronCluster.connect(
                new AeronCluster.Context()
                .egressListener(adminClient)
                .egressChannel("aeron:udp?endpoint=localhost:0")
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                .ingressChannel("aeron:udp")
                .ingressEndpoints(ingressEndpoints)))
        {
            adminClient.setAeronCluster(aeronCluster);
            addParticipant.setAdminClient(adminClient);
            new CommandLine(addParticipant).execute(args);
        }
    }

    /**
     * Ingress endpoint configuration
     * @param hostnames list of hostnames
     * @return ingress endpoint configuration
     */
    private static String ingressEndpoints(final List<String> hostnames)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++)
        {
            sb.append(i).append('=');
            sb.append(hostnames.get(i)).append(':').append(calculatePort(i, 10));
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }


    private static int calculatePort(final int nodeId, final int offset)
    {
        return PORT_BASE + (nodeId * ClusterConfig.PORTS_PER_NODE) + offset;
    }
}
