package io.confluent.kafka.connect.datagen;

public class DatagenTaskException extends RuntimeException {
  public DatagenTaskException(String message) {
    super(message);
  }

  public DatagenTaskException(String message, Throwable cause) {
    super(message, cause);
  }
}
