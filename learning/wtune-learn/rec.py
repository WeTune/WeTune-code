import itertools
import os
import tempfile

import numpy as np
import pandas as pd
import tensorflow as tf
import tensorflow_recommenders as tfrs
import matplotlib.pyplot as plt

from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.layers.experimental import preprocessing

plt.style.use('seaborn-whitegrid')

RND_SEED = 42

INPUT_MAX_OPS = 5
MAX_OPS = 5
TEST_MAX_OPS = 5

OPS = ['Append', 'Delete', 'Insert', 'Relabel']

FEATURE_INPUT_COLS = [
    'Subqueries', 'Tables', 'Predicates', 'GroupBys', 'OrderBys', 'Distincts'
]
OP_INPUT_COLS = ['Idx', *['Op' + str(i) for i in range(1, INPUT_MAX_OPS + 1)]]

FEATURE_COLS = ['Subqueries', 'Tables', 'GroupBys', 'OrderBys', 'Distincts']
OP_COLS = ['Op' + str(i) for i in range(1, MAX_OPS + 1)]


def df_to_onehot(ops):
    ops = pd.DataFrame(itertools.repeat(OPS, MAX_OPS),
                       index=OP_COLS).T.append(ops, ignore_index=True)

    ops = pd.get_dummies(ops)
    ops = ops.drop(index=range(0, len(OPS))).reset_index(drop=True)
    return ops


def op_seq_set():
    return pd.get_dummies(
        pd.DataFrame(itertools.chain(
            *[itertools.product(OPS, repeat=n)
              for n in range(1, MAX_OPS + 1)]),
                     columns=OP_COLS))


def slices(df, labels=None):
    labels = labels or df.columns
    return tf.data.Dataset.from_tensor_slices({l: df[l] for l in labels})


class QueryModel(tf.keras.Model):
    def __init__(self, layer_sizes=(32, )):
        super().__init__()

        self.data_set = None

        self.dense_layers = tf.keras.Sequential()
        for layer_size in layer_sizes[:-1]:
            self.dense_layers.add(layers.Dense(layer_size, activation="relu"))

        # No activation for the last layer.
        for layer_size in layer_sizes[-1:]:
            self.dense_layers.add(tf.keras.layers.Dense(layer_size))

    def data(self):
        if self.data_set is not None:
            return self.data_set

        feature_set = pd.read_csv(
            'features.csv',
            names=FEATURE_INPUT_COLS,
            sep=',',
            skipinitialspace=True
        ).drop(
            columns=[c for c in FEATURE_INPUT_COLS if c not in FEATURE_COLS])

        self.normal_layer = preprocessing.Normalization()
        self.normal_layer.adapt(feature_set.to_numpy())

        op_set = pd.read_csv(
            'operations.csv',
            names=OP_INPUT_COLS,
            sep=',',
            skipinitialspace=True).drop(
                columns=[c for c in OP_INPUT_COLS if c not in OP_COLS])
        op_set = df_to_onehot(op_set)

        return pd.concat([feature_set, op_set], axis=1)

    def call(self, inputs):
        return self.dense_layers(self.normal_layer([inputs]))


class CandidateModel(tf.keras.Model):
    def __init__(self, layer_sizes=(32, )):
        super().__init__()

        self.op_set = None

        self.dense_layers = tf.keras.Sequential()
        self.dense_layers.add(layers.InputLayer(input_shape=(20, )))

        for layer_size in layer_sizes[:-1]:
            self.dense_layers.add(layers.Dense(layer_size, activation="relu"))

        # No activation for the last layer.
        for layer_size in layer_sizes[-1:]:
            self.dense_layers.add(tf.keras.layers.Dense(layer_size))

    def data(self):
        if self.op_set is None:
            self.op_set = op_seq_set()
        return self.op_set

    def call(self, inputs):
        #  return self.dense_layers(inputs)
        if not tf.is_tensor(inputs):
            inputs = tf.convert_to_tensor(inputs)
        return self.dense_layers(tf.reshape(inputs, [1, 20]))


class RecommendModel(tfrs.models.Model):
    def __init__(self, query_model=None, candidate_model=None):
        super().__init__()

        self.task = None

        self.query_model = query_model or QueryModel()
        self.candidate_model = candidate_model or CandidateModel()

    def data(self):
        train_set = self.query_model.data().copy()
        feature_set = train_set[FEATURE_COLS]
        op_set = train_set.drop(columns=FEATURE_COLS)
        self.train_set = tf.data.Dataset.from_tensor_slices({
            'features':
            feature_set,
            'ops':
            op_set.to_numpy().astype('float32')
        })

        candidate_set = self.candidate_model.data().to_numpy().astype(
            'float32')
        self.candidate_set = tf.data.Dataset.from_tensor_slices(candidate_set)

        return self.train_set

    def compute_loss(self, features, training=False):
        if self.task is None:
            self.task = tfrs.tasks.Retrieval(
                metrics=tfrs.metrics.FactorizedTopK(
                    candidates=self.candidate_set.map(self.candidate_model)))

        query_embeddings = self.query_model(features['features'])
        candidates_embeddings = self.candidate_model(features['ops'])

        return self.task(query_embeddings,
                         candidates_embeddings,
                         compute_metrics=not training)

    def do_train():
        queries = self.query_model.data()
        candidates = self.candidate_model.data()


if __name__ == '__main__':
    r = RecommendModel()
    r.compile(optimizer=tf.keras.optimizers.Adagrad(0.1))
    history = r.fit(r.data(), epochs=100, verbose=0)
    pass
# vim:sw=4 ts=4
