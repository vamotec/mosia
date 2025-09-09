-- ==============================================================================
-- Database initialization script for Fetcher microservice
-- Creates necessary extensions and initial schema
-- ==============================================================================

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS fetcher;
CREATE SCHEMA IF NOT EXISTS metrics;

-- Set search path
SET search_path TO fetcher, public;

-- Create tables for fetching requests and results
CREATE TABLE IF NOT EXISTS fetch_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    workspace_id VARCHAR(255) NOT NULL,
    source_type VARCHAR(100) NOT NULL,
    source_url TEXT NOT NULL,
    parameters JSONB DEFAULT '{}',
    headers JSONB DEFAULT '{}',
    options JSONB DEFAULT '{}',
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS fetch_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID REFERENCES fetch_requests(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    data JSONB,
    error_message TEXT,
    size_bytes BIGINT,
    processing_time_seconds DECIMAL(10,3),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_fetch_requests_user_id ON fetch_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_fetch_requests_workspace_id ON fetch_requests(workspace_id);
CREATE INDEX IF NOT EXISTS idx_fetch_requests_status ON fetch_requests(status);
CREATE INDEX IF NOT EXISTS idx_fetch_requests_created_at ON fetch_requests(created_at);
CREATE INDEX IF NOT EXISTS idx_fetch_results_request_id ON fetch_results(request_id);
CREATE INDEX IF NOT EXISTS idx_fetch_results_status ON fetch_results(status);

-- Create metrics tables
CREATE TABLE IF NOT EXISTS metrics.service_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,6) NOT NULL,
    labels JSONB DEFAULT '{}',
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_metrics_service_name ON metrics.service_metrics(service_name);
CREATE INDEX IF NOT EXISTS idx_service_metrics_timestamp ON metrics.service_metrics(timestamp);

-- Create update trigger for fetch_requests
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_fetch_requests_updated_at 
    BEFORE UPDATE ON fetch_requests 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Grant permissions
GRANT USAGE ON SCHEMA fetcher TO postgres;
GRANT USAGE ON SCHEMA metrics TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA fetcher TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA metrics TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA fetcher TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA metrics TO postgres;