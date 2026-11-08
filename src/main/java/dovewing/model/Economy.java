package dovewing.model;

/**
 * An economy-class vehicle.
 *
 * <p>Deposit rules: the standard deposit is charged to casual renters;
 * members have their deposit waived.
 */
public class Economy extends Vehicle {

    public Economy(String model, String reg, double dailyRate, double deposit) {
        super(model, reg, dailyRate, deposit);
    }

    @Override
    public double calculateDeposit(RenterKind kind) {
        return (kind == RenterKind.MEMBER) ? 0.0 : getBaseDeposit();
    }

    @Override
    public VehicleCategory getCategory() {
        return VehicleCategory.ECONOMY;
    }
}
