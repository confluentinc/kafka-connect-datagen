package io.confluent.kafka.connect.datagen;

public class DatagenException extends RuntimeException {
  public DatagenException(String message) {
    super(message);
  }

  public DatagenException(String message, Throwable cause) {
    super(message, cause);
  }
}
