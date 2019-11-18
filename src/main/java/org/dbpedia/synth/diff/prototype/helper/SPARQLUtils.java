package org.dbpedia.synth.diff.prototype.helper;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.dbpedia.synth.diff.prototype.updates.UpdateHandler;
import org.dbpedia.synth.diff.prototype.updates.UpdateStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SPARQLUtils {

    private String datasetEndpoint;
    private String datasetReleaser;
    private String datasetName;
    private UpdateStyle updateStyle;

    public SPARQLUtils(String datasetEndpoint, String datasetName, String datasetReleaser, UpdateStyle style) {
        this.datasetEndpoint = datasetEndpoint;
        this.datasetName = datasetName;
        this.datasetReleaser = datasetReleaser;
        this.updateStyle = style;
    }


    public List<String> getFiles (String version) {
        Query fileQuery = QueryFactory.create( new ParameterizedSparqlString(""
                + "PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>\n"
                + "PREFIX dct:    <http://purl.org/dc/terms/>\n"
                + "PREFIX dcat:   <http://www.w3.org/ns/dcat#>\n"
                + "PREFIX db:     <https://databus.dbpedia.org/>\n"
                + "PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "\n"
                + "SELECT DISTINCT ?URL WHERE {\n"
                + " GRAPH ?g \n"
                + "  {\n"
                + "   <https://databus.dbpedia.org/"+datasetReleaser+"/"+datasetName+"> rdf:type dataid:Group .\n"
                + "   ?version rdf:type dataid:Version .\n"
                + "   ?file dcat:downloadURL ?URL .\n"
                + "   FILTER contains(str(?version), \""+version+"\")\n"
                + "  }\n"
                + "} "
        ).asQuery());

        List<QuerySolution> result = DBpediaClient.sendQuery(fileQuery, datasetEndpoint);

        List<String> fileURLS = new ArrayList<>();

        for (QuerySolution solution : result) {
            fileURLS.add(solution.getResource("?URL").getURI());
        }


        return fileURLS;
    }

    /**
     * Returns the latest diff-versions after the last used one, ordered oldest to latest
     * @return
     */

    public List<String> getNewVersions(String lastDiffVersion)  {
        Query versionsQuery = QueryFactory.create(new ParameterizedSparqlString(""
                + "PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>\n"
                + "PREFIX dct:    <http://purl.org/dc/terms/>\n"
                + "PREFIX dcat:   <http://www.w3.org/ns/dcat#>\n"
                + "PREFIX db:     <https://databus.dbpedia.org/>\n"
                + "PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "\n"
                + "SELECT DISTINCT ?version WHERE {\n"
                + "  GRAPH ?g {\n"
                + "    <https://databus.dbpedia.org/"+datasetReleaser+"/"+datasetName+"> rdf:type dataid:Group.\n"
                + "    ?version rdf:type dataid:Version.\n"
                + "  }\n"
                + "} \n"
                + "").asQuery());

        List<QuerySolution> resultSet = DBpediaClient.sendQuery(versionsQuery, datasetEndpoint);

        ArrayList<String> result = new ArrayList<>();


        for (QuerySolution solution : resultSet) {
            String version = Utils.getUriIdentifier(solution.getResource("version").getURI());
            if (version.compareTo(lastDiffVersion) > 0){
                result.add(version);
            }
        }
        Collections.sort(result);

        switch (updateStyle) {
            case FULL:
                return result;
            case NEXT:
                return result.subList(0, 1);
            default:
                System.exit(1);
                return null;
        }
    }
}
