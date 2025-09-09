package app.mosia.infra.features

import app.mosia.core.types.Constants.*
import zio.json.*
import zio.json.ast.Json
import zio.schema.{ DeriveSchema, Schema }
import zio.{ Task, ZIO }

import scala.reflect.ClassTag

object UserFeatures:
  enum FeaturesName(val value: String):
    case Administrator      extends FeaturesName("administrator")
    case EarlyAccess        extends FeaturesName("early_access")
    case AIEarlyAccess      extends FeaturesName("ai_early_access")
    case UnlimitedCopilot   extends FeaturesName("unlimited_copilot")
    case FreePlanV1         extends FeaturesName("free_plan_v1")
    case ProPlanV1          extends FeaturesName("pro_plan_v1")
    case LifetimeProPlanV1  extends FeaturesName("lifetime_pro_plan_v1")
    case UnlimitedWorkspace extends FeaturesName("unlimited_workspace")
    case TeamPlanV1         extends FeaturesName("team_plan_v1")

  object FeaturesName:
    val all: Set[FeaturesName]                        = FeaturesName.values.toSet
    def fromString(str: String): Option[FeaturesName] =
      FeaturesName.values.find(_.toString.equalsIgnoreCase(str))

  given caliban.schema.Schema[Any, FeaturesName] = caliban.schema.Schema.Auto.derived
  given caliban.schema.ArgBuilder[FeaturesName]  = caliban.schema.ArgBuilder.Auto.derived
  given JsonEncoder[FeaturesName]                = DeriveJsonEncoder.gen[FeaturesName]

  trait FeatureConfigProvider[C]:
    def schema: Schema[C]
    def decoder: JsonDecoder[C]
    def default: C
    def featuresType: FeaturesType
    def deprecatedVersion: Int

  object FeatureConfigProvider:
    def apply[C](using fcp: FeatureConfigProvider[C]): FeatureConfigProvider[C] = fcp

    given FeatureConfigProvider[EarlyAccessList] with
      def schema: Schema[EarlyAccessList]       = Schema[EarlyAccessList]
      def decoder: JsonDecoder[EarlyAccessList] = summon[JsonDecoder[EarlyAccessList]]
      def default: EarlyAccessList              = EarlyAccessList(Nil)
      def featuresType: FeaturesType            = FeaturesType.Feature
      override def deprecatedVersion: Int       = 2

    given FeatureConfigProvider[EmptyConfig] with
      def schema: Schema[EmptyConfig]       = Schema[EmptyConfig]
      def decoder: JsonDecoder[EmptyConfig] = summon[JsonDecoder[EmptyConfig]]
      def default: EmptyConfig              = EmptyConfig()
      def featuresType: FeaturesType        = FeaturesType.Feature
      override def deprecatedVersion: Int   = 1

    given FeatureConfigProvider[FreePlanV1Config] with
      def schema: Schema[FreePlanV1Config]       = Schema[FreePlanV1Config]
      def decoder: JsonDecoder[FreePlanV1Config] = summon[JsonDecoder[FreePlanV1Config]]
      def default: FreePlanV1Config              = FreePlanV1Config(
        UserPlanQuotaConfig(
          name = "Free",
          blobLimit = 10 * OneMB,
          businessBlobLimit = Some(100 * OneMB),
          storageQuota = 10 * OneGB,
          historyPeriod = 7 * OneDay,
          memberLimit = 3,
          copilotActionLimit = Some(10)
        )
      )
      def featuresType: FeaturesType             = FeaturesType.Quota
      override def deprecatedVersion: Int        = 4

    given FeatureConfigProvider[ProPlanV1Config] with
      def schema: Schema[ProPlanV1Config]       = Schema[ProPlanV1Config]
      def decoder: JsonDecoder[ProPlanV1Config] = JsonDecoder[ProPlanV1Config]
      def default: ProPlanV1Config              = ProPlanV1Config(
        UserPlanQuotaConfig(
          name = "Pro",
          blobLimit = 100 * OneMB,
          storageQuota = 100 * OneGB,
          historyPeriod = 30 * OneDay,
          memberLimit = 10,
          copilotActionLimit = Some(10)
        )
      )
      def featuresType: FeaturesType            = FeaturesType.Quota
      override def deprecatedVersion: Int       = 2

    given FeatureConfigProvider[LifetimeProPlanV1Config] with
      def schema: Schema[LifetimeProPlanV1Config]       = Schema[LifetimeProPlanV1Config]
      def decoder: JsonDecoder[LifetimeProPlanV1Config] = JsonDecoder[LifetimeProPlanV1Config]
      def default: LifetimeProPlanV1Config              = LifetimeProPlanV1Config(
        UserPlanQuotaConfig(
          name = "Lifetime Pro",
          blobLimit = 100 * OneMB,
          storageQuota = 1024 * OneGB,
          historyPeriod = 30 * OneDay,
          memberLimit = 10,
          copilotActionLimit = Some(10)
        )
      )
      def featuresType: FeaturesType                    = FeaturesType.Quota
      override def deprecatedVersion: Int               = 1

    given FeatureConfigProvider[WorkspaceQuotaConfig] with
      def schema: Schema[WorkspaceQuotaConfig]       = Schema[WorkspaceQuotaConfig]
      def decoder: JsonDecoder[WorkspaceQuotaConfig] = summon[JsonDecoder[WorkspaceQuotaConfig]]
      def default: WorkspaceQuotaConfig              = WorkspaceQuotaConfig(
        name = "Team Workspace",
        blobLimit = 500 * OneMB,
        storageQuota = 100 * OneGB,
        seatQuota = 20 * OneGB,
        historyPeriod = 30 * OneDay,
        memberLimit = 1
      )
      def featuresType: FeaturesType                 = FeaturesType.Feature
      override def deprecatedVersion: Int            = 1

  final case class FreePlanV1Config(inner: UserPlanQuotaConfig)
  final case class ProPlanV1Config(inner: UserPlanQuotaConfig)
  final case class LifetimeProPlanV1Config(inner: UserPlanQuotaConfig)

  given Schema[FreePlanV1Config]        = DeriveSchema.gen[FreePlanV1Config]
  given Schema[ProPlanV1Config]         = DeriveSchema.gen[ProPlanV1Config]
  given Schema[LifetimeProPlanV1Config] = DeriveSchema.gen[LifetimeProPlanV1Config]

  case class EmptyConfig()
  given Schema[EmptyConfig]      = DeriveSchema.gen[EmptyConfig]
  given JsonEncoder[EmptyConfig] = DeriveJsonEncoder.gen[EmptyConfig]
  given JsonDecoder[EmptyConfig] = DeriveJsonDecoder.gen[EmptyConfig]

  case class UserPlanQuotaConfig(
    name: String,
    blobLimit: Double,
    businessBlobLimit: Option[Double] = None,
    storageQuota: Double,
    historyPeriod: Double,
    memberLimit: Double,
    copilotActionLimit: Option[Double] = None
  )
  given Schema[UserPlanQuotaConfig]      = DeriveSchema.gen[UserPlanQuotaConfig]
  given JsonEncoder[UserPlanQuotaConfig] = DeriveJsonEncoder.gen[UserPlanQuotaConfig]
  given JsonDecoder[UserPlanQuotaConfig] = DeriveJsonDecoder.gen[UserPlanQuotaConfig]

  case class WorkspaceQuotaConfig(
    name: String,
    blobLimit: Double,
    businessBlobLimit: Option[Double] = None,
    storageQuota: Double,
    historyPeriod: Double,
    memberLimit: Double,
    seatQuota: Double
  )
  given Schema[WorkspaceQuotaConfig] = DeriveSchema.gen[WorkspaceQuotaConfig]

  case class EarlyAccessList(whitelist: List[String])
  given Schema[EarlyAccessList]      = DeriveSchema.gen[EarlyAccessList]
  given JsonEncoder[EarlyAccessList] = DeriveJsonEncoder.gen[EarlyAccessList]
  given JsonDecoder[EarlyAccessList] = DeriveJsonDecoder.gen[EarlyAccessList]

  given JsonCodec[EarlyAccessList]         = DeriveJsonCodec.gen[EarlyAccessList]
  given JsonCodec[FreePlanV1Config]        = DeriveJsonCodec.gen[FreePlanV1Config]
  given JsonCodec[ProPlanV1Config]         = DeriveJsonCodec.gen[ProPlanV1Config]
  given JsonCodec[LifetimeProPlanV1Config] = DeriveJsonCodec.gen[LifetimeProPlanV1Config]
  given JsonCodec[WorkspaceQuotaConfig]    = DeriveJsonCodec.gen[WorkspaceQuotaConfig]
  given JsonCodec[EmptyConfig]             = DeriveJsonCodec.gen[EmptyConfig]
