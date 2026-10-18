package library;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** The four domain operations: add book, register member, borrow, return. */
public final class LibraryService {

    private final Repository<Book, String> books;
    private final Repository<Member, String> members;
    private final Repository<Fine.OverdueFine, String> fines;
    private final LoanRepository loans;
    private final Clock clock;
    private final AtomicLong loanSeq = new AtomicLong();

    public LibraryService(Repository<Book, String> books, Repository<Member, String> members,
                          Repository<Fine.OverdueFine, String> fines, LoanRepository loans, Clock clock) {
        this.books = books;
        this.members = members;
        this.fines = fines;
        this.loans = loans;
        this.clock = clock;
    }

    /** Adds a title; if the ISBN already exists the copies are merged into the existing entry. */
    public Book addBook(Book book) {
        Book merged = books.findById(book.isbn())
                .map(existing -> existing.withCopies(existing.copies() + book.copies()))
                .orElse(book);
        return books.save(merged);
    }

    public Member registerMember(Member member) {
        if (members.findById(member.id()).isPresent()) {
            throw new IllegalStateException("Member already registered: " + member.id());
        }
        return members.save(member);
    }

    public BorrowResult borrow(String memberId, String isbn) {
        Optional<Member> member = members.findById(memberId);
        if (member.isEmpty()) return new BorrowResult.Rejected(BorrowResult.Reason.UNKNOWN_MEMBER);

        Optional<Book> book = books.findById(isbn);
        if (book.isEmpty()) return new BorrowResult.Rejected(BorrowResult.Reason.UNKNOWN_BOOK);
        if (book.get().copies() == 0) return new BorrowResult.Rejected(BorrowResult.Reason.NO_COPIES_AVAILABLE);

        MembershipType plan = member.get().type();
        if (loans.findActiveByMember(memberId).size() >= plan.maxLoans()) {
            return new BorrowResult.Rejected(BorrowResult.Reason.LOAN_LIMIT_REACHED);
        }

        LocalDate today = LocalDate.now(clock);
        Loan loan = loans.save(new Loan("L" + loanSeq.incrementAndGet(), isbn, memberId,
                today, today.plusDays(plan.loanDays()), null));
        books.save(book.get().withCopies(book.get().copies() - 1));
        return new BorrowResult.Success(loan);
    }

    /** Closes a loan and assesses a fine. Empty if the loan is unknown or already returned. */
    public Optional<Fine> returnBook(String loanId) {
        return loans.findById(loanId)
                .filter(Loan::isActive)
                .map(loan -> {
                    LocalDate today = LocalDate.now(clock);
                    loans.save(loan.closedOn(today));
                    books.findById(loan.isbn()).ifPresent(b -> books.save(b.withCopies(b.copies() + 1)));
                    return assess(loan, today);
                });
    }

    public long totalFines(String memberId) {
        return fines.findAll().stream()
                .filter(f -> f.memberId().equals(memberId))
                .mapToLong(Fine.OverdueFine::amount)
                .sum();
    }

    private Fine assess(Loan loan, LocalDate returnedOn) {
        long daysLate = ChronoUnit.DAYS.between(loan.dueOn(), returnedOn);
        if (daysLate <= 0) return new Fine.NoFine(loan.id());
        int rate = members.findById(loan.memberId()).map(m -> m.type().finePerDay()).orElse(0);
        return fines.save(new Fine.OverdueFine(loan.id(), loan.memberId(), daysLate, daysLate * rate));
    }

    /** Human-readable text built with pattern matching for switch. */
    public static String describe(BorrowResult result) {
        return switch (result) {
            case BorrowResult.Success(Loan loan) -> "Borrowed " + loan.isbn() + ", due " + loan.dueOn();
            case BorrowResult.Rejected(var reason) -> switch (reason) {
                case UNKNOWN_MEMBER      -> "Member not found";
                case UNKNOWN_BOOK        -> "Book not found";
                case NO_COPIES_AVAILABLE -> "No copies available";
                case LOAN_LIMIT_REACHED  -> "Loan limit reached";
            };
        };
    }

    public static String describe(Fine fine) {
        return switch (fine) {
            case Fine.NoFine nf                      -> "Returned on time, no fine";
            case Fine.OverdueFine(var id, var m, long days, long amt) -> days + " day(s) late, fine Rs. " + amt;
        };
    }
}
