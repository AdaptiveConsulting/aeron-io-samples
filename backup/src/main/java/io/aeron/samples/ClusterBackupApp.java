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

import io.aeron.ChannelUri;
import io.aeron.CommonContext;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ClusterBackup;
import io.aeron.cluster.ClusterBackupEventsListener;
import io.aeron.cluster.ClusterMember;
import io.aeron.cluster.RecordingLog;
import io.aeron.driver.MediaDriver;
import io.aeron.samples.cluster.ClusterConfig;
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
import java.util.concurrent.TimeUnit;

import static io.aeron.samples.cluster.ClusterConfig.MEMBER_FACING_PORT_OFFSET;
import static java.lang.Integer.parseInt;

/**
 * Sample cluster backup application
 */
public class ClusterBackupApp
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBackupApp.class);
    private static final long CLUSTER_BACKUP_INTERVAL_NS = TimeUnit.MINUTES.toNanos(1);

    /**
     * The main method.
     *
     * @param args command line args
     */
    public static void main(final String[] args)
    {
        final String clusterConsensusEndpoints = getClusterConsensusEndpoints();

        // Host for the Cluster Backup application. This will be used to construct
        // addresses that the cluster will use to connect back to Cluster Backup.
        final String backupHost = getBackupHost();

        // Context for the local Media Driver
        final MediaDriver.Context mediaDriverContext = mediaDriverContext();
        final String aeronDirectoryName = mediaDriverContext.aeronDirectoryName();

        // Context for the local Archive
        final Archive.Context localArchiveContext = localArchiveContext(aeronDirectoryName, backupHost);

        // Context for Cluster Backup application.
        final ClusterBackup.Context clusterBackupContext = clusterBackupContext(
            clusterConsensusEndpoints,
            aeronDirectoryName, backupHost
        );

        LOGGER.info("Cluster Directory: {} ", clusterBackupContext.clusterDirectoryName());
        LOGGER.info("Archive Directory: {} ", localArchiveContext.archiveDir());

        LOGGER.info("Connecting to cluster: {}", clusterConsensusEndpoints);

        try (
            ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
            ArchivingMediaDriver ignored = ArchivingMediaDriver.launch(mediaDriverContext, localArchiveContext);
            ClusterBackup ignored1 = ClusterBackup.launch(clusterBackupContext))
        {
            LOGGER.info("Started Cluster Backup...");
            barrier.await();
            LOGGER.info("Exiting");
        }
    }

    private static ClusterBackup.Context clusterBackupContext(
        final String clusterConsensusEndpoints, final String aeronDirectoryName, final String backupHost)
    {
        final ClusterBackup.Context clusterBackupContext = new ClusterBackup.Context();

        // The Cluster Backup application will create a subscription on this address,
        // and will send this address to the cluster, so it can publish to it.
        final String localConsensusChannelUri = localConsensusChannelUri(
            backupHost, clusterBackupContext.consensusChannel()
        );

        // Context for the archive client that connects to the cluster's archive.
        final AeronArchive.Context clusterArchiveClientContext = clusterArchiveClientContext();

        // The local archive will listen on this address, and instruct the cluster to publish here for replay.
        final String catchupEndpoint = backupHost + ":0";

        clusterBackupContext
            .catchupEndpoint(catchupEndpoint)
            .clusterConsensusEndpoints(clusterConsensusEndpoints)
            .consensusChannel(localConsensusChannelUri)
            .eventsListener(new LoggingBackupListener())
            .aeronDirectoryName(aeronDirectoryName)
            .clusterArchiveContext(clusterArchiveClientContext)
            .clusterDirectoryName("backup/cluster")
            .sourceType(ClusterBackup.SourceType.LEADER) // What kind of node(s) to connect to.
            .clusterBackupIntervalNs(CLUSTER_BACKUP_INTERVAL_NS) // How frequently to check for snapshots.
            .deleteDirOnStart(true);

        return clusterBackupContext;
    }

    private static String localConsensusChannelUri(final String backupHost, final String consensusChannel)
    {
        final ChannelUri consensusChannelUri = ChannelUri.parse(consensusChannel);
        final String backupStatusEndpoint = backupHost + ":9876";
        consensusChannelUri.put(CommonContext.ENDPOINT_PARAM_NAME, backupStatusEndpoint);
        return consensusChannelUri.toString();
    }

    private static AeronArchive.Context clusterArchiveClientContext()
    {
        return new AeronArchive.Context()
            .controlRequestChannel("aeron:udp")
            .controlResponseChannel("aeron:udp?endpoint=" + getBackupHost() + ":0");
    }

    private static Archive.Context localArchiveContext(final String aeronDirectoryName, final String backupHost)
    {
        return new Archive.Context()
            .archiveDir(new File("backup/archive"))
            .controlChannel("aeron:udp?endpoint=" + backupHost + ":0")
            .replicationChannel("aeron:udp?endpoint=localhost:0")
            .aeronDirectoryName(aeronDirectoryName);
    }

    private static MediaDriver.Context mediaDriverContext()
    {
        return new MediaDriver.Context().dirDeleteOnStart(true);
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

    private static String getClusterAddresses()
    {
        String clusterAddresses = System.getenv("CLUSTER_ADDRESSES");
        if (null == clusterAddresses || clusterAddresses.isEmpty())
        {
            clusterAddresses = System.getProperty("cluster.addresses", "localhost");
        }
        return clusterAddresses;
    }

    private static String getBackupHost()
    {
        final String backupHost = System.getenv("BACKUP_HOST");
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

        Arrays.stream(hosts).forEach(ClusterBackupApp::awaitDnsResolution);
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

    private static final class LoggingBackupListener implements ClusterBackupEventsListener
    {
        @Override
        public void onBackupQuery()
        {
            LOGGER.info("Sending backup query");
        }

        @Override
        public void onPossibleFailure(final Exception ex)
        {
            LOGGER.error("Possible failure detected", ex);
        }

        @Override
        public void onBackupResponse(
            final ClusterMember[] clusterMembers,
            final ClusterMember logSourceMember,
            final List<RecordingLog.Snapshot> snapshotsToRetrieve)
        {
            LOGGER.info("Response from Cluster. Log Source Member: {}. Cluster Members: {}. Snapshots to retrieve: {}",
                logSourceMember.id(), clusterMembersString(clusterMembers), snapshotsString(snapshotsToRetrieve)
            );
        }

        @Override
        public void onUpdatedRecordingLog(
            final RecordingLog recordingLog,
            final List<RecordingLog.Snapshot> snapshotsRetrieved)
        {
            LOGGER.info("Updating log for recording {}. Snapshots retrieved: {}",
                recordingLogString(recordingLog), snapshotsString(snapshotsRetrieved)
            );
        }

        @Override
        public void onLiveLogProgress(final long recordingId, final long recordingPosCounterId, final long logPosition)
        {
            LOGGER.info("Reached position {} in recording {}", logPosition, recordingId);
        }

        private static String clusterMembersString(final ClusterMember[] clusterMembers)
        {
            final StringJoiner clusterMembersString = new StringJoiner(", ", "[", "]");
            Arrays.stream(clusterMembers).forEach(member -> clusterMembersString.add(
                member.id() + ". " + member.consensusEndpoint() + " (" + (member.isLeader() ? "" : "not ") + "leader)"
            ));
            return clusterMembersString.toString();
        }

        private static String snapshotsString(final List<RecordingLog.Snapshot> snapshotsToRetrieve)
        {
            final StringJoiner snapshotsString = new StringJoiner(", ", "[", "]");
            snapshotsToRetrieve.forEach(snapshot ->
                snapshotsString.add("Snapshot recordingId: " + snapshot.recordingId)
            );
            return snapshotsString.toString();
        }

        private static String recordingLogString(final RecordingLog recordingLog)
        {
            final StringJoiner recordingLogString = new StringJoiner(", ", "[", "]");
            recordingLog.entries().forEach(entry ->
                recordingLogString.add("recordingId: " + entry.recordingId + ", logPosition: " + entry.logPosition)
            );
            return recordingLogString.toString();
        }
    }
}
