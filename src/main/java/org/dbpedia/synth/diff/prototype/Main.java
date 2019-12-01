package org.dbpedia.synth.diff.prototype;

import org.dbpedia.synth.diff.prototype.helper.Global;
import org.dbpedia.synth.diff.prototype.updates.UpdateHandler;
import org.dbpedia.synth.diff.prototype.updates.UpdateStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void  main (String[] args) {

    long starttime = System.currentTimeMillis();

    String endpoint = Global.getOptions().get("Dataset.endpoint");
    String datasetName = Global.getOptions().get("Dataset.name");
    String releaser = Global.getOptions().get("Dataset.releaser");
    String localPath = Global.getOptions().get("Local.path");
    UpdateStyle updateStyle = UpdateStyle.valueOf(Global.getOptions().get("Update.style").toUpperCase());

    UpdateHandler handler = new UpdateHandler(datasetName,endpoint,releaser,localPath, updateStyle);
    handler.handleUpdates();
    long millis = System.currentTimeMillis() - starttime;
    String time = String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

    logger.info("Finished upload of the diff of "+datasetName+", time: "+time);

    //System.out.println("a".compareTo("b"));

  }
}
