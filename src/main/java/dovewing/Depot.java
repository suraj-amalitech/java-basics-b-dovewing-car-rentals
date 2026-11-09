package dovewing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dovewing.model.Rental;
import dovewing.model.RentalStatus;
import dovewing.model.Renter;
import dovewing.model.Vehicle;

/**
 * The Dovewing depot: manages the fleet and all rental transactions.
 *
 * <p>{@link #rent(Renter, Vehicle, int)} is the single place where a rental is
 * created. It checks availability before proceeding, so the invariant
 * "a vehicle is either available or rented — never both, never rented twice"
 * is enforced here and backed by {@link Vehicle#rent()}.
 */
public class Depot {

    private final String name;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Renter>  renters  = new ArrayList<>();
    private final List<Rental>  rentals  = new ArrayList<>();

    public Depot(String name) {
        this.name = name;
    }

    // Fleet and renter management

    public void addVehicle(Vehicle v) {
        vehicles.add(v);
    }

    public void addRenter(Renter r) {
        renters.add(r);
    }

    // Rental operations

    /**
     * Rents {@code vehicle} to {@code renter} for {@code days} days.
     *
     * <p>If the vehicle is not available, the rental is refused and a message
     * is printed — the vehicle's state is not changed. This is the invariant
     * the business says must never be broken.
     *
     * @param renter  the person taking the vehicle
     * @param vehicle the vehicle being rented
     * @param days    number of rental days
     */
    public void rent(Renter renter, Vehicle vehicle, int days) {
        // ── The key guard: refuse if already out ─────────────────────────────
        if (!vehicle.isAvailable()) {
            System.out.println("REFUSED: " + vehicle.getModel()
                + " (" + vehicle.getReg() + ") is already rented - "
                + renter.getName() + "'s request cannot be processed.");
            return;
        }

        double cost    = vehicle.getDailyRate() * days;
        double deposit = vehicle.calculateDeposit(renter.getKind());

        vehicle.rent();   // flips the single availability flag

        Rental rental = new Rental(renter.getName(), vehicle.getReg(), days, cost, deposit);
        rentals.add(rental);
        renter.addHistory("Rented " + vehicle.getModel());

        System.out.println("Rented " + vehicle.getModel()
            + " (" + vehicle.getReg() + ") to " + renter.getName()
            + " for " + days + " days, cost GHS " + cost
            + ", deposit GHS " + deposit);
    }

    /**
     * Processes the return of a vehicle.
     *
     * <p>Marks the vehicle available and closes the most recent active rental
     * for that vehicle.
     *
     * @param vehicle the vehicle being returned
     */
    public void returnVehicle(Vehicle vehicle) {
        vehicle.returnCar();

        // Close the active rental for this vehicle (there is at most one,
        // because we enforce availability on rent).
        for (Rental r : rentals) {
            if (r.getVehicleReg().equals(vehicle.getReg())
                    && r.getStatus() == RentalStatus.ACTIVE) {
                r.markReturned();
                break;  // only one active rental possible per vehicle
            }
        }

        System.out.println("Returned " + vehicle.getModel()
            + " (" + vehicle.getReg() + ")");
    }

    // Reporting

    /**
     * Prints the end-of-day report: fleet status, active rentals, completed
     * returns, and a financial summary.
     *
     * <p>The summary section is the improvement beyond the brief. The original
     * tool showed only whether cars were out; it gave the owner no way to close
     * out the day without counting manually. The summary shows revenue earned
     * from completed rentals, deposits currently held on active rentals, and a
     * running total — information the depot needs every evening without
     * touching a spreadsheet.
     */
    public void printReport() {
        System.out.println();
        System.out.println("=== " + name + " - end of day ===");

        // ── Fleet status 
        System.out.println("Fleet status:");
        for (Vehicle v : vehicles) {
            System.out.println("  " + v);
        }

        // ── Active rentals 
        System.out.println("Active rentals:");
        boolean anyActive = false;
        for (Rental r : rentals) {
            if (r.getStatus() == RentalStatus.ACTIVE) {
                System.out.println("  Rental #" + r.getId() + ": "
                    + r.getVehicleReg() + " -> " + r.getRenterName()
                    + " (" + r.getDays() + " days)");
                anyActive = true;
            }
        }
        if (!anyActive) {
            System.out.println("  (none)");
        }

        // ── Completed returns today
        System.out.println("Completed returns:");
        boolean anyReturned = false;
        for (Rental r : rentals) {
            if (r.getStatus() == RentalStatus.RETURNED) {
                System.out.println("  Rental #" + r.getId() + ": "
                    + r.getVehicleReg() + " -> " + r.getRenterName()
                    + " (" + r.getDays() + " days, GHS " + r.getCost() + ")");
                anyReturned = true;
            }
        }
        if (!anyReturned) {
            System.out.println("  (none)");
        }

        // ── Financial summary
        double revenueCollected  = 0;   // from rentals that have been returned
        double depositsHeld      = 0;   // from rentals still active

        for (Rental r : rentals) {
            if (r.getStatus() == RentalStatus.RETURNED) {
                revenueCollected += r.getCost();
            } else {
                depositsHeld += r.getDeposit();
            }
        }

        System.out.println("Financial summary:");
        System.out.println("  Revenue from completed rentals : GHS " + revenueCollected);
        System.out.println("  Deposits held (active rentals) : GHS " + depositsHeld);
        System.out.println("  Total cash held                : GHS " + (revenueCollected + depositsHeld));
    }

    // Accessors (for testing / future use)

    public List<Rental> getRentals() {
        return Collections.unmodifiableList(rentals);
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public List<Renter> getRenters() {
        return Collections.unmodifiableList(renters);
    }
}
