/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.plan.nodes

import org.apache.flink.api.common.functions.Function
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.table.api.TableConfig
import org.apache.flink.table.codegen.{FunctionCodeGenerator, GeneratedFunction}
import org.apache.flink.types.Row

/**
  * Common class for batch and stream scans.
  */
trait CommonScan[T] {

  /**
    * We check if the input type is exactly the same as the internal row type.
    * A conversion is necessary if types differ.
    */
  private[flink] def needsConversion(
      externalTypeInfo: TypeInformation[Any],
      internalTypeInfo: TypeInformation[T]): Boolean =
    externalTypeInfo != internalTypeInfo

  private[flink] def generatedConversionFunction[F <: Function](
      config: TableConfig,
      functionClass: Class[F],
      inputType: TypeInformation[Any],
      expectedType: TypeInformation[Row],
      conversionOperatorName: String,
      fieldNames: Seq[String],
      inputFieldMapping: Option[Array[Int]] = None)
    : GeneratedFunction[F, Row] = {

    val generator = new FunctionCodeGenerator(
      config,
      false,
      inputType,
      None,
      inputFieldMapping)
    val conversion = generator.generateConverterResultExpression(expectedType, fieldNames)

    val body =
      s"""
         |${conversion.code}
         |return ${conversion.resultTerm};
         |""".stripMargin

    generator.generateFunction(
      conversionOperatorName,
      functionClass,
      body,
      expectedType)
  }

}
