# Auctions

The auction model is simple:

- auction enables buyers to acquire the entire auction item. There is no quantity involved.
- the best price submitted before endTime wins the auction
- the auction creator cannot interact with the auction once created – the winning bid is automatically accepted
- the auction is open for a fixed period of time, starting at startTime and ending at endTime
- bids will be rejected unless they bring price improvement

## Protocol

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

1. the auction creator sends a CreateAuctionCommand to the Auctions object in the cluster using `CreateAuctionCommand`. The command includes the auction details and a correlationId.
2. the Auctions object validates the command and sends a `CreateAuctionCommandResult` back to the creator, with the original correlationId and the result code. If successful, the result includes the auctionID.
3. if the result code is SUCCESS, the Auctions object sends a `NewAuctionEvent` to all connected clients with the auction details.