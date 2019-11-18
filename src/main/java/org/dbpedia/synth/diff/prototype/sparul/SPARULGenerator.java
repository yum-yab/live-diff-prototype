package org.dbpedia.synth.diff.prototype.sparul;
/**
 * Generates a SPARUL Query that is bound to a graph
 *
 * @author Dimitris Kontokostas
 * @since 9/24/14 10:22 AM
 */
public class SPARULGenerator {

    public String graph;

    public SPARULGenerator (String graph) {
        this.graph = graph;
    }


    public String insert(String triples) {
        return generate(triples, true, graph);
    }

    public String delete(String triples) { return generate(triples, false, graph); }

    public String deleteResource(String resource) {
        return "DELETE FROM <" + graph + "> {" +
                "  ?s ?p ?o" +
                " } WHERE {" +
                "  ?s ?p ?o." +
                "  FILTER ( ?s = <" + resource + ">)" +
                "}";
    }

    public String clearGraph() {
        return "CLEAR GRAPH <" + graph + "> ";
    }


    private String generate(String triples, boolean toAdd, String graph) {
        return (toAdd ? "INSERT DATA " : "DELETE DATA ") + "{ GRAPH <" + graph + "> {\n" + triples + "\n}} ";
    }
}
