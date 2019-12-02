package org.dbpedia.synth.diff.prototype.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LastVersionFileWriter {

    private final String pathToLastDiffVersion;
    private static final Logger logger = LoggerFactory.getLogger(LastVersionFileWriter.class);

    public LastVersionFileWriter() {
        String classespath = LastVersionFileWriter.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        Pattern normalPattern = Pattern.compile("(/.+)*/");
        Pattern jarPattern = Pattern.compile("(/.*)*/.*\\.jar");

        Matcher normalMatcher = normalPattern.matcher(classespath);
        Matcher jarMatcher = jarPattern.matcher(classespath);
        String returnpath;

        if (normalMatcher.matches()) {
            returnpath = classespath;
        } else if (jarMatcher.matches()) {
            returnpath = jarMatcher.group(1) + "/classes/";
        } else {
            returnpath = classespath;
        }
        File diffVersionFile = new File(returnpath+"lastDiff.version");

        if (diffVersionFile.exists() && diffVersionFile.isFile()) {
            this.pathToLastDiffVersion = diffVersionFile.getAbsolutePath();
        } else {
            logger.info("Couldnt find "+diffVersionFile.getAbsolutePath());
            System.exit(1);
            this.pathToLastDiffVersion = null;
        }
    }

    public String getLastVersion() {


        File lastDiffFile = new File(pathToLastDiffVersion);

        String lastDiff;

        try {
            BufferedReader br = new BufferedReader(new FileReader(lastDiffFile));
            String firstLine = br.readLine();
            lastDiff = firstLine.equals("") ? null : firstLine;
        } catch (IOException ioEx) {
            lastDiff = null;
        }
        return lastDiff;
    }

    public void writeLatestDiffVersion (String latestVersion) {
        try {
            FileWriter writer = new FileWriter(pathToLastDiffVersion);
            logger.info("Writing the new lastDiff value: "+latestVersion);
            writer.write(latestVersion);
            writer.close();
        } catch (IOException ioEx) {
            logger.error("There was an error writing to the lastDiff File:\n"+ioEx.getMessage());
        }
    }
}
