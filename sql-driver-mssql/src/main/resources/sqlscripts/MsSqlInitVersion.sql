CREATE TABLE MTD_VERSION
(
    VERSION_VALUE numeric(18, 0) NOT NULL,
    CONSTRAINT MTD_VERSION_PK PRIMARY KEY(VERSION_VALUE)
)
#GO
INSERT INTO MTD_VERSION (VERSION_VALUE) VALUES (0)
#GO