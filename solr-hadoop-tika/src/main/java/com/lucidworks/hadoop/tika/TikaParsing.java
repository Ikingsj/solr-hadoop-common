package com.lucidworks.hadoop.tika;

import com.lucidworks.hadoop.io.LWDocument;
import com.lucidworks.hadoop.process.TikaProcess;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

// basic tika parsing: this class needs more work, dynamic fields/ check errors ...
public class TikaParsing implements TikaProcess {
  private transient static Logger log = LoggerFactory.getLogger(TikaParsing.class);

  private static Parser parser = new AutoDetectParser();
  private static ParseContext context = new ParseContext();

  //TODO: use this flags here?
  public static final String TIKA_INCLUDE_IMAGES = "default.include.images";
  public static final String TIKA_FLATENN_COMPOUND = "default.faltten.compound";
  public static final String TIKA_ADD_FAILED_DOCS = "default.add.failed.docs";
  public static final String TIKA_ADD_ORIGINAL_CONTENT = "default.add.original.content";
  public static final String FIELD_MAPPING_RENAME_UNKNOWN = "default.rename.unknown";

  private static final String RAW_CONTENT = "_raw_content_";

  public static boolean includeImages = false;
  public static boolean flattenCompound = false;
  public static boolean addFailedDocs = false;
  public static boolean addOriginalContent = false;
  public static boolean renameUnknown = false;

  public static int MAX_TERM_LENGTH_UTF = 32766;

  private static void parseLWSolrDocument(
    LWDocument document,
    byte[] data) {
    ContentHandler text = new BodyContentHandler();
    InputStream input = new ByteArrayInputStream(data);
    Metadata metadata = new Metadata();
    LinkContentHandler links = new LinkContentHandler();
    ContentHandler handler = new TeeContentHandler(links, text);


    try {
      parser.parse(input, handler, metadata, context);
    } catch (IOException e) {
      e.printStackTrace();
      log.warn("IOException " + document.getId());
    } catch (SAXException e) {
      e.printStackTrace();
      log.warn("SAXException " + document.getId());
    } catch (TikaException e) {
      e.printStackTrace();
      log.warn("TikaException " + document.getId());
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    if (metadata != null) {
      for (String name : metadata.names()) {
        String value = metadata.get(name);
        document.addField(name, value);
      }
    }
    if (text != null) {
      String body = text.toString();
      if (body.length() > MAX_TERM_LENGTH_UTF) {
        body = body.substring(0, MAX_TERM_LENGTH_UTF);
      }
      document.addField("body", body);
    }
  }

  @Override
  public LWDocument[] tikaParsing(LWDocument document) {
    Object raw = document.getFirstFieldValue(RAW_CONTENT);
    if (raw instanceof byte[]) {
      parseLWSolrDocument(document, (byte[]) raw);
    }
    document.removeField(RAW_CONTENT);
    return new LWDocument[]{document};
  }
}
