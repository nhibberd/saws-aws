import sbt._
import Keys._
import com.ambiata.promulgate.project.ProjectPlugin._
import com.ambiata.promulgate.version.VersionPlugin._
import sbtassembly.Plugin._
import com.typesafe.sbt.SbtProguard._

object build extends Build {
  type Settings = Def.Setting[_]

  lazy val aws = Project(
    id = "saws-aws",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++
      projectSettings ++
      promulgate.library("com.ambiata.aws", "ambiata-oss") ++
      Seq(resolvers ++= resolversx) ++
      proguardSettings ++
      Seq[Settings](libraryDependencies ++= awsDependency) ++
      inConfig(ProguardPre)(ProguardSettings.default ++ dependenciesPre ++ Seq(managedClasspath <<= (managedClasspath, managedClasspath in Compile).map({ case (y, x) => y ++ x}))) ++
      dependenciesPre ++
      Seq[Settings](name := "saws-aws"
        , ProguardKeys.options in ProguardPre <<= (update, packageBin in Compile).map({ case (u, _) => Mappings.preshim(u) })
        , ProguardKeys.options in Proguard <<= (ProguardKeys.proguard in ProguardPre, name, version, update, packageBin in Compile).map({
            case(_, n, v, u, b) => Mappings.shim(n, v, u, b)
        })
        , javaOptions in (Proguard, ProguardKeys.proguard) := Seq("-Xmx2G")) ++
      addArtifact(name.apply(n => Artifact(s"$n", "jar", "jar")), (ProguardKeys.proguard in Proguard, packageBin in Compile, name, version).map({ case (_, s, n, v) => s.getParentFile / "proguard" / s"$n-proguard-$v.jar"}))
  )

  val ProguardPre = config("proguard-pre")

  def dependenciesPre: Seq[Setting[_]] = Seq(
    ivyConfigurations += ProguardPre,
    libraryDependencies <+= (ProguardKeys.proguardVersion in ProguardPre) { version =>
      "net.sf.proguard" % "proguard-base" % version % ProguardPre.name
    }
  )

  lazy val projectSettings: Seq[Settings] = Seq(
      name := "aws"
    , version in ThisBuild := "1.2.1"
    , organization := "com.ambiata"
    , scalaVersion := "2.11.2"
  )

  val awsDependency = Seq(
      "com.amazonaws"       %  "aws-java-sdk" % "1.9.0" exclude("joda-time", "joda-time") // This is declared with a wildcard
    , "com.owtelse.codec"   %  "base64"       % "1.0.6"
    , "javax.mail"          %  "mail"         % "1.4.7")

  val resolversx = Seq(
      Resolver.sonatypeRepo("releases")
    , Resolver.typesafeRepo("releases")
    , "cloudera"              at "https://repository.cloudera.com/content/repositories/releases"
    , Resolver.url("ambiata-oss", new URL("https://ambiata-oss.s3.amazonaws.com"))(Resolver.ivyStylePatterns)
    , "Scalaz Bintray Repo"   at "http://dl.bintray.com/scalaz/releases"
    // For 2.11 version of scala-ssh only
    , "spray.io"              at "http://repo.spray.io")

}