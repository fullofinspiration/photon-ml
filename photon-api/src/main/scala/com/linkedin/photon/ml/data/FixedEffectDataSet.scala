/*
 * Copyright 2017 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.photon.ml.data

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import com.linkedin.photon.ml.data.scoring.CoordinateDataScores
import com.linkedin.photon.ml.spark.RDDLike

/**
 * DataSet implementation for fixed effect data sets.
 *
 * @param labeledPoints The input data
 * @param featureShardId The feature shard id
 */
protected[ml] class FixedEffectDataSet(
    val labeledPoints: RDD[(Long, LabeledPoint)],
    val featureShardId: String)
  extends DataSet[FixedEffectDataSet]
  with RDDLike {

  lazy val numFeatures: Int = labeledPoints.first()._2.features.length

  /**
   * Add scores to data offsets.
   *
   * @param scores The scores used throughout the coordinate descent algorithm
   * @return An updated dataset with scores added to offsets
   */
  override def addScoresToOffsets(scores: CoordinateDataScores): FixedEffectDataSet = {

    val updatedLabeledPoints = labeledPoints
      .leftOuterJoin(scores.scores)
      .mapValues { case (LabeledPoint(label, features, offset, weight), scoredDatumOption) =>
        LabeledPoint(label, features, offset + scoredDatumOption.getOrElse(0.0), weight)
      }

    new FixedEffectDataSet(updatedLabeledPoints, featureShardId)
  }

  /**
   * Get the Spark context.
   *
   * @return The Spark context
   */
  override def sparkContext: SparkContext = labeledPoints.sparkContext

  /**
   * Assign a given name to [[labeledPoints]].
   *
   * @note Not used to reference models in the logic of photon-ml, only used for logging currently.
   *
   * @param name The parent name for all [[RDD]]s in this class
   * @return This object with the name of [[labeledPoints]] assigned
   */
  override def setName(name: String): FixedEffectDataSet = {

    labeledPoints.setName(name)

    this
  }

  /**
   * Set the storage level of [[labeledPoints]], and persist their values across the cluster the first time they are
   * computed.
   *
   * @param storageLevel The storage level
   * @return This object with the storage level of [[labeledPoints]] set
   */
  override def persistRDD(storageLevel: StorageLevel): FixedEffectDataSet = {

    if (!labeledPoints.getStorageLevel.isValid) labeledPoints.persist(storageLevel)

    this
  }

  /**
   * Mark [[labeledPoints]] as non-persistent, and remove all blocks for them from memory and disk.
   *
   * @return This object with [[labeledPoints]] marked non-persistent
   */
  override def unpersistRDD(): FixedEffectDataSet = {

    if (labeledPoints.getStorageLevel.isValid) labeledPoints.unpersist()

    this
  }

  /**
   * Materialize [[labeledPoints]] (Spark [[RDD]]s are lazy evaluated: this method forces them to be evaluated).
   *
   * @return This object with [[labeledPoints]] materialized
   */
  override def materialize(): FixedEffectDataSet = {

    materializeOnce(labeledPoints)

    this
  }

  /**
   * Build a summary string for the dataset.
   *
   * @return A String representation of the dataset
   */
  override def toSummaryString: String = {

    val numSamples = labeledPoints.count()
    val weightSum = labeledPoints.values.map(_.weight).sum()
    val responseSum = labeledPoints.values.map(_.label).sum()
    val featureStats = labeledPoints.values.map(_.features.activeSize).stats()

    s"numSamples: $numSamples\nweightSum: $weightSum\nresponseSum: $responseSum" +
        s"\nnumFeatures: $numFeatures\nfeatureStats: $featureStats"
  }
}

object FixedEffectDataSet {
  /**
   * Build an instance of a fixed effect dataset with the given configuration.
   *
   * @param gameDataSet The input dataset
   * @param fixedEffectDataConfiguration The data configuration object
   * @return A new dataset with given configuration
   */
  protected[ml] def buildWithConfiguration(
      gameDataSet: RDD[(Long, GameDatum)],
      fixedEffectDataConfiguration: FixedEffectDataConfiguration): FixedEffectDataSet = {

    val featureShardId = fixedEffectDataConfiguration.featureShardId
    val labeledPoints = gameDataSet.mapValues(_.generateLabeledPointWithFeatureShardId(featureShardId))
    new FixedEffectDataSet(labeledPoints, featureShardId)
  }
}
