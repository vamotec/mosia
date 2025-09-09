from datetime import datetime
from typing import Annotated

from langchain_core.messages import HumanMessage
from langchain_core.messages import RemoveMessage
from langchain_core.tools import tool

from ....grpc.clients.fetcher_client import FetcherClient
from ....config.settings import settings

def create_msg_delete():
    def delete_messages(state):
        """Clear messages and add placeholder for Anthropic compatibility"""
        messages = state["messages"]
        
        # Remove all messages
        removal_operations = [RemoveMessage(id=m.id) for m in messages]
        
        # Add a minimal placeholder message
        placeholder = HumanMessage(content="Continue")
        
        return {"messages": removal_operations + [placeholder]}
    
    return delete_messages


class Toolkit:
    """Toolkit for financial agents with gRPC-based data fetching."""
    
    _config = settings.copy()
    _shared_fetcher_client = None

    @classmethod
    def update_config(cls, config):
        """Update the class-level configuration."""
        cls._config.update(config)

    @property
    def config(self):
        """Access the configuration."""
        return self._config

    def __init__(self, config=None):
        if config:
            self.update_config(config)

    @classmethod
    async def get_fetcher_client(cls) -> FetcherClient:
        """Get or create shared fetcher client."""
        if cls._shared_fetcher_client is None:
            cls._shared_fetcher_client = FetcherClient(
                fetcher_host=cls._config["fetcher_host"],
                fetcher_port=cls._config["fetcher_grpc_port"]
            )
            await cls._shared_fetcher_client.connect()
        return cls._shared_fetcher_client

    @staticmethod
    @tool
    async def get_reddit_news(
        curr_date: Annotated[str, "Date you want to get news for in yyyy-mm-dd format"],
    ) -> str:
        """
        Retrieve global news from Reddit within a specified time frame.
        Args:
            curr_date (str): Date you want to get news for in yyyy-mm-dd format
        Returns:
            str: A formatted dataframe containing the latest global news from Reddit in the specified time frame.
        """
        fetcher = await Toolkit.get_fetcher_client()
        return await fetcher.fetch_reddit_news(curr_date, 7)

    @tool
    async def get_finnhub_news(
        self,
        ticker: Annotated[
            str,
            "Search query of a company, e.g. 'AAPL, TSM, etc.",
        ],
        start_date: Annotated[str, "Start date in yyyy-mm-dd format"],
        end_date: Annotated[str, "End date in yyyy-mm-dd format"],
    ):
        """
        Retrieve the latest news about a given stock from Finnhub within a date range
        Args:
            ticker (str): Ticker of a company. e.g. AAPL, TSM
            start_date (str): Start date in yyyy-mm-dd format
            end_date (str): End date in yyyy-mm-dd format
        Returns:
            str: A formatted dataframe containing news about the company within the date range from start_date to end_date
        """
        end_date_dt = datetime.strptime(end_date, "%Y-%m-%d")
        start_date_dt = datetime.strptime(start_date, "%Y-%m-%d")
        look_back_days = (end_date_dt - start_date_dt).days

        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_finnhub_news(ticker, end_date, look_back_days)

    @tool
    async def get_reddit_stock_info(
        self,
        curr_date: Annotated[str, "Current date you want to get news for"],
    ) -> str:
        """
        Retrieve the latest news about a given stock from Reddit, given the current date.
        Args:
            curr_date (str): current date in yyyy-mm-dd format to get news for
        Returns:
            str: A formatted dataframe containing the latest news about the company on the given date
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_reddit_news(curr_date, 7)

    @tool
    async def get_YFin_data(
        self,
        symbol: Annotated[str, "ticker symbol of the company"],
        start_date: Annotated[str, "Start date in yyyy-mm-dd format"],
        end_date: Annotated[str, "End date in yyyy-mm-dd format"],
    ) -> str:
        """
        Retrieve the stock price data for a given ticker symbol from Yahoo Finance.
        Args:
            symbol (str): Ticker symbol of the company, e.g. AAPL, TSM
            start_date (str): Start date in yyyy-mm-dd format
            end_date (str): End date in yyyy-mm-dd format
        Returns:
            str: A formatted dataframe containing the stock price data for the specified ticker symbol in the specified date range.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_yahoo_finance(symbol, start_date, end_date)

    @tool
    async def get_YFin_data_online(
        self,
        symbol: Annotated[str, "ticker symbol of the company"],
        start_date: Annotated[str, "Start date in yyyy-mm-dd format"],
        end_date: Annotated[str, "End date in yyyy-mm-dd format"],
    ) -> str:
        """
        Retrieve the stock price data for a given ticker symbol from Yahoo Finance.
        Args:
            symbol (str): Ticker symbol of the company, e.g. AAPL, TSM
            start_date (str): Start date in yyyy-mm-dd format
            end_date (str): End date in yyyy-mm-dd format
        Returns:
            str: A formatted dataframe containing the stock price data for the specified ticker symbol in the specified date range.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_yahoo_finance(symbol, start_date, end_date)

    @tool
    async def get_stockstats_indicators_report(
        self,
        symbol: Annotated[str, "ticker symbol of the company"],
        indicator: Annotated[
            str, "technical indicator to get the analysis and report of"
        ],
        curr_date: Annotated[
            str, "The current trading date you are trading on, YYYY-mm-dd"
        ],
        look_back_days: Annotated[int, "how many days to look back"] = 30,
    ) -> str:
        """
        Retrieve stock stats indicators for a given ticker symbol and indicator.
        Args:
            symbol (str): Ticker symbol of the company, e.g. AAPL, TSM
            indicator (str): Technical indicator to get the analysis and report of
            curr_date (str): The current trading date you are trading on, YYYY-mm-dd
            look_back_days (int): How many days to look back, default is 30
        Returns:
            str: A formatted dataframe containing the stock stats indicators for the specified ticker symbol and indicator.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_financial_data(
            "technical_indicators",
            symbol=symbol,
            parameters={
                "indicator": indicator,
                "date": curr_date,
                "lookback_days": look_back_days,
                "online": False
            }
        )

    @tool
    async def get_stockstats_indicators_report_online(
        self,
        symbol: Annotated[str, "ticker symbol of the company"],
        indicator: Annotated[
            str, "technical indicator to get the analysis and report of"
        ],
        curr_date: Annotated[
            str, "The current trading date you are trading on, YYYY-mm-dd"
        ],
        look_back_days: Annotated[int, "how many days to look back"] = 30,
    ) -> str:
        """
        Retrieve stock stats indicators for a given ticker symbol and indicator.
        Args:
            symbol (str): Ticker symbol of the company, e.g. AAPL, TSM
            indicator (str): Technical indicator to get the analysis and report of
            curr_date (str): The current trading date you are trading on, YYYY-mm-dd
            look_back_days (int): How many days to look back, default is 30
        Returns:
            str: A formatted dataframe containing the stock stats indicators for the specified ticker symbol and indicator.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_financial_data(
            "technical_indicators",
            symbol=symbol,
            parameters={
                "indicator": indicator,
                "date": curr_date,
                "lookback_days": look_back_days,
                "online": True
            }
        )

    @tool
    async def get_finnhub_company_insider_sentiment(
        self,
        ticker: Annotated[str, "ticker symbol for the company"],
        curr_date: Annotated[
            str,
            "current date of you are trading at, yyyy-mm-dd",
        ],
    ):
        """
        Retrieve insider sentiment information about a company (retrieved from public SEC information) for the past 30 days
        Args:
            ticker (str): ticker symbol of the company
            curr_date (str): current date you are trading at, yyyy-mm-dd
        Returns:
            str: a report of the sentiment in the past 30 days starting at curr_date
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_finnhub_insider_sentiment(ticker, curr_date, 30)

    @tool
    async def get_finnhub_company_insider_transactions(
        self,
        ticker: Annotated[str, "ticker symbol"],
        curr_date: Annotated[
            str,
            "current date you are trading at, yyyy-mm-dd",
        ],
    ):
        """
        Retrieve insider transaction information about a company (retrieved from public SEC information) for the past 30 days
        Args:
            ticker (str): ticker symbol of the company
            curr_date (str): current date you are trading at, yyyy-mm-dd
        Returns:
            str: a report of the company's insider transactions/trading information in the past 30 days
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_financial_data(
            "finnhub_insider_transactions",
            symbol=ticker,
            parameters={"date": curr_date, "lookback_days": 30}
        )

    @tool
    async def get_simfin_balance_sheet(
        self,
        ticker: Annotated[str, "ticker symbol"],
        freq: Annotated[
            str,
            "reporting frequency of the company's financial history: annual/quarterly",
        ],
        curr_date: Annotated[str, "current date you are trading at, yyyy-mm-dd"],
    ):
        """
        Retrieve the most recent balance sheet of a company
        Args:
            ticker (str): ticker symbol of the company
            freq (str): reporting frequency of the company's financial history: annual / quarterly
            curr_date (str): current date you are trading at, yyyy-mm-dd
        Returns:
            str: a report of the company's most recent balance sheet
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_simfin_balance_sheet(ticker, freq, curr_date)

    @tool
    async def get_simfin_cashflow(
        self,
        ticker: Annotated[str, "ticker symbol"],
        freq: Annotated[
            str,
            "reporting frequency of the company's financial history: annual/quarterly",
        ],
        curr_date: Annotated[str, "current date you are trading at, yyyy-mm-dd"],
    ):
        """
        Retrieve the most recent cash flow statement of a company
        Args:
            ticker (str): ticker symbol of the company
            freq (str): reporting frequency of the company's financial history: annual / quarterly
            curr_date (str): current date you are trading at, yyyy-mm-dd
        Returns:
                str: a report of the company's most recent cash flow statement
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_simfin_cashflow(ticker, freq, curr_date)

    @tool
    async def get_simfin_income_stmt(
        self,
        ticker: Annotated[str, "ticker symbol"],
        freq: Annotated[
            str,
            "reporting frequency of the company's financial history: annual/quarterly",
        ],
        curr_date: Annotated[str, "current date you are trading at, yyyy-mm-dd"],
    ):
        """
        Retrieve the most recent income statement of a company
        Args:
            ticker (str): ticker symbol of the company
            freq (str): reporting frequency of the company's financial history: annual / quarterly
            curr_date (str): current date you are trading at, yyyy-mm-dd
        Returns:
                str: a report of the company's most recent income statement
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_simfin_income(ticker, freq, curr_date)

    @tool
    async def get_google_news(
        self,
        query: Annotated[str, "Query to search with"],
        curr_date: Annotated[str, "Curr date in yyyy-mm-dd format"],
    ):
        """
        Retrieve the latest news from Google News based on a query and date range.
        Args:
            query (str): Query to search with
            curr_date (str): Current date in yyyy-mm-dd format
        Returns:
            str: A formatted string containing the latest news from Google News based on the query and date range.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_google_news(query, curr_date, 7)

    @tool
    async def get_stock_news_openai(
        self,
        ticker: Annotated[str, "the company's ticker"],
        curr_date: Annotated[str, "Current date in yyyy-mm-dd format"],
    ):
        """
        Retrieve the latest news about a given stock by using OpenAI's news API.
        Args:
            ticker (str): Ticker of a company. e.g. AAPL, TSM
            curr_date (str): Current date in yyyy-mm-dd format
        Returns:
            str: A formatted string containing the latest news about the company on the given date.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_financial_data(
            "openai_news",
            symbol=ticker,
            parameters={"date": curr_date}
        )

    @tool
    async def get_global_news_openai(
        self,
        curr_date: Annotated[str, "Current date in yyyy-mm-dd format"],
    ):
        """
        Retrieve the latest macroeconomics news on a given date using OpenAI's macroeconomics news API.
        Args:
            curr_date (str): Current date in yyyy-mm-dd format
        Returns:
            str: A formatted string containing the latest macroeconomic news on the given date.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_financial_data(
            "openai_global_news",
            parameters={"date": curr_date}
        )

    @tool
    async def get_fundamentals_openai(
        self,
        ticker: Annotated[str, "the company's ticker"],
        curr_date: Annotated[str, "Current date in yyyy-mm-dd format"],
    ):
        """
        Retrieve the latest fundamental information about a given stock on a given date by using OpenAI's news API.
        Args:
            ticker (str): Ticker of a company. e.g. AAPL, TSM
            curr_date (str): Current date in yyyy-mm-dd format
        Returns:
            str: A formatted string containing the latest fundamental information about the company on the given date.
        """
        toolkit = Toolkit()
        fetcher = await toolkit.get_fetcher_client()
        return await fetcher.fetch_financial_data(
            "openai_fundamentals",
            symbol=ticker,
            parameters={"date": curr_date}
        )