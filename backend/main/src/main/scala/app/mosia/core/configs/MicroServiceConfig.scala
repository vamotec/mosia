package app.mosia.core.configs

import zio.json.JsonCodec

case class MicroServiceConfig (
                                agentsHost: String,
                                agentsPort: Int,
                                fetcherHost: String,
                                fetcherPort: Int,
                                connectionTimeoutSeconds: Int = 30,
                                maxRetries: Int = 3
                              ) derives JsonCodec
