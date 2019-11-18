package org.dbpedia.synth.diff.prototype.changesets;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds a single changeset, must not be initialized with multiple changesets
 *
 * @author Dimitris Kontokostas
 * @since 9/25/14 11:05 AM
 */
public final class Changeset {
    private final String id;
    private final List<String> additions;
    private final List<String> deletions;
    private final List<String> cleared;
    private final List<String> reinserted;

    public Changeset(String id, List<String> additions, List<String> deletions, List<String> cleared, List<String> reinserted) {
        this.id = id;

        // Keep the changeset unique
        this.additions = Collections.unmodifiableList(additions);
        this.deletions = Collections.unmodifiableList(deletions);
        this.cleared = Collections.unmodifiableList(cleared);
        this.reinserted = Collections.unmodifiableList(reinserted);
    }

    public String getId() {
        return id;
    }

    public List<String> getAdditions() {
        return additions;
    }

    public List<String> getDeletions() {
        return deletions;
    }

    public int triplesAdded() {
        return additions.size();
    }

    public int triplesDeleted() {
        return deletions.size();
    }

    public int triplesCleared() {
        return cleared.size();
    }

    public int triplesReinserted() {
        return reinserted.size();
    }

    public List<String> getCleared() {
        return cleared;
    }

    public List<String> getReinserted() {
        return reinserted;
    }
}
