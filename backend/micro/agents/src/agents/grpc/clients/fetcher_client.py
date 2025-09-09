"""gRPC client for Fetcher service."""

from typing import Dict, Any

import grpc

from ...config.logging import get_logger
from ...generated import fetcher_service_pb2 as fetcher_pb2
from ...generated.fetcher_service_pb2_grpc import FetcherServiceStub


class FetcherClient:
    """Client for communicating with Fetcher microservice."""
    
    def __init__(self, fetcher_host: str = "localhost", fetcher_port: int = 50051):
        self.fetcher_host = fetcher_host
        self.fetcher_port = fetcher_port
        self.channel = None
        self.stub = None
        self.logger = get_logger(__name__)
    
    async def __aenter__(self):
        """Async context manager entry."""
        await self.connect()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        await self.disconnect()
    
    async def connect(self):
        """Establish connection to fetcher service."""
        try:
            self.channel = grpc.aio.insecure_channel(f"{self.fetcher_host}:{self.fetcher_port}")
            self.stub = FetcherServiceStub(self.channel)
            self.logger.info(f"Connected to Fetcher service at {self.fetcher_host}:{self.fetcher_port}")
        except Exception as e:
            self.logger.error(f"Failed to connect to Fetcher service: {e}")
            raise
    
    async def disconnect(self):
        """Close connection to fetcher service."""
        if self.channel:
            await self.channel.close()
            self.logger.info("Disconnected from Fetcher service")
    
    async def fetch_financial_data(
        self, 
        data_type: str, 
        symbol: str = None,
        parameters: Dict[str, Any] = None
    ) -> str:
        """
        Fetch financial data through fetcher service.
        
        Args:
            data_type: Type of financial data to fetch
            symbol: Stock symbol (optional)
            parameters: Additional parameters
            
        Returns:
            Formatted string with financial data
        """
        if not self.stub:
            raise RuntimeError("Client not connected. Call connect() first.")
        
        try:
            # Map financial data types to fetcher endpoints
            source_mapping = {
                "yahoo_finance": "https://query1.finance.yahoo.com/v8/finance/chart/",
                "finnhub_news": "https://finnhub.io/api/v1/company-news",
                "reddit_news": "https://www.reddit.com/r/investing/new.json",
                "simfin_balance": "https://simfin.com/api/v2/companies/statements",
                "simfin_cashflow": "https://simfin.com/api/v2/companies/statements",
                "simfin_income": "https://simfin.com/api/v2/companies/statements",
                "finnhub_insider": "https://finnhub.io/api/v1/stock/insider-sentiment",
                "google_news": "https://news.google.com/rss/search"
            }
            
            source_url = source_mapping.get(data_type, "")
            if symbol:
                source_url += symbol
            
            # Prepare request parameters
            request_params = parameters or {}
            if symbol:
                request_params["symbol"] = symbol
            
            # Create fetch request
            request = fetcher_pb2.FetchRequest(
                user_id="financial_agent",
                workspace_id="default",
                source_type="api",
                source_url=source_url,
                parameters=request_params,
                options=fetcher_pb2.FetchOptions(
                    timeout_seconds=30,
                    retry_count=3,
                    cache_enabled=True,
                    cache_ttl="300s",
                    output_format="json"
                )
            )
            
            # Make the request
            response = await self.stub.FetchExternalData(request)
            
            if response.status == "success":
                # Convert bytes data to string
                data_str = response.data.decode('utf-8')
                return data_str
            else:
                self.logger.error(f"Fetch failed: {response.error_message}")
                return f"Error fetching {data_type}: {response.error_message}"
                
        except Exception as e:
            self.logger.error(f"Error in fetch_financial_data: {e}")
            return f"Error: {str(e)}"
    
    async def fetch_reddit_news(self, curr_date: str, lookback_days: int = 7) -> str:
        """Fetch Reddit news data."""
        return await self.fetch_financial_data(
            "reddit_news", 
            parameters={"date": curr_date, "lookback_days": lookback_days}
        )
    
    async def fetch_finnhub_news(self, ticker: str, end_date: str, lookback_days: int) -> str:
        """Fetch Finnhub news data."""
        return await self.fetch_financial_data(
            "finnhub_news",
            symbol=ticker,
            parameters={"to": end_date, "lookback_days": lookback_days}
        )
    
    async def fetch_yahoo_finance(self, symbol: str, start_date: str, end_date: str) -> str:
        """Fetch Yahoo Finance data."""
        return await self.fetch_financial_data(
            "yahoo_finance",
            symbol=symbol,
            parameters={"period1": start_date, "period2": end_date}
        )
    
    async def fetch_simfin_balance_sheet(self, ticker: str, freq: str, curr_date: str) -> str:
        """Fetch SimFin balance sheet data."""
        return await self.fetch_financial_data(
            "simfin_balance",
            symbol=ticker,
            parameters={"statement": "bs", "fyear": curr_date, "period": freq}
        )
    
    async def fetch_simfin_cashflow(self, ticker: str, freq: str, curr_date: str) -> str:
        """Fetch SimFin cashflow data."""
        return await self.fetch_financial_data(
            "simfin_cashflow",
            symbol=ticker,
            parameters={"statement": "cf", "fyear": curr_date, "period": freq}
        )
    
    async def fetch_simfin_income(self, ticker: str, freq: str, curr_date: str) -> str:
        """Fetch SimFin income statement data."""
        return await self.fetch_financial_data(
            "simfin_income",
            symbol=ticker,
            parameters={"statement": "pl", "fyear": curr_date, "period": freq}
        )
    
    async def fetch_finnhub_insider_sentiment(self, ticker: str, curr_date: str, lookback_days: int) -> str:
        """Fetch Finnhub insider sentiment data."""
        return await self.fetch_financial_data(
            "finnhub_insider",
            symbol=ticker,
            parameters={"date": curr_date, "lookback_days": lookback_days}
        )
    
    async def fetch_google_news(self, query: str, curr_date: str, lookback_days: int) -> str:
        """Fetch Google news data."""
        return await self.fetch_financial_data(
            "google_news",
            parameters={"q": query, "date": curr_date, "lookback_days": lookback_days}
        )