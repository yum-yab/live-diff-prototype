package org.dbpedia.synth.diff.prototype.changesets;

import com.google.common.collect.Lists;
import me.tongfei.progressbar.ProgressBar;
import org.dbpedia.synth.diff.prototype.helper.Global;
import org.dbpedia.synth.diff.prototype.helper.Utils;
import org.dbpedia.synth.diff.prototype.sparul.SPARULException;
import org.dbpedia.synth.diff.prototype.sparul.SPARULExecutor;
import org.dbpedia.synth.diff.prototype.sparul.SPARULGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtuoso.jdbc4.VirtuosoException;

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

    private int querySize;

    public ChangesetExecutor(SPARULExecutor sparulExecutor, SPARULGenerator sparulGenerator) {
        this.sparulExecutor = sparulExecutor;
        this.sparulGenerator = sparulGenerator;
        this.querySize = Integer.parseInt(Global.getOptions().get("Store.querySize"));
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
            logger.info("Started the deleting of " + changeset.triplesDeleted() +" in graph "+ sparulGenerator.graph + "...");
            boolean status_d = executeAction(changeset.getDeletions(), Action.DELETE);
            logger.info("Patch " + changeset.getId() + " DELETED " + changeset.triplesDeleted() + " triples");
            status = status && status_d;
        }

        if (changeset.triplesAdded() > 0) {
            logger.info("Started the adding of " + changeset.triplesAdded() +" in graph "+ sparulGenerator.graph + "...");
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
        boolean result = true;
        if (triples.size() <= querySize) {
            String pattern = String.join("\n", triples);
            String sparul = action.equals(Action.ADD) ? sparulGenerator.insert(pattern) : sparulGenerator.delete(pattern);
            result = executeSparulWrapper(sparul);
        } else {
            int chunks = (int) Math.ceil((double)triples.size()/ querySize);
            logger.info("Too many triples, splitting the " + triples.size() + " triples into "+ chunks + " chunks...");
            ProgressBar pb = new ProgressBar("Sending to virtuoso:",chunks).start();
            for (List<String> subList : Lists.partition(triples, querySize)) {
                String pattern = Utils.generateStringFromList(subList, "\n");
                String sparul = action.equals(Action.ADD) ? sparulGenerator.insert(pattern) : sparulGenerator.delete(pattern);
                boolean queryResult = executeSparulWrapper(sparul);
                result = result && queryResult;
                pb.step();
            }
            pb.stop();
        }
        if (!result) logger.warn("Something went wrong with executing the query, try a smaller query size");
        return result;
    }


    private boolean executeSparulWrapper(String sparul) {
        try {
            sparulExecutor.executeSPARUL(sparul);
        } catch (SPARULException e) {
            logger.warn("Error in query execution");
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

}
