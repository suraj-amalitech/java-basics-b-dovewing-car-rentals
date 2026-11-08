package dovewing.model;

/**
 * A premium-class vehicle.
 *
 * <p>Deposit rules: the standard deposit is charged to casual renters;
 * members have their deposit waived.
 */
public class Premium extends Vehicle {

    public Premium(String model, String reg, double dailyRate, double deposit) {
        super(model, reg, dailyRate, deposit);
    }

    @Override
    public double calculateDeposit(RenterKind kind) {
        return (kind == RenterKind.MEMBER) ? 0.0 : getBaseDeposit();
    }

    @Override
    public VehicleCategory getCategory() {
        return VehicleCategory.PREMIUM;
    }
}
