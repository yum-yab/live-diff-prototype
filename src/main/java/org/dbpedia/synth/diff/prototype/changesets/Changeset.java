package org.dbpedia.synth.diff.prototype.changesets;

import java.io.File;
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
    private final File additions;
    private final File deletions;

    public Changeset(String id, File additions, File deletions) {
        this.id = id;

        // Keep the changeset unique
        this.additions = additions;
        this.deletions = deletions;
    }

    public String getId() {
        return id;
    }

    public File getAdditions() {
        return additions;
    }

    public File getDeletions() {
        return deletions;
    }

}
