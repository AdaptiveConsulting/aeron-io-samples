/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aeron.samples;

import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.cluster.ClusterBackup;
import io.aeron.cluster.ClusterStandby;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.samples.cluster.ClusterConfig;
import io.aeron.samples.infra.AppClusteredService;
import org.agrona.concurrent.ShutdownSignalBarrier;
import org.agrona.concurrent.SystemEpochClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import static io.aeron.samples.cluster.ClusterConfig.ARCHIVE_CONTROL_PORT_OFFSET;
import static io.aeron.samples.cluster.ClusterConfig.MEMBER_FACING_PORT_OFFSET;
import static java.lang.Integer.parseInt;

/**
 * Sample cluster backup application
 */
public class ClusterStandbyApp
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStandbyApp.class);
    public static final String DEFAULT_STANDBY_MEMBER_ID = "4";

    /**
     * The main method.
     *
     * @param args command line args
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static void main(final String[] args)
    {
        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
        final String standbyHost = getStandbyHost();
        final int standbyMemberId = getMemberId();
        final int basePort = getBasePort();
        final String hosts = getClusterAddresses();
        final List<String> hostAddresses = List.of(hosts.split(","));
        final File baseDir = getBaseDir(standbyMemberId);

        final ClusterConfig clusterConfig = ClusterConfig.create(
            standbyMemberId, hostAddresses, hostAddresses, basePort, new AppClusteredService());
        clusterConfig.baseDir(baseDir);

        final String standbyConsensusEndpoint = standbyHost + ":" + ClusterConfig.calculatePort(
            standbyMemberId, basePort, MEMBER_FACING_PORT_OFFSET);
        final String standbyArchiveEndpoint = standbyHost + ":" + ClusterConfig.calculatePort(
            standbyMemberId, basePort, ARCHIVE_CONTROL_PORT_OFFSET);
        final String standbyResponseEndpoint = standbyHost + ":0";
        final String clusterConsensusEndpoints = getClusterConsensusEndpoints();

        clusterConfig.archiveContext().archiveDir(new File("standby/archive"));

        // Context for Cluster Standby.
        final ClusterStandby.Context clusterStandbyContext = new ClusterStandby.Context()
            .clusterMemberId(standbyMemberId)                      // Mostly used to differentiate from other nodes in
                                                                   // the cluster for logging.
            .clusterConsensusEndpoints(clusterConsensusEndpoints)  // The endpoints of the primary cluster used for
                                                                   // backup queries & sending snapshot notifications.
            .standbyConsensusEndpoint(standbyConsensusEndpoint)    // Used for daisy-chained standbys to retrieve backup
                                                                   // query responses.
            .standbyArchiveEndpoint(standbyArchiveEndpoint)        // The endpoint for this standby's archive.
            .responseEndpoint(standbyResponseEndpoint)             // The endpoint that receives responses to requests
                                                                   // (e.g backup queries).
            .standbyDir(new File(baseDir, "standby"))
            .archiveContext(clusterConfig.aeronArchiveContext().clone())
            .aeronDirectoryName(clusterConfig.mediaDriverContext().aeronDirectoryName())
            .sourceType(ClusterBackup.SourceType.FOLLOWER) // What kind of node(s) to connect to.
            .standbySnapshotEnabled(true)
            .standbySnapshotNotificationsEnabled(true)
            .deleteDirOnStart(true);

        final ClusteredServiceContainer.Context clusteredServiceContext = clusterConfig.clusteredServiceContext();

        LOGGER.info("Standby Directory: {} ", clusterStandbyContext.standbyDirectoryName());
        LOGGER.info("Archive Directory: {} ", clusterConfig.archiveContext().archiveDir());
        LOGGER.info("Connecting to cluster: {}", clusterConsensusEndpoints);

        try (
            ArchivingMediaDriver ignored = ArchivingMediaDriver.launch(
                clusterConfig.mediaDriverContext(), clusterConfig.archiveContext());
            ClusterStandby ignored1 = ClusterStandby.launch(clusterStandbyContext);
            ClusteredServiceContainer ignored2 = ClusteredServiceContainer.launch(clusteredServiceContext))
        {
            LOGGER.info("Started Cluster Backup...");
            barrier.await();
            LOGGER.info("Exiting");
        }
    }

    /**
     * Get the cluster node id
     * @return cluster node id, default 0
     */
    private static int getMemberId()
    {
        String memberId = System.getenv("CLUSTER_NODE");
        if (null == memberId || memberId.isEmpty())
        {
            memberId = System.getProperty("node.id", DEFAULT_STANDBY_MEMBER_ID);
        }
        return parseInt(memberId);
    }

    private static String getClusterConsensusEndpoints()
    {
        final int portBase = getBasePort();
        final String hosts = getClusterAddresses();
        final String[] hostAddresses = hosts.split(",");
        awaitDnsResolution(hostAddresses);
        final StringJoiner endpointsBuilder = new StringJoiner(",");
        for (int nodeId = 0; nodeId < hostAddresses.length; nodeId++)
        {
            final int port = ClusterConfig.calculatePort(nodeId, portBase, MEMBER_FACING_PORT_OFFSET);
            endpointsBuilder.add(hostAddresses[nodeId] + ":" + port);
        }
        return endpointsBuilder.toString();
    }

    private static int getBasePort()
    {
        String portBaseString = System.getenv("CLUSTER_PORT_BASE");
        if (null == portBaseString || portBaseString.isEmpty())
        {
            portBaseString = System.getProperty("port.base", "9000");
        }
        return parseInt(portBaseString);
    }

    /***
     * Get the base directory for the cluster configuration
     * @param nodeId node id
     * @return base directory
     */
    private static File getBaseDir(final int nodeId)
    {
        final String baseDir = System.getenv("BASE_DIR");
        if (null == baseDir || baseDir.isEmpty())
        {
            return new File(System.getProperty("user.dir"), "node" + nodeId);
        }

        return new File(baseDir);
    }


    private static String getClusterAddresses()
    {
        String clusterAddresses = System.getenv("CLUSTER_ADDRESSES");
        if (null == clusterAddresses || clusterAddresses.isEmpty())
        {
            clusterAddresses = System.getProperty("cluster.addresses", "localhost");
        }
        return clusterAddresses;
    }

    private static String getStandbyHost()
    {
        final String backupHost = System.getenv("STANDBY_HOST");
        if (backupHost == null || backupHost.isEmpty())
        {
            return "localhost";
        }
        awaitDnsResolution(backupHost);
        return backupHost;
    }

    private static void awaitDnsResolution(final String[] hosts)
    {
        if (applyDnsDelay())
        {
            LOGGER.info("Waiting 5 seconds for DNS to be registered...");
            quietSleep(5000);
        }
        java.security.Security.setProperty("networkaddress.cache.ttl", "0");

        Arrays.stream(hosts).forEach(ClusterStandbyApp::awaitDnsResolution);
    }

    private static void awaitDnsResolution(final String host)
    {
        final long endTime = SystemEpochClock.INSTANCE.time() + 60000;
        boolean resolved = false;
        while (!resolved)
        {
            if (SystemEpochClock.INSTANCE.time() > endTime)
            {
                LOGGER.error("cannot resolve name {}, exiting", host);
                System.exit(-1);
            }

            try
            {
                InetAddress.getByName(host);
                resolved = true;
            }
            catch (final UnknownHostException e)
            {
                LOGGER.warn("cannot yet resolve name {}, retrying in 3 seconds", host);
                quietSleep(3000);
            }
        }
    }

    /**
     * Apply DNS delay
     *
     * @return true if DNS delay should be applied
     */
    private static boolean applyDnsDelay()
    {
        final String dnsDelay = System.getenv("DNS_DELAY");
        if (null == dnsDelay || dnsDelay.isEmpty())
        {
            return false;
        }
        return Boolean.parseBoolean(dnsDelay);
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
            LOGGER.warn("Interrupted while sleeping");
        }
    }
}
