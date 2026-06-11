ALTER TABLE shared_item
    ADD COLUMN membership_version BIGINT NOT NULL DEFAULT 1 AFTER key_version;
