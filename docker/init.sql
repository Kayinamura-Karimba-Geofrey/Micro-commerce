-- Creates one database per microservice.
-- Executed automatically the first time the postgres container starts
-- (mounted into /docker-entrypoint-initdb.d). It does NOT run again for an
-- existing postgres_data volume: reset with `docker compose down -v` after
-- changing this file.
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
