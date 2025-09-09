"""Web scraping engine for content extraction."""

import asyncio
from typing import Dict, List, Optional, Any
import aiohttp
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

from fetcher.config.settings import settings
from fetcher.config.logging import get_logger


class WebScraper:
    """Intelligent web scraping engine."""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.session: Optional[aiohttp.ClientSession] = None
        
    async def initialize(self) -> None:
        """Initialize the web scraper."""
        try:
            timeout = aiohttp.ClientTimeout(total=settings.fetching.default_timeout)
            self.session = aiohttp.ClientSession(
                timeout=timeout,
                headers={'User-Agent': settings.fetching.user_agent}
            )
            
            self.logger.info("Web scraper initialized")
            
        except Exception as e:
            self.logger.error("Failed to initialize web scraper", error=str(e))
            raise
    
    async def close(self) -> None:
        """Close the web scraper."""
        if self.session:
            await self.session.close()
            self.logger.info("Web scraper closed")
    
    async def scrape_url(
        self,
        url: str,
        selectors: Optional[Dict[str, str]] = None,
        javascript: bool = False,
        wait_for_element: Optional[str] = None
    ) -> Dict[str, Any]:
        """Scrape content from a URL."""
        start_time = asyncio.get_event_loop().time()
        
        try:
            if javascript:
                result = await self._scrape_with_selenium(url, selectors, wait_for_element)
            else:
                result = await self._scrape_with_aiohttp(url, selectors)
            
            processing_time = asyncio.get_event_loop().time() - start_time
            result["processing_time"] = processing_time
            
            self.logger.info("Web scraping completed",
                           url=url,
                           javascript=javascript,
                           processing_time_ms=int(processing_time * 1000))
            
            return result
            
        except Exception as e:
            self.logger.error("Web scraping failed", url=url, error=str(e))
            return {
                "status": "error",
                "error": str(e),
                "url": url
            }
    
    async def _scrape_with_aiohttp(
        self,
        url: str,
        selectors: Optional[Dict[str, str]] = None
    ) -> Dict[str, Any]:
        """Scrape using aiohttp (faster, no JavaScript support)."""
        if not self.session:
            raise RuntimeError("Web scraper not initialized")
        
        async with self.session.get(url) as response:
            if response.status >= 400:
                raise aiohttp.ClientResponseError(
                    request_info=response.request_info,
                    history=response.history,
                    status=response.status
                )
            
            html_content = await response.text()
            soup = BeautifulSoup(html_content, 'html.parser')
            
            # Extract content based on selectors
            extracted_data = {}
            
            if selectors:
                for name, selector in selectors.items():
                    elements = soup.select(selector)
                    if elements:
                        if len(elements) == 1:
                            extracted_data[name] = elements[0].get_text(strip=True)
                        else:
                            extracted_data[name] = [el.get_text(strip=True) for el in elements]
            else:
                # Default extraction
                extracted_data = await self._extract_default_content(soup)
            
            return {
                "status": "success",
                "url": url,
                "data": extracted_data,
                "metadata": {
                    "title": soup.title.string if soup.title else "",
                    "description": self._get_meta_content(soup, "description"),
                    "keywords": self._get_meta_content(soup, "keywords"),
                    "content_length": len(html_content),
                    "links_count": len(soup.find_all('a')),
                    "images_count": len(soup.find_all('img'))
                }
            }
    
    async def _scrape_with_selenium(
        self,
        url: str,
        selectors: Optional[Dict[str, str]] = None,
        wait_for_element: Optional[str] = None
    ) -> Dict[str, Any]:
        """Scrape using Selenium (supports JavaScript)."""
        
        # Configure Chrome options for headless mode
        chrome_options = Options()
        chrome_options.add_argument("--headless")
        chrome_options.add_argument("--no-sandbox")
        chrome_options.add_argument("--disable-dev-shm-usage")
        chrome_options.add_argument("--disable-gpu")
        chrome_options.add_argument(f"--user-agent={settings.fetching.user_agent}")
        
        driver = None
        try:
            # Run in executor to avoid blocking
            loop = asyncio.get_event_loop()
            driver = await loop.run_in_executor(
                None, 
                lambda: webdriver.Chrome(options=chrome_options)
            )
            
            # Navigate to URL
            await loop.run_in_executor(None, driver.get, url)
            
            # Wait for specific element if specified
            if wait_for_element:
                wait = WebDriverWait(driver, 10)
                await loop.run_in_executor(
                    None,
                    lambda: wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, wait_for_element)))
                )
            
            # Get page source and parse
            page_source = await loop.run_in_executor(None, lambda: driver.page_source)
            soup = BeautifulSoup(page_source, 'html.parser')
            
            # Extract content
            extracted_data = {}
            if selectors:
                for name, selector in selectors.items():
                    elements = soup.select(selector)
                    if elements:
                        if len(elements) == 1:
                            extracted_data[name] = elements[0].get_text(strip=True)
                        else:
                            extracted_data[name] = [el.get_text(strip=True) for el in elements]
            else:
                extracted_data = await self._extract_default_content(soup)
            
            return {
                "status": "success",
                "url": url,
                "data": extracted_data,
                "metadata": {
                    "title": soup.title.string if soup.title else "",
                    "description": self._get_meta_content(soup, "description"),
                    "keywords": self._get_meta_content(soup, "keywords"),
                    "content_length": len(page_source),
                    "javascript_enabled": True
                }
            }
            
        finally:
            if driver:
                await loop.run_in_executor(None, driver.quit)
    
    async def _extract_default_content(self, soup: BeautifulSoup) -> Dict[str, Any]:
        """Extract default content when no selectors are provided."""
        extracted = {}
        
        # Extract main text content
        main_content = soup.find('main') or soup.find('article') or soup.find('div', class_='content')
        if main_content:
            extracted["main_content"] = main_content.get_text(strip=True)
        else:
            # Fallback to body content
            body = soup.find('body')
            if body:
                extracted["main_content"] = body.get_text(strip=True)
        
        # Extract headings
        headings = []
        for i in range(1, 7):
            for heading in soup.find_all(f'h{i}'):
                headings.append({
                    "level": i,
                    "text": heading.get_text(strip=True)
                })
        extracted["headings"] = headings
        
        # Extract links
        links = []
        for link in soup.find_all('a', href=True):
            links.append({
                "text": link.get_text(strip=True),
                "href": link['href']
            })
        extracted["links"] = links[:20]  # Limit to first 20 links
        
        # Extract images
        images = []
        for img in soup.find_all('img', src=True):
            images.append({
                "alt": img.get('alt', ''),
                "src": img['src']
            })
        extracted["images"] = images[:10]  # Limit to first 10 images
        
        return extracted
    
    def _get_meta_content(self, soup: BeautifulSoup, name: str) -> str:
        """Get meta tag content."""
        meta = soup.find('meta', attrs={'name': name}) or soup.find('meta', attrs={'property': f'og:{name}'})
        return meta.get('content', '') if meta else ''
    
    async def scrape_sitemap(self, sitemap_url: str) -> List[str]:
        """Extract URLs from a sitemap."""
        try:
            result = await self.fetch(sitemap_url)
            if result["status"] != "success":
                return []
            
            soup = BeautifulSoup(result["data"], 'xml')
            urls = []
            
            # Extract URLs from sitemap
            for loc in soup.find_all('loc'):
                if loc.string:
                    urls.append(loc.string.strip())
            
            return urls
            
        except Exception as e:
            self.logger.error("Sitemap parsing failed", sitemap_url=sitemap_url, error=str(e))
            return []
    
    async def extract_structured_data(self, url: str) -> Dict[str, Any]:
        """Extract structured data (JSON-LD, microdata) from a page."""
        try:
            result = await self.fetch(url)
            if result["status"] != "success":
                return {}
            
            soup = BeautifulSoup(result["data"], 'html.parser')
            structured_data = {}
            
            # Extract JSON-LD
            json_ld_scripts = soup.find_all('script', type='application/ld+json')
            if json_ld_scripts:
                import json
                json_ld_data = []
                for script in json_ld_scripts:
                    try:
                        data = json.loads(script.string)
                        json_ld_data.append(data)
                    except json.JSONDecodeError:
                        continue
                structured_data["json_ld"] = json_ld_data
            
            # Extract Open Graph data
            og_data = {}
            for meta in soup.find_all('meta', property=lambda x: x and x.startswith('og:')):
                og_data[meta['property']] = meta.get('content', '')
            if og_data:
                structured_data["open_graph"] = og_data
            
            # Extract Twitter Card data
            twitter_data = {}
            for meta in soup.find_all('meta', attrs={'name': lambda x: x and x.startswith('twitter:')}):
                twitter_data[meta['name']] = meta.get('content', '')
            if twitter_data:
                structured_data["twitter_card"] = twitter_data
            
            return structured_data
            
        except Exception as e:
            self.logger.error("Structured data extraction failed", url=url, error=str(e))
            return {}