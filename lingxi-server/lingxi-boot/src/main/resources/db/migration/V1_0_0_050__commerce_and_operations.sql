-- R07 会员订单、权益与运营治理
CREATE TABLE IF NOT EXISTS pay_product (
 id BIGINT PRIMARY KEY, product_key VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, scene VARCHAR(64) NOT NULL DEFAULT 'ALL', billing_period VARCHAR(24) NOT NULL,
 age_policy VARCHAR(24) NOT NULL, entitlement_key VARCHAR(64) NOT NULL, entitlement_amount BIGINT NOT NULL,
 status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_pay_product_key(product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_price (
 id BIGINT PRIMARY KEY, product_id BIGINT NOT NULL, version_no INT NOT NULL, amount_minor BIGINT NOT NULL,
 currency CHAR(3) NOT NULL, status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_pay_price_version(product_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_order (
 id BIGINT PRIMARY KEY, order_no VARCHAR(64) NOT NULL, business_order_key VARCHAR(128) NOT NULL, user_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL, price_id BIGINT NOT NULL, price_version INT NOT NULL, amount_minor BIGINT NOT NULL,
 currency CHAR(3) NOT NULL, channel VARCHAR(32) NOT NULL, status VARCHAR(32) NOT NULL, payment_reference VARCHAR(255) NULL,
 refunded_minor BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_order_no(order_no), UNIQUE KEY uk_pay_order_business(business_order_key), KEY idx_pay_order_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_order_item (
 id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, product_id BIGINT NOT NULL, price_id BIGINT NOT NULL,
 quantity INT NOT NULL, amount_minor BIGINT NOT NULL, snapshot_json JSON NOT NULL, UNIQUE KEY uk_pay_order_item(order_id,product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_transaction (
 id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL, transaction_id VARCHAR(128) NOT NULL,
 amount_minor BIGINT NOT NULL, currency CHAR(3) NOT NULL, raw_digest CHAR(64) NOT NULL, verified_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_transaction(channel,transaction_id), KEY idx_pay_transaction_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_refund (
 id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL, refund_transaction_id VARCHAR(128) NOT NULL,
 amount_minor BIGINT NOT NULL, reason VARCHAR(512) NOT NULL, status VARCHAR(24) NOT NULL, created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_refund(channel,refund_transaction_id), KEY idx_pay_refund_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_subscription (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL,
 channel_subscription_id VARCHAR(128) NOT NULL, status VARCHAR(32) NOT NULL, period_end DATETIME(6) NULL, cancel_mode VARCHAR(24) NULL,
 version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 last_reconciled_at DATETIME(6) NULL,
 UNIQUE KEY uk_pay_subscription_channel(channel,channel_subscription_id), KEY idx_pay_subscription_user(user_id,product_id,status),
 KEY idx_pay_subscription_reconcile(last_reconciled_at,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_entitlement (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, resource_key VARCHAR(64) NOT NULL, balance BIGINT NOT NULL,
 expires_at DATETIME(6) NULL, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_entitlement(user_id,resource_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_entitlement_ledger (
 id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, resource_key VARCHAR(64) NOT NULL, delta BIGINT NOT NULL,
 balance_after BIGINT NOT NULL, source_type VARCHAR(32) NOT NULL, source_id VARCHAR(128) NOT NULL,
 command_id VARCHAR(128) NOT NULL, created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_ledger_command(command_id), KEY idx_pay_ledger_user(user_id,resource_key,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pay_channel_callback (
 id BIGINT PRIMARY KEY, channel VARCHAR(32) NOT NULL, callback_id VARCHAR(128) NOT NULL, digest CHAR(64) NOT NULL,
 status VARCHAR(24) NOT NULL, received_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_pay_callback(channel,callback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_support_ticket (
 id BIGINT PRIMARY KEY, ticket_no VARCHAR(64) NOT NULL, user_id BIGINT NOT NULL, category VARCHAR(32) NOT NULL,
 subject VARCHAR(200) NOT NULL, description TEXT NOT NULL, status VARCHAR(24) NOT NULL, priority VARCHAR(16) NOT NULL,
 assignee_admin_id BIGINT NULL, version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_ops_ticket_no(ticket_no), KEY idx_ops_ticket_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_feature_flag (
 id BIGINT PRIMARY KEY, flag_key VARCHAR(64) NOT NULL, current_release_id BIGINT NULL, mandatory_policy BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(24) NOT NULL, updated_at DATETIME(6) NOT NULL, UNIQUE KEY uk_ops_flag_key(flag_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_experiment (
 id BIGINT PRIMARY KEY, experiment_key VARCHAR(64) NOT NULL, hypothesis VARCHAR(512) NOT NULL,
 audience_rule JSON NOT NULL, metrics_json JSON NOT NULL, status VARCHAR(24) NOT NULL, current_release_id BIGINT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, UNIQUE KEY uk_ops_experiment_key(experiment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_config_release (
 id BIGINT PRIMARY KEY, release_key VARCHAR(128) NOT NULL, config_type VARCHAR(64) NOT NULL, version_no INT NOT NULL,
 content_ref VARCHAR(512) NOT NULL, content_digest CHAR(64) NOT NULL, gray_rule JSON NULL, status VARCHAR(32) NOT NULL,
 created_by BIGINT NOT NULL, approved_by BIGINT NULL, published_by BIGINT NULL, previous_release_id BIGINT NULL,
 failure_reason VARCHAR(512) NULL, lock_version BIGINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uk_ops_release_key(release_key), UNIQUE KEY uk_ops_release_version(config_type,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ops_audit_log (
 id BIGINT PRIMARY KEY, admin_id BIGINT NOT NULL, action VARCHAR(64) NOT NULL, object_type VARCHAR(64) NOT NULL,
 object_id VARCHAR(128) NOT NULL, reason VARCHAR(512) NOT NULL, ticket_no VARCHAR(64) NOT NULL,
 before_digest VARCHAR(128) NULL, after_digest VARCHAR(128) NULL, result VARCHAR(24) NOT NULL,
 request_id VARCHAR(128) NOT NULL, created_at DATETIME(6) NOT NULL, KEY idx_ops_audit_admin(admin_id,created_at),
 KEY idx_ops_audit_object(object_type,object_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
