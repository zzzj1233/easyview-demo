CREATE DATABASE IF NOT EXISTS demo DEFAULT CHARSET utf8mb4;
USE demo;

CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku         VARCHAR(32)  NOT NULL,
    qty         INT          NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'NEW',
    intent_id   VARCHAR(64),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sku (sku)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory (
    sku         VARCHAR(32)  PRIMARY KEY,
    stock       INT          NOT NULL,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO inventory (sku, stock) VALUES
    ('SKU-1', 1000), ('SKU-2', 500), ('SKU-3', 200), ('SKU-4', 50), ('SKU-5', 10)
ON DUPLICATE KEY UPDATE stock=VALUES(stock);

INSERT INTO orders (sku, qty, status, intent_id) VALUES
    ('SKU-1', 2, 'NEW',     'INT-1001'),
    ('SKU-2', 1, 'PAID',    'INT-1002'),
    ('SKU-3', 5, 'SHIPPED', 'INT-1003'),
    ('SKU-4', 1, 'NEW',     'INT-1004'),
    ('SKU-1', 3, 'CLOSED',  'INT-1005');
