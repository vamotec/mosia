package app.mosia.infra.repository.registry

import app.mosia.core.types.Constants.*
import app.mosia.infra.features.UserFeatures.*
import app.mosia.infra.features.UserFeatures.FeaturesName.*
import zio.json.*
import zio.json.ast.Json
import zio.schema.codec.JsonCodec
import zio.schema.{ DeriveSchema, Schema }

import scala.reflect.ClassTag

object FeaturesRegistry:
  type ConfigOf[F <: FeaturesName] = F match
    case EarlyAccess.type        => EarlyAccessList
    case FreePlanV1.type         => FreePlanV1Config
    case ProPlanV1.type          => ProPlanV1Config
    case LifetimeProPlanV1.type  => LifetimeProPlanV1Config
    case TeamPlanV1.type         => WorkspaceQuotaConfig
    case Administrator.type      => EmptyConfig
    case AIEarlyAccess.type      => EmptyConfig
    case UnlimitedCopilot.type   => EmptyConfig
    case UnlimitedWorkspace.type => EmptyConfig

  // 获取 schema、decoder、default 的静态方法
  def getSchema[T <: FeaturesName](using p: FeatureConfigProvider[ConfigOf[T]]): Schema[ConfigOf[T]] =
    p.schema

  def getDecoder[T <: FeaturesName](using p: FeatureConfigProvider[ConfigOf[T]]): JsonDecoder[ConfigOf[T]] =
    p.decoder

  def getDefaultConfig[T <: FeaturesName](using p: FeatureConfigProvider[ConfigOf[T]]): ConfigOf[T] =
    p.default
