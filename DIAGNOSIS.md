# Dovewing Car Rentals — Diagnosis

> Written before any code changes. The original program was compiled and run
> first; the output is quoted below as evidence.

---

## Observed output of the original program

```
Rented Toyota Vitz (GT-1234-20) to Kofi Mensah for 3 days, cost GHS 450.0, deposit GHS 500.0
Rented Toyota Prado (GR-5678-22) to Ama Owusu for 2 days, cost GHS 1200.0, deposit GHS 0.0
Returned Toyota Prado (GR-5678-22)
Rented Toyota Vitz (GT-1234-20) to Ama Owusu for 2 days, cost GHS 300.0, deposit GHS 0.0

=== Dovewing Car Rentals - end of day ===
Fleet status:
  Toyota Vitz (GT-1234-20, ECONOMY): RENTED
  Toyota Prado (GR-5678-22, PREMIUM): AVAILABLE
Active rentals:
  Rental #1: GT-1234-20 -> Kofi Mensah (3 days)
  Rental #3: GT-1234-20 -> Ama Owusu (2 days)
```

One physical car, two active rental records, two customers who believe they
have it. That is the contradiction the depot's paper backup exists to catch.

---

## Defects, root causes, and business risks

### Defect 1 — `rent()` never checks availability  *(most dangerous)*

**What is wrong.** `Depot.rent()` sets `v.status = "RENTED"` and creates a new
`Rental` record unconditionally, regardless of whether the vehicle is already
out. The line `d.rent(ama, vitz, 2)` succeeds silently even though Kofi already
has the Vitz.

**Root cause.** There is no guard condition; availability is never tested before
a rental is created.

**Why it is the most dangerous defect.** A car-rental business's core promise
is that a vehicle handed to one customer is not promised to another. Violating
that promise is an immediate customer-facing failure: one renter drives away
while the other is told the car is not here, or both show up at the same time.
It is also a liability question — two signed rental agreements for the same
vehicle. No amount of careful bookkeeping after the fact repairs that. This is
the defect that makes the staff keep a paper backup, which means the software
has stopped being the source of truth.

---

### Defect 2 — Two independent sources of truth for vehicle status

**What is wrong.** Whether a vehicle is rented is tracked in two separate
places: `Vehicle.status` (a plain `String` field) and the presence of an
`ACTIVE` `Rental` record in `Depot.rentals`. They are kept in sync by hand
across `rent()` and `returnVehicle()`. There is nothing that guarantees they
agree.

**Root cause.** Status has no single owner. Because both places are public and
mutable, any code path that touches one and not the other leaves the system in a
contradicted state. `returnVehicle()` even papers over defect 1 silently: it
marks *every* active rental for a given reg as `RETURNED` in one pass, which
means the double-rental record is cleaned up without anyone noticing it existed.

**Business risk.** The fleet report says a car is available while a rental
record says it is active, or vice versa. Any report the system produces is
untrustworthy, which is exactly why the paper backup exists.

---

### Defect 3 — All fields are public and mutable (no encapsulation)

**What is wrong.** Every field on `Vehicle`, `Renter`, and `Rental` is declared
`public`. `Vehicle.status` can be written from anywhere with
`v.status = "AVAILABLE"`, bypassing every rule in `Depot`.

**Root cause.** The classes are data bags, not objects with behaviour. There is
no fence around the invariant, so the invariant cannot be enforced — even
adding a guard to `rent()` is not enough, because any other code path can flip
the field directly.

**Business risk.** Any future maintenance — pricing changes, adding the
commercial-van class, a new report — risks corrupting status just by touching
the wrong field. This is almost certainly the mechanism behind "a tweak to
premium pricing once broke how economy returns were handled": with everything
public, changes to one area of the code can accidentally touch state that
belongs to another.

---

### Defect 4 — String literals for categories, kinds, and statuses

**What is wrong.** `"ECONOMY"`, `"PREMIUM"`, `"CASUAL"`, `"MEMBER"`,
`"AVAILABLE"`, and `"RENTED"` are all bare `String` constants compared with
`.equals()`. A typo such as `"ECONOMY "` or `"economy"` compiles without error
and silently falls through every branch.

**Root cause.** No compile-time type safety. The `rent()` method has no `else`
branch, so an unknown category produces a rental record with cost `0.0` and
deposit `0.0`, and the vehicle's status is never set to `RENTED` — a partially
created rental in an unknown state.

**Business risk.** Silent incorrect pricing (charging nothing, waiving deposits
that should not be waived) and state corruption that only shows up at the end
of the day — if it shows up at all.

---

### Defect 5 — Copy-paste duplication in `rent()` hides a design flaw

**What is wrong.** The `ECONOMY` and `PREMIUM` branches in `rent()` are
identical line for line. The category string changes which branch is entered;
the logic inside is the same.

**Root cause.** Vehicle categories are strings, not types. There is no
per-category behaviour — the method branches on a string and then does the same
thing either way. When rates *do* differ by category (which they do — different
`dailyRate` and `deposit` values are set on construction), the variation comes
from instance fields, not from the type.

**Business risk.** The depot is adding a commercial-van class next quarter.
Under this design, that means adding a third `else if` branch that copies the
same logic a third time. A future developer will inevitably diverge the branches
and create category-specific bugs, as has apparently already happened once.

---

### Defect 6 — Static mutable ID counters

**What is wrong.** `Vehicle.counter`, `Renter.counter`, and `Rental.counter`
are `static` fields incremented in every constructor. ID values depend on the
global run order of the whole program.

**Root cause.** ID generation is a hidden shared side effect. Every constructor
call modifies class-level state.

**Business risk.** Lower risk than the above, but untestable: you cannot
construct a `Vehicle` in isolation without advancing a counter that affects
every other `Vehicle` in the same JVM run. This makes automated verification
unreliable and makes IDs in reports meaningless across runs.

---

## Most dangerous defect

**Defect 1** — the missing availability check. The others are design problems
that make the code fragile and hard to change. Defect 1 is an active
correctness failure that already produces wrong output in the demo scenario the
original author left in `main`. It is the direct cause of the paper backup, and
the direct cause of two customers believing they have the same car.

---

## Planned structure

```
src/main/java/
  dovewing/
    model/
      VehicleCategory.java   // enum: ECONOMY, PREMIUM (COMMERCIAL_VAN ready to add)
      RenterKind.java        // enum: CASUAL, MEMBER
      RentalStatus.java      // enum: ACTIVE, RETURNED
      Vehicle.java           // abstract class; owns availability state privately
      Economy.java           // extends Vehicle; Economy deposit/rate rules
      Premium.java           // extends Vehicle; Premium deposit/rate rules
      Renter.java            // immutable-ish; kind as enum
      Rental.java            // immutable record of one rental transaction
    Depot.java               // fleet + rental list; rent() enforces availability
    RentalTracker.java       // main; same demo scenario as original
```

### Key structural decisions

- `Vehicle` is an abstract class with a **private** `boolean rented` field and
  `rent()` / `returnCar()` methods that enforce the state machine. External
  code can only ask `isAvailable()` — it cannot write to the field.
- `Economy` and `Premium` each override a `calculateDeposit(RenterKind)` method
  so deposit rules live in the vehicle type, not in a branching switch. Adding
  `CommercialVan` is one new subclass, zero changes to existing code.
- `Depot.rent()` checks `v.isAvailable()` and rejects the rental with a clear
  message if the vehicle is already out. This is the one behaviour change the
  brief explicitly authorises.
- All string sentinels replaced with enums so the compiler catches typos.
- `Rental` is effectively immutable after construction (final fields); status
  change on return is the only mutation and goes through a method.

### How the van fits

To add `CommercialVan` next quarter:
1. Add `COMMERCIAL_VAN` to `VehicleCategory` (one line).
2. Write `CommercialVan extends Vehicle`, overriding `calculateDeposit()` with
   van-specific rules.
3. Nothing else changes.

Under the original design you would add a third copy-paste branch to `rent()`,
which is exactly the change that previously broke something.

---

*Design sketch is attached as `design-sketch.svg` in this repository.*
