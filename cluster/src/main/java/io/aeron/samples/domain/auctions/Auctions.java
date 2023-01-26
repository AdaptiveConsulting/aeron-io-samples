/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

import io.aeron.samples.domain.IdGenerators;
import io.aeron.samples.domain.participants.Participants;
import io.aeron.samples.domaininfra.AuctionResponder;
import io.aeron.samples.infra.SessionMessageContextImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model for the auctions in the cluster
 */
public class Auctions
{
    private final SessionMessageContextImpl context;
    private final AuctionResponder auctionResponder;
    private final Participants participants;
    private final IdGenerators idGenerators;
    private final List<Auction> auctionList;

    /**
     * Constructor
     * @param context the session message context
     * @param participants the participant data
     * @param idGenerators the idgenerator to use to generate new ids
     * @param auctionResponder the object used to respond to auction actions
     */
    public Auctions(final SessionMessageContextImpl context, final Participants participants,
        final IdGenerators idGenerators, final AuctionResponder auctionResponder)
    {
        this.context = context;
        this.auctionResponder = auctionResponder;
        this.auctionList = new ArrayList<>();
        this.participants = participants;
        this.idGenerators = idGenerators;
    }

    /**
     * Creates an auction
     * @param createdByParticipantId the participant who created the auction
     * @param startTime the start time of the auction. Bids cannot be added before this time.
     * @param endTime the end time of the auction, at which time no more bids can be added and the result is computed
     * @param name the name of the auction
     * @param description the description
     */
    public void addAuction(final long createdByParticipantId, final long startTime,
        final long endTime, final String name, final String description)
    {
        final var result = validate(createdByParticipantId, startTime, endTime, name, description);

        if (result != AddAuctionResult.SUCCESS)
        {
            auctionResponder.rejectAddAuction(result);
            return;
        }

        final long auctionId = idGenerators.incrementAndGetAuctionId();
        final var auction = new Auction(auctionId, createdByParticipantId, startTime, endTime, name, description);
        auctionList.add(auction);

        auctionResponder.onAuctionAdded(auctionId, result);
    }

    /**
     * Validates the auction parameters
     * @param createdByParticipantId the participant who created the auction
     * @param startTime the start time of the auction which cannot be the current cluster time or earlier
     * @param endTime the end time of the auction which must be after the start time
     * @param name the name of the auction which must not be null or blank
     * @param description the description which must not be null or blank
     * @return the result of the validation
     */
    private AddAuctionResult validate(final long createdByParticipantId, final long startTime, final long endTime,
        final String name, final String description)
    {
        if (startTime <= context.getClusterTime())
        {
            return AddAuctionResult.INVALID_START_TIME;
        }
        if (endTime <= startTime)
        {
            return AddAuctionResult.INVALID_END_TIME;
        }
        if (!participants.isKnownParticipant(createdByParticipantId))
        {
            return AddAuctionResult.UNKNOWN_PARTICIPANT;
        }
        if (name == null || name.isBlank())
        {
            return AddAuctionResult.INVALID_NAME;
        }
        if (description == null || description.isBlank())
        {
            return AddAuctionResult.INVALID_DESCRIPTION;
        }
        return AddAuctionResult.SUCCESS;
    }

}
