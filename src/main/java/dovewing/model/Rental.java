package dovewing.model;

/**
 * An immutable record of a single rental transaction.
 *
 * <p>All fields are set at creation time. The only post-construction mutation
 * allowed is marking the rental as returned via {@link #markReturned()}.
 */
public class Rental {

    private static int counter = 1;

    private final int id;
    private final String renterName;
    private final String vehicleReg;
    private final int days;
    private final double cost;
    private final double deposit;
    private RentalStatus status;

    public Rental(String renterName, String vehicleReg, int days, double cost, double deposit) {
        this.id          = counter++;
        this.renterName  = renterName;
        this.vehicleReg  = vehicleReg;
        this.days        = days;
        this.cost        = cost;
        this.deposit     = deposit;
        this.status      = RentalStatus.ACTIVE;
    }

    /**
     * Marks this rental as returned. Safe to call more than once (idempotent).
     */
    public void markReturned() {
        this.status = RentalStatus.RETURNED;
    }

    public int getId()            { return id; }
    public String getRenterName() { return renterName; }
    public String getVehicleReg() { return vehicleReg; }
    public int getDays()          { return days; }
    public double getCost()       { return cost; }
    public double getDeposit()    { return deposit; }
    public RentalStatus getStatus() { return status; }
}
