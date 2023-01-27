/*
 * Copyright (c) 2023 Adaptive Financial Consulting
 */

package io.aeron.samples.domain.auctions;

import io.aeron.samples.domain.IdGenerators;
import io.aeron.samples.domain.participants.Participants;
import io.aeron.samples.domaininfra.AuctionResponder;
import io.aeron.samples.infra.SessionMessageContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Domain model for the auctions in the cluster
 */
public class Auctions
{
    private final SessionMessageContext context;
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
    public Auctions(final SessionMessageContext context, final Participants participants,
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
     * @param correlationId the correlation id for this request
     * @param createdByParticipantId the participant who created the auction
     * @param startTime the start time of the auction. Bids cannot be added before this time.
     * @param endTime the end time of the auction, at which time no more bids can be added and the result is computed
     * @param name the name of the auction
     * @param description the description
     */
    public void addAuction(final String correlationId, final long createdByParticipantId, final long startTime,
        final long endTime, final String name, final String description)
    {
        final var result = validate(createdByParticipantId, startTime, endTime, name, description);

        if (result != AddAuctionResult.SUCCESS)
        {
            auctionResponder.rejectAddAuction(correlationId, result);
            return;
        }

        final long auctionId = idGenerators.incrementAndGetAuctionId();
        final var auction = new Auction(auctionId, createdByParticipantId, startTime, endTime, name, description);
        auctionList.add(auction);

        auctionResponder.onAuctionAdded(correlationId, auctionId, result, startTime, endTime, name, description);
    }

    /**
     * Loads an auction from the snapshot
     * @param auctionId the auction id
     * @param createdByParticipantId the participant who created the auction
     * @param startTime the start time of the auction
     * @param endTime the end time of the auction
     * @param name the name of the auction
     * @param description the description
     */
    public void restoreAuction(final long auctionId, final long createdByParticipantId, final long startTime,
        final long endTime, final String name, final String description)
    {
        final var auction = new Auction(auctionId, createdByParticipantId, startTime, endTime, name, description);
        auctionList.add(auction);
    }

    /**
     * Adds a bid to an existing auction
     * @param auctionId the auction id
     * @param participantId the participant who is bidding
     * @param price the price of the bid, in whole cents
     * @param correlationId the correlation id for this request
     */
    public void addBid(final long auctionId, final long participantId, final long price, final String correlationId)
    {
        final var optionalAuction = auctionList.stream().filter(a -> a.getAuctionId() == auctionId).findFirst();
        if (optionalAuction.isEmpty())
        {
            auctionResponder.rejectAddBid(correlationId, auctionId, AddAuctionBidResult.UNKNOWN_AUCTION);
            return;
        }

        final var auction = optionalAuction.get();
        final var validationResult = validateBid(auction, participantId, price);
        if (validationResult != AddAuctionBidResult.SUCCESS)
        {
            auctionResponder.rejectAddBid(correlationId, auctionId, validationResult);
        }

        auction.setWinningBid(participantId, price, context.getClusterTime());

        auctionResponder.onAuctionUpdated(correlationId, auction.getAuctionId(), auction.getAuctionStatus(),
            auction.getCurrentPrice(), auction.getBidCount(), auction.getLastUpdateTime());
    }


    /**
     * Gets the list of auctions after sorting it by auction id
     * @return the list of auctions
     */
    public List<Auction> getAuctionList()
    {
        auctionList.sort(Comparator.comparingLong(Auction::getAuctionId));
        return auctionList;
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

    /**
     * Validates the bid parameters.
     * The auction must be open at this time.
     * The bidding participant must be known and cannot be the creator.
     * The price must be over zero, and a price improvement on the current price.
     * @param auction the auction
     * @param participantId the participant who is bidding
     * @param price the price of the bid
     * @return the result of the validation
     */
    private AddAuctionBidResult validateBid(final Auction auction, final long participantId, final long price)
    {
        if (auction.getStartTime() > context.getClusterTime())
        {
            return AddAuctionBidResult.AUCTION_NOT_OPEN;
        }
        if (auction.getEndTime() <= context.getClusterTime())
        {
            return AddAuctionBidResult.AUCTION_NOT_OPEN;
        }
        if (!participants.isKnownParticipant(participantId))
        {
            return AddAuctionBidResult.UNKNOWN_PARTICIPANT;
        }
        if (price <= 0 || price <= auction.currentPrice())
        {
            return AddAuctionBidResult.INVALID_PRICE;
        }
        if (auction.getCreatedByParticipantId() == participantId)
        {
            return AddAuctionBidResult.CANNOT_SELF_BID;
        }
        return AddAuctionBidResult.SUCCESS;
    }
}
