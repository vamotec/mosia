-- 创建金融时序数据表
CREATE TABLE price_data (
    time TIMESTAMPTZ NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    open DECIMAL(20,8),
    high DECIMAL(20,8),
    low DECIMAL(20,8),
    close DECIMAL(20,8),
    volume BIGINT,
    adjusted_close DECIMAL(20,8)
);

-- 创建超级表
SELECT create_hypertable('price_data', 'time', 'symbol', number_partitions => 4);

-- 创建索引
CREATE INDEX idx_price_symbol_time ON price_data (symbol, time DESC);
CREATE INDEX idx_price_time ON price_data (time DESC);

-- 创建技术指标表
CREATE TABLE technical_indicators (
    time TIMESTAMPTZ NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    indicator_name VARCHAR(50) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    value DECIMAL(20,8),
    signal VARCHAR(10), -- BUY/SELL/HOLD
    confidence DECIMAL(3,2)
);

SELECT create_hypertable('technical_indicators', 'time', 'symbol', number_partitions => 4);