package fi.oph.tor.schema

import org.json4s.JsonAST._
import org.reflections.Reflections

import scala.annotation.{ClassfileAnnotation, StaticAnnotation}
import scala.reflect.api.JavaUniverse
import scala.reflect.runtime.{universe => ru}

sealed trait SchemaType {
  def description: Option[String] = None
}

case class OptionalType(x: SchemaType) extends SchemaType
case class ListType(x: SchemaType) extends SchemaType
case class DateType() extends SchemaType
case class StringType() extends SchemaType
case class BooleanType() extends SchemaType
case class NumberType() extends SchemaType
case class ClassType(fullClassName: String, properties: List[Property], override val description: Option[String]) extends SchemaType
case class ClassTypeRef(fullClassName: String) extends SchemaType
case class OneOf(types: List[SchemaType]) extends SchemaType
case class Property(key: String, tyep: SchemaType, description: Option[String])

case class Description(text: String) extends StaticAnnotation

object ScalaJsonSchema {
  private lazy val schemaTypeForScala = Map(
    "org.joda.time.DateTime" -> DateType(),
    "java.util.Date" -> DateType(),
    "java.time.LocalDate" -> DateType(),
    "java.lang.String" -> StringType(),
    "scala.Boolean" -> BooleanType(),
    "scala.Int" -> NumberType(),
    "scala.Long" -> NumberType(),
    "scala.Double" -> NumberType()
  )

  def descriptionForSymbol(symbol: ru.Symbol): Option[String] = {
    symbol.annotations.find { annotation =>
      annotation.tree.tpe.toString == classOf[Description].getName
    }.map { annotation =>
      annotation.tree.children.tail.mkString(" ").replaceAll("\"$|^\"", "").replace("\\\"", "\"")
    }
  }

  private def createClassSchema(tpe: ru.Type, previousTypes: collection.mutable.Set[String]): SchemaType = {
    val className: String = tpe.typeSymbol.fullName
    if (previousTypes.contains(className)) {
      ClassTypeRef(className)
    } else {
      previousTypes.add(className)

      val params = tpe.typeSymbol.asClass.primaryConstructor.typeSignature.paramLists.head
      val propertiesList: List[Property] = params.map{ paramSymbol =>
        val term = paramSymbol.asTerm
        val termType = createSchema(term.typeSignature, previousTypes)
        val termName: String = term.name.decoded.trim
        Property(termName, termType, descriptionForSymbol(term))
      }.toList.sortBy(_.key)

      ClassType(className, propertiesList, descriptionForSymbol(tpe.typeSymbol))
    }
  }


  def createSchema(tpe: ru.Type, previousTypes: collection.mutable.Set[String] = collection.mutable.Set.empty): SchemaType = {
    val typeName = tpe.typeSymbol.fullName

    if (typeName == "scala.Option") {
      // Option[T] becomes the schema of T with required set to false
      OptionalType(createSchema(tpe.asInstanceOf[ru.TypeRefApi].args.head, previousTypes))
    } else if (tpe.baseClasses.exists(s => s.fullName == "scala.collection.Traversable" ||
      s.fullName == "scala.Array" ||
      s.fullName == "scala.Seq" ||
      s.fullName == "scala.List" ||
      s.fullName == "scala.Vector")) {
      // (Traversable)[T] becomes a schema with items set to the schema of T
      ListType(createSchema(tpe.asInstanceOf[ru.TypeRefApi].args.head, previousTypes))
    } else {
      schemaTypeForScala.getOrElse(typeName, {
        if (tpe.typeSymbol.isClass) {
          if (tpe.typeSymbol.isAbstract) {
            OneOf(findImplementations(tpe, previousTypes))
          } else {
            createClassSchema(tpe, previousTypes)
          }
        } else {
          throw new RuntimeException("What is this type: " + tpe)
        }
      })
    }
  }


  def findImplementations(tpe: ru.Type, previousTypes: collection.mutable.Set[String]): List[SchemaType] = {
    import collection.JavaConverters._
    import reflect.runtime.currentMirror

    val javaClass: Class[_] = Class.forName(tpe.typeSymbol.asClass.fullName)
    val reflections = new Reflections(javaClass.getPackage.getName)

    val implementationClasses = reflections.getSubTypesOf(javaClass).asScala

    implementationClasses.toList.map { klass =>
      createSchema(currentMirror.classSymbol(klass).toType, previousTypes)
    }
  }

  private def toJsonProperties(properties: List[Property]): JValue = {
    def appendDescription(obj: JObject, desc: Option[String]) = desc match {
      case Some(description) => obj.merge(JObject(("description" -> JString(description))))
      case _ => obj
    }

    JObject(properties
      .map { property =>
        (property.key, appendDescription(toJsonSchema(property.tyep).asInstanceOf[JObject], property.description))
      }
    )
  }
  private def toRequiredProperties(properties: List[Property]): Option[(String, JValue)] = {
    val requiredProperties = properties.toList.filter(!_.tyep.isInstanceOf[OptionalType])
    requiredProperties match {
      case Nil => None
      case _ => Some("required", JArray(requiredProperties.map{property => JString(property.key)}))
    }
  }

  def descriptionJson(description: Option[String]): Option[(String, JValue)] = description.map(("description" -> JString(_)))

  def toJsonSchema(t: SchemaType): JValue = t match {
    case DateType() => JObject(("type" -> JString("string")), ("format" -> JString("date")))
    case StringType() => JObject(("type" -> JString("string")))
    case BooleanType() => JObject(("type") -> JString("boolean"))
    case NumberType() => JObject(("type") -> JString("number"))
    case ListType(x) => JObject(("type") -> JString("array"), (("items" -> toJsonSchema(x))))
    case OptionalType(x) => toJsonSchema(x)
    case ClassTypeRef(fullClassName: String) => JObject(("$ref") -> toUri(fullClassName))
    case ClassType(fullClassName, properties, description) => JObject(List(("type" -> JString("object")), ("properties" -> toJsonProperties(properties)), ("id" -> toUri(fullClassName))) ++ descriptionJson(description).toList ++ toRequiredProperties(properties).toList)
    case OneOf(types) => JObject(("oneOf" -> JArray(types.map(toJsonSchema(_)))))
  }

  def toUri(fullClassName: String) = JString("#" + fullClassName.replace(".", "_"))
}