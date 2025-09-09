"""
Base Data Models
标准化数据模型基类，适配AI分析需求
"""

from typing import Any, Dict, List, Optional, Union
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
import json


class TimeseriesFrequency(Enum):
    """时间序列频率"""
    TICK = "tick"
    MINUTE = "1min"
    MINUTE_5 = "5min"
    MINUTE_15 = "15min"
    MINUTE_30 = "30min"
    HOUR = "1h"
    HOUR_4 = "4h"
    DAILY = "1d"
    WEEKLY = "1w"
    MONTHLY = "1M"
    QUARTERLY = "1Q"
    YEARLY = "1Y"


class CurrencyCode(Enum):
    """货币代码"""
    USD = "USD"
    CNY = "CNY"
    HKD = "HKD"
    EUR = "EUR"
    JPY = "JPY"
    GBP = "GBP"
    SGD = "SGD"
    AUD = "AUD"
    CAD = "CAD"
    CHF = "CHF"


@dataclass
class MultiLanguageText:
    """多语言文本"""
    zh_cn: Optional[str] = None
    zh_tw: Optional[str] = None
    en: Optional[str] = None
    ja: Optional[str] = None
    ko: Optional[str] = None
    
    def get_text(self, language: str = "en") -> Optional[str]:
        """获取指定语言的文本"""
        return getattr(self, language.replace("-", "_"), None)
    
    def to_dict(self) -> Dict[str, Optional[str]]:
        """转换为字典"""
        return {
            "zh_cn": self.zh_cn,
            "zh_tw": self.zh_tw,
            "en": self.en,
            "ja": self.ja,
            "ko": self.ko
        }


@dataclass
class AIMetadata:
    """AI增强元数据"""
    semantic_tags: Dict[str, str] = field(default_factory=dict)
    field_descriptions: Dict[str, str] = field(default_factory=dict)
    analysis_hints: Dict[str, str] = field(default_factory=dict)
    related_symbols: List[str] = field(default_factory=list)
    keywords: List[str] = field(default_factory=list)
    context_summary: Optional[str] = None
    ai_features: Dict[str, float] = field(default_factory=dict)
    
    def add_semantic_tag(self, key: str, value: str) -> None:
        """添加语义标签"""
        self.semantic_tags[key] = value
    
    def add_field_description(self, field: str, description: str) -> None:
        """添加字段描述"""
        self.field_descriptions[field] = description
    
    def add_ai_feature(self, feature: str, value: float) -> None:
        """添加AI特征"""
        self.ai_features[feature] = value

    def add_analysis_hint(self, param, param1):
        pass


@dataclass
class BaseDataModel:
    """基础数据模型"""
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    symbol: Optional[str] = None
    provider_id: Optional[str] = None
    ai_metadata: AIMetadata = field(default_factory=AIMetadata)
    custom_fields: Dict[str, Any] = field(default_factory=dict)
    
    def __post_init__(self):
        """初始化后处理"""
        if isinstance(self.timestamp, str):
            self.timestamp = datetime.fromisoformat(self.timestamp.replace('Z', '+00:00'))
        
        # 确保时区信息
        if self.timestamp.tzinfo is None:
            self.timestamp = self.timestamp.replace(tzinfo=timezone.utc)
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典，便于AI处理"""
        result = {}
        for key, value in self.__dict__.items():
            if isinstance(value, datetime):
                result[key] = value.isoformat()
            elif isinstance(value, Enum):
                result[key] = value.value
            elif hasattr(value, 'to_dict'):
                result[key] = value.to_dict()
            else:
                result[key] = value
        return result
    
    def to_json(self) -> str:
        """转换为JSON字符串"""
        return json.dumps(self.to_dict(), ensure_ascii=False, indent=2)
    
    def add_ai_context(self, context: str) -> None:
        """添加AI上下文"""
        self.ai_metadata.context_summary = context
    
    def get_field_description(self, field: str) -> Optional[str]:
        """获取字段描述"""
        return self.ai_metadata.field_descriptions.get(field)


@dataclass
class TimeseriesDataPoint(BaseDataModel):
    """时间序列数据点基类"""
    open_value: Optional[float] = None
    high_value: Optional[float] = None
    low_value: Optional[float] = None
    close_value: Optional[float] = None
    volume: Optional[float] = None
    frequency: Optional[TimeseriesFrequency] = None
    
    def __post_init__(self):
        super().__post_init__()
        
        # 添加字段描述
        self.ai_metadata.add_field_description("open_value", "Opening price/value for the period")
        self.ai_metadata.add_field_description("high_value", "Highest price/value for the period")
        self.ai_metadata.add_field_description("low_value", "Lowest price/value for the period")
        self.ai_metadata.add_field_description("close_value", "Closing price/value for the period")
        self.ai_metadata.add_field_description("volume", "Trading volume for the period")
        
        # 添加语义标签
        self.ai_metadata.add_semantic_tag("data_type", "timeseries")
        self.ai_metadata.add_semantic_tag("category", "financial")


@dataclass
class PriceData(TimeseriesDataPoint):
    """价格数据模型"""
    currency: CurrencyCode = CurrencyCode.USD
    adjusted_close: Optional[float] = None
    dividend_amount: Optional[float] = None
    split_ratio: Optional[float] = None
    change: Optional[float] = None
    change_percent: Optional[float] = None
    
    def __post_init__(self):
        super().__post_init__()
        
        # 计算涨跌额和涨跌幅
        if self.close_value and self.open_value:
            self.change = self.close_value - self.open_value
            if self.open_value != 0:
                self.change_percent = (self.change / self.open_value) * 100
        
        # 添加价格相关的字段描述
        self.ai_metadata.add_field_description("adjusted_close", "Price adjusted for dividends and splits")
        self.ai_metadata.add_field_description("change", "Absolute price change from open to close")
        self.ai_metadata.add_field_description("change_percent", "Percentage price change from open to close")
        
        # 语义标签
        self.ai_metadata.add_semantic_tag("value_type", "price")
        self.ai_metadata.add_semantic_tag("currency", self.currency.value)


@dataclass
class TechnicalIndicators:
    """技术指标数据"""
    # 趋势指标
    sma_5: Optional[float] = None
    sma_10: Optional[float] = None
    sma_20: Optional[float] = None
    sma_50: Optional[float] = None
    sma_200: Optional[float] = None
    ema_12: Optional[float] = None
    ema_26: Optional[float] = None
    
    # 动量指标
    rsi: Optional[float] = None
    macd: Optional[float] = None
    macd_signal: Optional[float] = None
    macd_histogram: Optional[float] = None
    stoch_k: Optional[float] = None
    stoch_d: Optional[float] = None
    
    # 波动率指标
    bollinger_upper: Optional[float] = None
    bollinger_middle: Optional[float] = None
    bollinger_lower: Optional[float] = None
    atr: Optional[float] = None
    
    # 成交量指标
    volume_sma: Optional[float] = None
    volume_ratio: Optional[float] = None
    
    def to_dict(self) -> Dict[str, Optional[float]]:
        """转换为字典"""
        return {k: v for k, v in self.__dict__.items() if v is not None}
    
    def get_trend_score(self) -> Optional[float]:
        """计算趋势强度分数"""
        if not all([self.sma_20, self.sma_50, self.ema_12, self.ema_26]):
            return None
        
        # 简单的趋势强度计算
        short_trend = 1 if self.ema_12 > self.ema_26 else -1
        medium_trend = 1 if self.sma_20 > self.sma_50 else -1
        
        return (short_trend + medium_trend) / 2


@dataclass
class AIFeatures:
    """AI分析特征"""
    volatility: Optional[float] = None
    beta: Optional[float] = None
    momentum_1d: Optional[float] = None
    momentum_5d: Optional[float] = None
    momentum_20d: Optional[float] = None
    mean_reversion: Optional[float] = None
    trend_strength: Optional[float] = None
    volume_profile: Optional[float] = None
    pattern_signals: List[str] = field(default_factory=list)
    sentiment_score: Optional[float] = None
    prediction_confidence: Optional[float] = None
    anomaly_score: Optional[float] = None
    
    # 自定义特征
    custom_features: Dict[str, float] = field(default_factory=dict)
    
    def add_custom_feature(self, name: str, value: float) -> None:
        """添加自定义特征"""
        self.custom_features[name] = value
    
    def get_feature_vector(self) -> List[float]:
        """获取特征向量，用于机器学习"""
        features = []
        
        # 基础特征
        numeric_fields = [
            'volatility', 'beta', 'momentum_1d', 'momentum_5d', 'momentum_20d',
            'mean_reversion', 'trend_strength', 'volume_profile',
            'sentiment_score', 'prediction_confidence', 'anomaly_score'
        ]
        
        for field in numeric_fields:
            value = getattr(self, field)
            features.append(value if value is not None else 0.0)
        
        # 自定义特征
        features.extend(list(self.custom_features.values()))
        
        return features
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "volatility": self.volatility,
            "beta": self.beta,
            "momentum_1d": self.momentum_1d,
            "momentum_5d": self.momentum_5d,
            "momentum_20d": self.momentum_20d,
            "mean_reversion": self.mean_reversion,
            "trend_strength": self.trend_strength,
            "volume_profile": self.volume_profile,
            "pattern_signals": self.pattern_signals,
            "sentiment_score": self.sentiment_score,
            "prediction_confidence": self.prediction_confidence,
            "anomaly_score": self.anomaly_score,
            "custom_features": self.custom_features
        }


@dataclass
class EnhancedPriceData(PriceData):
    """增强型价格数据，包含技术指标和AI特征"""
    technical_indicators: Optional[TechnicalIndicators] = None
    ai_features: Optional[AIFeatures] = None
    
    def __post_init__(self):
        super().__post_init__()
        
        if self.technical_indicators is None:
            self.technical_indicators = TechnicalIndicators()
        
        if self.ai_features is None:
            self.ai_features = AIFeatures()
        
        # 语义标签
        self.ai_metadata.add_semantic_tag("enriched", "true")
        self.ai_metadata.add_semantic_tag("has_technicals", "true")
        self.ai_metadata.add_semantic_tag("has_ai_features", "true")


@dataclass
class DataValidationResult:
    """数据验证结果"""
    is_valid: bool
    errors: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)
    completeness_score: float = 0.0
    quality_score: float = 0.0
    
    def add_error(self, error: str) -> None:
        """添加错误"""
        self.errors.append(error)
        self.is_valid = False
    
    def add_warning(self, warning: str) -> None:
        """添加警告"""
        self.warnings.append(warning)


class BaseDataValidator:
    """数据验证器基类"""
    
    @staticmethod
    def validate_price_data(data: PriceData) -> DataValidationResult:
        """验证价格数据"""
        result = DataValidationResult(is_valid=True)
        
        # 基本字段检查
        required_fields = ['timestamp', 'symbol', 'close_value']
        missing_fields = []
        
        for field in required_fields:
            if getattr(data, field) is None:
                missing_fields.append(field)
        
        if missing_fields:
            result.add_error(f"Missing required fields: {missing_fields}")
        
        # 价格合理性检查
        if data.close_value is not None and data.close_value <= 0:
            result.add_error("Close price must be positive")
        
        if data.volume is not None and data.volume < 0:
            result.add_error("Volume cannot be negative")
        
        # OHLC一致性检查
        if all([data.open_value, data.high_value, data.low_value, data.close_value]):
            if not (data.low_value <= data.open_value <= data.high_value):
                result.add_warning("Open price outside of high-low range")
            if not (data.low_value <= data.close_value <= data.high_value):
                result.add_warning("Close price outside of high-low range")
        
        # 计算完整性分数
        total_fields = 10  # 主要字段数量
        non_null_fields = sum(1 for field in 
            ['open_value', 'high_value', 'low_value', 'close_value', 'volume',
             'adjusted_close', 'dividend_amount', 'split_ratio', 'change', 'change_percent']
            if getattr(data, field) is not None)
        
        result.completeness_score = non_null_fields / total_fields
        
        # 质量分数（简单计算）
        if result.is_valid:
            result.quality_score = result.completeness_score * (1 - len(result.warnings) * 0.1)
        else:
            result.quality_score = 0.0
        
        return result
    
    @staticmethod
    def validate_timeseries_consistency(data_points: List[PriceData]) -> DataValidationResult:
        """验证时间序列数据一致性"""
        result = DataValidationResult(is_valid=True)
        
        if len(data_points) < 2:
            return result
        
        # 检查时间序列是否排序
        timestamps = [dp.timestamp for dp in data_points]
        if timestamps != sorted(timestamps):
            result.add_warning("Time series data is not chronologically ordered")
        
        # 检查是否有重复时间戳
        if len(timestamps) != len(set(timestamps)):
            result.add_warning("Duplicate timestamps found in time series")
        
        # 检查异常的价格跳跃
        for i in range(1, len(data_points)):
            prev_close = data_points[i-1].close_value
            curr_open = data_points[i].open_value
            
            if prev_close and curr_open:
                gap_percent = abs((curr_open - prev_close) / prev_close) * 100
                if gap_percent > 20:  # 20%的价格跳跃
                    result.add_warning(f"Large price gap detected at {data_points[i].timestamp}")
        
        return result