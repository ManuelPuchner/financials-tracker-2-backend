CREATE TABLE mcc_codes (
    mcc                  VARCHAR(4)   PRIMARY KEY,
    edited_description   VARCHAR(255),
    combined_description VARCHAR(255),
    usda_description     VARCHAR(255),
    irs_description      VARCHAR(255),
    irs_reportable       BOOLEAN
);

-- Note: no FK constraint added here because mcc_codes is populated at application
-- startup (via ApplicationRunner / classpath CSV), which runs after Flyway migrations.
-- The @ManyToOne join in the JPA entity handles the relationship at the application layer.
