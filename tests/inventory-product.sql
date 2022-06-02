SET 'auto.offset.reset' = 'earliest';

CREATE STREAM inventory_stream (
  id BIGINT,
  quantity INTEGER,
  productid BIGINT
) WITH (
  KAFKA_TOPIC = 'inventory',
  VALUE_FORMAT = 'JSON'
);

CREATE STREAM product_stream (
  id BIGINT,
  name VARCHAR,
  description VARCHAR,
  price DOUBLE
) WITH (
  KAFKA_TOPIC = 'product',
  VALUE_FORMAT = 'JSON'
);

-- Denormalize data, joining facts (inventory) with the dimension (product)
CREATE TABLE inventory_latest WITH (KAFKA_TOPIC = 'inventory_latest') AS
SELECT
  p.id AS product_id,
  SUM(i.quantity) AS item_quantity
FROM inventory_stream i
  LEFT JOIN product_stream p
  WITHIN 1 HOUR
  ON i.productid = p.id
GROUP BY p.id
EMIT CHANGES;

-- Test with
-- SELECT * FROM inventory_latest WHERE product_id = 10000000015 EMIT CHANGES;
