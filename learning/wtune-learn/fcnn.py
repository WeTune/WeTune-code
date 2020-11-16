#!env python3

import itertools
import numpy as np
import tensorflow as tf
import pandas as pd
import matplotlib.pyplot as plt
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.layers.experimental import preprocessing

MAX_OPS = 5
TEST_MAX_OPS = 5

operations = ['Append', 'Delete', 'Insert', 'Relabel']

feature_columns = ['Subqueries', 'Tables', 'GroupBys', 'OrderBys', 'Distincts']
op_columns = ['Op' + str(i) for i in range(1, MAX_OPS + 1)]
column_names = [
    'CaseIndex', 'Subqueries', 'Tables', 'Predicates', 'GroupBys', 'OrderBys',
    'Distincts', *op_columns
]


def make_labels(features, label):
    return pd.DataFrame(label,
                        index=np.arange(len(features)),
                        columns=['Prob'])


def plot_loss(history):
    plt.plot(history.history['loss'], label='loss')
    plt.plot(history.history['val_loss'], label='val_loss')
    plt.ylim([0, 0.5])
    plt.xlabel('Epoch')
    plt.ylabel('Error')
    plt.legend()
    plt.grid(True)
    plt.show()


def random_sample(base):
    ret = base.copy().append(base, ignore_index=True)
    rand = pd.DataFrame([random_step() for i in range(len(ret))],
                        columns=op_columns)
    ret.loc[:, op_columns] = rand
    return pd.concat([ret, base, base]).drop_duplicates(keep=False)
    #  max_ops = len(base.columns) - len(feature_columns)
    #  rand = base.copy().append(base, ignore_index=True)
    #  for i in range(1, max_ops + 1):
    #  col_name = 'Op' + str(i)
    #  rand[col_name] = pd.DataFrame(
    #  np.random.choice(operations,
    #  size=len(rand)))[rand[col_name].notna()]
    #  return pd.concat([rand, base, base]).drop_duplicates(keep=False)


def df_to_onehot(ops):
    max_ops = len(ops.columns)
    ops = pd.DataFrame(itertools.repeat(operations, len(ops.columns)),
                       index=op_columns[:max_ops]).T.append(ops,
                                                            ignore_index=True)

    ops = pd.get_dummies(ops)
    ops = ops.drop(index=range(0, 4)).reset_index(drop=True)
    return ops


def op_seq_of_len(n):
    return itertools.product(operations, repeat=n)


def op_seq_set(max_length):
    df = pd.get_dummies(
        pd.DataFrame(
            itertools.chain(
                *[op_seq_of_len(n) for n in range(1, max_length + 1)])))
    for i in op_columns[max_length:]:
        for op in operations:
            df[op + '_' + str(i)] = 0

    return np.array(df)


def random_step():
    rand_len = np.random.randint(MAX_OPS) + 1
    return tuple(np.random.choice(operations, rand_len))


def available_step(history):
    ret = []
    for path in history:
        if len(path) >= MAX_OPS: continue
        for op in operations:
            new = (*path, op)
            if new not in history:
                ret.append(new)
    #  return sorted(ret, key=lambda x: -len(x))
    return ret


def step_to_onehot(step):
    idx = np.array([operations.index(op) for op in step])
    return np.concatenate((np.eye(len(operations))[idx.reshape(-1)].flatten(),
                           np.zeros(
                               ((MAX_OPS - len(idx)) * len(operations), ))))


def onehot_to_step(encoded):
    encoded = encoded.reshape((-1, len(operations)))
    return tuple(operations[np.where(arr == 1)[0][0]] for arr in encoded
                 if np.any(arr))


#  seq = sequence_set(5)


class Model:
    def __init__(self):
        self.model = None
        self.raw_dataset = None
        self.features = None
        self.labels = None

    def load_dataset(self):
        dataset = pd.read_csv('data.csv',
                              names=column_names,
                              sep=',',
                              skipinitialspace=True).drop_duplicates()
        self.raw_dataset = dataset.copy()

        dataset = dataset.drop(columns=['CaseIndex'])
        dataset = dataset.drop(columns=['Predicates'])

        #  dataset = dataset.drop(index=dataset[dataset['Op5'].notna()].index)
        #  dataset = dataset.drop(columns=op_columns[4:])

        positive = dataset
        negative = random_sample(dataset)

        dataset = positive.append(negative, ignore_index=True)
        self.feats = dataset[feature_columns].copy()
        self.ops = df_to_onehot(dataset.drop(feature_columns, axis=1))

        positive_labels = make_labels(positive, 1.)
        negative_labels = make_labels(negative, 0.)

        self.labels = positive_labels.append(negative_labels,
                                             ignore_index=True)

        #  print(self.features)
        return self.feats, self.ops, self.labels

    def make_model(self):
        i0 = layers.Input(shape=(len(self.feats.columns), ))
        i1 = layers.Input(shape=(len(self.ops.columns), ))

        n0 = preprocessing.Normalization()
        n1 = preprocessing.Normalization()

        n0.adapt(np.array(self.feats))
        n1.adapt(np.array(self.ops))

        n0 = n0(i0)
        n1 = n1(i1)

        concat = layers.concatenate([n0, n1])

        x0 = layers.Dense(32, activation='sigmoid')(concat)

        out = layers.Dense(1, activation='sigmoid')(x0)

        self.model = keras.models.Model(inputs=[i0, i1], outputs=out)
        self.model.compile(optimizer=tf.optimizers.Adam(learning_rate=0.05),
                           loss='mean_squared_error')
        return self.model

    def train(self):
        if self.model is None:
            self.make_model()

        return self.model.fit(x=[self.feats, self.ops],
                              y=self.labels,
                              epochs=300,
                              verbose=0,
                              validation_split=0.2)

    def save(self):
        self.model.save('model.h5')

    def load(self):
        self.model = tf.keras.models.load_model('model.h5')

    def predict_next(self, row, history):
        next_steps = available_step(history)
        to_predict = np.array([step_to_onehot(s) for s in next_steps])
        probs = self.model(
            [np.tile(row.values, (len(to_predict), 1)), to_predict])
        pred_idx = np.argmax(probs, axis=None)
        return to_predict[pred_idx], next_steps[pred_idx], probs[
            pred_idx].numpy()[0]

    def eval_single(self, row, true):
        true_encoded = true.to_numpy()
        true_decoded = onehot_to_step(true_encoded)
        num_step = 0
        history = {()}
        while True:
            num_step += 1
            next_encoded, next_step, prob = self.predict_next(row, history)
            #  print(true_decoded, next_step, prob)
            if np.array_equal(true_encoded, next_encoded):
                break
            history.add(next_step)
        return num_step

    def eval(self):
        tests = self.feats[self.labels['Prob'] == 1]

        k = 10
        total, succ = 0, 0
        p = []
        rank = []
        for i, row in tests.iterrows():
            true = self.ops.iloc[i]
            if len(true[true == 1]) > TEST_MAX_OPS:
                continue
            total += 1
            num_step = self.eval_single(row, true)
            #  p.append(round(true_p, 5))
            rank.append(num_step)
            if num_step < k:
                succ += 1
            #  break

        #  p = sorted(p, reverse=True)
        rank = sorted(rank)

        print("total: ", total, "succ: ", succ, "%succ: ", succ / total)

        #  print("[prob] ", "avg: ", round(np.mean(p), 2), "\tmin: ", p[-1],
        #  "\tmax: ", p[0], "\tp50: ", p[int(len(p) * 0.5)], "\tp90: ",
        #  p[int(len(p) * 0.9)], "\tp99: ", p[int(len(p) * 0.99)])

        print("[rank] ", "avg: ", round(np.mean(rank),
                                        2), "\tmin: ", rank[0], "\tmax: ",
              rank[-1], "\tp50: ", rank[int(len(rank) * 0.5)], "\tp90: ",
              rank[int(len(rank) * 0.9)], "p95: ", rank[int(len(rank) * 0.95)],
              "\tp99: ", rank[int(len(rank) * 0.99)])

        plt.hist(rank,
                 bins=[*np.arange(0.01, 16.01, 1), 1000],
                 density=True,
                 histtype='step',
                 cumulative=True)
        plt.xlim([0, 15])
        plt.xticks(range(0, 16))
        plt.yticks([*np.arange(0, 1.0, step=0.1), 0.95, 1.0])
        plt.grid()
        plt.show()


if __name__ == '__main__':
    np.random.seed(0)
    tf.random.set_seed(0)

    #  print(onehot_to_step(step_to_onehot(('Delete', 'Delete', 'Insert'))))
    model = Model()
    feats, ops, labels = model.load_dataset()
    m = model.make_model()
    #  print(m.summary())
    model.train()
    #  plot_loss(model.train())
    #  print(seq)
    model.eval()
    #  print(feats)
    #  print(ops)
    #  print(labels)

# vim:sw=4 ts=4
