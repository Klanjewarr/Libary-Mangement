package library;

/** Membership plans; each plan carries its own borrowing rules. */
public enum MembershipType {
    STANDARD(3, 14, 2),
    PREMIUM(5, 21, 1);

    private final int maxLoans;
    private final int loanDays;
    private final int finePerDay; // rupees

    MembershipType(int maxLoans, int loanDays, int finePerDay) {
        this.maxLoans = maxLoans;
        this.loanDays = loanDays;
        this.finePerDay = finePerDay;
    }

    public int maxLoans()   { return maxLoans; }
    public int loanDays()   { return loanDays; }
    public int finePerDay() { return finePerDay; }
}
