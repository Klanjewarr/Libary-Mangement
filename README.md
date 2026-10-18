# Library Management (Java 21)

A small, in-memory domain model for a college library, written for the OOP with Java large assignment (M3.LA.01).

## What does the system do?
It keeps a catalogue of books, registers library members, lends books to members and charges a fine when a book comes back late. All data is held in memory behind repository interfaces, so a real database could replace it later without touching the service.

## Who uses it?
- **Librarian (primary user)** - adds books, registers members, issues and receives books through `LibraryService`.
- **Member (indirect user)** - their membership plan (STANDARD / PREMIUM) decides how many books they can hold, for how many days, and the fine per late day.
- **Developer / tester** - uses the service API and the JUnit suite.

## The 4 main domain operations
1. **Add book** - `addBook(Book)`: a new ISBN creates a catalogue entry; an existing ISBN adds more copies.
2. **Register member** - `registerMember(Member)`: one registration per member id.
3. **Borrow book** - `borrow(memberId, isbn)`: checks member, book, stock and loan limit; returns `Success(loan)` or `Rejected(reason)`.
4. **Return book** - `returnBook(loanId)`: closes the loan, restocks the book and returns a `Fine` (`NoFine` or `OverdueFine`).

## Business rules
| Plan | Max loans | Loan period | Fine per late day |
|---|---|---|---|
| STANDARD | 3 | 14 days | Rs. 2 |
| PREMIUM | 5 | 21 days | Rs. 1 |

## Java 21 features used
Records (`Book`, `Member`, `Loan`, fines), sealed interfaces (`Fine`, `BorrowResult`), `Optional` (repository lookups, `returnBook`), pattern matching for `switch` with record patterns (`LibraryService.describe`), composition (service holds repositories, no inheritance between services and stores).

## Build and test
```
mvn test                      # JUnit 5 + JaCoCo report in target/site/jacoco
```
Without Maven: `javac -d out -cp <junit-console-standalone.jar> src/main/java/library/*.java src/test/java/library/*.java`
then `java -jar <junit-console-standalone.jar> -cp out --scan-classpath`.
