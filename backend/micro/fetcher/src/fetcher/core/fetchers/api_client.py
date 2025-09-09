"""API client for external data fetching."""

import asyncio
from typing import Dict, List, Optional, Any
import aiohttp
from tenacity import retry, stop_after_attempt, wait_exponential

from fetcher.config.settings import settings
from fetcher.config.logging import get_logger


class APIClient:
    """Generic API client with rate limiting and retry logic."""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.session: Optional[aiohttp.ClientSession] = None
        self.rate_limiter = {}
        
    async def __aenter__(self):
        """Async context manager entry."""
        await self.initialize()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        await self.close()
    
    async def initialize(self) -> None:
        """Initialize the API client."""
        try:
            timeout = aiohttp.ClientTimeout(total=settings.fetching.default_timeout)
            connector = aiohttp.TCPConnector(limit=settings.fetching.max_concurrent_fetches)
            
            self.session = aiohttp.ClientSession(
                timeout=timeout,
                connector=connector,
                headers={
                    'User-Agent': settings.fetching.user_agent
                }
            )
            
            self.logger.info("API client initialized")
            
        except Exception as e:
            self.logger.error("Failed to initialize API client", error=str(e))
            raise
    
    async def close(self) -> None:
        """Close the API client."""
        if self.session:
            await self.session.close()
            self.logger.info("API client closed")
    
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=4, max=10)
    )
    async def fetch(
        self,
        url: str,
        method: str = "GET",
        headers: Optional[Dict[str, str]] = None,
        params: Optional[Dict[str, Any]] = None,
        data: Optional[Any] = None,
        json_data: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Fetch data from external API with retry logic."""
        if not self.session:
            raise RuntimeError("API client not initialized")
        
        start_time = asyncio.get_event_loop().time()
        
        try:
            # Apply rate limiting
            await self._apply_rate_limiting(url)
            
            # Prepare request
            request_headers = headers or {}
            
            async with self.session.request(
                method=method,
                url=url,
                headers=request_headers,
                params=params,
                data=data,
                json=json_data
            ) as response:
                
                # Check response status
                if response.status >= 400:
                    error_text = await response.text()
                    raise aiohttp.ClientResponseError(
                        request_info=response.request_info,
                        history=tuple(response.history),
                        status=response.status,
                        message=error_text
                    )
                
                # Get response data
                content_type = response.headers.get('Content-Type', '')
                
                if 'application/json' in content_type:
                    data = await response.json()
                elif 'text/' in content_type:
                    data = await response.text()
                else:
                    data = await response.read()
                
                processing_time = asyncio.get_event_loop().time() - start_time
                
                self.logger.info("API request completed",
                               url=url,
                               method=method,
                               status=response.status,
                               processing_time_ms=int(processing_time * 1000))
                
                return {
                    "status": "success",
                    "status_code": response.status,
                    "data": data,
                    "headers": dict(response.headers),
                    "content_type": content_type,
                    "processing_time": processing_time,
                    "size_bytes": len(str(data)) if isinstance(data, str) else len(data) if isinstance(data, bytes) else 0
                }
        
        except Exception as e:
            processing_time = asyncio.get_event_loop().time() - start_time
            self.logger.error("API request failed",
                            url=url,
                            method=method,
                            error=str(e),
                            processing_time_ms=int(processing_time * 1000))
            
            return {
                "status": "error",
                "error": str(e),
                "processing_time": processing_time
            }
    
    async def fetch_multiple(
        self,
        requests: List[Dict[str, Any]],
        max_concurrent: int = None
    ) -> List[Dict[str, Any]]:
        """Fetch multiple URLs concurrently."""
        max_concurrent = max_concurrent or settings.fetching.max_concurrent_fetches
        
        semaphore = asyncio.Semaphore(max_concurrent)
        
        async def fetch_with_semaphore(request_data):
            async with semaphore:
                return await self.fetch(**request_data)
        
        tasks = [fetch_with_semaphore(req) for req in requests]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        # Convert exceptions to error responses
        processed_results = []
        for result in results:
            if isinstance(result, Exception):
                processed_results.append({
                    "status": "error",
                    "error": str(result),
                    "processing_time": 0
                })
            else:
                processed_results.append(result)
        
        return processed_results
    
    async def _apply_rate_limiting(self, url: str) -> None:
        """Apply rate limiting per domain."""
        # Simple rate limiting implementation
        # In production, you'd use a more sophisticated rate limiter
        domain = url.split('://')[1].split('/')[0] if '://' in url else url.split('/')[0]
        
        current_time = asyncio.get_event_loop().time()
        
        if domain not in self.rate_limiter:
            self.rate_limiter[domain] = []
        
        # Remove old requests
        self.rate_limiter[domain] = [
            timestamp for timestamp in self.rate_limiter[domain]
            if current_time - timestamp < 60  # 1 minute window
        ]
        
        # Check rate limit
        if len(self.rate_limiter[domain]) >= settings.rate_limit.requests_per_minute:
            sleep_time = 60 - (current_time - self.rate_limiter[domain][0])
            if sleep_time > 0:
                await asyncio.sleep(sleep_time)
        
        # Add current request
        self.rate_limiter[domain].append(current_time)