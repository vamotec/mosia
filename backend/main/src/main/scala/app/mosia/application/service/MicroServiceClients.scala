package app.mosia.application.service

import app.mosia.core.configs.{AppConfig, MicroServiceConfig}
import app.mosia.grpc.agents_service.ZioAgentsService.AgentsServiceClient
import app.mosia.grpc.fetcher_service.ZioFetcherService.FetcherServiceClient
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import zio.*
import zio.config.*

trait MicroServiceClients:
  def agentsClient: AgentsServiceClient
  def fetcherClient: FetcherServiceClient

case class MicroServiceClientsImpl(
    agentsClient: AgentsServiceClient,
    fetcherClient: FetcherServiceClient
) extends MicroServiceClients

object MicroServiceClientsImpl:
  
  def createChannel(host: String, port: Int): ZIO[Any, Throwable, ManagedChannel] =
    ZIO.attempt {
      ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .build()
    }
  
  def createAgentsClient(config: MicroServiceConfig): ZIO[Any, Throwable, AgentsServiceClient] =
    for
      channel <- createChannel(config.agentsHost, config.agentsPort)
      client <- ZIO.succeed(AgentsServiceClient(channel))
    yield client
  
  def createFetcherClient(config: MicroServiceConfig): ZIO[Any, Throwable, FetcherServiceClient] =
    for
      channel <- createChannel(config.fetcherHost, config.fetcherPort)
      client <- ZIO.succeed(FetcherServiceClient(channel))
    yield client
  
  def layer: ZLayer[Any, Throwable, MicroServiceConfig] =
    ZLayer.fromZIO {
      for
        config <- ZIO.succeed(MicroServiceConfig(
          agentsHost = "agents-service", // Docker service name
          agentsPort = 50051,
          fetcherHost = "fetcher-service", // Docker service name
          fetcherPort = 50052
        ))
        agentsClient <- createAgentsClient(config)
        fetcherClient <- createFetcherClient(config)
      yield MicroserviceClientsImpl(agentsClient, fetcherClient)
    }