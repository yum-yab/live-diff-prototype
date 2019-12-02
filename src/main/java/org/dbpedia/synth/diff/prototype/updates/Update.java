package org.dbpedia.synth.diff.prototype.updates;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.dbpedia.synth.diff.prototype.updates.FileType;
import org.dbpedia.synth.diff.prototype.changesets.Changeset;
import org.dbpedia.synth.diff.prototype.changesets.ChangesetExecutor;
import org.dbpedia.synth.diff.prototype.helper.Utils;
import org.dbpedia.synth.diff.prototype.sparul.SPARULGenerator;
import org.dbpedia.synth.diff.prototype.sparul.SPARULVosExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Update {

  private String id;

  String version;

  private static final Logger logger = LoggerFactory.getLogger(Update.class);
  // key: graphname value: Map with key filetype and Value DBpediaFile
  Map<String, Map<FileType, DBpediaFile>> updateFiles;

  public Update (String id, String version, Map<String, Map<FileType, DBpediaFile>> updateFiles) {
    this.id = id;
    this.version = version;
    this.updateFiles = updateFiles;
  }



  void applyUpdate() {
    for (String graph : updateFiles.keySet()) {

      logger.info("Starting the update with version "+ this.version+" on graph "+graph+"...");

      Map fileMap = updateFiles.get(graph);
      DBpediaFile addsfile = ((DBpediaFile) fileMap.get(FileType.ADDS));
      DBpediaFile deletesfile = ((DBpediaFile) fileMap.get(FileType.DELETES));
      File realAddsFile = new File(addsfile.getFullPath());
      File realDeletesFile = new File(deletesfile.getFullPath());
      // download the related files
      Utils.downloadFile(addsfile.downloadURL, addsfile.filepath);
      Utils.downloadFile(deletesfile.downloadURL, deletesfile.filepath);

      // add the files to the graph
      ChangesetExecutor executor = new ChangesetExecutor(new SPARULVosExecutor(), new SPARULGenerator(graph));
      //List<String> adds = Utils.getTriplesFromBzip2File(addsfile.filepath + File.separator + addsfile.filename);
      //List<String> deletes = Utils.getTriplesFromBzip2File(deletesfile.filepath + File.separator + deletesfile.filename);
      if (Utils.checkFileValidity(realAddsFile) && Utils.checkFileValidity(realDeletesFile)){
        Changeset changeset = new Changeset(id + "-" + graph,realAddsFile,realDeletesFile);
        //Changeset changeset = new Changeset(id + "-" + graph, adds, new HashSet<>(), new HashSet<>(), new HashSet<>());
        executor.applyChangeset(changeset);
      }
    }
  }
}


