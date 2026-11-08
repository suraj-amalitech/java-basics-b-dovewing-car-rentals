package dovewing;

import dovewing.model.Economy;
import dovewing.model.Premium;
import dovewing.model.Rental;
import dovewing.model.RentalStatus;
import dovewing.model.Renter;
import dovewing.model.RenterKind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Depot rental logic.
 *
 * Each test is self-contained: it creates its own Depot, vehicles, and renters
 * so test order does not matter and static ID counters do not affect assertions
 * about business behaviour (we assert on content, not IDs).
 */
class DepotTest {

    private Depot depot;
    private Economy vitz;
    private Premium prado;
    private Renter kofi;   // CASUAL
    private Renter ama;    // MEMBER

    @BeforeEach
    void setUp() {
        depot = new Depot("Dovewing Car Rentals");

        vitz  = new Economy("Toyota Vitz",  "GT-1234-20", 150.00, 500.00);
        prado = new Premium("Toyota Prado", "GR-5678-22", 600.00, 2000.00);
        depot.addVehicle(vitz);
        depot.addVehicle(prado);

        kofi = new Renter("Kofi Mensah", RenterKind.CASUAL);
        ama  = new Renter("Ama Owusu",   RenterKind.MEMBER);
        depot.addRenter(kofi);
        depot.addRenter(ama);
    }

    // -------------------------------------------------------------------------
    // Core invariant: a vehicle cannot be rented twice
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Renting an already-rented vehicle is refused and leaves exactly one active rental")
    void doubleRentIsRefused() {
        depot.rent(kofi, vitz, 3);   // succeeds
        depot.rent(ama,  vitz, 2);   // must be refused

        long activeForVitz = depot.getRentals().stream()
            .filter(r -> r.getVehicleReg().equals("GT-1234-20"))
            .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
            .count();

        assertEquals(1, activeForVitz,
            "Only one active rental should exist for a vehicle that is already rented");
    }

    @Test
    @DisplayName("After a double-rent attempt, the vehicle stays RENTED (not flipped to available)")
    void vehicleRemainsRentedAfterRefusal() {
        depot.rent(kofi, vitz, 3);
        depot.rent(ama,  vitz, 2);   // refused

        assertFalse(vitz.isAvailable(),
            "Vehicle must still be marked rented after a refused second rental");
    }

    // -------------------------------------------------------------------------
    // Cost and deposit calculations
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Casual renter on economy vehicle pays full deposit")
    void casualRenterPaysFullDeposit() {
        depot.rent(kofi, vitz, 3);

        Rental rental = depot.getRentals().get(0);
        assertEquals(500.00, rental.getDeposit(), 0.001,
            "Casual renter should pay the full deposit");
    }

    @Test
    @DisplayName("Member renter on economy vehicle pays zero deposit")
    void memberRenterDepositWaived() {
        depot.rent(ama, vitz, 3);

        Rental rental = depot.getRentals().get(0);
        assertEquals(0.00, rental.getDeposit(), 0.001,
            "Member renter should have deposit waived");
    }

    @Test
    @DisplayName("Member renter on premium vehicle pays zero deposit")
    void memberRenterDepositWaivedOnPremium() {
        depot.rent(ama, prado, 2);

        Rental rental = depot.getRentals().get(0);
        assertEquals(0.00, rental.getDeposit(), 0.001,
            "Member renter deposit should be waived on premium vehicles too");
    }

    @Test
    @DisplayName("Rental cost is dailyRate x days regardless of renter kind")
    void rentalCostCalculation() {
        depot.rent(kofi, vitz, 3);  // 150 x 3 = 450

        Rental rental = depot.getRentals().get(0);
        assertEquals(450.00, rental.getCost(), 0.001,
            "Cost should be dailyRate * days");
    }

    // -------------------------------------------------------------------------
    // Return behaviour
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Returning a vehicle makes it available again")
    void returnMakesVehicleAvailable() {
        depot.rent(ama, prado, 2);
        depot.returnVehicle(prado);

        assertTrue(prado.isAvailable(),
            "Vehicle should be available after return");
    }

    @Test
    @DisplayName("Returning a vehicle marks its rental as RETURNED")
    void returnClosesRentalRecord() {
        depot.rent(ama, prado, 2);
        depot.returnVehicle(prado);

        Rental rental = depot.getRentals().get(0);
        assertEquals(RentalStatus.RETURNED, rental.getStatus(),
            "Rental record should be RETURNED after vehicle is returned");
    }

    @Test
    @DisplayName("A returned vehicle can be rented again")
    void returnedVehicleCanBeRentedAgain() {
        depot.rent(ama,  prado, 2);
        depot.returnVehicle(prado);
        depot.rent(kofi, prado, 1);   // should succeed

        long activeForPrado = depot.getRentals().stream()
            .filter(r -> r.getVehicleReg().equals("GR-5678-22"))
            .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
            .count();

        assertEquals(1, activeForPrado,
            "Re-renting a returned vehicle should create exactly one new active rental");
    }

    // -------------------------------------------------------------------------
    // Financial summary inputs
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Revenue and deposits reflect the correct completed/active split")
    void financialSummaryInputs() {
        depot.rent(kofi, vitz,  3);   // active:  cost 450, deposit 500
        depot.rent(ama,  prado, 2);   // returned: cost 1200, deposit 0
        depot.returnVehicle(prado);

        double revenueCollected = depot.getRentals().stream()
            .filter(r -> r.getStatus() == RentalStatus.RETURNED)
            .mapToDouble(Rental::getCost)
            .sum();

        double depositsHeld = depot.getRentals().stream()
            .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
            .mapToDouble(Rental::getDeposit)
            .sum();

        assertEquals(1200.00, revenueCollected, 0.001, "Revenue from returned rentals");
        assertEquals(500.00,  depositsHeld,     0.001, "Deposits held on active rentals");
    }
}
