package org.dbpedia.synth.diff.prototype.helper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.jena.ext.xerces.impl.xpath.regex.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;


/**
 * Description
 *
 * @author Dimitris Kontokostas
 * @since 9/24/14 1:16 PM
 */
public final class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);


    private static final Pattern tripleRegex = Pattern.compile("<(.*)> <(.*)> (\".*\"\\^\\^)?<(.*)>[ ]*\\.");

    private Utils() {
    }


    public static List<String> getTriplesFromBzip2File (File file) {
        //decompress file
        String decompressedFilePath = decompressBzip2File(file);
        List<String> result;
        if (decompressedFilePath != null){
            result = getTriplesFromFile(decompressedFilePath);
        } else {
            result = null;
        }
        File decompressedFile = new File(decompressedFilePath);
        if (decompressedFile.exists()) {
            decompressedFile.delete();
        }


        return result;
    }

    private static List<String> getTriplesFromFile(String filename) {
        List<String> lines = new ArrayList<>();

        logger.info("Loading and validating triples from file "+ filename);

        try (
                FileInputStream fileInputStream= new FileInputStream(filename);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
                BufferedReader in = new BufferedReader(inputStreamReader)
        ) {

            String line;
            while ((line = in.readLine()) != null) {
                String triple = line.trim();

                // Ends with is a hack for not correctly decompressed changesets
                if (!triple.isEmpty() && !triple.startsWith("#") && tripleIsValid(triple)) {
                    lines.add(triple);
                }

            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File " + filename + " not fount!", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UnsupportedEncodingException: ", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException in file " + filename, e);
        }

        return lines;

    }

    public static String getFileAsString(String filename) {
        StringBuilder str = new StringBuilder();

        try (
                FileInputStream fileInputStream= new FileInputStream(filename);
                InputStreamReader in = new InputStreamReader(fileInputStream, "UTF-8")
        ) {
            int ch;
            while ((ch = in.read()) != -1) {
                str.append((char) ch);
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File " + filename + " not fount!", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UnsupportedEncodingException: ", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException in file " + filename, e);
        }

        return str.toString();
    }

    private static boolean deleteFile(String filename) {
        try {
            File file = new File(filename);
            boolean retVal = file.delete();
            return retVal;
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateStringFromList(Collection<String> strList, String sep) {

        StringBuilder finalString = new StringBuilder();


        for (String str : strList) {
            finalString.append(str);
            finalString.append(sep);
        }

        return finalString.toString();
    }

    public static boolean writeTriplesToFile(List<String> triples, String filename) {

        try (
                FileOutputStream fileOutputStream = new FileOutputStream(filename);
                OutputStreamWriter out = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)
        ) {

            for (String triple : triples) {
                out.write(triple + "\n");
            }

            return true;

        } catch (IOException e) {
            logger.error("Error writing file: " + filename, e);
        }

        return false;
    }

    private static String decompressBzip2File (File file) {

        logger.info("Decompressing the file "+file);
        String filename = getUriIdentifier(file.getAbsolutePath());
        String outFilename = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf("."));
        try (InputStream is = Files.newInputStream(file.toPath());
             BufferedInputStream in = new BufferedInputStream(is);
             BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
             OutputStreamWriter outStream = new OutputStreamWriter(new FileOutputStream(outFilename), StandardCharsets.UTF_8);
             InputStreamReader inStream = new InputStreamReader(bzIn, StandardCharsets.UTF_8)
        )
        {
            int character;
            while ((character = inStream.read()) != -1) {
                outStream.write(character);
            }

        }  catch (EOFException e) {
            // probably Wrong compression, out stream will close and existing contents will remain
            // but might leave incomplete triples
            logger.error("EOFException in compressed file: " + filename + " - Trying to recover");
        } catch (IOException ioe) {
            logger.warn("File " + filename + " cannot be decompressed due to " + ioe.getMessage(), ioe);
            outFilename = null;
        } finally {
            Utils.deleteFile(filename);
        }
        return outFilename;
    }

    /**
     * Decompresses the passed GZip file, and returns the filename of the decompressed file
     *
     * @param filename The filename of compressed file
     * @return The filename of the output file, or empty string if a problem occurs
     */
    public static String decompressGZipFile(String filename) {

        String outFilename;
        //The output filename is the same as input filename without last .gz
        int lastDotPosition = filename.lastIndexOf('.');
        outFilename = filename.substring(0, lastDotPosition);

        try (
                FileInputStream fis = new FileInputStream(filename);
                //GzipCompressorInputStream(
                GZIPInputStream gis = new GZIPInputStream(fis);
                InputStreamReader isr = new InputStreamReader(gis, "UTF8");
                //BufferedReader in = new BufferedReader(isr);
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outFilename), "UTF8")
        ) {
            int character;
            while ((character = isr.read()) != -1) {
                out.write(character);

            }

            logger.debug("File : " + filename + " decompressed successfully to " + outFilename);
        } catch (EOFException e) {
            // probably Wrong compression, out stream will close and existing contents will remain
            // but might leave incomplete triples
            logger.error("EOFException in compressed file: " + filename + " - Trying to recover");
        } catch (IOException ioe) {
            logger.warn("File " + filename + " cannot be decompressed due to " + ioe.getMessage(), ioe);
            outFilename = "";
        } finally {
            Utils.deleteFile(filename);
        }
        return outFilename;
    }

    /**
     * Downloads the file with passed URL to the passed folder
     * http://stackoverflow.com/a/921400/318221
     *
     * @param fileURL    URL of the file that should be downloaded
     * @param folderPath The path to which this file should be saved
     * @return The local full path of the downloaded file, empty string is returned if a problem occurs
     */
    public static String downloadFile(String fileURL, String folderPath) {

        //Extract filename only without full path
        int lastSlashPos = fileURL.lastIndexOf('/');
        if (lastSlashPos < 0) {
            return null;
        }
        logger.info("Downloading file "+ fileURL+ "...");
        String fullFileName = folderPath + fileURL.substring(lastSlashPos + 1);

        //Create parent folder if it does not already exist
        File file = new File(fullFileName);

        URL url;

        try {
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            return null;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() != 200) {
                conn.getErrorStream().read();
                conn.disconnect();
                return null;
            }
            InputStream in = conn.getInputStream();
            Closeable res = in;
            try {
                ReadableByteChannel rbc = Channels.newChannel(in);
                FileOutputStream fos = new FileOutputStream(file);

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } finally {
                res.close();
                conn.disconnect();
            }
        } catch (IOException e) {
            return null;
        }

        return fullFileName;
    }

    public static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    }

    public static boolean tripleIsValid (String triple) {
        // get the URIS out of the triple
        Matcher tripleMatcher = tripleRegex.matcher(triple);

        if (tripleMatcher.find()) {
            String subjectUri = tripleMatcher.group(1);
            String predicateUri = tripleMatcher.group(2);
            String objectUri = tripleMatcher.group(4);

            if (subjectUri != null && predicateUri != null && objectUri != null) {
                // a triple is valid if every uri in it is valid
                return validateURI(subjectUri) && validateURI(predicateUri) && validateURI(objectUri);
            }
        }
        return false;
    }

    private static boolean validateURI(String uri) {
        try {
            URI.create(uri);
            return true;
        } catch (Exception e){
            logger.warn("Found invalid URI: "+uri+". Removing it from triples ...");

            return false;
        }
    }

    public static String getUriIdentifier(String uri){
        Pattern uriRegex = Pattern.compile(".*\\.[\\w]+(\\/.+)*\\/(.+)");
        Matcher m = uriRegex.matcher(uri);

        if (m.find()){
            return m.group(2);
        } else {
            return null;
        }
    }

    public static boolean checkFileValidity(File file) {
        if (file != null && file.exists() && file.isFile()) return true; else return false;
    }
}
