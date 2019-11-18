package org.dbpedia.synth.diff.prototype.changesets;

import com.google.common.collect.Lists;
import org.dbpedia.synth.diff.prototype.helper.Utils;
import org.dbpedia.synth.diff.prototype.sparul.SPARULException;
import org.dbpedia.synth.diff.prototype.sparul.SPARULExecutor;
import org.dbpedia.synth.diff.prototype.sparul.SPARULGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Applies a changeset in a SPARULExecutor using a SPARULGenerator
 *
 * @author Dimitris Kontokostas
 * @since 9/26/14 9:34 AM
 */
public class ChangesetExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ChangesetExecutor.class);

    private final SPARULExecutor sparulExecutor;

    private final SPARULGenerator sparulGenerator;

    private enum Action {ADD, DELETE}

    public ChangesetExecutor(SPARULExecutor sparulExecutor, SPARULGenerator sparulGenerator) {
        this.sparulExecutor = sparulExecutor;
        this.sparulGenerator = sparulGenerator;
    }

    public boolean applyChangeset(Changeset changeset) {

        boolean status = true;

        // First clear resources (if any)
        if (changeset.triplesCleared() > 0) {
            boolean status_c = executeClearResources(changeset.getCleared());
            logger.info("Patch " + changeset.getId() + " CLEARED " + changeset.triplesCleared() + " resources");
            status = status && status_c;
        }

        // Deletions must be executed before additions

        if (changeset.triplesDeleted() > 0) {
            boolean status_d = executeAction(changeset.getDeletions(), Action.DELETE);
            logger.info("Patch " + changeset.getId() + " DELETED " + changeset.triplesDeleted() + " triples");
            status = status && status_d;
        }

        if (changeset.triplesAdded() > 0) {
            boolean status_a = executeAction(changeset.getAdditions(), Action.ADD);
            logger.info("Patch " + changeset.getId() + " ADDED " + changeset.triplesAdded() + " triples");
            status = status && status_a;
        }

        if (changeset.triplesReinserted() > 0) {
            boolean status_a = executeAction(changeset.getReinserted(), Action.ADD);
            logger.info("Patch " + changeset.getId() + " REINSERTED " + changeset.triplesReinserted() + " resources");
            status = status && status_a;
        }

        return status;

    }

    public void clearGraph() {
        executeSparulWrapper(sparulGenerator.clearGraph());
    }

    private boolean executeClearResources(Collection<String> resources) {
        boolean status = true;
        for (String resource : resources) {
            boolean result = executeSparulWrapper(sparulGenerator.deleteResource(resource));
            if (!result) {
                logger.error("Could not clear triples for <" + resource + ">");
            }
            status = status && result;

        }
        return status;
    }

    private boolean executeAction(List<String> triples, Action action) {
        boolean result = false;
        if (triples.size() < 1000) {
            String pattern = Utils.generateStringFromList(triples, "\n");
            String sparul = action.equals(Action.ADD) ? sparulGenerator.insert(pattern) : sparulGenerator.delete(pattern);
            result = executeSparulWrapper(sparul);
            if (result) {
                logger.info("Added "+ triples.size() +" triples to "+ sparulGenerator.graph);
            }
        } else {
            int chunks = (int) Math.ceil((double)triples.size()/900);
            logger.info("Too many triples, splitting the " + triples.size() + " triples into "+ chunks + " chunks...");
            for (List<String> subList : Lists.partition(triples, 900)) {
                executeAction(subList, action);
                result = true;
            }
        }

        if (result) {
            return true;
        }
        // if only 1 triple just log and return
        if (triples.size() == 1) {
            // size = 1, get triple from collection
            String triple = "";
            for (String s : triples) {
                triple = s;
            }
            logger.error("Cannot " + action.toString() + " triple: \n" + triple);
            return false;
        }

        logger.warn("Tried to " + action.toString() + " " + triples.size() + " but failed, splitting into chunks to spot the error");
        // Split collection and retry
        // In the end we will go to one (or more) single problematic triples, log it (previous block) and finish
        for (List<String> subList : Lists.partition(triples, 5)) {
            executeAction(subList, action);
        }

        return false;
    }


    // inefficient, dont use
    private <T> Collection<Collection<T>> splitCollection(Collection<T> collection, int chunks) {
        ArrayList<Collection<T>> lists = new ArrayList<>();
        for (int i = 0; i < chunks; i++) {
            lists.add(new ArrayList<T>());
        }
        int counter = 0;
        for (T item : collection) {
            int index = counter % chunks;
            lists.get(index).add(item);
            counter++;
        }
        return lists;
    }

    private boolean executeSparulWrapper(String sparul) {
        try {
            sparulExecutor.executeSPARUL(sparul);
        } catch (SPARULException e) {
            logger.warn("Error in query execution");
            return false;
        }
        return true;
    }

}
