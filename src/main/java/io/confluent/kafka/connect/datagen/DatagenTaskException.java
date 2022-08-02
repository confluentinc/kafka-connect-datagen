package io.confluent.kafka.connect.datagen;

class DatagenTaskException extends RuntimeException {
  public DatagenTaskException(String message) {
    super(message);
  }

  public DatagenTaskException(String message, Throwable cause) {
    super(message, cause);
  }
}
