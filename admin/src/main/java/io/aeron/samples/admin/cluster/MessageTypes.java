/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.admin.cluster;

/**
 * Message types
 */
public final class MessageTypes
{
    private MessageTypes()
    {
    }

    public static final int CLUSTER_PASSTHROUGH = 1;
    public static final int CLUSTER_CLIENT_CONTROL = 2;
}
