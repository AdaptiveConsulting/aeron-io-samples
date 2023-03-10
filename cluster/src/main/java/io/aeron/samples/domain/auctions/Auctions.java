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

package io.aeron.samples.domain.auctions;

import io.aeron.samples.domain.participants.Participants;
import io.aeron.samples.infra.ClusterClientResponder;
import io.aeron.samples.infra.SessionMessageContext;
import io.aeron.samples.infra.TimerManager;
import org.agrona.collections.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Domain model for the auctions in the cluster
 */
public class Auctions
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Auctions.class);
    private static final long MINIMUM_DURATION = TimeUnit.SECONDS.toMillis(20);
    private static final long REMOVAL_TIMER_DURATION = TimeUnit.SECONDS.toMillis(60);
    private final SessionMessageContext context;
    private final ClusterClientResponder clusterClientResponder;
    private final TimerManager timerManager;
    private final Participants participants;
    private final List<Auction> auctionList;
    private final MutableLong idGenerator = new MutableLong(0);

    /**
     * Constructor
     *
     * @param context          the session message context
     * @param participants     the participant data
     * @param clusterClientResponder the object used to respond to auction actions
     * @param timerManager     the timer manager
     */
    public Auctions(
        final SessionMessageContext context,
        final Participants participants,
        final ClusterClientResponder clusterClientResponder,
        final TimerManager timerManager)
    {
        this.context = context;
        this.clusterClientResponder = clusterClientResponder;
        this.timerManager = timerManager;
        this.auctionList = new ArrayList<>();
        this.participants = participants;
    }

    /**
     * Creates an auction if the parameters are validated successfully
     *
     * @param correlationId          the correlation id for this request
     * @param createdByParticipantId the participant who created the auction
     * @param startTime              the start time of the auction. Bids cannot be added before this time.
     * @param endTime                the end time of the auction, at which time no more bids can be added and the result
     *                               is computed
     * @param name                   the name of the auction
     * @param description            the description
     */
    public void addAuction(
        final long createdByParticipantId,
        final long startTime,
        final long endTime,
        final String correlationId,
        final String name,
        final String description)
    {
        final var result = validate(createdByParticipantId, startTime, endTime, name, description);

        if (result != AddAuctionResult.SUCCESS)
        {
            clusterClientResponder.rejectAddAuction(correlationId, result);
            return;
        }

        final var auctionId = idGenerator.incrementAndGet();

        LOGGER.info("Creating new auction '{}' with id {}", name, auctionId);

        final var auction = new Auction(auctionId, createdByParticipantId, startTime, endTime, name, description,
            -1L);
        auctionList.add(auction);

        clusterClientResponder.onAuctionAdded(correlationId, auctionId, result, startTime, endTime, name, description);

        final var startCorrelationId = timerManager.scheduleTimer(startTime, () -> openAuction(auctionId));
        final var endCorrelationId = timerManager.scheduleTimer(endTime, () -> closeAuction(auctionId));
        final var removeCorrelationId = timerManager.scheduleTimer(
            endTime + REMOVAL_TIMER_DURATION, () -> removeAuction(auctionId));

        auction.setStartTimerCorrelationId(startCorrelationId);
        auction.setEndTimerCorrelationId(endCorrelationId);
        auction.setRemovalTimerCorrelationId(removeCorrelationId);
    }

    /**
     * Loads an auction from the snapshot
     *
     * @param auctionId                     the auction id
     * @param createdByParticipantId        the participant who created the auction
     * @param startTime                     the start time of the auction
     * @param startTimerTimerCorrelationId  the timer correlation id for the start timer
     * @param endTime                       the end time of the auction
     * @param endTimerTimerCorrelationId    the timer correlation id for the end timer
     * @param removeTimerTimerCorrelationId the timer correlation id for the removal timer
     * @param winningParticipantId          the winning participant id
     * @param name                          the name of the auction
     * @param description                   the description
     */
    public void restoreAuction(
        final long auctionId,
        final long createdByParticipantId,
        final long startTime,
        final long startTimerTimerCorrelationId,
        final long endTime,
        final long endTimerTimerCorrelationId,
        final long removeTimerTimerCorrelationId,
        final long winningParticipantId,
        final String name,
        final String description)
    {
        final var auction = new Auction(
            auctionId, createdByParticipantId, startTime, endTime, name, description, winningParticipantId);
        auctionList.add(auction);

        //Aeron Cluster is already snapshotting the cluster timer state, so we just need to rehydrate the internal
        //state of the TimerManager on snapshot restore.
        timerManager.restoreTimer(startTimerTimerCorrelationId, () -> openAuction(auctionId));
        timerManager.restoreTimer(endTimerTimerCorrelationId, () -> closeAuction(auctionId));
        timerManager.restoreTimer(removeTimerTimerCorrelationId, () -> removeAuction(auctionId));
    }

    /**
     * Restores the auction id generator from snapshot
     *
     * @param auctionId the auction id
     */
    public void restoreAuctionId(final long auctionId)
    {
        LOGGER.info("Auction ID restored to {}", auctionId);
        idGenerator.set(auctionId);
    }

    /**
     * Gets the current auction id; used for snapshotting
     * @return the next auction id
     */
    public long getAuctionId()
    {
        return idGenerator.get();
    }

    /**
     * Opens an auction if it was in the PRE_OPEN state and is known.
     * @param auctionId the auction id
     */
    public void openAuction(final long auctionId)
    {
        final var optionalAuction = getAuctionById(auctionId);
        if (optionalAuction.isEmpty())
        {
            LOGGER.error("Unknown auction id {}, cannot transition from PRE_OPEN to OPEN", auctionId);
            return;
        }

        final var auction = optionalAuction.get();

        if (auction.getStartTime() > context.getClusterTime())
        {
            LOGGER.error("Auction {} start time is not yet reached, cannot transition to closed", auctionId);
            return;
        }

        if (transitionAuction(auction, AuctionStatus.PRE_OPEN, AuctionStatus.OPEN))
        {
            LOGGER.info("Opening auction with id {}", auctionId);
            broadcastStateUpdate(auctionId);
        }
    }

    /**
     * Closes an auction if it was in the OPEN state and is known.
     * @param auctionId the auction id
     */
    public void closeAuction(final long auctionId)
    {
        final var optionalAuction = getAuctionById(auctionId);
        if (optionalAuction.isEmpty())
        {
            LOGGER.error("Unknown auction id {}, cannot transition from OPEN to CLOSED", auctionId);
            return;
        }

        final var auction = optionalAuction.get();

        if (auction.getEndTime() > context.getClusterTime())
        {
            LOGGER.error("Auction {} end time is not yet reached, cannot transition to closed", auctionId);
            return;
        }

        if (transitionAuction(auction, AuctionStatus.OPEN, AuctionStatus.CLOSED))
        {
            LOGGER.info("Closing auction with id {}", auctionId);
            broadcastStateUpdate(auctionId);
        }

    }

    /**
     * Removes an auction if it is known
     * @param auctionId the auction id
     */
    public void removeAuction(final long auctionId)
    {
        LOGGER.info("Removing auction with id {}", auctionId);
        auctionList.removeIf(auction -> auction.getAuctionId() == auctionId);
    }

    /**
     * Transitions an auction to the next state, if known and in the previously expected state
     * @param auction           the auction to transition
     * @param expectedStatus    the expected status
     * @param newStatus         the new status
     * @return true, if the transition was successful, false otherwise
     */
    private boolean transitionAuction(
        final Auction auction,
        final AuctionStatus expectedStatus,
        final AuctionStatus newStatus)
    {

        if (auction.getAuctionStatus() != expectedStatus)
        {
            LOGGER.error(
                "Unknown auction id {}, cannot transition from {} to {}", auction.getAuctionId(), expectedStatus,
                newStatus);
            return false;
        }
        auction.setAuctionStatus(newStatus);
        return true;
    }

    /**
     * Adds a bid to an existing auction
     *
     * @param auctionId     the auction id
     * @param participantId the participant who is bidding
     * @param price         the price of the bid, in whole cents
     * @param correlationId the correlation id for this request
     */
    public void addBid(final long auctionId, final long participantId, final long price, final String correlationId)
    {
        final var optionalAuction = getAuctionById(auctionId);
        if (optionalAuction.isEmpty())
        {
            clusterClientResponder.rejectAddBid(correlationId, auctionId, AddAuctionBidResult.UNKNOWN_AUCTION);
            return;
        }

        final var auction = optionalAuction.get();
        final var validationResult = validateBid(auction, participantId, price);
        if (validationResult != AddAuctionBidResult.SUCCESS)
        {
            logValidationResult(auctionId, participantId, price, validationResult);
            clusterClientResponder.rejectAddBid(correlationId, auctionId, validationResult);
            return;
        }

        LOGGER.info("Price improvement bid of {} is now winning auction with id {}", price, auctionId);
        auction.setWinningBid(participantId, price, context.getClusterTime());

        clusterClientResponder.onAuctionUpdated(
            correlationId, auction.getAuctionId(), auction.getAuctionStatus(), auction.getCurrentPrice(),
            auction.getBidCount(), auction.getLastUpdateTime(), auction.getWinningParticipantId());
    }


    /**
     * Gets the list of auctions after sorting it by auction id
     *
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
     * @param startTime              the start time of the auction which cannot be the current cluster time or earlier
     * @param endTime                the end time of the auction which must be after the start time
     * @param name                   the name of the auction which must not be null or blank
     * @param description            the description which must not be null or blank
     * @return the result of the validation
     */
    private AddAuctionResult validate(
        final long createdByParticipantId,
        final long startTime,
        final long endTime,
        final String name,
        final String description)
    {
        if (startTime <= context.getClusterTime())
        {
            return AddAuctionResult.INVALID_START_TIME;
        }
        if (endTime <= startTime)
        {
            return AddAuctionResult.INVALID_END_TIME;
        }
        if (startTime + MINIMUM_DURATION > endTime)
        {
            return AddAuctionResult.INVALID_DURATION;
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
     *
     * @param auction       the auction
     * @param participantId the participant who is bidding
     * @param price         the price of the bid
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

    /**
     * Gets an auction by id
     * @param auctionId the auction id
     * @return the auction within an Optional
     */
    private Optional<Auction> getAuctionById(final long auctionId)
    {
        return auctionList.stream().filter(a -> a.getAuctionId() == auctionId).findFirst();
    }

    /**
     * Broadcasts the state update to all cluster clients
     * @param auctionId the auction id
     */
    private void broadcastStateUpdate(final long auctionId)
    {
        final var optionalAuction = getAuctionById(auctionId);
        if (optionalAuction.isPresent())
        {
            final var auction = optionalAuction.get();
            clusterClientResponder.onAuctionStateUpdate(
                auction.getAuctionId(), auction.getAuctionStatus(), auction.getCurrentPrice(), auction.getBidCount(),
                auction.getLastUpdateTime(), auction.getWinningParticipantId());
        }
    }

    /**
     * Logs the validation result
     * @param auctionId the auction id
     * @param participantId the participant id
     * @param price the price
     * @param validationResult the validation result
     */
    private void logValidationResult(
        final long auctionId,
        final long participantId,
        final long price,
        final AddAuctionBidResult validationResult)
    {
        switch (validationResult)
        {
            case CANNOT_SELF_BID ->
            {
                LOGGER.error("Participant {} cannot bid on their own auction with id {}", participantId, auctionId);
            }
            case AUCTION_NOT_OPEN ->
            {
                LOGGER.error("Auction with id {} is not open, cannot add bid", auctionId);
            }
            case PRICE_BELOW_CURRENT_WINNING_BID ->
            {
                LOGGER.error("Bid price {} is below current price for auction with id {}", price, auctionId);
            }
            case INVALID_PRICE ->
            {
                LOGGER.error("Bid price {} is invalid for auction with id {}", price, auctionId);
            }
            case UNKNOWN_AUCTION ->
            {
                LOGGER.error("Unknown auction with id {}", auctionId);
            }
            case UNKNOWN_PARTICIPANT ->
            {
                LOGGER.error("Unknown participant with id {}", participantId);
            }
            default ->
            {
                //do nothing
            }
        }
    }
}
