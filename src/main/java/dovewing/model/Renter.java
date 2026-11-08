package dovewing.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A person registered to rent vehicles from the depot.
 */
public class Renter {

    private static int counter = 1;

    private final int id;
    private final String name;
    private final RenterKind kind;
    private final List<String> history = new ArrayList<>();

    public Renter(String name, RenterKind kind) {
        this.id   = counter++;
        this.name = name;
        this.kind = kind;
    }

    /** Records a rental event in this renter's history. */
    public void addHistory(String entry) {
        history.add(entry);
    }

    public int getId()              { return id; }
    public String getName()         { return name; }
    public RenterKind getKind()     { return kind; }

    /** Returns an unmodifiable view of the rental history. */
    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
