package org.rabix.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerboseLogger {

  public static final String LOGGER_NAME = "verbose";
  
  private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
  
  public static void log(String message) {
    logger.info(message);
  }
  
}
