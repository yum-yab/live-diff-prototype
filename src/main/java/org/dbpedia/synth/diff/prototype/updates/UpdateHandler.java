package org.dbpedia.synth.diff.prototype.updates;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.ext.xerces.impl.xpath.regex.Match;
import org.apache.jena.query.*;
import org.dbpedia.synth.diff.prototype.helper.DBpediaClient;
import org.dbpedia.synth.diff.prototype.changesets.ChangesetExecutor;
import org.dbpedia.synth.diff.prototype.helper.LastVersionFileWriter;
import org.dbpedia.synth.diff.prototype.helper.SPARQLUtils;
import org.dbpedia.synth.diff.prototype.helper.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateHandler {

  private String datasetName;
  private String lastDiffVersion;
  private String datasetReleaser;
  private String datasetEndpoint;
  private String localPath;
  private static final Logger logger = LoggerFactory.getLogger(UpdateHandler.class);
  private SPARQLUtils sparqlUtils;
  private LastVersionFileWriter lastVersionFileWriter;

  // patterns to generate the names
  private final Pattern splitFilename = Pattern.compile("([a-z\\-]+)(_[a-zA-Z-=_]+)(.[\\w\\.]+)");



  public UpdateHandler (String datasetName, String endpoint, String releaser, String localPath, UpdateStyle updateStyle) {
    this.datasetName = datasetName;
    this.datasetEndpoint = endpoint;
    this.datasetReleaser = releaser;
    this.sparqlUtils = new SPARQLUtils(datasetEndpoint, datasetName, datasetReleaser, updateStyle);
    this.lastVersionFileWriter = new LastVersionFileWriter();

    File localDir = new File(localPath);
    if (localDir.exists() && localDir.isDirectory()) {
      this.localPath = localDir.getPath();
    } else {
      logger.error("Couldn't find local Directory "+localPath+" , please use a valid directory.");
      System.exit(1);
    }

    String lastDiff = this.lastVersionFileWriter.getLastVersion();
    if (lastDiff != null) {
      this.lastDiffVersion = lastDiff;
    } else {
      logger.error("No lastVersion defined in the last Version file.");
      System.exit(1);
    }
  }


  public void handleUpdates() {
    logger.info("Starting the Update of "+ datasetName+ "...");
    List<String> versions = sparqlUtils.getNewVersions(lastDiffVersion);
    List<Update> updateList = generateUpdates(versions);
    createDirStructure(updateList);

    for (Update update : updateList) {
      update.applyUpdate();
    }
    lastVersionFileWriter.writeLatestDiffVersion(versions.get(versions.size()-1));
  }

  private List<Update> generateUpdates(List<String> versions) {

    if (versions.isEmpty()) {
      logger.info("No new diff versions. Exiting...");
      System.exit(0);
    }
    logger.info("Found "+versions.size()+" new versions of "+datasetName+"...");

    List<Update> result = new ArrayList<>();

    for (String version : versions) {
      Map<String, Map<FileType, DBpediaFile>> updateFiles = new HashMap<>();
      List<String> files = sparqlUtils.getFiles(version);
      for (String fileURL : files) {
        String filename = Utils.getUriIdentifier(fileURL);
        String graphname = generateGraphname(fileURL);
        DBpediaFile dbpediaFile = generateDBpediaFile(fileURL, version);
        FileType type = getFileTypeFromURL(fileURL);
        if (type != null && graphname != null && dbpediaFile != null) {
          if (updateFiles.containsKey(graphname)) {
            updateFiles.get(graphname).put(type, dbpediaFile);
          } else {
            updateFiles.put(graphname, new HashMap<FileType, DBpediaFile>());
            updateFiles.get(graphname).put(type, dbpediaFile);
          }
        } else {
          logger.warn("There was an error parsing the file "+ fileURL);
        }
      }
      String updateId = datasetName + "-UPDATE-" + version;
      result.add(new Update(updateId, version, updateFiles));
    }
    logger.info("Found " +result.get(0).updateFiles.keySet().size() + " different graphs");

    return result;
  }

  private String generateGraphname(String fileURL) {
    String filename = Utils.getUriIdentifier(fileURL);
    Matcher m = splitFilename.matcher(filename);
    String artifactName;
    String lang;

    if (m.find()) {
      artifactName = getArtifactName(m.group(1));
      lang = getLangFromTags(m.group(2).split("_"));
    } else {
      return null;
    }

    return  artifactName + "-" + lang.toUpperCase();
  }

  private DBpediaFile generateDBpediaFile(String fileURL, String version) {
    String filename = Utils.getUriIdentifier(fileURL);
    Matcher m = splitFilename.matcher(filename);
    String artifactName = null;
    if (m.find()) {
      artifactName = getArtifactName(m.group(1));
    }
    String[] pathArray = {localPath, datasetName, artifactName, version, ""};
    String path = String.join(File.separator, pathArray);

    if (artifactName != null) {
      DBpediaFile dbpediaFile = new DBpediaFile(filename, path, fileURL, artifactName);
      return dbpediaFile;
    } else {
     return null;
    }
  }

  private void createDirStructure (List<Update> updateList) {
    List<String> artifacts = new ArrayList<>();
    List<String> versions = new ArrayList<>();

    // get the different artifacts
    for (Map map : updateList.get(0).updateFiles.values()) {
      for (Object obj :  map.values()) {
        DBpediaFile dbpediaFile = (DBpediaFile) obj;
        if (!artifacts.contains(dbpediaFile.artifact)) {
          artifacts.add(dbpediaFile.artifact);
        }
      }
    }

    // get the different versions
    for (Update update : updateList) {
      versions.add(update.version);
    }
    new File(localPath +"/"+ datasetName).mkdir();

    for (String artifact : artifacts) {
      String artifactPath = localPath + File.separator + datasetName + File.separator + artifact;
      new File(artifactPath).mkdir();
      for (String version : versions) {
        String versionPath = localPath +File.separator+ datasetName + File.separator + artifact + File.separator + version;
        new File(localPath + File.separator + datasetName + File.separator + artifact + File.separator + version).mkdir();
      }
    }
  }

  private String getArtifactName (String artifactName) {
    Pattern p = Pattern.compile("(.*)-diff");
    Matcher m = p.matcher(artifactName);

    if (m.find()) {
      return m.group(1);
    } else {
      return artifactName;
    }
  }

  private String getLangFromTags (String[] tags) {
    // pattern to recognize the lang code
    Pattern langpattern = Pattern.compile("lang=([\\w]+)");
    String lang = null;
    for (String tag : tags) {
      Matcher m = langpattern.matcher(tag);
      if (m.find()) {
        lang = m.group(1);
      }
    }
    return lang;
  }

  private FileType getFileTypeFromURL (String fileURL) {
    FileType type = null;

    Matcher m = splitFilename.matcher(fileURL);

    if (m.find()) {
      String[] tags = m.group(2).split("_");

      for (String tag : tags) {
        try {
          type = FileType.valueOf(tag.toUpperCase());
        } catch (IllegalArgumentException ignored) {

        }
      }
    }
    return type;
  }
}

