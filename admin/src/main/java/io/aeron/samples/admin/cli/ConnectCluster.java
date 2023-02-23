/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cli;

import io.aeron.samples.cluster.admin.protocol.ConnectClusterEncoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderEncoder;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

import static io.aeron.samples.admin.util.ClusterConnectUtil.getThisHostName;
import static io.aeron.samples.admin.util.ClusterConnectUtil.tryGetClusterHostsFromEnv;
import static io.aeron.samples.admin.util.ClusterConnectUtil.tryGetResponsePortFromEnv;

/**
 * Adds a participant to the cluster
 */
@CommandLine.Command(name = "connect", mixinStandardHelpOptions = false,
    description = "Connects to the cluster")
public class ConnectCluster implements Runnable
{
    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final ConnectClusterEncoder connectClusterEncoder = new ConnectClusterEncoder();
    @CommandLine.ParentCommand
    CliCommands parent;
    @SuppressWarnings("all")
    @CommandLine.Option(names = "baseport", description = "The base port to connect to")
    private Integer baseport = 9000;
    @SuppressWarnings("all")
    @CommandLine.Option(names = "hostnames", description = "The cluster address(es) to connect to. " +
        "Multiple addresses can be specified by separating them with a comma.")
    private String hostnames = tryGetClusterHostsFromEnv();
    @SuppressWarnings("all")
    @CommandLine.Option(names = "thishost", description = "The response hostname (defaulted to localhost).")
    private String localhost = getThisHostName();
    @CommandLine.Option(names = "port", description = "The port to use for communication. The default, 0," +
        " will auto-assign a port")
    private Integer port = tryGetResponsePortFromEnv();

    /**
     * sends a connect cluster via the comms channel
     */
    public void run()
    {
        connectClusterEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);
        connectClusterEncoder.baseport(baseport);
        connectClusterEncoder.port(port);
        connectClusterEncoder.clusterHosts(hostnames);
        connectClusterEncoder.localhostName(localhost);

        parent.offerClusterClientMessage(
            buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + connectClusterEncoder.encodedLength());
    }


}
