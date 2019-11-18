package org.dbpedia.synth.diff.prototype;

import com.google.common.collect.Lists;
import org.dbpedia.synth.diff.prototype.helper.Global;
import org.dbpedia.synth.diff.prototype.helper.Utils;
import org.dbpedia.synth.diff.prototype.updates.UpdateHandler;
import org.dbpedia.synth.diff.prototype.updates.UpdateStyle;
import org.slf4j.Logger;

import java.util.List;


public class Main {

  private static String graph = "specific-mappingbased-properties";
  private static String startfile = "specific-mappingbased-properties_lang=de.ttl";
  private static String addsfile = "specific-mappingbased-properties-diff_lang=de_adds.ttl";
  private static String deletesfile = "specific-mappingbased-properties-diff_lang=de_deletes.ttl";
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);


  public static void  main (String[] args) {
    //SPARULGenerator generator = new SPARULGenerator(graph);
    // insert the starting triples into virtuoso
    /*
    List<String> starttriples = Utils.getTriplesFromFile(startfile);
    List<String> addtriples = Utils.getTriplesFromFile(addsfile);
    List<String> deletetriples = Utils.getTriplesFromFile(deletesfile);
    String repoURL = Global.getOptions().get("repoEndpoint");

    String id =  "first-try";
    Changeset startset = new Changeset(id, starttriples, new HashSet<>(), new HashSet<>(), new HashSet<>());
    Changeset changeset = new Changeset(id, addtriples, deletetriples, new HashSet<>(), new HashSet<>());
    ChangesetExecutor executor = new ChangesetExecutor(new SPARULVosExecutor(), new SPARULGenerator(graph));
    executor.applyChangeset(changeset);
    */

    String endpoint = Global.getOptions().get("Dataset.endpoint");
    String datasetName = Global.getOptions().get("Dataset.name");
    String releaser = Global.getOptions().get("Dataset.releaser");
    String localPath = Global.getOptions().get("Local.path");
    UpdateStyle updateStyle = UpdateStyle.valueOf(Global.getOptions().get("Update.style").toUpperCase());

    UpdateHandler handler = new UpdateHandler(datasetName,endpoint,releaser,localPath, updateStyle);
    handler.handleUpdates();


    //System.out.println("a".compareTo("b"));

  }
}
