package dovewing.model;

/**
 * A vehicle in the Dovewing fleet.
 *
 * <p>Availability is the single source of truth: it is owned by this class
 * behind a private field and can only change through {@link #rent()} and
 * {@link #returnCar()}. No external code can put a vehicle into a contradicted
 * state (e.g. "RENTED" in the fleet report but with no active rental record)
 * because there is no public field to corrupt.
 *
 * <p>Subclasses supply category-specific deposit rules by overriding
 * {@link #calculateDeposit(RenterKind)}. Adding a new vehicle class (e.g.
 * CommercialVan) requires only a new subclass — nothing in this class or in
 * {@code Depot} needs to change.
 */
public abstract class Vehicle {

    private static int counter = 1;

    private final int id;
    private final String model;
    private final String reg;
    private final double dailyRate;
    private final double baseDeposit;
    private boolean rented;   // single, private source of truth for availability

    protected Vehicle(String model, String reg, double dailyRate, double baseDeposit) {
        this.id = counter++;
        this.model = model;
        this.reg = reg;
        this.dailyRate = dailyRate;
        this.baseDeposit = baseDeposit;
        this.rented = false;
    }

    // Availability — enforced state machine

    /** Returns true if the vehicle is available to be rented. */
    public boolean isAvailable() {
        return !rented;
    }

    /**
     * Marks this vehicle as rented.
     *
     * @throws IllegalStateException if the vehicle is already rented — this is
     *         the invariant the business says must never be broken.
     */
    public void rent() {
        if (rented) {
            throw new IllegalStateException(
                model + " (" + reg + ") is already rented and cannot be rented again.");
        }
        rented = true;
    }

    /** Marks this vehicle as available again after a return. */
    public void returnCar() {
        rented = false;
    }

    // Deposit rule — subclasses define category-specific behaviour

    /**
     * Returns the deposit amount that should be charged for this vehicle given
     * the renter's membership kind. Subclasses override this to apply their own
     * deposit rules.
     *
     * @param kind the renter's membership kind
     * @return the deposit amount in GHS
     */
    public abstract double calculateDeposit(RenterKind kind);

    // Category label — subclasses identify themselves

    /** Returns the {@link VehicleCategory} this vehicle belongs to. */
    public abstract VehicleCategory getCategory();

    // Accessors

    public int getId()           { return id; }
    public String getModel()     { return model; }
    public String getReg()       { return reg; }
    public double getDailyRate() { return dailyRate; }
    public double getBaseDeposit() { return baseDeposit; }

    /** Returns "RENTED" or "AVAILABLE" — for display in reports. */
    public String getStatusLabel() {
        return rented ? "RENTED" : "AVAILABLE";
    }

    @Override
    public String toString() {
        return model + " (" + reg + ", " + getCategory() + "): " + getStatusLabel();
    }
}
