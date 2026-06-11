ALTER TABLE shared_item
    ADD COLUMN key_version BIGINT NOT NULL DEFAULT 1 AFTER version;

ALTER TABLE shared_item_member
    MODIFY encrypted_item_key_base64 TEXT NULL,
    ADD COLUMN updated_at DATETIME(6) NULL AFTER created_at;

UPDATE shared_item_member
   SET updated_at = created_at
 WHERE updated_at IS NULL;
