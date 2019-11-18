package org.dbpedia.synth.diff.prototype.updates;

public class DBpediaFile {
    final String filename;
    final String filepath;
    final String downloadURL;
    final String artifact;

    DBpediaFile(String filename, String filepath, String URL, String artifact) {
        this.filename = filename;
        this.filepath = filepath;
        this.downloadURL = URL;
        this.artifact = artifact;
    }
}
