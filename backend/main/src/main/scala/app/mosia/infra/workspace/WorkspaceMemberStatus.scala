package app.mosia.infra.workspace

enum WorkspaceMemberStatus:
  case Accepted,
    AllocatingSeat,
    NeedMoreSeat,
    NeedMoreSeatAndReview,
    Pending,
    UnderReview
