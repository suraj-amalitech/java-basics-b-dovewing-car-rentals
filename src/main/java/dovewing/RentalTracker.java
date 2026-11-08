package dovewing;

import dovewing.model.Economy;
import dovewing.model.Premium;
import dovewing.model.Renter;
import dovewing.model.RenterKind;

/**
 * Entry point for the Dovewing fleet and rental tracker.
 *
 * <p>This demo reproduces the exact scenario from the original program so the
 * output can be compared side-by-side. The one intentional behaviour change:
 * the second rental of the Vitz (which is already out with Kofi) is now
 * refused with a clear message instead of silently accepted — this is the
 * invariant the business said must never be broken.
 */
public class RentalTracker {

    public static void main(String[] args) {
        Depot d = new Depot("Dovewing Car Rentals");

        Economy vitz  = new Economy("Toyota Vitz",  "GT-1234-20", 150.00, 500.00);
        Premium prado = new Premium("Toyota Prado", "GR-5678-22", 600.00, 2000.00);
        d.addVehicle(vitz);
        d.addVehicle(prado);

        Renter kofi = new Renter("Kofi Mensah",  RenterKind.CASUAL);
        Renter ama  = new Renter("Ama Owusu",    RenterKind.MEMBER);
        d.addRenter(kofi);
        d.addRenter(ama);

        // a normal day
        d.rent(kofi, vitz,  3);
        d.rent(ama,  prado, 2);
        d.returnVehicle(prado);

        // the Vitz is already out with Kofi — this is now refused clearly
        // instead of silently accepted (the change the business asked for)
        d.rent(ama, vitz, 2);

        d.printReport();
    }
}
