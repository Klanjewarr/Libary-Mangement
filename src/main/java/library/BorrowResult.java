package library;

/** Result of a borrow request: success carries the loan, failure carries a reason. */
public sealed interface BorrowResult permits BorrowResult.Success, BorrowResult.Rejected {

    enum Reason { UNKNOWN_MEMBER, UNKNOWN_BOOK, NO_COPIES_AVAILABLE, LOAN_LIMIT_REACHED }

    record Success(Loan loan) implements BorrowResult { }

    record Rejected(Reason reason) implements BorrowResult { }
}
