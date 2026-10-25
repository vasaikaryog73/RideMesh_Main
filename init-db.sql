-- RideMesh Phase 3 Database Initialization
CREATE DATABASE ridemesh_user_db;
CREATE DATABASE ridemesh_ride_db;
CREATE DATABASE ridemesh_wallet_db;
CREATE DATABASE notify_db;

-- Connect and enable PostGIS for spatial queries on ride & user DBs
\c ridemesh_user_db;
CREATE EXTENSION IF NOT EXISTS postgis;

\c ridemesh_ride_db;
CREATE EXTENSION IF NOT EXISTS postgis;
