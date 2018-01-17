from pydoc import locate
from mq import *
from sklearn.model_selection import train_test_split


def data_iterator(X, y, batch, epoch):
    eNum = 0
    while eNum < epoch:
        for i in xrange(0, len(X), batch):
            upper = min(batch + i, len(X))
            yield X[i:upper], y[i:upper]
        eNum += 1


class Training(object):
    def __init__(self, host, port, user, password, model_name, metric_name, metric_class, queue_name, job_id, probability=True):
        self.publisher = MqPublisher(host, port, user, password)
        self.model = locate(model_name)()
        self.probability = probability
        self.job_id = job_id
        self.queue_name = queue_name
        self.iter = 0
        self.metric_name = metric_name
        self.metric = locate(metric_class)

    def monitor(self, y_pred, y_true):
        ret = self.metric(y_true, y_pred)
        self.iter += 1
        metrics = {"id": self.job_id,
                  "epoch":self.iter,
                  "metrics": {self.metric_name: ret}}
        self.publisher.publish(self.queue_name, json.dumps(metrics))
        return self.metric_name, ret

    def sklearn_gd_training(self, X, y, batch, epoch, **kwargs):
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        self.model.set_params(**kwargs)
        if self.probability:
            for batch_x, batch_y in data_iterator(X_train, y_train, batch, epoch):
                self.model.partial_fit(batch_x, batch_y)
                y_pred = self.model.predict_proba(X_test)
                self.monitor(y_test, y_pred)
        else:
            for batch_x, batch_y in data_iterator(X_train, y_train, batch, epoch):
                self.model.partial_fit(batch_x, batch_y)
                y_pred = self.model.predict(X_test)
                self.monitor(y_test, y_pred)
        self.model.partial_fit(X_test, y_test)

    def gbdt_training(self, X, y, **kwargs):
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        self.model.set_params(**kwargs)
        self.model.fit(X_train, y_train, eval_set=[(X_test, y_test)], eval_metric=self.monitor)
        self.model.fit(X_test, y_test)


    def get_model(self):
        return self.model














