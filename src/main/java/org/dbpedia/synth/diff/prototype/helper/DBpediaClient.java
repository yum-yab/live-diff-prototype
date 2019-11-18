package org.dbpedia.synth.diff.prototype.helper;


import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Sends SPARQL Queries to the SPARQL Endpoint
 */
public final class DBpediaClient {

  private static final Logger logger = LoggerFactory.getLogger(DBpediaClient.class);
  /**
   * sends query to DB service
   *
   * @param query The request query
   * @return String response from DB service
   */

  public static List<QuerySolution> sendQuery(Query query, String endpoint) {
    ResultSet resultSet;
    try (QueryExecution execution = QueryExecutionFactory.sparqlService(endpoint, query)) {
      resultSet = execution.execSelect();
      logger.info("Query sent to: " + endpoint);
      return ResultSetFormatter.toList(resultSet);
    } catch (Exception e) {
      logger.warn("Tried to send the query to the SparqlService... " + e.getMessage());
      throw e;
    }
  }

}

