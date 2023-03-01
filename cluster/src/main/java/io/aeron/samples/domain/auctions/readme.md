# Auctions

The auction model is simple:

- auction enables buyers to acquire the entire auction item. There is no quantity involved.
- the best price submitted before endTime wins the auction
- the auction creator cannot interact with the auction once created – the winning bid is automatically accepted
- the auction is open for a fixed period of time, starting at startTime and ending at endTime
- bids will be rejected unless they bring price improvement

## Protocol

### General points

The protocol assumes that:

- Gateways are responsible for distributing data to their user sessions. The cluster does not track user sessions that are managed by the gateways. 
- Gateways are able to store some minimal state. On connection, they can retrieve the current auction state from the cluster, and after that will maintain their state via `NewAuctionEvent` and `AuctionUpdateEvent` messages. In turn, they can distribute the current state of the world as needed to their clients.
- UUID based 36 character correlationIds will be used to track request/response flows.

### Create Auction

```
┌──────────┐                                ┌──────────────┐
│          ├──────────────────────────────► │              │
│Auction   │ 1. CreateAuctionCommand        │              │
│Creator   │                                │   Auctions   │
│          │                                │   (Cluster)  │
│          │ ◄──────────────────────────────┤              │
└──────────┘  2. CreateAuctionCommandResult │              │
             (SUCCESS or validation failure)│              │
┌──────────┐                                │              │
│          │                                │              │
│All       │ ◄──────────────────────────────┤              │
│Connected │  3. NewAuctionEvent if SUCCESS │              │
│Clients   │                                │              │
│          │                                │              │
└──────────┘                                └──────────────┘
```

The create auction flow operates as follows:

1. the auction creator sends a `CreateAuctionCommand` to the Auctions object in the cluster. The command includes the auction details and a correlationId.
2. the `Auctions` object validates the command and sends a `CreateAuctionCommandResult` back to the creator, with the original correlationId and the result code. If successful, the result includes the auctionID.
3. if the result code is `SUCCESS`, the Auctions object sends a `NewAuctionEvent` to all connected clients with the auction details.

### Add Auction Bid

```
┌──────────┐                                ┌──────────────┐
│          ├──────────────────────────────► │              │
│Auction   │ 1. AddAuctionBidCommand        │              │
│Bidder    │                                │   Auctions   │
│          │                                │   (Cluster)  │
│          │ ◄──────────────────────────────┤              │
└──────────┘  2. AddAuctionBidCommandResult │              │
             (SUCCESS or validation failure)│              │
┌──────────┐                                │              │
│          │                                │              │
│All       │ ◄──────────────────────────────┤              │
│Connected │  3. AuctionUpdateEvent         │              │
│Clients   │     if SUCCESS                 │              │
│          │                                │              │
└──────────┘                                └──────────────┘
```


The add auction bid flow operates as follows:

1. the auction bidder sends a `AddAuctionBidCommand` to the Auctions object in the cluster using `CreateAuctionCommand`. The command includes the auction details and a correlationId.
2. the Auctions object validates the command and sends a `CreateAuctionCommandResult` back to the creator, with the original correlationId and the result code. If successful, the result includes the auctionID.
3. if the result code is SUCCESS, the Auctions object sends a `NewAuctionEvent` to all connected clients with the auction details.


## Snapshots

Auction data are written to snapshot only if the startTime is in the future.
Open and already closed auctions will not be written to the snapshot.